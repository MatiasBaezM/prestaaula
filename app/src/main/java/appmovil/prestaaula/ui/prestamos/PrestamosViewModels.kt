package appmovil.prestaaula.ui.prestamos

import androidx.lifecycle.ViewModel
import appmovil.prestaaula.data.Docentes
import appmovil.prestaaula.data.InventarioRepository
import appmovil.prestaaula.data.PrestamoRepository
import appmovil.prestaaula.data.ReservaRepository
import appmovil.prestaaula.model.Equipo
import appmovil.prestaaula.model.EstadoDevolucion
import appmovil.prestaaula.model.Prestamo
import appmovil.prestaaula.model.Reserva
import appmovil.prestaaula.model.Usuario
import appmovil.prestaaula.model.Validaciones
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate

// Encargado CRA: entrega un equipo a un docente.
class RegistrarPrestamoViewModel(
    inventario: InventarioRepository,
    reservas: ReservaRepository,
    private val prestamos: PrestamoRepository,
    val hoy: LocalDate = LocalDate.now(),
) : ViewModel() {
    val equipos: StateFlow<List<Equipo>> = inventario.equipos
    val reservas: StateFlow<List<Reserva>> = reservas.reservas
    val docentes: List<Usuario> = Docentes.demo

    fun registrar(equipoId: Int, docente: Usuario, curso: String): String? =
        Validaciones.curso(curso) ?: prestamos.registrarPrestamo(equipoId, docente, curso)
}

// Encargado CRA: recibe un equipo y anota su estado.
class RegistrarDevolucionViewModel(
    inventario: InventarioRepository,
    private val prestamos: PrestamoRepository,
    val hoy: LocalDate = LocalDate.now(),
) : ViewModel() {
    val equipos: StateFlow<List<Equipo>> = inventario.equipos
    val registro: StateFlow<List<Prestamo>> = prestamos.prestamos

    fun recibir(prestamoId: Int, estado: EstadoDevolucion, observaciones: String, foto: String? = null): String? =
        Validaciones.observaciones(estado, observaciones)
            ?: prestamos.registrarDevolucion(prestamoId, estado, observaciones, foto)
}

// Docente: confirma su retiro y avisa su devolución.
class RetiroDevolucionViewModel(
    inventario: InventarioRepository,
    reservas: ReservaRepository,
    private val prestamos: PrestamoRepository,
    private val usuario: Usuario,
    val hoy: LocalDate = LocalDate.now(),
) : ViewModel() {
    val equipos: StateFlow<List<Equipo>> = inventario.equipos
    val reservas: StateFlow<List<Reserva>> = reservas.reservas
    val registro: StateFlow<List<Prestamo>> = prestamos.prestamos

    fun misPrestamos(todos: List<Prestamo>): List<Prestamo> =
        todos.filter { it.docenteId == usuario.id && it.activo }

    fun misReservasDeHoy(todas: List<Reserva>): List<Reserva> =
        todas.filter { it.docenteId == usuario.id && it.fecha == hoy }

    fun confirmarRetiro(id: Int) = prestamos.confirmarRetiro(id, usuario.id)
    fun avisarDevolucion(id: Int) = prestamos.avisarDevolucion(id, usuario.id)
}
