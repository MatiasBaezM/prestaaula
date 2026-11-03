package appmovil.prestaaula

import appmovil.prestaaula.data.InventarioRepository
import appmovil.prestaaula.data.ReservaRepository
import appmovil.prestaaula.model.Bloques
import appmovil.prestaaula.model.Equipo
import appmovil.prestaaula.model.EstadoEquipo
import appmovil.prestaaula.model.Ocupacion
import appmovil.prestaaula.model.Reserva
import appmovil.prestaaula.model.Rol
import appmovil.prestaaula.model.TipoEquipo
import appmovil.prestaaula.model.Usuario
import appmovil.prestaaula.model.ocupacionEnBloque
import appmovil.prestaaula.ui.disponibilidad.DisponibilidadViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class DisponibilidadTest : PruebaConBaseDeDatos() {

    private val hoy = LocalDate.of(2026, 10, 29) // jueves
    private val proyector = Equipo(1, "PY-001", TipoEquipo.PROYECTOR, "Epson")
    private val reserva = Reserva(1, equipoId = 1, docenteId = 101, docente = "Prof. Rojas", curso = "7°A", fecha = hoy, bloque = 2)

    @Test
    fun `hay 9 bloques de 45 minutos desde las 08_00`() {
        assertEquals(9, Bloques.todos.size)
        assertEquals("08:00–08:45", Bloques.todos.first().etiqueta)
        assertEquals("08:45–09:30", Bloques.porNumero(2)?.etiqueta)
        assertEquals("14:00–14:45", Bloques.todos.last().etiqueta)
    }

    @Test
    fun `deLaHora encuentra el bloque y respeta los bordes`() {
        assertEquals(1, Bloques.deLaHora(8 * 60)?.numero)
        assertEquals(1, Bloques.deLaHora(8 * 60 + 44)?.numero)
        assertEquals(2, Bloques.deLaHora(8 * 60 + 45)?.numero)
        assertNull(Bloques.deLaHora(7 * 60 + 59))
        assertEquals(9, Bloques.deLaHora(14 * 60 + 44)?.numero)
        assertNull(Bloques.deLaHora(14 * 60 + 45))
    }

    @Test
    fun `equipo libre cuando no hay reserva en ese bloque`() {
        assertEquals(Ocupacion.Libre, ocupacionEnBloque(proyector, listOf(reserva), hoy, 1, hoy))
        assertEquals(Ocupacion.Libre, ocupacionEnBloque(proyector, listOf(reserva), hoy.plusDays(1), 2, hoy))
    }

    @Test
    fun `reserva ocupa solo su equipo, fecha y bloque`() {
        assertEquals(Ocupacion.Reservado(reserva), ocupacionEnBloque(proyector, listOf(reserva), hoy, 2, hoy))
        val otro = proyector.copy(id = 2, codigo = "PY-002")
        assertEquals(Ocupacion.Libre, ocupacionEnBloque(otro, listOf(reserva), hoy, 2, hoy))
    }

    @Test
    fun `mantencion bloquea siempre y prestado solo hoy`() {
        val enMantencion = proyector.copy(estado = EstadoEquipo.EN_MANTENCION)
        assertEquals(Ocupacion.EnMantencion, ocupacionEnBloque(enMantencion, emptyList(), hoy.plusDays(3), 1, hoy))

        val prestado = proyector.copy(estado = EstadoEquipo.PRESTADO)
        assertEquals(Ocupacion.Prestado, ocupacionEnBloque(prestado, emptyList(), hoy, 1, hoy))
        assertEquals(Ocupacion.Libre, ocupacionEnBloque(prestado, emptyList(), hoy.plusDays(1), 1, hoy))
    }

    @Before
    fun equipos() = guardar(equipos = listOf(proyector))

    // 07:00 por defecto: ningún bloque de hoy ha terminado y el resultado no depende de la hora real.
    private fun vm(hoy: LocalDate, rol: Rol = Rol.DOCENTE, minutosAhora: Int = 7 * 60) =
        DisponibilidadViewModel(
            InventarioRepository(db),
            ReservaRepository(db) { hoy.atTime(8, 0) },
            Usuario(1, "Usuario", "u@x.cl", rol),
            hoy,
            minutosAhora,
        )

    @Test
    fun `viewmodel ofrece 7 dias habiles empezando hoy`() {
        val vm = vm(hoy)
        assertEquals(7, vm.dias.size)
        assertEquals(hoy, vm.dias.first())
        assertTrue(vm.dias.none { it.dayOfWeek == DayOfWeek.SATURDAY || it.dayOfWeek == DayOfWeek.SUNDAY })
        assertEquals(hoy, vm.fecha.value)
        assertEquals(setOf(1), vm.bloques.value)
    }

    @Test
    fun `si hoy es fin de semana parte el lunes`() {
        val vm = vm(LocalDate.of(2026, 10, 31)) // sábado
        assertEquals(DayOfWeek.MONDAY, vm.dias.first().dayOfWeek)
        assertEquals(vm.dias.first(), vm.fecha.value)
    }

    @Test
    fun `viewmodel guarda fecha, bloques y filtro`() {
        val vm = vm(hoy)
        vm.elegirFecha(hoy.plusDays(1))
        vm.alternarBloque(2)
        vm.filtrarPor(TipoEquipo.PROYECTOR)
        assertEquals(hoy.plusDays(1), vm.fecha.value)
        assertEquals(setOf(1, 2), vm.bloques.value)
        assertEquals(TipoEquipo.PROYECTOR, vm.filtroTipo.value)
    }

    @Test
    fun `solo el docente puede reservar desde la pantalla de disponibilidad`() {
        assertTrue(vm(hoy, Rol.DOCENTE).puedeReservar)
        for (rol in listOf(Rol.ENCARGADO_CRA, Rol.JEFE_UTP, Rol.JEFE_ADMIN_FINANZAS)) {
            assertFalse("${rol.name} no debería poder reservar", vm(hoy, rol).puedeReservar)
        }
    }

    @Test
    fun `un rol sin permiso no crea reservas aunque llame a reservar`() {
        val vm = vm(hoy, Rol.ENCARGADO_CRA)
        assertEquals("Tu rol no puede reservar equipos", resultado { vm.reservar(1, "7°A", it) })
        assertTrue(reservasEnBase().isEmpty())
    }

    @Test
    fun `el docente reserva el equipo libre del tramo elegido`() {
        val vm = vm(hoy)
        vm.alternarBloque(2)
        assertNull(resultado { vm.reservar(1, "7°A", it) })
        assertEquals(setOf(1, 2), reservasEnBase().map { it.bloque }.toSet())
        // La pantalla observa la tabla: la reserva nueva le llega sola.
        esperarQue { vm.reservas.value.size == 2 }
    }

    @Test
    fun `una reserva que choca muestra el error y no guarda nada`() {
        val vm = vm(hoy)
        assertNull(resultado { vm.reservar(1, "7°A", it) })
        assertEquals("El equipo ya está reservado en el bloque 1", resultado { vm.reservar(1, "8°B", it) })
        assertEquals(1, reservasEnBase().size)
    }
}
