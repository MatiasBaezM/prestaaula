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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class InventarioTest {

    private val base = listOf(
        Equipo(1, "NB-001", TipoEquipo.NOTEBOOK, "Lenovo"),
        Equipo(2, "NB-002", TipoEquipo.NOTEBOOK, "HP"),
        Equipo(3, "PY-001", TipoEquipo.PROYECTOR, "Epson"),
    )

    @Test
    fun `agregar asigna id y codigo correlativo por tipo`() {
        val repo = InventarioRepository(base)
        val nuevo = repo.agregar(TipoEquipo.NOTEBOOK, "  Dell ")

        assertEquals(4, nuevo.id)
        assertEquals("NB-003", nuevo.codigo)
        assertEquals("Dell", nuevo.marca)
        assertEquals(EstadoEquipo.DISPONIBLE, nuevo.estado)
        assertEquals(4, repo.equipos.value.size)
    }

    @Test
    fun `primer equipo de un tipo nuevo parte en 001`() {
        val repo = InventarioRepository(base)
        assertEquals("TB-001", repo.agregar(TipoEquipo.TABLET, "Samsung").codigo)
    }

    @Test
    fun `el codigo nuevo nunca repite uno existente aunque falten numeros`() {
        // Solo existe NB-002: "cantidad + 1" daría NB-002 otra vez.
        val repo = InventarioRepository(listOf(Equipo(1, "NB-002", TipoEquipo.NOTEBOOK, "HP")))
        assertEquals("NB-003", repo.agregar(TipoEquipo.NOTEBOOK, "Dell").codigo)
        assertEquals(2, repo.equipos.value.map { it.codigo }.distinct().size)
    }

    @Test
    fun `cambiar estado actualiza solo ese equipo`() {
        val repo = InventarioRepository(base)
        repo.cambiarEstado(2, EstadoEquipo.PRESTADO)

        assertEquals(EstadoEquipo.PRESTADO, repo.equipos.value.first { it.id == 2 }.estado)
        assertEquals(EstadoEquipo.DISPONIBLE, repo.equipos.value.first { it.id == 1 }.estado)
    }

    private val hoy = LocalDate.of(2026, 10, 29)

    private fun vm(reservas: List<Reserva> = emptyList()): InventarioViewModel {
        val inventario = InventarioRepository(base)
        return InventarioViewModel(inventario, ReservaRepository(inventario, reservas), hoy)
    }

    @Test
    fun `viewmodel rechaza marca vacia y acepta una valida`() {
        val vm = vm()

        assertEquals("Ingresa la marca", vm.agregar(TipoEquipo.TABLET, "  "))
        assertEquals(3, vm.equipos.value.size)

        assertNull(vm.agregar(TipoEquipo.TABLET, "Apple"))
        assertEquals(4, vm.equipos.value.size)
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
        assertNull(vm.agregar(TipoEquipo.NOTEBOOK, "Dell", " nb-010 ", EstadoEquipo.EN_MANTENCION))

        val nuevo = vm.equipos.value.last()
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

        assertEquals("Ya existe un equipo con el código NB-001", vm.agregar(TipoEquipo.NOTEBOOK, "Dell", "NB-001"))
        assertEquals(3, vm.equipos.value.size)
    }

    @Test
    fun `un equipo nuevo no puede entrar como prestado`() {
        val vm = vm()
        assertEquals(
            "Un equipo nuevo no puede entrar como prestado",
            vm.agregar(TipoEquipo.NOTEBOOK, "Dell", "NB-004", EstadoEquipo.PRESTADO),
        )
        assertEquals(3, vm.equipos.value.size)
    }

    // --- Marcar como dañado ---

    @Test
    fun `marcar danado deja el equipo en mantencion y no se puede reservar`() {
        val inventario = InventarioRepository(base)
        val reservas = ReservaRepository(inventario, emptyList()) { hoy.atTime(8, 0) }
        val vm = InventarioViewModel(inventario, reservas, hoy)

        assertNull(vm.marcarDanado(1))
        assertEquals(EstadoEquipo.EN_MANTENCION, inventario.equipos.value.first { it.id == 1 }.estado)

        val resultado = reservas.reservar(1, 101, "Prof. Rojas", "7°A", hoy, setOf(2))
        assertTrue(resultado is ResultadoReserva.Error)
        assertTrue(reservas.reservas.value.isEmpty())
    }

    @Test
    fun `solo se marca danado un equipo disponible`() {
        val inventario = InventarioRepository(base)
        val vm = InventarioViewModel(inventario, ReservaRepository(inventario, emptyList()), hoy)
        val error = "Solo se puede marcar como dañado un equipo disponible"

        inventario.cambiarEstado(2, EstadoEquipo.PRESTADO)
        assertEquals(error, vm.marcarDanado(2)) // prestado: se marca al devolverlo
        assertNull(vm.marcarDanado(1))
        assertEquals(error, vm.marcarDanado(1)) // ya en mantención
        assertEquals(error, vm.marcarDanado(99)) // no existe
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
        assertEquals(2, vm.reservasPendientes(1))
        assertEquals(1, vm.reservasPendientes(2))
        assertEquals(0, vm.reservasPendientes(3))
    }
}
