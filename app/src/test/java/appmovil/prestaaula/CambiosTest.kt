package appmovil.prestaaula

import appmovil.prestaaula.data.db.Convertidores
import appmovil.prestaaula.data.db.diferencias
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class CambiosTest {

    private data class Fila(val id: Int, val valor: String)

    @Test
    fun `sin cambios no hay nada que escribir`() {
        val lista = listOf(Fila(1, "a"), Fila(2, "b"))
        assertTrue(diferencias(lista, lista.toList()) { it.id }.vacio)
    }

    @Test
    fun `detecta filas nuevas, modificadas y borradas`() {
        val antes = listOf(Fila(1, "a"), Fila(2, "b"), Fila(3, "c"))
        val ahora = listOf(Fila(1, "a"), Fila(2, "B"), Fila(4, "d"))

        val cambios = diferencias(antes, ahora) { it.id }

        assertEquals(listOf(Fila(2, "B"), Fila(4, "d")), cambios.guardar)
        assertEquals(listOf(3), cambios.borrar)
    }

    @Test
    fun `desde vacio se guarda todo`() {
        val ahora = listOf(Fila(1, "a"), Fila(2, "b"))
        val cambios = diferencias(emptyList(), ahora) { it.id }
        assertEquals(ahora, cambios.guardar)
        assertTrue(cambios.borrar.isEmpty())
    }

    @Test
    fun `convertidores de fecha ida y vuelta`() {
        val c = Convertidores()
        val fecha = LocalDate.of(2026, 10, 30)
        val fechaHora = LocalDateTime.of(2026, 10, 30, 8, 15)

        assertEquals("2026-10-30", c.fechaATexto(fecha))
        assertEquals(fecha, c.textoAFecha(c.fechaATexto(fecha)))
        assertEquals(fechaHora, c.textoAFechaHora(c.fechaHoraATexto(fechaHora)))
        assertNull(c.textoAFecha(null))
        assertNull(c.fechaHoraATexto(null))
    }
}
