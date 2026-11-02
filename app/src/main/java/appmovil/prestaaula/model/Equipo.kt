package appmovil.prestaaula.model

enum class TipoEquipo(val etiqueta: String, val prefijo: String) {
    NOTEBOOK("Notebook", "NB"),
    TABLET("Tablet", "TB"),
    PROYECTOR("Proyector", "PY"),
    CABLE_HDMI("Cable HDMI", "HD"),
}

enum class EstadoEquipo(val etiqueta: String) {
    DISPONIBLE("Disponible"),
    PRESTADO("Prestado"),
    EN_MANTENCION("En mantención"),
}

// codigo = número de identificación interno visible (ej. NB-001)
data class Equipo(
    val id: Int,
    val codigo: String,
    val tipo: TipoEquipo,
    val marca: String,
    val estado: EstadoEquipo = EstadoEquipo.DISPONIBLE,
)
