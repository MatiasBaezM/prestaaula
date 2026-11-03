package appmovil.prestaaula

import appmovil.prestaaula.data.db.Convertidores
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

// Fechas <-> texto ISO-8601, como quedan guardadas en las columnas de SQLite.
class ConvertidoresTest {

    private val c = Convertidores()

    @Test
    fun `una fecha se guarda como texto ISO y vuelve igual`() {
        val fecha = LocalDate.of(2026, 10, 30)
        assertEquals("2026-10-30", c.fechaATexto(fecha))
        assertEquals(fecha, c.textoAFecha(c.fechaATexto(fecha)))
    }

    @Test
    fun `una fecha con hora se guarda como texto ISO y vuelve igual`() {
        val fechaHora = LocalDateTime.of(2026, 10, 30, 8, 15)
        assertEquals("2026-10-30T08:15", c.fechaHoraATexto(fechaHora))
        assertEquals(fechaHora, c.textoAFechaHora(c.fechaHoraATexto(fechaHora)))
    }

    @Test
    fun `null se guarda y se lee como null`() {
        assertNull(c.fechaATexto(null))
        assertNull(c.textoAFecha(null))
        assertNull(c.fechaHoraATexto(null))
        assertNull(c.textoAFechaHora(null))
    }
}
