package appmovil.prestaaula

import appmovil.prestaaula.model.EstadoDevolucion
import appmovil.prestaaula.model.Validaciones
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ValidacionesTest {

    @Test
    fun `correo vacio, sin arroba o sin dominio es invalido`() {
        assertEquals("Ingresa tu correo", Validaciones.correo("   "))
        assertEquals("Escribe un correo válido (nombre@colegio.cl)", Validaciones.correo("cra"))
        assertEquals("Escribe un correo válido (nombre@colegio.cl)", Validaciones.correo("cra@marcelapaz"))
        assertEquals("Escribe un correo válido (nombre@colegio.cl)", Validaciones.correo("cra @marcelapaz.cl"))
    }

    @Test
    fun `correo valido se acepta con espacios alrededor`() {
        assertNull(Validaciones.correo("cra@marcelapaz.cl"))
        assertNull(Validaciones.correo("  CRA@MarcelaPaz.CL "))
    }

    @Test
    fun `clave vacia es invalida y cualquier otra se acepta`() {
        assertEquals("Ingresa tu contraseña", Validaciones.clave(""))
        assertNull(Validaciones.clave(" "))
        assertNull(Validaciones.clave("cra123"))
    }

    @Test
    fun `curso acepta los formatos habituales`() {
        listOf("7°A", "7A", "7 A", "2°M", "12°B", " 6°a ").forEach { assertNull(it, Validaciones.curso(it)) }
    }

    @Test
    fun `curso rechaza vacio y formatos raros`() {
        assertEquals("Ingresa el curso", Validaciones.curso("  "))
        listOf("A7", "septimo", "7°", "°A", "123A", "7°AB").forEach {
            assertEquals(it, "Usa el formato 7°A", Validaciones.curso(it))
        }
    }

    @Test
    fun `marca exige entre 2 y 30 caracteres`() {
        assertEquals("Ingresa la marca", Validaciones.marca("  "))
        assertEquals("La marca debe tener al menos 2 letras", Validaciones.marca("A"))
        assertEquals("Máximo 30 caracteres", Validaciones.marca("x".repeat(31)))
        assertNull(Validaciones.marca("  Dell "))
        assertNull(Validaciones.marca("x".repeat(30)))
    }

    @Test
    fun `observaciones son opcionales solo con el equipo en buen estado`() {
        assertNull(Validaciones.observaciones(EstadoDevolucion.BUEN_ESTADO, ""))
        val pedir = "Describe el problema en las observaciones"
        assertEquals(pedir, Validaciones.observaciones(EstadoDevolucion.CON_DANOS, "  "))
        assertEquals(pedir, Validaciones.observaciones(EstadoDevolucion.FALTAN_ACCESORIOS, ""))
        assertNull(Validaciones.observaciones(EstadoDevolucion.CON_DANOS, "Pantalla rota"))
    }

    @Test
    fun `observaciones tienen un maximo de 200 caracteres`() {
        assertEquals(
            "Máximo 200 caracteres",
            Validaciones.observaciones(EstadoDevolucion.BUEN_ESTADO, "x".repeat(201)),
        )
        assertNull(Validaciones.observaciones(EstadoDevolucion.BUEN_ESTADO, "x".repeat(200)))
    }
}
