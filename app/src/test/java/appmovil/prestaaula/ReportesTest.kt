package appmovil.prestaaula

import appmovil.prestaaula.data.InventarioRepository
import appmovil.prestaaula.model.Equipo
import appmovil.prestaaula.model.EstadoDevolucion
import appmovil.prestaaula.model.EstadoEquipo
import appmovil.prestaaula.model.Periodo
import appmovil.prestaaula.model.Prestamo
import appmovil.prestaaula.model.TipoEquipo
import appmovil.prestaaula.model.equiposConIncidencias
import appmovil.prestaaula.model.filtrarPorPeriodo
import appmovil.prestaaula.model.resumenUso
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class ReportesTest {

    private val hoy = LocalDate.of(2026, 10, 29)
    private val ahora = hoy.atTime(10, 0)

    private val equipos = listOf(
        Equipo(1, "NB-001", TipoEquipo.NOTEBOOK, "Lenovo"),
        Equipo(2, "NB-002", TipoEquipo.NOTEBOOK, "HP", EstadoEquipo.EN_MANTENCION),
        Equipo(3, "PY-001", TipoEquipo.PROYECTOR, "Epson"),
        Equipo(4, "TB-001", TipoEquipo.TABLET, "Samsung"),
    )

    private fun prestamo(
        id: Int, equipoId: Int, docente: String, diasAtras: Long = 0,
        estado: EstadoDevolucion? = EstadoDevolucion.BUEN_ESTADO, obs: String = "",
    ): Prestamo {
        val retiro: LocalDateTime = ahora.minusDays(diasAtras)
        return Prestamo(
            id, equipoId, docenteId = docente.length, docente = docente, curso = "7°A", retiro = retiro,
            devolucion = if (estado == null) null else retiro.plusHours(1),
            estadoDevolucion = estado, observaciones = obs,
        )
    }

    private val historial = listOf(
        prestamo(1, 1, "Rojas", 0),
        prestamo(2, 1, "Soto", 3),
        prestamo(3, 1, "Rojas", 10),
        prestamo(4, 2, "Soto", 1, EstadoDevolucion.CON_DANOS, "Pantalla trizada"),
        prestamo(5, 3, "Rojas", 8, EstadoDevolucion.FALTAN_ACCESORIOS, "Sin cable"),
        prestamo(6, 3, "Díaz", 40, EstadoDevolucion.CON_DANOS, "Lente rayado"),
        prestamo(7, 4, "Díaz", 0, estado = null), // en curso
    )

    @Test
    fun `el periodo cuenta hoy como el primer dia`() {
        assertEquals(listOf(1, 7), filtrarPorPeriodo(historial, Periodo.HOY, hoy).map { it.id })
        assertEquals(listOf(1, 2, 4, 7), filtrarPorPeriodo(historial, Periodo.SEMANA, hoy).map { it.id })
        assertEquals(listOf(1, 2, 3, 4, 5, 7), filtrarPorPeriodo(historial, Periodo.MES, hoy).map { it.id })
        assertEquals(7, filtrarPorPeriodo(historial, Periodo.TODO, hoy).size)
    }

    @Test
    fun `el resumen cuenta total, en curso y con problemas`() {
        val r = resumenUso(historial, equipos)
        assertEquals(7, r.total)
        assertEquals(1, r.enCurso)
        assertEquals(3, r.conProblemas)
    }

    @Test
    fun `los rankings van de mayor a menor con desempate alfabetico`() {
        val r = resumenUso(historial, equipos)

        assertEquals(listOf("Notebook" to 4, "Proyector" to 2, "Tablet" to 1), r.porTipo.map { it.etiqueta to it.valor })
        assertEquals(listOf("NB-001" to 3, "PY-001" to 2, "NB-002" to 1, "TB-001" to 1), r.masUsados.map { it.etiqueta to it.valor })
        assertEquals(listOf("Rojas" to 3, "Díaz" to 2, "Soto" to 2), r.porDocente.map { it.etiqueta to it.valor })
    }

    @Test
    fun `el ranking respeta el limite`() {
        assertEquals(2, resumenUso(historial, equipos, limite = 2).masUsados.size)
    }

    @Test
    fun `incidencias incluye equipos danados o en mantencion y ordena por reincidencia`() {
        val lista = equiposConIncidencias(historial, equipos)

        assertEquals(listOf("PY-001", "NB-002"), lista.map { it.equipo.codigo })
        val proyector = lista.first()
        assertEquals(2, proyector.incidencias.size)
        assertTrue(proyector.reincidente)
        assertEquals("Sin cable", proyector.incidencias.first().observaciones) // la más reciente primero
        assertEquals("Rojas", proyector.ultimoResponsable?.docente)
    }

    @Test
    fun `un equipo en mantencion sin historial igual aparece`() {
        val lista = equiposConIncidencias(emptyList(), equipos)
        assertEquals(listOf("NB-002"), lista.map { it.equipo.codigo })
        assertTrue(lista.single().enMantencion)
        assertFalse(lista.single().reincidente)
    }

    @Test
    fun `solo un equipo en mantencion se puede marcar reparado`() {
        val inventario = InventarioRepository(equipos)

        assertFalse(inventario.marcarReparado(1)) // ya disponible
        assertFalse(inventario.marcarReparado(99)) // no existe
        assertTrue(inventario.marcarReparado(2))
        assertEquals(EstadoEquipo.DISPONIBLE, inventario.equipos.value.first { it.id == 2 }.estado)
    }
}
