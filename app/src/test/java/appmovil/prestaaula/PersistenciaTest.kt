package appmovil.prestaaula

import appmovil.prestaaula.data.db.aEntidad
import appmovil.prestaaula.data.db.aModelo
import appmovil.prestaaula.model.Equipo
import appmovil.prestaaula.model.EstadoDevolucion
import appmovil.prestaaula.model.EstadoEquipo
import appmovil.prestaaula.model.Prestamo
import appmovil.prestaaula.model.Reserva
import appmovil.prestaaula.model.TipoEquipo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

// Conversión modelo <-> fila de SQLite. La prueba contra Room real está en androidTest (BaseDatosTest).
class PersistenciaTest {

    @Test
    fun `equipos sobreviven ida y vuelta`() {
        val lista = listOf(
            Equipo(1, "NB-001", TipoEquipo.NOTEBOOK, "Lenovo"),
            Equipo(2, "PY-001", TipoEquipo.PROYECTOR, "Epson", EstadoEquipo.EN_MANTENCION),
        )
        assertEquals(lista, lista.map { it.aEntidad().aModelo() })
    }

    @Test
    fun `reservas sobreviven ida y vuelta`() {
        val lista = listOf(Reserva(1, 2, 3, "Prof. Díaz", "7°A", LocalDate.of(2026, 10, 30), 4))
        assertEquals(lista, lista.map { it.aEntidad().aModelo() })
    }

    @Test
    fun `prestamos activos y cerrados sobreviven ida y vuelta`() {
        val activo = Prestamo(1, 5, 102, "Prof. Soto", "6°A", LocalDateTime.of(2026, 10, 29, 8, 5), retiroConfirmado = true)
        val cerrado = Prestamo(
            2, 3, 103, "Prof. Núñez", "2°M", LocalDateTime.of(2026, 10, 27, 9, 0),
            devolucion = LocalDateTime.of(2026, 10, 27, 10, 0),
            estadoDevolucion = EstadoDevolucion.CON_DANOS, observaciones = "Pantalla \"trizada\" ñandú",
            foto = "dev_1.jpg",
        )
        val lista = listOf(activo, cerrado)

        val vuelta = lista.map { it.aEntidad().aModelo() }

        assertEquals(lista, vuelta)
        assertNull(vuelta.first().devolucion)
        assertNull(vuelta.first().estadoDevolucion)
        assertNull(vuelta.first().foto)
    }
}
