package appmovil.prestaaula

import appmovil.prestaaula.data.InventarioRepository
import appmovil.prestaaula.data.ReservaRepository
import appmovil.prestaaula.data.ResultadoReserva
import appmovil.prestaaula.model.Equipo
import appmovil.prestaaula.model.EstadoEquipo
import appmovil.prestaaula.model.Reserva
import appmovil.prestaaula.model.Rol
import appmovil.prestaaula.model.TipoEquipo
import appmovil.prestaaula.model.Usuario
import appmovil.prestaaula.model.agruparReservas
import appmovil.prestaaula.ui.reservas.ReservarViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class ReservaTest {

    // Jueves 29-oct-2026, 08:00 (bloque 1 en curso).
    private val ahora = LocalDateTime.of(2026, 10, 29, 8, 0)
    private val hoy = ahora.toLocalDate()
    private val manana = hoy.plusDays(1)

    private val inventario = InventarioRepository(
        listOf(
            Equipo(1, "PY-001", TipoEquipo.PROYECTOR, "Epson"),
            Equipo(2, "NB-001", TipoEquipo.NOTEBOOK, "Lenovo", EstadoEquipo.EN_MANTENCION),
            Equipo(3, "TB-001", TipoEquipo.TABLET, "Samsung", EstadoEquipo.PRESTADO),
        ),
    )

    private fun repo(inicial: List<Reserva> = emptyList()) =
        ReservaRepository(inventario, inicial) { ahora }

    private fun ReservaRepository.reservar(
        equipoId: Int = 1, docenteId: Int = 1, curso: String = "7°A",
        fecha: LocalDate = manana, bloques: Set<Int> = setOf(2),
    ) = reservar(equipoId, docenteId, "Docente $docenteId", curso, fecha, bloques)

    private fun ResultadoReserva.error() = (this as ResultadoReserva.Error).mensaje

    @Test
    fun `reserva de varios bloques seguidos crea una reserva por bloque`() {
        val r = repo()
        val res = r.reservar(bloques = setOf(2, 3, 4)) as ResultadoReserva.Exito

        assertEquals(listOf(2, 3, 4), res.reservas.map { it.bloque })
        assertEquals(3, r.reservas.value.size)
    }

    @Test
    fun `no permite choque en el mismo equipo, dia y bloque`() {
        val r = repo()
        r.reservar(docenteId = 1, bloques = setOf(2, 3))

        assertEquals("El equipo ya está reservado en el bloque 3", r.reservar(docenteId = 2, bloques = setOf(3, 4)).error())
        assertEquals(2, r.reservas.value.size) // nada se guardó
    }

    @Test
    fun `mismo bloque en otro dia o en otro equipo no choca`() {
        val r = repo()
        r.reservar(bloques = setOf(2))
        assertTrue(r.reservar(fecha = manana.plusDays(1), bloques = setOf(2)) is ResultadoReserva.Exito)
        assertTrue(r.reservar(equipoId = 3, fecha = manana, bloques = setOf(2)) is ResultadoReserva.Exito)
    }

    @Test
    fun `bloques no seguidos o vacios se rechazan`() {
        val r = repo()
        assertEquals("Los bloques deben ser seguidos", r.reservar(bloques = setOf(1, 3)).error())
        assertEquals("Elige al menos un bloque", r.reservar(bloques = emptySet()).error())
        assertEquals("Elige al menos un bloque", r.reservar(bloques = setOf(10)).error())
    }

    @Test
    fun `curso vacio se rechaza`() {
        assertEquals("Ingresa el curso", repo().reservar(curso = "  ").error())
    }

    @Test
    fun `equipo en mantencion o prestado hoy no se puede reservar`() {
        val r = repo()
        assertEquals("El equipo está en mantención", r.reservar(equipoId = 2).error())
        assertEquals("El equipo está prestado en este momento", r.reservar(equipoId = 3, fecha = hoy, bloques = setOf(2)).error())
        assertTrue(r.reservar(equipoId = 3, fecha = manana) is ResultadoReserva.Exito) // mañana ya volvió
    }

    @Test
    fun `no se reserva en el pasado ni en bloques ya terminados`() {
        val tarde = LocalDateTime.of(2026, 10, 29, 10, 0) // bloque 3 en curso
        val r = ReservaRepository(inventario, emptyList()) { tarde }

        assertEquals("No puedes reservar en una fecha pasada", r.reservar(fecha = hoy.minusDays(1)).error())
        assertEquals("Uno de los bloques elegidos ya terminó", r.reservar(fecha = hoy, bloques = setOf(1, 2)).error())
        assertTrue(r.reservar(fecha = hoy, bloques = setOf(3, 4)) is ResultadoReserva.Exito) // el en curso sí
    }

    @Test
    fun `solo el dueno puede cancelar y se libera el horario`() {
        val r = repo()
        val ids = (r.reservar(docenteId = 1, bloques = setOf(2, 3)) as ResultadoReserva.Exito).reservas.map { it.id }

        assertFalse(r.cancelar(ids, docenteId = 2))
        assertEquals(2, r.reservas.value.size)

        assertTrue(r.cancelar(ids, docenteId = 1))
        assertTrue(r.reservas.value.isEmpty())
        assertTrue(r.reservar(docenteId = 2, bloques = setOf(2, 3)) is ResultadoReserva.Exito)
    }

    @Test
    fun `agrupa bloques seguidos y separa los que no lo son`() {
        val base = Reserva(0, 1, 1, "D", "7°A", manana, 0)
        val grupos = agruparReservas(
            listOf(
                base.copy(id = 1, bloque = 2), base.copy(id = 2, bloque = 3),
                base.copy(id = 3, bloque = 6),
                base.copy(id = 4, bloque = 1, curso = "8°B"),
            ),
        )
        assertEquals(3, grupos.size)
        val doble = grupos.first { it.curso == "7°A" && it.desde == 2 }
        assertEquals(3, doble.hasta)
        assertEquals(listOf(1, 2), doble.ids)
    }

    @Test
    fun `viewmodel alterna bloques manteniendo un tramo seguido`() {
        val vm = vmReservar(minutosAhora = 7 * 60)
        vm.alternarBloque(3); assertEquals(setOf(3), vm.bloques.value) // no contiguo a 1: reinicia
        vm.alternarBloque(4); assertEquals(setOf(3, 4), vm.bloques.value) // extiende
        vm.alternarBloque(2); assertEquals(setOf(2, 3, 4), vm.bloques.value)
        vm.alternarBloque(4); assertEquals(setOf(2, 3), vm.bloques.value) // acorta por el extremo
        vm.alternarBloque(7); assertEquals(setOf(7), vm.bloques.value) // no contiguo: reinicia
        vm.alternarBloque(7); assertEquals(setOf(7), vm.bloques.value) // nunca queda vacío
    }

    @Test
    fun `tocar un bloque del medio deja solo ese bloque`() {
        val vm = vmReservar(minutosAhora = 7 * 60)
        vm.alternarBloque(2); vm.alternarBloque(3); vm.alternarBloque(4)
        vm.alternarBloque(3)
        assertEquals(setOf(3), vm.bloques.value)
    }

    // Hora fija: el resultado no depende de a qué hora se corran los tests.
    private fun vmReservar(minutosAhora: Int) = ReservarViewModel(
        inventario, repo(), Usuario(1, "Docente", "d@x.cl", Rol.DOCENTE), hoy, minutosAhora,
    )

    @Test
    fun `a media jornada parte hoy en el bloque en curso y no ofrece los terminados`() {
        val vm = vmReservar(minutosAhora = 10 * 60) // 10:00 → bloque 3 (09:30–10:15) en curso

        assertEquals(hoy, vm.fecha.value)
        assertEquals(setOf(3), vm.bloques.value)
        assertTrue(vm.bloqueTerminado(hoy, 2))
        assertFalse(vm.bloqueTerminado(hoy, 3))
        assertFalse(vm.bloqueTerminado(manana, 1)) // mañana todo está disponible

        vm.alternarBloque(2) // terminado: se ignora
        assertEquals(setOf(3), vm.bloques.value)
    }

    @Test
    fun `fuera de horario parte en el siguiente dia habil`() {
        val vm = vmReservar(minutosAhora = 16 * 60)

        assertEquals(manana, vm.fecha.value)
        assertEquals(setOf(1), vm.bloques.value)
    }

    @Test
    fun `volver a hoy fuera de horario deja sin bloques en vez de ofrecer uno pasado`() {
        val vm = vmReservar(minutosAhora = 16 * 60)
        vm.elegirFecha(hoy)
        assertTrue(vm.bloques.value.isEmpty())

        vm.elegirFecha(manana)
        assertEquals(setOf(1), vm.bloques.value)
    }

    @Test
    fun `al cambiar a hoy se descartan los bloques ya terminados del tramo`() {
        val vm = vmReservar(minutosAhora = 10 * 60)
        vm.elegirFecha(manana)
        vm.alternarBloque(2); vm.alternarBloque(1) // tramo 1..3 mañana
        assertEquals(setOf(1, 2, 3), vm.bloques.value)

        vm.elegirFecha(hoy)
        assertEquals(setOf(3), vm.bloques.value)
    }
}
