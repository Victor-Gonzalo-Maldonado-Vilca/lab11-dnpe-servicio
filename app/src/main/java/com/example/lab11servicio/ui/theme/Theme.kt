package com.example.lab11servicio.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// Esquema oscuro fijo para que el componente se vea igual que en el reproductor Nocturne.
private val NocturneColorScheme = darkColorScheme(
    primary = NocturneMorado,
    secondary = NocturneCian,
    background = NocturneFondo,
    surface = NocturneSuperficie,
    onPrimary = NocturneTexto,
    onBackground = NocturneTexto,
    onSurface = NocturneTexto,
)

@Composable
fun NocturneTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NocturneColorScheme,
        typography = Typography,
        content = content
    )
}
