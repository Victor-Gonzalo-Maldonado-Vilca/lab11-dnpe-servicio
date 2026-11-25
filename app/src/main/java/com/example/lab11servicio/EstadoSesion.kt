package com.example.lab11servicio

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Estado de la sesión de escucha compartido entre el servicio (que lo escribe)
 * y la interfaz (que lo observa). Vive en el proceso de la app.
 */
object EstadoSesion {

    data class Sesion(
        val activa: Boolean = false,
        val transcurridoSeg: Long = 0,
        val limiteSeg: Long? = null,           // temporizador de sueño (null = sin límite)
        val pista: String = "",
        val actualizaciones: Int = 0,          // veces que se actualizó la notificación
        val audio: Boolean = false,            // MediaPlayer reproduciendo
    ) {
        val restanteSeg: Long? get() = limiteSeg?.let { (it - transcurridoSeg).coerceAtLeast(0) }
    }

    private val _sesion = MutableStateFlow(Sesion())
    val sesion: StateFlow<Sesion> = _sesion.asStateFlow()

    /** true mientras la Activity está visible (onStart..onStop). Se registra en cada tick. */
    @Volatile
    var appVisible: Boolean = false

    internal fun actualizar(nueva: Sesion) {
        _sesion.value = nueva
    }
}

/** 75 -> "01:15"; 3725 -> "1:02:05". */
fun formatearTiempo(segundos: Long): String {
    val s = segundos.coerceAtLeast(0)
    val h = s / 3600
    val m = (s % 3600) / 60
    val r = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, r) else "%02d:%02d".format(m, r)
}

/** Catálogo local de Nocturne: la notificación muestra la pista en curso (cambia cada 30 s). */
object Catalogo {
    val pistas = listOf(
        "Luz de medianoche · Aurora Sur",
        "Ciudad dormida · Los Faroles",
        "Marea lenta · Puerto Azul",
        "Brisa del Misti · Andes Lo-Fi",
        "Constelaciones · Aurora Sur",
    )

    fun pistaEn(segundo: Long): String = pistas[((segundo / 30) % pistas.size).toInt()]
}
