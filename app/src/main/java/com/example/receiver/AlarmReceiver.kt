package com.example.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity

class AlarmReceiver : BroadcastReceiver() {
    companion object {
        const val CHANNEL_ID = "rfx_journal_notifications"
        const val CHANNEL_NAME = "Pengingat RFX Journal"
        const val NOTIFICATION_ID_BASE = 2000
    }

    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "Target Proyek Selesai!"
        val description = intent.getStringExtra("description") ?: "Periksa jadwal mingguan Anda sekarang."
        val id = intent.getLongExtra("id", System.currentTimeMillis())

        Log.d("AlarmReceiver", "Menerima alarm untuk: $title - $description")

        showNotification(context, title, description, id.toInt())
    }

    private fun showNotification(context: Context, title: String, description: String, id: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Buat Notification Channel untuk Android Oreo (API 26) ke atas
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                this.description = "Notifikasi pengingat untuk target dan jadwal mingguan"
                enableLights(true)
                lightColor = 0xFFDC2626.toInt() // Red 600 glow
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Ketika notifikasi diklik, buka MainActivity
        val clickIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            id,
            clickIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Standard system fallback icon
            .setContentTitle(title)
            .setContentText(description)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setColor(0xFFDC2626.toInt()) // Red 600 accent
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID_BASE + id, notification)
    }
}
