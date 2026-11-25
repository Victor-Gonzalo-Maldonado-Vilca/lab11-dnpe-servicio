package com.example.lab11servicio

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Sesión de escucha de Nocturne: servicio en primer plano (tipo mediaPlayback) que
 * reproduce el audio, cuenta el tiempo transcurrido y actualiza cada segundo la
 * notificación persistente. Se detiene desde la app, desde la acción de la notificación
 * o al vencer el temporizador de sueño.
 */
class SesionEscuchaService : Service() {

    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var temporizador: Job? = null
    private var reproductor: MediaPlayer? = null
    private var inicio = 0L
    private var limiteSeg: Long? = null

    override fun onBind(intent: Intent?): IBinder? = null   // servicio iniciado, no vinculado

    override fun onCreate() {
        super.onCreate()
        NotificacionSesion.crearCanal(this)
        Log.i(TAG, "onCreate")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_DETENER -> {
                Log.i(TAG, "DETENER solicitado (origen=${intent.getStringExtra(EXTRA_ORIGEN) ?: "notificacion"})")
                detener()
            }
            else -> iniciar(intent?.getLongExtra(EXTRA_LIMITE_SEG, 0L)?.takeIf { it > 0 })
        }
        // Si el sistema mata el proceso, no se recrea solo: la sesión la inicia el usuario.
        return START_NOT_STICKY
    }

    private fun iniciar(limite: Long?) {
        if (temporizador?.isActive == true) {
            Log.i(TAG, "INICIAR ignorado: la sesión ya está activa")
            return
        }
        limiteSeg = limite
        inicio = SystemClock.elapsedRealtime()
        val primera = EstadoSesion.Sesion(activa = true, limiteSeg = limite, pista = Catalogo.pistaEn(0))
        EstadoSesion.actualizar(primera)

        // Debe llamarse pocos segundos después de startForegroundService(); si no, ANR.
        ServiceCompat.startForeground(
            this,
            NotificacionSesion.NOTIFICACION_ID,
            NotificacionSesion.construir(this, primera),
            // El tipo de servicio existe desde Android 10 (API 29); antes se omite.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0,
        )
        Log.i(TAG, "startForeground id=${NotificacionSesion.NOTIFICACION_ID} tipo=mediaPlayback limite=${limite ?: "sin limite"}")
        iniciarAudio()

        val gestor = getSystemService(NotificationManager::class.java)
        temporizador = alcance.launch {
            var tick = 0
            while (isActive) {
                // Espera hasta el próximo segundo exacto: el tiempo se calcula con el reloj
                // monotónico, así que los retrasos de un tick no se acumulan.
                val ms = SystemClock.elapsedRealtime() - inicio
                delay(1000 - ms % 1000)
                val seg = (SystemClock.elapsedRealtime() - inicio) / 1000
                tick++
                val sesion = EstadoSesion.Sesion(
                    activa = true,
                    transcurridoSeg = seg,
                    limiteSeg = limiteSeg,
                    pista = Catalogo.pistaEn(seg),
                    actualizaciones = tick,
                    audio = reproductor?.isPlaying == true,
                )
                EstadoSesion.actualizar(sesion)
                gestor.notify(NotificacionSesion.NOTIFICACION_ID, NotificacionSesion.construir(this@SesionEscuchaService, sesion))
                Log.d(TAG, "TICK n=$tick t=${formatearTiempo(seg)} app=${if (EstadoSesion.appVisible) "visible" else "segundo_plano"} audio=${sesion.audio}")

                if (limiteSeg != null && seg >= limiteSeg!!) {
                    Log.i(TAG, "Temporizador de sueño cumplido (${formatearTiempo(seg)})")
                    detener()
                }
            }
        }
    }

    private fun iniciarAudio() {
        try {
            reproductor = MediaPlayer.create(
                this, R.raw.ambiente_nocturne,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
                0,
            ).apply {
                isLooping = true
                setVolume(0.4f, 0.4f)
                start()
            }
            Log.i(TAG, "Audio iniciado (MediaPlayer, bucle)")
        } catch (e: Exception) {
            // Sin audio la sesión y el temporizador siguen funcionando.
            Log.w(TAG, "No se pudo iniciar el audio: ${e.message}")
        }
    }

    private fun detener() {
        temporizador?.cancel()
        temporizador = null
        reproductor?.run { stop(); release() }
        reproductor = null
        val final = EstadoSesion.sesion.value
        EstadoSesion.actualizar(EstadoSesion.Sesion(activa = false, transcurridoSeg = final.transcurridoSeg, actualizaciones = final.actualizaciones))
        // Quita la notificación y sale del primer plano; luego el servicio se destruye.
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
        Log.i(TAG, "stopForeground(REMOVE) + stopSelf: duracion=${formatearTiempo(final.transcurridoSeg)} actualizaciones=${final.actualizaciones}")
    }

    override fun onDestroy() {
        temporizador?.cancel()
        reproductor?.release()
        alcance.cancel()
        if (EstadoSesion.sesion.value.activa) {
            EstadoSesion.actualizar(EstadoSesion.sesion.value.copy(activa = false))
        }
        Log.i(TAG, "onDestroy")
        super.onDestroy()
    }

    companion object {
        const val TAG = "NocturneSesion"
        const val ACTION_DETENER = "com.example.lab11servicio.DETENER"
        const val EXTRA_LIMITE_SEG = "limiteSeg"
        const val EXTRA_ORIGEN = "origen"

        /** Inicia la sesión; [limiteSeg] activa el temporizador de sueño. */
        fun iniciar(contexto: Context, limiteSeg: Long? = null) {
            val intent = Intent(contexto, SesionEscuchaService::class.java)
                .putExtra(EXTRA_LIMITE_SEG, limiteSeg ?: 0L)
            ContextCompat.startForegroundService(contexto, intent)
            Log.i(TAG, "UI -> startForegroundService (limite=${limiteSeg ?: "sin limite"})")
        }

        fun detener(contexto: Context) {
            contexto.startService(
                Intent(contexto, SesionEscuchaService::class.java)
                    .setAction(ACTION_DETENER)
                    .putExtra(EXTRA_ORIGEN, "app"),
            )
        }
    }
}
