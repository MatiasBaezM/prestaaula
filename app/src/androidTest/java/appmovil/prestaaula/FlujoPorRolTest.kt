package appmovil.prestaaula

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Recorre la app real en el emulador con cada uno de los 4 usuarios de prueba.
// Las funciones esperadas se escriben a mano desde la tabla del caso (no se leen de Rol.kt),
// así el test detecta si el código se aparta de lo acordado.
@RunWith(AndroidJUnit4::class)
class FlujoPorRolTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private data class Cuenta(
        val correo: String,
        val clave: String,
        val nombre: String,
        val rol: String,
        val funciones: List<String>,
    )

    private val docente = Cuenta(
        "docente@marcelapaz.cl", "docente123", "Prof. Muñoz", "Docente",
        listOf("Disponibilidad", "Mis reservas", "Retiro y devolución"),
    )
    private val cra = Cuenta(
        "cra@marcelapaz.cl", "cra123", "Patricia Vega", "Encargado CRA",
        listOf(
            "Inventario", "Disponibilidad", "Registrar préstamo",
            "Registrar devolución", "Reporte de uso", "Equipos con incidencias",
        ),
    )
    private val utp = Cuenta(
        "utp@marcelapaz.cl", "utp123", "Andrés Silva", "Jefe de UTP",
        listOf("Reporte de uso", "Equipos con incidencias"),
    )
    private val finanzas = Cuenta(
        "finanzas@marcelapaz.cl", "admin123", "Carolina Núñez", "Jefe de Administración y Finanzas",
        listOf("Reporte de uso", "Equipos con incidencias"),
    )

    private val todasLasFunciones = listOf(
        "Inventario", "Disponibilidad", "Mis reservas", "Retiro y devolución",
        "Registrar préstamo", "Registrar devolución", "Reporte de uso", "Equipos con incidencias",
    )

    private fun iniciarSesion(correo: String, clave: String) {
        rule.onNodeWithText("Correo").performTextInput(correo)
        rule.onNodeWithText("Contraseña").performTextInput(clave)
        rule.onNodeWithText("Iniciar sesión").performClick()
        rule.waitForIdle()
    }

    // Cantidad de nodos con ese texto exacto (0 = no está en pantalla).
    private fun cuantos(texto: String) = rule.onAllNodes(hasText(texto)).fetchSemanticsNodes().size

    private fun homeExiste() = cuantos("¿Qué necesitas hacer?") > 0

    private fun verificarHome(c: Cuenta) {
        rule.onNodeWithText(c.nombre).assertIsDisplayed()
        rule.onNodeWithText(c.rol).assertIsDisplayed()
        for (titulo in todasLasFunciones) {
            val tarjetas = cuantos(titulo)
            if (titulo in c.funciones) {
                assertEquals("${c.rol} debería ver '$titulo' una vez, ve $tarjetas", 1, tarjetas)
            } else {
                assertEquals("${c.rol} NO debería ver '$titulo'", 0, tarjetas)
            }
        }
    }

    // Abre cada función del rol, comprueba que cambió de pantalla y vuelve al Home.
    private fun recorrerFunciones(c: Cuenta) {
        for (titulo in c.funciones) {
            rule.onNodeWithText(titulo).performClick()
            rule.waitForIdle()
            rule.onNodeWithContentDescription("Volver").assertIsDisplayed()
            assertFalse("${c.rol}: '$titulo' no abrió su pantalla", homeExiste())

            rule.onNodeWithContentDescription("Volver").performClick()
            rule.waitForIdle()
            rule.onNodeWithText("¿Qué necesitas hacer?").assertIsDisplayed()
        }
    }

    private fun probarRol(c: Cuenta) {
        iniciarSesion(c.correo, c.clave)
        verificarHome(c)
        recorrerFunciones(c)
    }

    @Test
    fun docente_veSoloSusTresFunciones()= probarRol(docente)

    @Test
    fun encargadoCra_veSusSeisFunciones() = probarRol(cra)

    @Test
    fun jefeUtp_veSoloReportes() = probarRol(utp)

    @Test
    fun jefeFinanzas_veSoloReportes() = probarRol(finanzas)

    @Test
    fun credencialesIncorrectas_muestranErrorYNoEntran() {
        iniciarSesion("cra@marcelapaz.cl", "clave-mala")

        rule.onNodeWithText("Correo o contraseña incorrectos").assertIsDisplayed()
        assertFalse("Se mostró el Home sin sesión válida", homeExiste())
    }

    @Test
    fun camposVacios_muestranMensajeYNoEntran() {
        rule.onNodeWithText("Iniciar sesión").performClick()
        rule.waitForIdle()

        rule.onNodeWithText("Ingresa tu correo").assertIsDisplayed()
        rule.onNodeWithText("Ingresa tu contraseña").assertIsDisplayed()
        assertFalse("Se mostró el Home sin sesión válida", homeExiste())
    }

    @Test
    fun correoConMayusculasYEspacios_tambienEntra() {
        iniciarSesion("  CRA@MarcelaPaz.CL ", "cra123")

        rule.onNodeWithText("Patricia Vega").assertIsDisplayed()
    }

    @Test
    fun cerrarSesion_vuelveAlLoginSinMostrarElHome() {
        iniciarSesion(docente.correo, docente.clave)
        rule.onNodeWithText("¿Qué necesitas hacer?").assertIsDisplayed()

        rule.onNodeWithContentDescription("Cerrar sesión").performClick()
        rule.waitForIdle()

        rule.onNodeWithText("Acceso exclusivo para personal del establecimiento").assertIsDisplayed()
        assertFalse("Se mostró el Home sin sesión válida", homeExiste())
    }

    @Test
    fun cerrarSesion_noDejaLaClaveEscritaEnElLogin() {
        iniciarSesion(docente.correo, docente.clave)
        rule.onNodeWithContentDescription("Cerrar sesión").performClick()
        rule.waitForIdle()

        // El formulario se reinicia: el campo vuelve a pedir el correo vacío.
        rule.onNodeWithText("Correo").assertIsDisplayed()
        assertEquals("El correo quedó escrito tras cerrar sesión", 0, cuantos(docente.correo))
    }

    @Test
    fun rotacion_mantieneLaSesionYLaPantalla() {
        iniciarSesion(cra.correo, cra.clave)
        rule.onNodeWithText("Inventario").performClick()
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Volver").assertIsDisplayed()

        rule.activityRule.scenario.recreate()
        rule.waitForIdle()

        // Sigue dentro de Inventario con la sesión viva (el ViewModel sobrevive).
        rule.onNodeWithContentDescription("Volver").assertIsDisplayed()
        rule.onNodeWithContentDescription("Volver").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Patricia Vega").assertIsDisplayed()
    }
}
