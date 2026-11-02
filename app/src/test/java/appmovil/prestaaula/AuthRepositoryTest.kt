package appmovil.prestaaula

import appmovil.prestaaula.data.AuthRepository
import appmovil.prestaaula.data.ResultadoLogin
import appmovil.prestaaula.model.Funcion
import appmovil.prestaaula.model.Rol
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthRepositoryTest {

    private val repo = AuthRepository()

    private fun rolDe(correo: String, clave: String): Rol? =
        (repo.iniciarSesion(correo, clave) as? ResultadoLogin.Exito)?.usuario?.rol

    @Test
    fun `los cuatro usuarios de prueba inician sesion con su rol`() {
        assertEquals(Rol.DOCENTE, rolDe("docente@marcelapaz.cl", "docente123"))
        assertEquals(Rol.ENCARGADO_CRA, rolDe("cra@marcelapaz.cl", "cra123"))
        assertEquals(Rol.JEFE_UTP, rolDe("utp@marcelapaz.cl", "utp123"))
        assertEquals(Rol.JEFE_ADMIN_FINANZAS, rolDe("finanzas@marcelapaz.cl", "admin123"))
    }

    @Test
    fun `el correo se normaliza con trim y minusculas`() {
        assertEquals(Rol.DOCENTE, rolDe("  Docente@MarcelaPaz.CL ", "docente123"))
    }

    @Test
    fun `clave incorrecta devuelve error`() {
        val r = repo.iniciarSesion("cra@marcelapaz.cl", "mala")
        assertEquals(ResultadoLogin.Error("Correo o contraseña incorrectos"), r)
    }

    @Test
    fun `usuario inexistente devuelve el mismo error generico`() {
        val r = repo.iniciarSesion("nadie@marcelapaz.cl", "cra123")
        assertEquals(ResultadoLogin.Error("Correo o contraseña incorrectos"), r)
    }

    @Test
    fun `campos vacios devuelven mensaje de ingreso`() {
        val esperado = ResultadoLogin.Error("Ingresa tu correo y contraseña")
        assertEquals(esperado, repo.iniciarSesion("", ""))
        assertEquals(esperado, repo.iniciarSesion("cra@marcelapaz.cl", ""))
        assertEquals(esperado, repo.iniciarSesion("   ", "cra123"))
    }

    @Test
    fun `cada rol solo puede las funciones de la tabla`() {
        assertEquals(3, Rol.DOCENTE.funciones.size)
        assertTrue(Rol.DOCENTE.puede(Funcion.DISPONIBILIDAD))
        assertFalse(Rol.DOCENTE.puede(Funcion.INVENTARIO))
        assertEquals(6, Rol.ENCARGADO_CRA.funciones.size)
        assertEquals(
            listOf(Funcion.REPORTE_USO, Funcion.EQUIPOS_INCIDENCIAS),
            Rol.JEFE_UTP.funciones,
        )
        assertEquals(Rol.JEFE_UTP.funciones, Rol.JEFE_ADMIN_FINANZAS.funciones)
        assertFalse(Rol.JEFE_UTP.puede(Funcion.REGISTRAR_PRESTAMO))
    }
}
