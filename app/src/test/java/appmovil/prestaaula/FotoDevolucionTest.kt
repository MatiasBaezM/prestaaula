package appmovil.prestaaula

import appmovil.prestaaula.data.DatosDemo
import appmovil.prestaaula.data.PrestamoRepository
import appmovil.prestaaula.data.db.aEntidad
import appmovil.prestaaula.data.db.aModelo
import appmovil.prestaaula.model.EstadoDevolucion
import appmovil.prestaaula.model.Prestamo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

class FotoDevolucionTest : PruebaConBaseDeDatos() {

    private val retiro = LocalDateTime.of(2026, 3, 2, 9, 0)
    private lateinit var repo: PrestamoRepository

    // Parte con los datos de ejemplo de la app (incluye un préstamo activo: TB-002).
    @Before
    fun datosDeEjemplo() = prueba {
        DatosDemo.cargarSiEstaVacia(db)
        repo = PrestamoRepository(db)
    }

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
    fun laDevolucionGuardaLaFotoEnElPrestamo() = prueba {
        val activo = prestamosEnBase().first { it.activo }

        val error = repo.registrarDevolucion(activo.id, EstadoDevolucion.CON_DANOS, "Pantalla rota", "dev_9.jpg")

        assertNull(error)
        assertEquals("dev_9.jpg", prestamosEnBase().first { it.id == activo.id }.foto)
    }

    @Test
    fun siLaDevolucionFallaNoSeGuardaLaFoto() = prueba {
        val activo = prestamosEnBase().first { it.activo }

        val error = repo.registrarDevolucion(activo.id, EstadoDevolucion.CON_DANOS, "", "dev_9.jpg")

        assertEquals("Describe el problema en las observaciones", error)
        assertNull(prestamosEnBase().first { it.id == activo.id }.foto)
    }

    @Test
    fun losDatosDeEjemploSeCarganUnaSolaVez() = prueba {
        val equipos = equiposEnBase().size
        DatosDemo.cargarSiEstaVacia(db) // segunda apertura de la app: la base ya tiene datos
        assertEquals(equipos, equiposEnBase().size)
        assertEquals(DatosDemo.equipos(), equiposEnBase())
    }
}
