package appmovil.prestaaula.model

import java.time.LocalDate

enum class Periodo(val etiqueta: String, val dias: Int?) {
    HOY("Hoy", 1),
    SEMANA("7 días", 7),
    MES("30 días", 30),
    TODO("Todo", null),
}

// Préstamos retirados dentro del período (hoy cuenta como el día 1).
fun filtrarPorPeriodo(prestamos: List<Prestamo>, periodo: Periodo, hoy: LocalDate): List<Prestamo> {
    val dias = periodo.dias ?: return prestamos
    val desde = hoy.minusDays((dias - 1).toLong())
    return prestamos.filter { !it.retiro.toLocalDate().isBefore(desde) }
}

data class Conteo(val etiqueta: String, val valor: Int)

data class ResumenUso(
    val total: Int,
    val enCurso: Int,
    val conProblemas: Int,
    val porTipo: List<Conteo>,
    val masUsados: List<Conteo>,
    val porDocente: List<Conteo>,
)

private fun Prestamo.huboProblema() =
    estadoDevolucion != null && estadoDevolucion != EstadoDevolucion.BUEN_ESTADO

fun resumenUso(prestamos: List<Prestamo>, equipos: List<Equipo>, limite: Int = 5): ResumenUso {
    val porId = equipos.associateBy { it.id }
    // Mayor a menor; a igual valor, orden alfabético para que sea estable.
    fun ranking(clave: (Prestamo) -> String?): List<Conteo> =
        prestamos.mapNotNull(clave)
            .groupingBy { it }.eachCount()
            .map { Conteo(it.key, it.value) }
            .sortedWith(compareByDescending<Conteo> { it.valor }.thenBy { it.etiqueta })

    return ResumenUso(
        total = prestamos.size,
        enCurso = prestamos.count { it.activo },
        conProblemas = prestamos.count { it.huboProblema() },
        porTipo = ranking { porId[it.equipoId]?.tipo?.etiqueta },
        masUsados = ranking { porId[it.equipoId]?.codigo }.take(limite),
        porDocente = ranking { it.docente }.take(limite),
    )
}

// Un equipo con devoluciones dañadas/incompletas y/o hoy en mantención.
data class IncidenciaEquipo(
    val equipo: Equipo,
    val incidencias: List<Prestamo>, // la más reciente primero
    val ultimoResponsable: Prestamo?,
) {
    val enMantencion: Boolean get() = equipo.estado == EstadoEquipo.EN_MANTENCION
    val reincidente: Boolean get() = incidencias.size >= 2
}

fun equiposConIncidencias(prestamos: List<Prestamo>, equipos: List<Equipo>): List<IncidenciaEquipo> =
    equipos
        .map { e ->
            val propios = prestamos.filter { it.equipoId == e.id }
            IncidenciaEquipo(
                equipo = e,
                incidencias = propios.filter { it.huboProblema() }.sortedByDescending { it.retiro },
                ultimoResponsable = propios.maxByOrNull { it.retiro },
            )
        }
        .filter { it.incidencias.isNotEmpty() || it.enMantencion }
        .sortedWith(
            compareByDescending<IncidenciaEquipo> { it.incidencias.size }
                .thenByDescending { it.enMantencion }
                .thenBy { it.equipo.codigo },
        )
