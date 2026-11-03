package appmovil.prestaaula.ui.prestamos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import appmovil.prestaaula.ui.ejecutar
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

// Encargado CRA: entrega un equipo a un docente.
class RegistrarPrestamoViewModel(
    inventario: InventarioRepository,
    reservas: ReservaRepository,
    private val prestamos: PrestamoRepository,
    val hoy: LocalDate = LocalDate.now(),
) : ViewModel() {
    val equipos: StateFlow<List<Equipo>> =
        inventario.equipos.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val reservas: StateFlow<List<Reserva>> =
        reservas.reservas.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val docentes: List<Usuario> = Docentes.demo

    // Avisa el mensaje de error, o null si quedó registrado.
    fun registrar(equipoId: Int, docente: Usuario, curso: String, alTerminar: (String?) -> Unit = {}): Job =
        ejecutar(alTerminar) {
            Validaciones.curso(curso) ?: prestamos.registrarPrestamo(equipoId, docente, curso)
        }
}

// Encargado CRA: recibe un equipo y anota su estado.
class RegistrarDevolucionViewModel(
    inventario: InventarioRepository,
    private val prestamos: PrestamoRepository,
    val hoy: LocalDate = LocalDate.now(),
) : ViewModel() {
    val equipos: StateFlow<List<Equipo>> =
        inventario.equipos.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val registro: StateFlow<List<Prestamo>> =
        prestamos.prestamos.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Avisa el mensaje de error, o null si quedó registrada.
    fun recibir(
        prestamoId: Int,
        estado: EstadoDevolucion,
        observaciones: String,
        foto: String? = null,
        alTerminar: (String?) -> Unit = {},
    ): Job = ejecutar(alTerminar) {
        Validaciones.observaciones(estado, observaciones)
            ?: prestamos.registrarDevolucion(prestamoId, estado, observaciones, foto)
    }
}

// Docente: confirma su retiro y avisa su devolución.
class RetiroDevolucionViewModel(
    inventario: InventarioRepository,
    reservas: ReservaRepository,
    private val prestamos: PrestamoRepository,
    private val usuario: Usuario,
    val hoy: LocalDate = LocalDate.now(),
) : ViewModel() {
    val equipos: StateFlow<List<Equipo>> =
        inventario.equipos.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val reservas: StateFlow<List<Reserva>> =
        reservas.reservas.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val registro: StateFlow<List<Prestamo>> =
        prestamos.prestamos.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun misPrestamos(todos: List<Prestamo>): List<Prestamo> =
        todos.filter { it.docenteId == usuario.id && it.activo }

    fun misReservasDeHoy(todas: List<Reserva>): List<Reserva> =
        todas.filter { it.docenteId == usuario.id && it.fecha == hoy }

    fun confirmarRetiro(id: Int, alTerminar: (String?) -> Unit = {}): Job = ejecutar(alTerminar) {
        if (prestamos.confirmarRetiro(id, usuario.id)) null else "No se pudo confirmar el retiro"
    }

    fun avisarDevolucion(id: Int, alTerminar: (String?) -> Unit = {}): Job = ejecutar(alTerminar) {
        if (prestamos.avisarDevolucion(id, usuario.id)) null else "No se pudo avisar la devolución"
    }
}
