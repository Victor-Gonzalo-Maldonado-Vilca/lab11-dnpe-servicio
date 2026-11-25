package com.example.lab11servicio

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.os.SystemClock
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Pruebas del servicio real en el dispositivo: se inicia, se consulta la notificación
 * publicada (NotificationManager.activeNotifications) y se detiene.
 */
@RunWith(AndroidJUnit4::class)
class SesionEscuchaServiceTest {

    private val contexto: Context = ApplicationProvider.getApplicationContext()
    private val gestor = contexto.getSystemService(NotificationManager::class.java)

    @Before
    fun concederPermiso() {
        InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand("pm grant ${contexto.packageName} android.permission.POST_NOTIFICATIONS").close()
        // El estado es un objeto compartido: cada prueba parte de una sesión limpia.
        EstadoSesion.actualizar(EstadoSesion.Sesion())
    }

    @After
    fun detenerSiQuedoActivo() {
        SesionEscuchaService.detener(contexto)
        esperarHasta(3_000) { !EstadoSesion.sesion.value.activa }
    }

    private fun notificacion(): Notification? =
        gestor.activeNotifications.firstOrNull { it.id == NotificacionSesion.NOTIFICACION_ID }?.notification

    private fun titulo(): String? = notificacion()?.extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString()

    private fun esperarHasta(maxMs: Long, condicion: () -> Boolean): Boolean {
        val fin = SystemClock.elapsedRealtime() + maxMs
        while (SystemClock.elapsedRealtime() < fin) {
            if (condicion()) return true
            Thread.sleep(50)
        }
        return condicion()
    }

    @Test
    fun iniciar_publicaNotificacionPersistenteDeServicioEnPrimerPlano() {
        SesionEscuchaService.iniciar(contexto)

        assertTrue(esperarHasta(3_000) { notificacion() != null })
        val n = notificacion()!!
        assertTrue("debe ser ongoing", n.flags and Notification.FLAG_ONGOING_EVENT != 0)
        assertTrue("debe pertenecer a un FGS", n.flags and Notification.FLAG_FOREGROUND_SERVICE != 0)
        assertEquals(NotificacionSesion.CANAL_ID, n.channelId)
        assertTrue(titulo()!!.startsWith("Sesión de escucha · "))
        assertEquals("Detener", n.actions.single().title.toString())
        assertTrue(EstadoSesion.sesion.value.activa)
    }

    @Test
    fun notificacion_actualizaElTiempoTranscurrido() {
        SesionEscuchaService.iniciar(contexto)

        assertTrue(esperarHasta(4_000) { titulo() == "Sesión de escucha · 00:02" })
        val antes = titulo()
        assertTrue(esperarHasta(3_000) { titulo() == "Sesión de escucha · 00:04" })
        assertNotEquals(antes, titulo())
        assertTrue(EstadoSesion.sesion.value.actualizaciones >= 4)
    }

    @Test
    fun detener_retiraLaNotificacionYCesanLasActualizaciones() {
        SesionEscuchaService.iniciar(contexto)
        assertTrue(esperarHasta(4_000) { EstadoSesion.sesion.value.run { activa && actualizaciones >= 2 } })

        SesionEscuchaService.detener(contexto)

        assertTrue(esperarHasta(3_000) { notificacion() == null })
        assertFalse(EstadoSesion.sesion.value.activa)
        val contador = EstadoSesion.sesion.value.actualizaciones
        Thread.sleep(2_500)                                    // más de dos ticks
        assertEquals(contador, EstadoSesion.sesion.value.actualizaciones)
        assertNull(notificacion())
    }

    @Test
    fun temporizadorDeSueno_detieneElServicioAlVencer() {
        SesionEscuchaService.iniciar(contexto, limiteSeg = 3)
        assertTrue(esperarHasta(3_000) { notificacion() != null })
        assertNotNull(EstadoSesion.sesion.value.limiteSeg)

        assertTrue(esperarHasta(6_000) { !EstadoSesion.sesion.value.activa && notificacion() == null })
        assertEquals(3L, EstadoSesion.sesion.value.transcurridoSeg)
    }
}
