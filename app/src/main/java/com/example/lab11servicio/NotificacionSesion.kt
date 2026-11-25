package com.example.lab11servicio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

/** Canal y notificación persistente de la sesión de escucha. */
object NotificacionSesion {
    const val CANAL_ID = "sesion_escucha"
    const val NOTIFICACION_ID = 1101

    /** El canal solo existe desde Android 8 (API 26); antes no hace falta crearlo. */
    fun crearCanal(contexto: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val canal = NotificationChannel(
            CANAL_ID,
            "Sesión de escucha",
            NotificationManager.IMPORTANCE_LOW,      // sin sonido ni ventana emergente en cada actualización
        ).apply {
            description = "Muestra el tiempo de la sesión de escucha de Nocturne mientras está activa."
            setShowBadge(false)
        }
        contexto.getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
    }

    fun construir(contexto: Context, sesion: EstadoSesion.Sesion): Notification {
        // Tocar la notificación abre la app.
        val abrir = PendingIntent.getActivity(
            contexto, 0,
            Intent(contexto, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        // Acción "Detener": envía ACTION_DETENER al propio servicio.
        val detener = PendingIntent.getService(
            contexto, 1,
            Intent(contexto, SesionEscuchaService::class.java).setAction(SesionEscuchaService.ACTION_DETENER),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val tiempo = formatearTiempo(sesion.transcurridoSeg)
        val detalle = sesion.restanteSeg?.let { " · se detiene en ${formatearTiempo(it)}" } ?: ""

        return NotificationCompat.Builder(contexto, CANAL_ID)
            .setSmallIcon(R.drawable.ic_notificacion)
            .setContentTitle("Sesión de escucha · $tiempo")
            .setContentText(sesion.pista + detalle)
            .setSubText("Nocturne")
            .setColor(0xFF8B5CF6.toInt())
            .setOngoing(true)                         // persistente mientras el servicio está activo
            .setOnlyAlertOnce(true)                   // las actualizaciones no vuelven a sonar ni vibrar
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(abrir)
            .addAction(R.drawable.ic_detener, "Detener", detener)
            .build()
    }
}
