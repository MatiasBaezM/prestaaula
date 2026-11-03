package appmovil.prestaaula.ui.reportes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import appmovil.prestaaula.data.InventarioRepository
import appmovil.prestaaula.data.PrestamoRepository
import appmovil.prestaaula.model.Equipo
import appmovil.prestaaula.model.Periodo
import appmovil.prestaaula.model.Prestamo
import appmovil.prestaaula.model.Rol
import appmovil.prestaaula.model.Usuario
import appmovil.prestaaula.ui.ejecutar
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

class ReporteUsoViewModel(
    inventario: InventarioRepository,
    prestamos: PrestamoRepository,
    val hoy: LocalDate = LocalDate.now(),
) : ViewModel() {
    val equipos: StateFlow<List<Equipo>> =
        inventario.equipos.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val registro: StateFlow<List<Prestamo>> =
        prestamos.prestamos.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _periodo = MutableStateFlow(Periodo.SEMANA)
    val periodo: StateFlow<Periodo> = _periodo.asStateFlow()

    fun elegirPeriodo(periodo: Periodo) {
        _periodo.value = periodo
    }
}

class IncidenciasViewModel(
    private val inventario: InventarioRepository,
    prestamos: PrestamoRepository,
    usuario: Usuario,
    val hoy: LocalDate = LocalDate.now(),
) : ViewModel() {
    val equipos: StateFlow<List<Equipo>> =
        inventario.equipos.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val registro: StateFlow<List<Prestamo>> =
        prestamos.prestamos.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // UTP y Finanzas solo consultan; cerrar una mantención es tarea del CRA.
    val puedeMarcarReparado: Boolean = usuario.rol == Rol.ENCARGADO_CRA

    fun marcarReparado(equipoId: Int, alTerminar: (String?) -> Unit = {}): Job = ejecutar(alTerminar) {
        when {
            !puedeMarcarReparado -> "Solo el Encargado CRA puede cerrar una mantención"
            inventario.marcarReparado(equipoId) -> null
            else -> "El equipo no está en mantención"
        }
    }
}
