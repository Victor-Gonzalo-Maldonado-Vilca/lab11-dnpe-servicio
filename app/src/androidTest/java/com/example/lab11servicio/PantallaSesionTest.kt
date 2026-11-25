package com.example.lab11servicio

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** La pantalla inicia el servicio, muestra el tiempo que avanza y lo detiene. */
@RunWith(AndroidJUnit4::class)
class PantallaSesionTest {

    @get:Rule
    val regla = createAndroidComposeRule<MainActivity>()

    @Before
    fun concederPermiso() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand("pm grant ${ctx.packageName} android.permission.POST_NOTIFICATIONS").close()
    }

    private fun textoDe(tag: String): String =
        regla.onNodeWithTag(tag).fetchSemanticsNode().config[SemanticsProperties.Text].joinToString { it.text }

    @Test
    fun iniciar_muestraElTiempoAvanzando_yDetener_vuelveAlEstadoInicial() {
        regla.onNodeWithTag("botonIniciar").performClick()

        regla.waitUntil(4_000) { textoDe("tiempo") == "00:02" }
        regla.onNodeWithTag("estadoServicio").assertTextEquals("En primer plano (mediaPlayback)")

        regla.onNodeWithTag("botonDetener").performClick()

        regla.waitUntil(3_000) { regla.onAllNodesWithTagExists("botonIniciar") }
        regla.onNodeWithTag("estadoServicio").assertTextEquals("Detenido")
        regla.onNodeWithTag("ultimaSesion").assertIsDisplayed()
    }

    private fun androidx.compose.ui.test.junit4.AndroidComposeTestRule<*, *>.onAllNodesWithTagExists(tag: String) =
        onAllNodes(androidx.compose.ui.test.hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()
}
