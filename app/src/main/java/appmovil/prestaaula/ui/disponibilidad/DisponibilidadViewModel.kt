package appmovil.prestaaula.ui.disponibilidad

import androidx.lifecycle.ViewModel
import appmovil.prestaaula.data.InventarioRepository
import appmovil.prestaaula.data.ReservaRepository
import appmovil.prestaaula.model.Bloques
import appmovil.prestaaula.model.Equipo
import appmovil.prestaaula.model.Reserva
import appmovil.prestaaula.model.TipoEquipo
import appmovil.prestaaula.model.diasHabiles
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

class DisponibilidadViewModel(
    inventario: InventarioRepository,
    reservas: ReservaRepository,
    val hoy: LocalDate = LocalDate.now(),
    bloqueInicial: Int = Bloques.actual(),
) : ViewModel() {

    val equipos: StateFlow<List<Equipo>> = inventario.equipos
    val reservas: StateFlow<List<Reserva>> = reservas.reservas

    val dias: List<LocalDate> = diasHabiles(hoy)

    private val _fecha = MutableStateFlow(dias.first())
    val fecha: StateFlow<LocalDate> = _fecha.asStateFlow()

    private val _bloque = MutableStateFlow(bloqueInicial)
    val bloque: StateFlow<Int> = _bloque.asStateFlow()

    private val _filtroTipo = MutableStateFlow<TipoEquipo?>(null)
    val filtroTipo: StateFlow<TipoEquipo?> = _filtroTipo.asStateFlow()

    fun elegirFecha(fecha: LocalDate) {
        _fecha.value = fecha
    }

    fun elegirBloque(numero: Int) {
        _bloque.value = numero
    }

    fun filtrarPor(tipo: TipoEquipo?) {
        _filtroTipo.value = tipo
    }
}
