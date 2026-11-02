package appmovil.prestaaula.ui.reportes

import androidx.lifecycle.ViewModel
import appmovil.prestaaula.data.InventarioRepository
import appmovil.prestaaula.data.PrestamoRepository
import appmovil.prestaaula.model.Equipo
import appmovil.prestaaula.model.Periodo
import appmovil.prestaaula.model.Prestamo
import appmovil.prestaaula.model.Rol
import appmovil.prestaaula.model.Usuario
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

class ReporteUsoViewModel(
    inventario: InventarioRepository,
    prestamos: PrestamoRepository,
    val hoy: LocalDate = LocalDate.now(),
) : ViewModel() {
    val equipos: StateFlow<List<Equipo>> = inventario.equipos
    val registro: StateFlow<List<Prestamo>> = prestamos.prestamos

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
    val equipos: StateFlow<List<Equipo>> = inventario.equipos
    val registro: StateFlow<List<Prestamo>> = prestamos.prestamos

    // UTP y Finanzas solo consultan; cerrar una mantención es tarea del CRA.
    val puedeMarcarReparado: Boolean = usuario.rol == Rol.ENCARGADO_CRA

    fun marcarReparado(equipoId: Int): Boolean = puedeMarcarReparado && inventario.marcarReparado(equipoId)
}
