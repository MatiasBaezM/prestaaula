package appmovil.prestaaula

import appmovil.prestaaula.data.InventarioRepository
import appmovil.prestaaula.model.Equipo
import appmovil.prestaaula.model.EstadoEquipo
import appmovil.prestaaula.model.TipoEquipo
import appmovil.prestaaula.ui.inventario.InventarioViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

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

    @Test
    fun `viewmodel rechaza marca vacia y acepta una valida`() {
        val vm = InventarioViewModel(InventarioRepository(base))

        assertEquals("Ingresa la marca", vm.agregar(TipoEquipo.TABLET, "  "))
        assertEquals(3, vm.equipos.value.size)

        assertNull(vm.agregar(TipoEquipo.TABLET, "Apple"))
        assertEquals(4, vm.equipos.value.size)
    }

    @Test
    fun `filtro por tipo se guarda y se limpia`() {
        val vm = InventarioViewModel(InventarioRepository(base))
        vm.filtrarPor(TipoEquipo.PROYECTOR)
        assertEquals(TipoEquipo.PROYECTOR, vm.filtroTipo.value)
        vm.filtrarPor(null)
        assertNull(vm.filtroTipo.value)
    }
}
