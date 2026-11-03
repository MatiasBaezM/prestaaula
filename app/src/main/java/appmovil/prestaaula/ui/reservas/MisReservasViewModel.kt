package appmovil.prestaaula.ui.reservas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import appmovil.prestaaula.data.InventarioRepository
import appmovil.prestaaula.data.ReservaRepository
import appmovil.prestaaula.model.Equipo
import appmovil.prestaaula.model.GrupoReserva
import appmovil.prestaaula.model.Reserva
import appmovil.prestaaula.model.Usuario
import appmovil.prestaaula.ui.ejecutar
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

class MisReservasViewModel(
    inventario: InventarioRepository,
    private val repositorio: ReservaRepository,
    private val usuario: Usuario,
    val hoy: LocalDate = LocalDate.now(),
) : ViewModel() {

    val equipos: StateFlow<List<Equipo>> =
        inventario.equipos.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val reservas: StateFlow<List<Reserva>> =
        repositorio.reservas.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Solo las del usuario y de hoy en adelante.
    fun propias(todas: List<Reserva>): List<Reserva> =
        todas.filter { it.docenteId == usuario.id && !it.fecha.isBefore(hoy) }

    // Al borrarse de la tabla, la tarjeta desaparece sola de la lista (la pantalla observa la base).
    fun cancelar(grupo: GrupoReserva, alTerminar: (String?) -> Unit = {}): Job = ejecutar(alTerminar) {
        if (repositorio.cancelar(grupo.ids, usuario.id)) null else "No se pudo cancelar la reserva"
    }
}
