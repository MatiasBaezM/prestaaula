package appmovil.prestaaula

import appmovil.prestaaula.model.Rol
import appmovil.prestaaula.ui.login.AuthViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthViewModelTest {

    private fun AuthViewModel.login(correo: String, clave: String) {
        onCorreoChange(correo)
        onClaveChange(clave)
        iniciarSesion()
    }

    @Test
    fun `login correcto guarda el usuario y limpia el formulario`() {
        val vm = AuthViewModel()
        vm.login("cra@marcelapaz.cl", "cra123")

        assertEquals(Rol.ENCARGADO_CRA, vm.usuarioActual.value?.rol)
        assertEquals("", vm.form.value.clave)
        assertNull(vm.form.value.error)
    }

    @Test
    fun `credenciales incorrectas muestran error y no abren sesion`() {
        val vm = AuthViewModel()
        vm.login("cra@marcelapaz.cl", "mala")

        assertNull(vm.usuarioActual.value)
        assertEquals("Correo o contraseña incorrectos", vm.form.value.error)
    }

    @Test
    fun `campos vacios marcan un error por campo y no abren sesion`() {
        val vm = AuthViewModel()
        vm.iniciarSesion()

        assertNull(vm.usuarioActual.value)
        assertEquals("Ingresa tu correo", vm.form.value.correoError)
        assertEquals("Ingresa tu contraseña", vm.form.value.claveError)
        assertEquals(1, vm.form.value.intentos)
    }

    @Test
    fun `correo con formato invalido no llega a validarse contra el repositorio`() {
        val vm = AuthViewModel()
        vm.login("cra-sin-arroba", "cra123")

        assertNull(vm.usuarioActual.value)
        assertEquals("Escribe un correo válido (nombre@colegio.cl)", vm.form.value.correoError)
        assertNull(vm.form.value.claveError)
        assertNull(vm.form.value.error)
    }

    @Test
    fun `cada intento invalido suma un intento para animar el error`() {
        val vm = AuthViewModel()
        vm.iniciarSesion()
        vm.iniciarSesion()

        assertEquals(2, vm.form.value.intentos)
    }

    @Test
    fun `escribir en un campo borra el error`() {
        val vm = AuthViewModel()
        vm.login("cra@marcelapaz.cl", "mala")
        assertNotNull(vm.form.value.error)

        vm.onCorreoChange("a")
        assertNull(vm.form.value.error)
    }

    @Test
    fun `mostrar clave alterna`() {
        val vm = AuthViewModel()
        assertFalse(vm.form.value.mostrarClave)
        vm.toggleMostrarClave()
        assertTrue(vm.form.value.mostrarClave)
    }

    @Test
    fun `cerrar sesion limpia la sesion y el formulario`() {
        val vm = AuthViewModel()
        vm.login("docente@marcelapaz.cl", "docente123")
        assertNotNull(vm.usuarioActual.value)

        vm.cerrarSesion()
        assertNull(vm.usuarioActual.value)
        assertEquals("", vm.form.value.correo)
        assertEquals("", vm.form.value.clave)
    }
}
