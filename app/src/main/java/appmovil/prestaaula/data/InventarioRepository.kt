package appmovil.prestaaula.data

import appmovil.prestaaula.model.Equipo
import appmovil.prestaaula.model.EstadoEquipo
import appmovil.prestaaula.model.TipoEquipo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// Fuente única del inventario en memoria; PrestaAulaApp la persiste en Room (SQLite)
// al detectar cambios, sin que préstamos y reservas dependan de la base de datos.
class InventarioRepository(inicial: List<Equipo> = equiposDemo()) {

    private val _equipos = MutableStateFlow(inicial)
    val equipos: StateFlow<List<Equipo>> = _equipos.asStateFlow()

    // Siguiente al mayor número usado (no "cantidad + 1"), para no repetir un código existente.
    fun siguienteCodigo(tipo: TipoEquipo): String = siguienteCodigo(_equipos.value, tipo)

    // `codigo` null = asignarlo automáticamente. El estado inicial es Disponible o En mantención.
    fun agregar(
        tipo: TipoEquipo,
        marca: String,
        codigo: String? = null,
        estado: EstadoEquipo = EstadoEquipo.DISPONIBLE,
    ): Equipo {
        lateinit var nuevo: Equipo
        _equipos.update { lista ->
            nuevo = Equipo(
                id = (lista.maxOfOrNull { it.id } ?: 0) + 1,
                codigo = codigo?.trim()?.uppercase() ?: siguienteCodigo(lista, tipo),
                tipo = tipo,
                marca = marca.trim(),
                estado = estado,
            )
            lista + nuevo
        }
        return nuevo
    }

    // El CRA saca de circulación un equipo dañado: deja de poder reservarse y prestarse hasta repararlo.
    // Solo aplica a un equipo Disponible; uno prestado se marca al devolverlo, con sus observaciones.
    fun marcarDanado(id: Int): Boolean {
        if (_equipos.value.none { it.id == id && it.estado == EstadoEquipo.DISPONIBLE }) return false
        cambiarEstado(id, EstadoEquipo.EN_MANTENCION)
        return true
    }

    // Cierra una mantención: solo aplica a equipos que están en mantención.
    fun marcarReparado(id: Int): Boolean {
        if (_equipos.value.none { it.id == id && it.estado == EstadoEquipo.EN_MANTENCION }) return false
        cambiarEstado(id, EstadoEquipo.DISPONIBLE)
        return true
    }

    fun cambiarEstado(id: Int, estado: EstadoEquipo) {
        _equipos.update { lista -> lista.map { if (it.id == id) it.copy(estado = estado) else it } }
    }
}

private fun siguienteCodigo(lista: List<Equipo>, tipo: TipoEquipo): String {
    val numero = (lista.filter { it.tipo == tipo }
        .mapNotNull { it.codigo.substringAfter('-').toIntOrNull() }
        .maxOrNull() ?: 0) + 1
    return "%s-%03d".format(tipo.prefijo, numero)
}

private fun equiposDemo(): List<Equipo> = listOf(
    Equipo(1, "NB-001", TipoEquipo.NOTEBOOK, "Lenovo"),
    Equipo(2, "NB-002", TipoEquipo.NOTEBOOK, "HP"),
    Equipo(3, "NB-003", TipoEquipo.NOTEBOOK, "Acer", EstadoEquipo.EN_MANTENCION),
    Equipo(4, "TB-001", TipoEquipo.TABLET, "Samsung"),
    Equipo(5, "TB-002", TipoEquipo.TABLET, "Samsung", EstadoEquipo.PRESTADO),
    Equipo(6, "PY-001", TipoEquipo.PROYECTOR, "Epson"),
    Equipo(7, "PY-002", TipoEquipo.PROYECTOR, "BenQ"),
    Equipo(8, "HD-001", TipoEquipo.CABLE_HDMI, "Genérico"),
    Equipo(9, "HD-002", TipoEquipo.CABLE_HDMI, "Genérico"),
)
