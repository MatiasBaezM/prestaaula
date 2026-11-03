package appmovil.prestaaula.ui.inventario

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import appmovil.prestaaula.data.InventarioRepository
import appmovil.prestaaula.data.ReservaRepository
import appmovil.prestaaula.model.Equipo
import appmovil.prestaaula.model.EstadoEquipo
import appmovil.prestaaula.model.Reserva
import appmovil.prestaaula.model.TipoEquipo
import appmovil.prestaaula.model.Validaciones
import appmovil.prestaaula.model.siguienteCodigo
import appmovil.prestaaula.ui.ejecutar
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

class InventarioViewModel(
    private val repositorio: InventarioRepository,
    reservas: ReservaRepository,
    private val hoy: LocalDate = LocalDate.now(),
) : ViewModel() {

    // stateIn convierte el Flow de Room en un StateFlow que la pantalla observa; empieza vacío
    // mientras llega la primera lectura y se actualiza solo con cada cambio de la tabla.
    val equipos: StateFlow<List<Equipo>> =
        repositorio.equipos.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val reservas: StateFlow<List<Reserva>> =
        reservas.reservas.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _filtroTipo = MutableStateFlow<TipoEquipo?>(null)
    val filtroTipo: StateFlow<TipoEquipo?> = _filtroTipo.asStateFlow()

    fun filtrarPor(tipo: TipoEquipo?) {
        _filtroTipo.value = tipo
    }

    fun codigoSugerido(tipo: TipoEquipo): String = siguienteCodigo(equipos.value, tipo)

    // Error del campo código (formato, tipo y repetido), o null si es válido.
    fun errorCodigo(tipo: TipoEquipo, codigo: String): String? =
        Validaciones.codigo(codigo, tipo, equipos.value.map { it.codigo })

    // Avisa el mensaje de error, o null si se agregó. Un equipo nuevo entra Disponible o En mantención.
    fun agregar(
        tipo: TipoEquipo,
        marca: String,
        codigo: String? = null,
        estado: EstadoEquipo = EstadoEquipo.DISPONIBLE,
        alTerminar: (String?) -> Unit = {},
    ): Job = ejecutar(alTerminar) {
        val error = Validaciones.marca(marca)
            ?: codigo?.let { errorCodigo(tipo, it) }
            ?: if (estado == EstadoEquipo.PRESTADO) "Un equipo nuevo no puede entrar como prestado" else null
        when {
            error != null -> error
            // Otro equipo tomó el código justo antes (lo revisa el repositorio dentro de la transacción).
            repositorio.agregar(tipo, marca, codigo, estado) == null -> "Ya existe un equipo con ese código"
            else -> null
        }
    }

    // Reservas de hoy en adelante que quedarían sobre un equipo fuera de servicio.
    fun reservasPendientes(equipoId: Int): Int =
        reservas.value.count { it.equipoId == equipoId && !it.fecha.isBefore(hoy) }

    // Avisa el mensaje de error, o null si quedó marcado como dañado.
    fun marcarDanado(equipoId: Int, alTerminar: (String?) -> Unit = {}): Job = ejecutar(alTerminar) {
        if (repositorio.marcarDanado(equipoId)) null
        else "Solo se puede marcar como dañado un equipo disponible"
    }
}
