package appmovil.prestaaula.ui.disponibilidad

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import appmovil.prestaaula.data.InventarioRepository
import appmovil.prestaaula.data.ReservaRepository
import appmovil.prestaaula.data.ResultadoReserva
import appmovil.prestaaula.model.Bloques
import appmovil.prestaaula.model.Equipo
import appmovil.prestaaula.model.Reserva
import appmovil.prestaaula.model.TipoEquipo
import appmovil.prestaaula.model.Usuario
import appmovil.prestaaula.model.Validaciones
import appmovil.prestaaula.model.diasHabiles
import appmovil.prestaaula.ui.ejecutar
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.LocalTime

// Una sola pantalla para consultar y reservar: todos ven qué equipos están libres en un tramo
// de bloques; solo el rol con `puedeReservar` (docente) puede reservar un equipo libre.
class DisponibilidadViewModel(
    inventario: InventarioRepository,
    private val repositorio: ReservaRepository,
    private val usuario: Usuario,
    val hoy: LocalDate = LocalDate.now(),
    minutosAhora: Int = LocalTime.now().let { it.hour * 60 + it.minute },
) : ViewModel() {

    // Equipos y reservas desde Room: la tabla de disponibilidad se recalcula sola con cada cambio.
    val equipos: StateFlow<List<Equipo>> =
        inventario.equipos.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val reservas: StateFlow<List<Reserva>> =
        repositorio.reservas.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val dias: List<LocalDate> = diasHabiles(hoy)
    val puedeReservar: Boolean = usuario.rol.puedeReservar

    // Bloques de hoy que ya terminaron: no se ofrecen (siempre son un prefijo 1..k).
    private val terminadosHoy: Set<Int> =
        Bloques.todos.filter { it.finMin <= minutosAhora }.map { it.numero }.toSet()

    fun bloqueTerminado(fecha: LocalDate, numero: Int): Boolean = fecha == hoy && numero in terminadosHoy

    private fun primerBloqueDisponible(fecha: LocalDate): Int? =
        Bloques.todos.firstOrNull { !bloqueTerminado(fecha, it.numero) }?.numero

    // Si la jornada de hoy ya terminó, se parte en el siguiente día hábil.
    private val _fecha = MutableStateFlow(dias.firstOrNull { primerBloqueDisponible(it) != null } ?: dias.first())
    val fecha: StateFlow<LocalDate> = _fecha.asStateFlow()

    // Un tramo de bloques seguidos; vacío solo si el día elegido ya no tiene bloques.
    private val _bloques = MutableStateFlow(setOfNotNull(primerBloqueDisponible(_fecha.value)))
    val bloques: StateFlow<Set<Int>> = _bloques.asStateFlow()

    private val _filtroTipo = MutableStateFlow<TipoEquipo?>(null)
    val filtroTipo: StateFlow<TipoEquipo?> = _filtroTipo.asStateFlow()

    fun elegirFecha(fecha: LocalDate) {
        _fecha.value = fecha
        // Quitar un prefijo de bloques terminados deja el tramo igual de seguido.
        val vigentes = _bloques.value.filterNot { bloqueTerminado(fecha, it) }.toSet()
        _bloques.value = vigentes.ifEmpty { setOfNotNull(primerBloqueDisponible(fecha)) }
    }

    fun filtrarPor(tipo: TipoEquipo?) {
        _filtroTipo.value = tipo
    }

    // Extiende o acorta el tramo por sus extremos; un bloque no contiguo empieza uno nuevo.
    fun alternarBloque(n: Int) {
        if (bloqueTerminado(_fecha.value, n)) return
        val actual = _bloques.value
        if (actual.isEmpty()) {
            _bloques.value = setOf(n)
            return
        }
        val min = actual.min()
        val max = actual.max()
        _bloques.value = when {
            n in actual && actual.size == 1 -> actual
            n in actual && (n == min || n == max) -> actual - n
            n == min - 1 || n == max + 1 -> actual + n
            else -> setOf(n)
        }
    }

    // Avisa el mensaje de error, o null si la reserva quedó guardada.
    fun reservar(equipoId: Int, curso: String, alTerminar: (String?) -> Unit = {}): Job {
        // Fecha y tramo se leen al tocar "Confirmar", no cuando termine de guardarse.
        val fecha = _fecha.value
        val bloques = _bloques.value
        return ejecutar(alTerminar) {
            if (!puedeReservar) return@ejecutar "Tu rol no puede reservar equipos"
            Validaciones.curso(curso)?.let { return@ejecutar it }
            when (val r = repositorio.reservar(equipoId, usuario.id, usuario.nombre, curso, fecha, bloques)) {
                is ResultadoReserva.Exito -> null
                is ResultadoReserva.Error -> r.mensaje
            }
        }
    }
}
