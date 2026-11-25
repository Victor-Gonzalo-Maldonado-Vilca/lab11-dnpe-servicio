package com.example.lab11servicio

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.lab11servicio.ui.theme.NocturneTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            NocturneTheme {
                PantallaSesion()
            }
        }
    }

    // Marca cuándo la app está visible: el servicio lo registra en cada actualización,
    // lo que permite comprobar que sigue activo con la app en segundo plano.
    override fun onStart() {
        super.onStart()
        EstadoSesion.appVisible = true
        Log.i(SesionEscuchaService.TAG, "Activity onStart: app VISIBLE")
    }

    override fun onStop() {
        EstadoSesion.appVisible = false
        Log.i(SesionEscuchaService.TAG, "Activity onStop: app en SEGUNDO PLANO")
        super.onStop()
    }
}
