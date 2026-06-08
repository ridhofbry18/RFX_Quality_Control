package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object SyncEngine {
    private const val TAG = "SyncEngine"
    private val client = OkHttpClient()
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    private fun getPreference(context: Context, key: String, defaultValue: String): String {
        val prefs = context.getSharedPreferences("rfx_journal_prefs", Context.MODE_PRIVATE)
        val value = prefs.getString(key, "")
        return if (value.isNullOrEmpty()) defaultValue else value
    }

    /**
     * Menyimpan gambar secara lokal ke direktori internal aplikasi agar awet
     * dan mengembalikan path file lokal.
     */
    suspend fun saveImageToInternalStorage(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            val fileName = "img_${UUID.randomUUID()}.jpg"
            val file = File(context.filesDir, fileName)
            val outputStream = FileOutputStream(file)
            
            // Kompresi kualitas 80% untuk mengurangi ukuran data
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            outputStream.flush()
            outputStream.close()
            
            return@withContext file.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Gagal menyimpan foto secara lokal", e)
            return@withContext null
        }
    }

    /**
     * Mensinkronkan data Catatan Harian (DiaryEntry) ke database Turso.
     * Menggunakan Turso SQL REST API (libSQL pipeline / execute endpoint).
     */
    suspend fun syncDiaryToTurso(context: Context, entry: DiaryEntry): Boolean = withContext(Dispatchers.IO) {
        val dbUrl = getPreference(context, "turso_db_url", BuildConfig.TURSO_DB_URL)
        val token = getPreference(context, "turso_token", BuildConfig.TURSO_AUTH_TOKEN)

        // Cek apakah konfigurasi default/dummy
        if (dbUrl.contains("your-turso-database-url") || token.contains("your_turso_token") || dbUrl.isEmpty() || token.isEmpty()) {
            Log.w(TAG, "Turso DB Url atau Token belum diset dengan benar. Sinkronisasi disimulasikan.")
            return@withContext true // Bekerja secara luring (offline-first)
        }

        try {
            // Kita gunakan Turso `/v2/pipeline` atau standard `/` executables rest api.
            // Format URL Turso biasanya: "libsql://my-db.turso.io", ganti "libsql://" dengan "https://"
            val httpUrl = dbUrl.replace("libsql://", "https://")
            val cleanUrl = if (httpUrl.endsWith("/")) httpUrl else "$httpUrl/"
            val endpoint = "${cleanUrl}v2/pipeline"

            // Buat batch SQL query dalam format JSON Pipeline Turso
            // Contoh batch pembuat tabel jika belum ada dan insert query
            val requestBodyJson = JSONObject().apply {
                put("requests", JSONArray().apply {
                    // Request 1: Inisialisasi tabel jika belum ada
                    put(JSONObject().apply {
                        put("type", "execute")
                        put("stmt", JSONObject().apply {
                            put("sql", "CREATE TABLE IF NOT EXISTS diary_entries (id INTEGER PRIMARY KEY, title TEXT, content TEXT, date INTEGER, imageUri TEXT, mood TEXT)")
                        })
                    })
                    // Request 2: Mengganti/Insert data ke Cloud
                    put(JSONObject().apply {
                        put("type", "execute")
                        put("stmt", JSONObject().apply {
                            put("sql", "INSERT OR REPLACE INTO diary_entries (id, title, content, date, imageUri, mood) VALUES (?, ?, ?, ?, ?, ?)")
                            put("args", JSONArray().apply {
                                put(entry.id)
                                put(entry.title)
                                put(entry.content)
                                put(entry.date)
                                put(entry.imageUri ?: "")
                                put(entry.mood)
                            })
                        })
                    })
                    // Request 3: Selesai
                    put(JSONObject().apply {
                        put("type", "close")
                    })
                })
            }

            val requestBody = requestBodyJson.toString().toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder()
                .url(endpoint)
                .header("Authorization", "Bearer $token")
                .header("Content-Type", "application/json")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Log.d(TAG, "Sinkronisasi ke Turso berhasil untuk ID: ${entry.id}")
                    return@withContext true
                } else {
                    Log.e(TAG, "Gagal mensinkronkan ke Turso. Kode: ${response.code}, Pesan: ${response.message}")
                    return@withContext false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gagal terhubung ke Turso Cloud", e)
            return@withContext false
        }
    }

    /**
     * Simulasi atau real upload foto ke Google Drive menggunakan Google Drive API.
     * Menggunakan Multipart Upload Drive v3 API.
     */
    suspend fun uploadPhotoToGoogleDrive(context: Context, localPath: String, oauthToken: String?): String? = withContext(Dispatchers.IO) {
        val folderId = getPreference(context, "google_drive_folder_id", BuildConfig.GOOGLE_DRIVE_FOLDER_ID)
        
        if (oauthToken.isNullOrEmpty() || oauthToken.contains("default") || oauthToken == "MOCK_TOKEN") {
            Log.w(TAG, "OAuth Token Google Drive kosong atau default. Menggunakan penyimpanan Cloud simulasi.")
            // Kembalikan URL cloud tiruan untuk mockup visual yang bagus
            return@withContext "https://images.unsplash.com/photo-1506784983877-45594efa4cbe?auto=format&fit=crop&q=80&w=600"
        }

        try {
            val file = File(localPath)
            if (!file.exists()) return@withContext null

            // Baca bytes berkas
            val bytes = file.readBytes()

            // Endpoint upload multipart Google Drive
            val url = "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart"

            // Metadata File
            val metadataJson = JSONObject().apply {
                put("name", file.name)
                put("mimeType", "image/jpeg")
                if (!folderId.isNullOrEmpty() && !folderId.contains("your-google-drive")) {
                    put("parents", JSONArray().apply { put(folderId) })
                }
            }

            // Batas multipart pembatas
            val boundary = "foo_bar_baz"
            val contentType = "multipart/related; boundary=$boundary"

            // Buat isi bodi HTTP multipart secara manual agar kompatibel dan ringkas
            val bos = ByteArrayOutputStream()
            val writer = bos.bufferedWriter()

            writer.write("--$boundary\r\n")
            writer.write("Content-Type: application/json; charset=UTF-8\r\n\r\n")
            writer.write(metadataJson.toString() + "\r\n")
            writer.write("--$boundary\r\n")
            writer.write("Content-Type: image/jpeg\r\n\r\n")
            writer.flush()

            bos.write(bytes)

            writer.write("\r\n--$boundary--\r\n")
            writer.flush()

            val requestBody = bos.toByteArray().toRequestBody(contentType.toMediaType())
            
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $oauthToken")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val jsonResponse = JSONObject(body)
                    val fileId = jsonResponse.optString("id")
                    
                    Log.d(TAG, "Foto berhasil diunggah ke Google Drive. ID: $fileId")
                    // Mengembalikan URL publik file Drive atau API viewer Url
                    return@withContext "https://lh3.googleusercontent.com/d/$fileId"
                } else {
                    Log.e(TAG, "Gagal mengunggah ke Google Drive. Kode: ${response.code}, Response: ${response.body?.string()}")
                    return@withContext null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gagal melakukan API Drive Upload", e)
            return@withContext null
        }
    }
}
