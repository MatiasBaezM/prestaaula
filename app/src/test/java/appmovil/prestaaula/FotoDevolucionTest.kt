package appmovil.prestaaula

import appmovil.prestaaula.data.Codec
import appmovil.prestaaula.data.InventarioRepository
import appmovil.prestaaula.data.PrestamoRepository
import appmovil.prestaaula.data.ReservaRepository
import appmovil.prestaaula.model.EstadoDevolucion
import appmovil.prestaaula.model.Prestamo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime

class FotoDevolucionTest {

    private val retiro = LocalDateTime.of(2026, 3, 2, 9, 0)

    @Test
    fun laFotoSobreviveAlGuardarYLeerElJson() {
        val p = Prestamo(1, 5, 102, "Prof. Soto", "6°A", retiro, foto = "dev_123.jpg")
        val leido = Codec.prestamosDeJson(Codec.prestamosAJson(listOf(p))).single()
        assertEquals("dev_123.jpg", leido.foto)
    }

    @Test
    fun prestamoSinFotoSeGuardaYSeLeeComoNull() {
        val p = Prestamo(1, 5, 102, "Prof. Soto", "6°A", retiro)
        assertNull(Codec.prestamosDeJson(Codec.prestamosAJson(listOf(p))).single().foto)
    }

    @Test
    fun archivosGuardadosAntesDeLaFotoSiguenLeyendose() {
        val antiguo = """[{"id":1,"equipoId":5,"docenteId":102,"docente":"Prof. Soto","curso":"6°A",
            "retiro":"2026-03-02T09:00","retiroConfirmado":true,"devolucionAvisada":false,
            "devolucion":null,"estadoDevolucion":null,"observaciones":""}]"""
        assertNull(Codec.prestamosDeJson(antiguo).single().foto)
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
