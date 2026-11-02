package appmovil.prestaaula.ui.inventario

import androidx.lifecycle.ViewModel
import appmovil.prestaaula.data.InventarioRepository
import appmovil.prestaaula.data.ReservaRepository
import appmovil.prestaaula.model.Equipo
import appmovil.prestaaula.model.EstadoEquipo
import appmovil.prestaaula.model.TipoEquipo
import appmovil.prestaaula.model.Validaciones
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

class InventarioViewModel(
    private val repositorio: InventarioRepository,
    private val reservas: ReservaRepository,
    private val hoy: LocalDate = LocalDate.now(),
) : ViewModel() {

    val equipos: StateFlow<List<Equipo>> = repositorio.equipos

    private val _filtroTipo = MutableStateFlow<TipoEquipo?>(null)
    val filtroTipo: StateFlow<TipoEquipo?> = _filtroTipo.asStateFlow()

    fun filtrarPor(tipo: TipoEquipo?) {
        _filtroTipo.value = tipo
    }

    fun codigoSugerido(tipo: TipoEquipo): String = repositorio.siguienteCodigo(tipo)

    // Error del campo código (formato, tipo y repetido), o null si es válido.
    fun errorCodigo(tipo: TipoEquipo, codigo: String): String? =
        Validaciones.codigo(codigo, tipo, equipos.value.map { it.codigo })

    // Devuelve el mensaje de error, o null si se agregó. Un equipo nuevo entra Disponible o En mantención.
    fun agregar(
        tipo: TipoEquipo,
        marca: String,
        codigo: String? = null,
        estado: EstadoEquipo = EstadoEquipo.DISPONIBLE,
    ): String? {
        Validaciones.marca(marca)?.let { return it }
        if (codigo != null) errorCodigo(tipo, codigo)?.let { return it }
        if (estado == EstadoEquipo.PRESTADO) return "Un equipo nuevo no puede entrar como prestado"
        repositorio.agregar(tipo, marca, codigo, estado)
        return null
    }

    // Reservas de hoy en adelante que quedarían sobre un equipo fuera de servicio.
    fun reservasPendientes(equipoId: Int): Int =
        reservas.reservas.value.count { it.equipoId == equipoId && !it.fecha.isBefore(hoy) }

    // Devuelve el mensaje de error, o null si quedó marcado como dañado.
    fun marcarDanado(equipoId: Int): String? =
        if (repositorio.marcarDanado(equipoId)) null
        else "Solo se puede marcar como dañado un equipo disponible"
}
