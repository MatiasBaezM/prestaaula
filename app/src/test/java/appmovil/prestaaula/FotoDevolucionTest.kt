package appmovil.prestaaula

import appmovil.prestaaula.data.InventarioRepository
import appmovil.prestaaula.data.PrestamoRepository
import appmovil.prestaaula.data.ReservaRepository
import appmovil.prestaaula.data.db.aEntidad
import appmovil.prestaaula.data.db.aModelo
import appmovil.prestaaula.model.EstadoDevolucion
import appmovil.prestaaula.model.Prestamo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime

class FotoDevolucionTest {

    private val retiro = LocalDateTime.of(2026, 3, 2, 9, 0)

    @Test
    fun laFotoSobreviveAlGuardarYLeerLaFila() {
        val p = Prestamo(1, 5, 102, "Prof. Soto", "6°A", retiro, foto = "dev_123.jpg")
        assertEquals("dev_123.jpg", p.aEntidad().aModelo().foto)
    }

    @Test
    fun prestamoSinFotoSeGuardaYSeLeeComoNull() {
        val p = Prestamo(1, 5, 102, "Prof. Soto", "6°A", retiro)
        assertNull(p.aEntidad().aModelo().foto)
    }

    @Test
    fun laDevolucionGuardaLaFotoEnElPrestamo() {
        val inventario = InventarioRepository()
        val repo = PrestamoRepository(inventario, ReservaRepository(inventario))
        val activo = repo.prestamos.value.first { it.activo }

        val error = repo.registrarDevolucion(activo.id, EstadoDevolucion.CON_DANOS, "Pantalla rota", "dev_9.jpg")

        assertNull(error)
        assertEquals("dev_9.jpg", repo.prestamos.value.first { it.id == activo.id }.foto)
    }

    @Test
    fun siLaDevolucionFallaNoSeGuardaLaFoto() {
        val inventario = InventarioRepository()
        val repo = PrestamoRepository(inventario, ReservaRepository(inventario))
        val activo = repo.prestamos.value.first { it.activo }

        val error = repo.registrarDevolucion(activo.id, EstadoDevolucion.CON_DANOS, "", "dev_9.jpg")

        assertEquals("Describe el problema en las observaciones", error)
        assertNull(repo.prestamos.value.first { it.id == activo.id }.foto)
    }
}
