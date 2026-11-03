package appmovil.prestaaula

import appmovil.prestaaula.data.Docentes
import appmovil.prestaaula.data.InventarioRepository
import appmovil.prestaaula.data.PrestamoRepository
import appmovil.prestaaula.model.Equipo
import appmovil.prestaaula.model.EstadoDevolucion
import appmovil.prestaaula.model.EstadoEquipo
import appmovil.prestaaula.model.Reserva
import appmovil.prestaaula.model.TipoEquipo
import appmovil.prestaaula.model.ultimoResponsable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

class PrestamoTest : PruebaConBaseDeDatos() {

    // Jueves 29-oct-2026, 08:50 (bloque 2 en curso: 08:45–09:30).
    private var reloj = LocalDateTime.of(2026, 10, 29, 8, 50)
    private val hoy = reloj.toLocalDate()

    private val rojas = Docentes.demo.first { it.id == 101 }
    private val soto = Docentes.demo.first { it.id == 102 }

    private lateinit var repo: PrestamoRepository

    @Before
    fun datos() {
        guardar(
            equipos = listOf(
                Equipo(1, "PY-001", TipoEquipo.PROYECTOR, "Epson"),
                Equipo(2, "NB-001", TipoEquipo.NOTEBOOK, "Lenovo", EstadoEquipo.EN_MANTENCION),
                Equipo(3, "NB-002", TipoEquipo.NOTEBOOK, "HP"),
            ),
            reservas = listOf(
                Reserva(1, 1, 101, "Prof. Rojas", "7°A", hoy, 2),
                Reserva(2, 1, 101, "Prof. Rojas", "7°A", hoy, 3),
                Reserva(3, 1, 102, "Prof. Soto", "8°B", hoy, 5),
                Reserva(4, 1, 101, "Prof. Rojas", "7°A", hoy, 7), // otra clase de Rojas, más tarde
            ),
        )
        repo = PrestamoRepository(db) { reloj }
    }

    @Test
    fun `prestar deja el equipo prestado y registra al docente`() = prueba {
        assertNull(repo.registrarPrestamo(3, rojas, " 7°A "))

        assertEquals(EstadoEquipo.PRESTADO, estadoDe(3))
        val p = prestamosEnBase().single()
        assertEquals("Prof. Rojas", p.docente)
        assertEquals("7°A", p.curso)
        assertEquals(reloj, p.retiro)
        assertTrue(p.activo)
    }

    @Test
    fun `entregar cumple solo el tramo en curso y conserva las clases posteriores`() = prueba {
        assertNull(repo.registrarPrestamo(1, rojas, "7°A"))

        // Se cumplió el tramo 2–3 de Rojas. Siguen: Soto (bloque 5) y la otra clase de Rojas (bloque 7).
        assertEquals(listOf(3, 4), reservasEnBase().map { it.id })
    }

    @Test
    fun `no se entrega a otro docente un equipo reservado en este bloque`() = prueba {
        assertEquals("Está reservado en este bloque por Prof. Rojas", repo.registrarPrestamo(1, soto, "8°B"))
        assertEquals(EstadoEquipo.DISPONIBLE, estadoDe(1))
        assertTrue(prestamosEnBase().isEmpty())
        assertEquals(4, reservasEnBase().size) // la transacción no tocó nada
    }

    @Test
    fun `equipo prestado o en mantencion no se puede prestar`() = prueba {
        assertEquals("El equipo está en mantención", repo.registrarPrestamo(2, rojas, "7°A"))
        repo.registrarPrestamo(3, rojas, "7°A")
        assertEquals("El equipo ya está prestado", repo.registrarPrestamo(3, soto, "8°B"))
        assertEquals("El equipo no existe", repo.registrarPrestamo(99, rojas, "7°A"))
        assertEquals("Ingresa el curso", repo.registrarPrestamo(1, rojas, " "))
    }

    @Test
    fun `devolver en buen estado libera el equipo`() = prueba {
        repo.registrarPrestamo(3, rojas, "7°A")
        reloj = reloj.plusMinutes(40)

        assertNull(repo.registrarDevolucion(1, EstadoDevolucion.BUEN_ESTADO, ""))

        assertEquals(EstadoEquipo.DISPONIBLE, estadoDe(3))
        val p = prestamosEnBase().single()
        assertFalse(p.activo)
        assertEquals(reloj, p.devolucion)
        assertEquals(EstadoDevolucion.BUEN_ESTADO, p.estadoDevolucion)
    }

    @Test
    fun `devolver con danos o faltantes manda a mantencion y exige observaciones`() = prueba {
        repo.registrarPrestamo(3, rojas, "7°A")

        assertEquals("Describe el problema en las observaciones", repo.registrarDevolucion(1, EstadoDevolucion.CON_DANOS, "  "))
        assertEquals(EstadoEquipo.PRESTADO, estadoDe(3)) // nada cambió

        assertNull(repo.registrarDevolucion(1, EstadoDevolucion.FALTAN_ACCESORIOS, " Falta el cargador "))
        assertEquals(EstadoEquipo.EN_MANTENCION, estadoDe(3))
        assertEquals("Falta el cargador", prestamosEnBase().single().observaciones)
    }

    @Test
    fun `no se puede devolver dos veces`() = prueba {
        repo.registrarPrestamo(3, rojas, "7°A")
        repo.registrarDevolucion(1, EstadoDevolucion.BUEN_ESTADO, "")

        assertEquals("El equipo ya fue devuelto", repo.registrarDevolucion(1, EstadoDevolucion.BUEN_ESTADO, ""))
        assertEquals("El préstamo no existe", repo.registrarDevolucion(50, EstadoDevolucion.BUEN_ESTADO, ""))
    }

    @Test
    fun `el docente confirma retiro y avisa devolucion solo de sus prestamos`() = prueba {
        repo.registrarPrestamo(3, rojas, "7°A")

        assertFalse(repo.confirmarRetiro(1, soto.id)) // ajeno
        assertTrue(repo.confirmarRetiro(1, rojas.id))
        assertTrue(repo.avisarDevolucion(1, rojas.id))
        val p = prestamosEnBase().single()
        assertTrue(p.retiroConfirmado && p.devolucionAvisada)
        assertTrue(p.activo) // avisar no devuelve: sigue en manos del CRA recibirlo

        repo.registrarDevolucion(1, EstadoDevolucion.BUEN_ESTADO, "")
        assertFalse(repo.avisarDevolucion(1, rojas.id)) // ya cerrado
    }

    @Test
    fun `el historial identifica al ultimo responsable`() = prueba {
        repo.registrarPrestamo(3, rojas, "7°A")
        repo.registrarDevolucion(1, EstadoDevolucion.CON_DANOS, "Pantalla trizada")
        InventarioRepository(db).cambiarEstado(3, EstadoEquipo.DISPONIBLE) // vuelve de mantención
        reloj = reloj.plusDays(1)
        repo.registrarPrestamo(3, soto, "8°B")

        val historial = prestamosEnBase()
        assertEquals("Prof. Soto", ultimoResponsable(3, historial)?.docente)
        assertNotNull(ultimoResponsable(3, historial))
        assertNull(ultimoResponsable(1, historial))
    }
}
