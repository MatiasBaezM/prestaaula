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

// Código para un equipo nuevo: el siguiente al mayor número usado del tipo (no "cantidad + 1",
// para no repetir un código existente si faltan números).
fun siguienteCodigo(equipos: List<Equipo>, tipo: TipoEquipo): String {
    val numero = (equipos.filter { it.tipo == tipo }
        .mapNotNull { it.codigo.substringAfter('-').toIntOrNull() }
        .maxOrNull() ?: 0) + 1
    return "%s-%03d".format(tipo.prefijo, numero)
}
