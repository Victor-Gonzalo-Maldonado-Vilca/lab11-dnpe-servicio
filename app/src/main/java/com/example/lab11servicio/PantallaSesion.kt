package com.example.lab11servicio

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lab11servicio.ui.theme.NocturneCian
import com.example.lab11servicio.ui.theme.NocturneFondo
import com.example.lab11servicio.ui.theme.NocturneMorado
import com.example.lab11servicio.ui.theme.NocturneSuperficie
import com.example.lab11servicio.ui.theme.NocturneTexto
import com.example.lab11servicio.ui.theme.NocturneTextoSuave

/** Opciones del temporizador de sueño (segundos; null = sin límite). */
private val OPCIONES = listOf<Pair<String, Long?>>("Sin límite" to null, "1 min" to 60L, "15 min" to 900L, "30 min" to 1800L)

/**
 * Pantalla "Sesión de escucha": inicia y detiene el servicio en primer plano y
 * muestra el mismo tiempo que la notificación, observando EstadoSesion.
 */
@Composable
fun PantallaSesion() {
    val contexto = LocalContext.current
    val sesion by EstadoSesion.sesion.collectAsStateWithLifecycle()
    var limite by rememberSaveable { mutableStateOf<Long?>(null) }

    fun tienePermiso() = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(contexto, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    var permiso by remember { mutableStateOf(tienePermiso()) }

    // Android 13+: se pide el permiso antes de iniciar; si se niega, el servicio igual
    // funciona, pero la notificación solo aparece en el administrador de tareas.
    val solicitar = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        permiso = concedido
        Log.i(SesionEscuchaService.TAG, "Permiso POST_NOTIFICATIONS concedido=$concedido")
        SesionEscuchaService.iniciar(contexto, limite)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF231C45), NocturneFondo)))
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("NOCTURNE · SESIÓN", color = NocturneTextoSuave, fontSize = 13.sp, letterSpacing = 2.sp)
        Text(
            "Sesión de escucha",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Reloj(sesion)

        Text(
            if (sesion.activa) sesion.pista else "Reproduce tu biblioteca con el temporizador visible en la notificación.",
            color = NocturneTextoSuave, fontSize = 14.sp, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().testTag("pista"),
        )

        Text("Temporizador de sueño", color = NocturneTexto, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OPCIONES.forEach { (texto, valor) ->
                val elegido = limite == valor
                Text(
                    texto,
                    color = if (elegido) Color.White else NocturneTextoSuave,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .background(if (elegido) NocturneMorado else NocturneSuperficie, RoundedCornerShape(50))
                        .border(1.dp, if (elegido) NocturneMorado else Color(0x33FFFFFF), RoundedCornerShape(50))
                        .clickable(enabled = !sesion.activa) { limite = valor }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .testTag("limite_$texto"),
                )
            }
        }

        if (!sesion.activa) {
            Button(
                onClick = {
                    if (!tienePermiso() && Build.VERSION.SDK_INT >= 33) {
                        solicitar.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        SesionEscuchaService.iniciar(contexto, limite)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("botonIniciar"),
            ) { Text("Iniciar sesión de escucha", fontSize = 16.sp) }
        } else {
            Button(
                onClick = { SesionEscuchaService.detener(contexto) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE5484D)),
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("botonDetener"),
            ) { Text("Detener sesión", fontSize = 16.sp) }
        }

        Tarjeta {
            Fila("Servicio", if (sesion.activa) "En primer plano (mediaPlayback)" else "Detenido", "estadoServicio")
            Fila("Notificación", if (sesion.activa) "Persistente · ${sesion.actualizaciones} actualizaciones" else "Retirada")
            Fila("Audio", if (sesion.audio) "Reproduciendo en bucle" else "—")
            Fila("Permiso de notificaciones", if (permiso) "Concedido" else "Pendiente")
            if (!sesion.activa && sesion.transcurridoSeg > 0) {
                Fila("Última sesión", formatearTiempo(sesion.transcurridoSeg), "ultimaSesion")
            }
        }

        Text(
            "Puedes salir de la app: el tiempo sigue avanzando en la notificación y se detiene con «Detener».",
            color = NocturneTextoSuave, fontSize = 12.sp,
        )
    }
}

@Composable
private fun Reloj(sesion: EstadoSesion.Sesion) {
    val giro = rememberInfiniteTransition(label = "giro")
    val angulo by giro.animateFloat(
        0f, 360f, infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Restart), label = "angulo",
    )
    // Progreso del segundo actual o, con temporizador de sueño, de la sesión completa.
    val progreso = sesion.limiteSeg?.let { sesion.transcurridoSeg.toFloat() / it } ?: ((sesion.transcurridoSeg % 60) / 60f)
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Canvas(Modifier.size(230.dp)) {
            val grosor = 14.dp.toPx()
            drawCircle(Color(0x22FFFFFF), style = Stroke(grosor))
            drawArc(
                Brush.sweepGradient(listOf(NocturneMorado, NocturneCian, NocturneMorado)),
                startAngle = -90f, sweepAngle = 360f * progreso, useCenter = false,
                style = Stroke(grosor, cap = StrokeCap.Round),
            )
            if (sesion.activa) {
                val r = size.minDimension / 2 - grosor * 2.2f
                val rad = Math.toRadians(angulo.toDouble())
                drawCircle(NocturneCian, 6.dp.toPx(), center + Offset((r * Math.cos(rad)).toFloat(), (r * Math.sin(rad)).toFloat()))
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                formatearTiempo(sesion.transcurridoSeg),
                color = NocturneTexto, fontSize = 46.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag("tiempo"),
            )
            Text(
                when {
                    !sesion.activa -> "detenida"
                    sesion.restanteSeg != null -> "quedan ${formatearTiempo(sesion.restanteSeg!!)}"
                    else -> "transcurrido"
                },
                color = NocturneCian, fontSize = 14.sp,
            )
        }
    }
}

@Composable
private fun Tarjeta(contenido: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().background(NocturneSuperficie.copy(alpha = 0.92f), RoundedCornerShape(18.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) { contenido() }
}

@Composable
private fun Fila(etiqueta: String, valor: String, tag: String? = null) {
    Row(Modifier.fillMaxWidth()) {
        Text(etiqueta, color = NocturneTextoSuave, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(
            valor, color = NocturneTexto, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
            modifier = if (tag != null) Modifier.testTag(tag) else Modifier,
        )
    }
    Spacer(Modifier.height(0.dp))
}
