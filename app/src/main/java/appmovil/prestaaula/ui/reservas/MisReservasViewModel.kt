package appmovil.prestaaula.ui.reservas

import androidx.lifecycle.ViewModel
import appmovil.prestaaula.data.InventarioRepository
import appmovil.prestaaula.data.ReservaRepository
import appmovil.prestaaula.model.Equipo
import appmovil.prestaaula.model.GrupoReserva
import appmovil.prestaaula.model.Reserva
import appmovil.prestaaula.model.Usuario
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate

class MisReservasViewModel(
    inventario: InventarioRepository,
    private val repositorio: ReservaRepository,
    private val usuario: Usuario,
    val hoy: LocalDate = LocalDate.now(),
) : ViewModel() {

    val equipos: StateFlow<List<Equipo>> = inventario.equipos
    val reservas: StateFlow<List<Reserva>> = repositorio.reservas

    // Solo las del usuario y de hoy en adelante.
    fun propias(todas: List<Reserva>): List<Reserva> =
        todas.filter { it.docenteId == usuario.id && !it.fecha.isBefore(hoy) }

    fun cancelar(grupo: GrupoReserva): Boolean = repositorio.cancelar(grupo.ids, usuario.id)
}
