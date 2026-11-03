package appmovil.prestaaula

import appmovil.prestaaula.data.InventarioRepository
import appmovil.prestaaula.data.ReservaRepository
import appmovil.prestaaula.data.ResultadoReserva
import appmovil.prestaaula.model.Equipo
import appmovil.prestaaula.model.EstadoEquipo
import appmovil.prestaaula.model.Reserva
import appmovil.prestaaula.model.TipoEquipo
import appmovil.prestaaula.ui.inventario.InventarioViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class InventarioTest : PruebaConBaseDeDatos() {

    private val base = listOf(
        Equipo(1, "NB-001", TipoEquipo.NOTEBOOK, "Lenovo"),
        Equipo(2, "NB-002", TipoEquipo.NOTEBOOK, "HP"),
        Equipo(3, "PY-001", TipoEquipo.PROYECTOR, "Epson"),
    )

    private fun repo(equipos: List<Equipo> = base): InventarioRepository {
        guardar(equipos = equipos)
        return InventarioRepository(db)
    }

    @Test
    fun `agregar asigna id y codigo correlativo por tipo`() = prueba {
        val nuevo = repo().agregar(TipoEquipo.NOTEBOOK, "  Dell ")!!

        assertEquals(4, nuevo.id) // lo asignó SQLite
        assertEquals("NB-003", nuevo.codigo)
        assertEquals("Dell", nuevo.marca)
        assertEquals(EstadoEquipo.DISPONIBLE, nuevo.estado)
        assertEquals(nuevo, equiposEnBase().last()) // quedó guardado tal cual
    }

    @Test
    fun `primer equipo de un tipo nuevo parte en 001`() = prueba {
        assertEquals("TB-001", repo().agregar(TipoEquipo.TABLET, "Samsung")!!.codigo)
    }

    @Test
    fun `el codigo nuevo nunca repite uno existente aunque falten numeros`() = prueba {
        // Solo existe NB-002: "cantidad + 1" daría NB-002 otra vez.
        val repo = repo(listOf(Equipo(1, "NB-002", TipoEquipo.NOTEBOOK, "HP")))
        assertEquals("NB-003", repo.agregar(TipoEquipo.NOTEBOOK, "Dell")!!.codigo)
        assertEquals(2, equiposEnBase().map { it.codigo }.distinct().size)
    }

    @Test
    fun `un codigo repetido no se guarda`() = prueba {
        assertNull(repo().agregar(TipoEquipo.NOTEBOOK, "Dell", "NB-001"))
        assertEquals(3, equiposEnBase().size)
    }

    @Test
    fun `cambiar estado actualiza solo ese equipo`() = prueba {
        repo().cambiarEstado(2, EstadoEquipo.PRESTADO)

        assertEquals(EstadoEquipo.PRESTADO, estadoDe(2))
        assertEquals(EstadoEquipo.DISPONIBLE, estadoDe(1))
    }

    @Test
    fun `solo un equipo en mantencion se puede marcar reparado`() = prueba {
        val repo = repo(base + Equipo(4, "NB-003", TipoEquipo.NOTEBOOK, "Acer", EstadoEquipo.EN_MANTENCION))

        assertFalse(repo.marcarReparado(1)) // ya disponible
        assertFalse(repo.marcarReparado(99)) // no existe
        assertTrue(repo.marcarReparado(4))
        assertEquals(EstadoEquipo.DISPONIBLE, estadoDe(4))
    }

    // --- ViewModel ---

    private val hoy = LocalDate.of(2026, 10, 29)

    private fun vm(reservas: List<Reserva> = emptyList()): InventarioViewModel {
        guardar(equipos = base, reservas = reservas)
        val vm = InventarioViewModel(InventarioRepository(db), ReservaRepository(db), hoy)
        esperarQue { vm.equipos.value.size == base.size } // primera lectura de Room
        return vm
    }

    @Test
    fun `viewmodel rechaza marca vacia y acepta una valida`() {
        val vm = vm()

        assertEquals("Ingresa la marca", resultado { vm.agregar(TipoEquipo.TABLET, "  ", alTerminar = it) })
        assertEquals(3, equiposEnBase().size)

        assertNull(resultado { vm.agregar(TipoEquipo.TABLET, "Apple", alTerminar = it) })
        assertEquals(4, equiposEnBase().size)
        esperarQue { vm.equipos.value.size == 4 } // la pantalla se entera sola del cambio
    }

    @Test
    fun `filtro por tipo se guarda y se limpia`() {
        val vm = vm()
        vm.filtrarPor(TipoEquipo.PROYECTOR)
        assertEquals(TipoEquipo.PROYECTOR, vm.filtroTipo.value)
        vm.filtrarPor(null)
        assertNull(vm.filtroTipo.value)
    }

    // --- Agregar con todos los campos ---

    @Test
    fun `el codigo sugerido es el siguiente libre del tipo`() {
        val vm = vm()
        assertEquals("NB-003", vm.codigoSugerido(TipoEquipo.NOTEBOOK))
        assertEquals("TB-001", vm.codigoSugerido(TipoEquipo.TABLET))
    }

    @Test
    fun `se puede agregar un equipo con codigo propio y estado inicial`() {
        val vm = vm()
        assertNull(resultado { vm.agregar(TipoEquipo.NOTEBOOK, "Dell", " nb-010 ", EstadoEquipo.EN_MANTENCION, it) })

        val nuevo = equiposEnBase().last()
        assertEquals("NB-010", nuevo.codigo)
        assertEquals("Dell", nuevo.marca)
        assertEquals(EstadoEquipo.EN_MANTENCION, nuevo.estado)
    }

    @Test
    fun `el codigo se valida por formato, tipo y repeticion`() {
        val vm = vm()
        assertEquals("Ingresa el número de identificación", vm.errorCodigo(TipoEquipo.NOTEBOOK, "  "))
        assertEquals("Usa el formato NB-001", vm.errorCodigo(TipoEquipo.NOTEBOOK, "NB-1"))
        assertEquals("Un notebook debe empezar con NB-", vm.errorCodigo(TipoEquipo.NOTEBOOK, "TB-001"))
        assertEquals("Ya existe un equipo con el código NB-001", vm.errorCodigo(TipoEquipo.NOTEBOOK, "nb-001"))
        assertNull(vm.errorCodigo(TipoEquipo.NOTEBOOK, "NB-004"))

        assertEquals(
            "Ya existe un equipo con el código NB-001",
            resultado { vm.agregar(TipoEquipo.NOTEBOOK, "Dell", "NB-001", alTerminar = it) },
        )
        assertEquals(3, equiposEnBase().size)
    }

    @Test
    fun `un equipo nuevo no puede entrar como prestado`() {
        val vm = vm()
        assertEquals(
            "Un equipo nuevo no puede entrar como prestado",
            resultado { vm.agregar(TipoEquipo.NOTEBOOK, "Dell", "NB-004", EstadoEquipo.PRESTADO, it) },
        )
        assertEquals(3, equiposEnBase().size)
    }

    // --- Marcar como dañado ---

    @Test
    fun `marcar danado deja el equipo en mantencion y no se puede reservar`() = prueba {
        val vm = vm()
        val reservas = ReservaRepository(db) { hoy.atTime(8, 0) }

        assertNull(resultado { vm.marcarDanado(1, it) })
        assertEquals(EstadoEquipo.EN_MANTENCION, estadoDe(1))

        val intento = reservas.reservar(1, 101, "Prof. Rojas", "7°A", hoy, setOf(2))
        assertTrue(intento is ResultadoReserva.Error)
        assertTrue(reservasEnBase().isEmpty())
    }

    @Test
    fun `solo se marca danado un equipo disponible`() = prueba {
        val vm = vm()
        val error = "Solo se puede marcar como dañado un equipo disponible"

        InventarioRepository(db).cambiarEstado(2, EstadoEquipo.PRESTADO)
        assertEquals(error, resultado { vm.marcarDanado(2, it) }) // prestado: se marca al devolverlo
        assertNull(resultado { vm.marcarDanado(1, it) })
        assertEquals(error, resultado { vm.marcarDanado(1, it) }) // ya en mantención
        assertEquals(error, resultado { vm.marcarDanado(99, it) }) // no existe
    }

    @Test
    fun `cuenta las reservas de hoy en adelante del equipo`() {
        fun reserva(id: Int, equipo: Int, fecha: LocalDate) =
            Reserva(id, equipo, 101, "Prof. Rojas", "7°A", fecha, bloque = id)
        val vm = vm(
            listOf(
                reserva(1, 1, hoy.minusDays(1)), // ya pasó
                reserva(2, 1, hoy),
                reserva(3, 1, hoy.plusDays(2)),
                reserva(4, 2, hoy), // otro equipo
            ),
        )
        esperarQue { vm.reservasPendientes(1) == 2 }
        assertEquals(1, vm.reservasPendientes(2))
        assertEquals(0, vm.reservasPendientes(3))
    }
}
