package appmovil.prestaaula

import appmovil.prestaaula.model.Funcion
import appmovil.prestaaula.model.Rol
import appmovil.prestaaula.model.funcionPermitida
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GuardiaAccesoTest {

    @Test
    fun `cada rol accede exactamente a sus funciones y a ninguna otra`() {
        for (rol in Rol.entries) {
            for (funcion in Funcion.entries) {
                val esperado = if (funcion in rol.funciones) funcion else null
                assertEquals("${rol.name} -> ${funcion.name}", esperado, funcionPermitida(rol, funcion.name))
            }
        }
    }

    @Test
    fun `un docente no puede entrar a funciones del CRA ni a reportes`() {
        assertNull(funcionPermitida(Rol.DOCENTE, Funcion.INVENTARIO.name))
        assertNull(funcionPermitida(Rol.DOCENTE, Funcion.REGISTRAR_PRESTAMO.name))
        assertNull(funcionPermitida(Rol.DOCENTE, Funcion.REPORTE_USO.name))
    }

    @Test
    fun `UTP y Finanzas no pueden operar prestamos ni reservar`() {
        for (rol in listOf(Rol.JEFE_UTP, Rol.JEFE_ADMIN_FINANZAS)) {
            assertNull(funcionPermitida(rol, Funcion.REGISTRAR_PRESTAMO.name))
            assertNull(funcionPermitida(rol, Funcion.REGISTRAR_DEVOLUCION.name))
            assertNull(funcionPermitida(rol, Funcion.RESERVAR_EQUIPO.name))
        }
    }

    @Test
    fun `nombres invalidos o nulos no resuelven a ninguna funcion`() {
        for (rol in Rol.entries) {
            assertNull(funcionPermitida(rol, null))
            assertNull(funcionPermitida(rol, ""))
            assertNull(funcionPermitida(rol, "inexistente"))
            assertNull(funcionPermitida(rol, "inventario")) // el nombre distingue mayúsculas
        }
    }
}
