package com.example.lab11servicio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Pruebas locales (JVM) del formato del tiempo y del estado de la sesión. */
class FormatoTiempoTest {

    @Test
    fun formatea_minutosYSegundos() {
        assertEquals("00:00", formatearTiempo(0))
        assertEquals("00:07", formatearTiempo(7))
        assertEquals("01:15", formatearTiempo(75))
        assertEquals("59:59", formatearTiempo(3599))
    }

    @Test
    fun formatea_horas_yNoAceptaNegativos() {
        assertEquals("1:00:00", formatearTiempo(3600))
        assertEquals("1:02:05", formatearTiempo(3725))
        assertEquals("00:00", formatearTiempo(-5))
    }

    @Test
    fun restante_delTemporizadorDeSueno() {
        assertNull(EstadoSesion.Sesion(transcurridoSeg = 10).restanteSeg)
        assertEquals(50L, EstadoSesion.Sesion(transcurridoSeg = 10, limiteSeg = 60).restanteSeg)
        assertEquals(0L, EstadoSesion.Sesion(transcurridoSeg = 70, limiteSeg = 60).restanteSeg)
    }

    @Test
    fun pista_cambiaCada30Segundos() {
        assertEquals(Catalogo.pistas[0], Catalogo.pistaEn(0))
        assertEquals(Catalogo.pistas[0], Catalogo.pistaEn(29))
        assertEquals(Catalogo.pistas[1], Catalogo.pistaEn(30))
        assertEquals(Catalogo.pistas[0], Catalogo.pistaEn(30L * Catalogo.pistas.size))
    }
}
