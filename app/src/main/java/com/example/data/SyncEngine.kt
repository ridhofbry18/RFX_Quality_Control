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

    private fun toHranaValue(value: Any?): JSONObject {
        val json = JSONObject()
        if (value == null) {
            json.put("type", "null")
        } else {
            when (value) {
                is Long, is Int, is Short, is Byte -> {
                    json.put("type", "integer")
                    json.put("value", value.toString())
                }
                is Double, is Float -> {
                    json.put("type", "float")
                    json.put("value", value.toDouble())
                }
                is Boolean -> {
                    json.put("type", "integer")
                    json.put("value", if (value) "1" else "0")
                }
                else -> {
                    json.put("type", "text")
                    json.put("value", value.toString())
                }
            }
        }
        return json
    }

    private suspend fun getPipelineEndpoint(dbUrl: String): String? {
        var httpUrl = dbUrl.trim()
        if (httpUrl.isEmpty() || httpUrl.contains("your-turso-database-url")) return null
        if (httpUrl.startsWith("libsql://")) {
            httpUrl = httpUrl.replace("libsql://", "https://")
        } else if (!httpUrl.startsWith("http://") && !httpUrl.startsWith("https://")) {
            httpUrl = "https://$httpUrl"
        }
        val cleanUrl = if (httpUrl.endsWith("/")) httpUrl else "$httpUrl/"
        return "${cleanUrl}v2/pipeline"
    }

    /**
     * Mensinkronkan data Catatan Harian (DiaryEntry) ke database Turso.
     * Menggunakan Turso SQL REST API (libSQL pipeline / execute endpoint).
     */
    suspend fun syncDiaryToTurso(context: Context, entry: DiaryEntry): Boolean = withContext(Dispatchers.IO) {
        val dbUrl = getPreference(context, "turso_db_url", BuildConfig.TURSO_DB_URL)
        val token = getPreference(context, "turso_token", BuildConfig.TURSO_AUTH_TOKEN)
        val endpoint = getPipelineEndpoint(dbUrl)

        if (endpoint == null || token.isEmpty() || token.contains("your_turso_token")) {
            Log.w(TAG, "Turso DB Url atau Token belum diset dengan benar. Sinkronisasi disimulasikan.")
            return@withContext true
        }

        try {
            val requestBodyJson = JSONObject().apply {
                put("requests", JSONArray().apply {
                    put(JSONObject().apply {
                        put("type", "execute")
                        put("stmt", JSONObject().apply {
                            put("sql", "CREATE TABLE IF NOT EXISTS diary_entries (id INTEGER PRIMARY KEY, title TEXT, content TEXT, date INTEGER, imageUri TEXT, mood TEXT)")
                        })
                    })
                    put(JSONObject().apply {
                        put("type", "execute")
                        put("stmt", JSONObject().apply {
                            put("sql", "INSERT OR REPLACE INTO diary_entries (id, title, content, date, imageUri, mood) VALUES (?, ?, ?, ?, ?, ?)")
                            put("args", JSONArray().apply {
                                put(toHranaValue(entry.id))
                                put(toHranaValue(entry.title))
                                put(toHranaValue(entry.content))
                                put(toHranaValue(entry.date))
                                put(toHranaValue(entry.imageUri))
                                put(toHranaValue(entry.mood))
                            })
                        })
                    })
                    put(JSONObject().apply { put("type", "close") })
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
                    Log.e(TAG, "Gagal mensinkronkan ke Turso. Kode: ${response.code}, Response: ${response.body?.string()}")
                    return@withContext false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gagal terhubung ke Turso Cloud", e)
            return@withContext false
        }
    }

    /**
     * Mensinkronkan data peliharaan virtual ke database Turso.
     */
    suspend fun syncPetToTurso(
        context: Context,
        stage: Int,
        hunger: Int,
        cleanliness: Int,
        happiness: Int,
        energy: Int,
        hasPoop: Boolean,
        playCount: Int
    ): Boolean = withContext(Dispatchers.IO) {
        val dbUrl = getPreference(context, "turso_db_url", BuildConfig.TURSO_DB_URL)
        val token = getPreference(context, "turso_token", BuildConfig.TURSO_AUTH_TOKEN)
        val endpoint = getPipelineEndpoint(dbUrl)

        if (endpoint == null || token.isEmpty() || token.contains("your_turso_token")) {
            return@withContext true
        }

        try {
            val requestBodyJson = JSONObject().apply {
                put("requests", JSONArray().apply {
                    put(JSONObject().apply {
                        put("type", "execute")
                        put("stmt", JSONObject().apply {
                            put("sql", "CREATE TABLE IF NOT EXISTS pet_status (id INTEGER PRIMARY KEY, stage INTEGER, hunger INTEGER, cleanliness INTEGER, happiness INTEGER, energy INTEGER, hasPoop INTEGER, playCount INTEGER)")
                        })
                    })
                    put(JSONObject().apply {
                        put("type", "execute")
                        put("stmt", JSONObject().apply {
                            put("sql", "INSERT OR REPLACE INTO pet_status (id, stage, hunger, cleanliness, happiness, energy, hasPoop, playCount) VALUES (1, ?, ?, ?, ?, ?, ?, ?)")
                            put("args", JSONArray().apply {
                                put(toHranaValue(stage))
                                put(toHranaValue(hunger))
                                put(toHranaValue(cleanliness))
                                put(toHranaValue(happiness))
                                put(toHranaValue(energy))
                                put(toHranaValue(if (hasPoop) 1 else 0))
                                put(toHranaValue(playCount))
                            })
                        })
                    })
                    put(JSONObject().apply { put("type", "close") })
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
                    Log.d(TAG, "Sinkronisasi Pet ke Turso berhasil")
                    return@withContext true
                } else {
                    Log.e(TAG, "Gagal mensinkronkan Pet ke Turso. Kode: ${response.code}")
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
