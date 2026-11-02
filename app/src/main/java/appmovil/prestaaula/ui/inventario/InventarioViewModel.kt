package appmovil.prestaaula.ui.inventario

import androidx.lifecycle.ViewModel
import appmovil.prestaaula.data.InventarioRepository
import appmovil.prestaaula.model.Equipo
import appmovil.prestaaula.model.TipoEquipo
import appmovil.prestaaula.model.Validaciones
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class InventarioViewModel(
    private val repositorio: InventarioRepository,
) : ViewModel() {

    val equipos: StateFlow<List<Equipo>> = repositorio.equipos

    private val _filtroTipo = MutableStateFlow<TipoEquipo?>(null)
    val filtroTipo: StateFlow<TipoEquipo?> = _filtroTipo.asStateFlow()

    fun filtrarPor(tipo: TipoEquipo?) {
        _filtroTipo.value = tipo
    }

    // Devuelve el mensaje de error, o null si se agregó.
    fun agregar(tipo: TipoEquipo, marca: String): String? {
        Validaciones.marca(marca)?.let { return it }
        repositorio.agregar(tipo, marca)
        return null
    }
}
