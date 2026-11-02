package appmovil.prestaaula.model

import java.time.DayOfWeek
import java.time.LocalDate

// Próximos días hábiles (el colegio no funciona sábado ni domingo), empezando desde `desde`.
fun diasHabiles(desde: LocalDate, cantidad: Int = 7): List<LocalDate> =
    generateSequence(desde) { it.plusDays(1) }
        .filter { it.dayOfWeek != DayOfWeek.SATURDAY && it.dayOfWeek != DayOfWeek.SUNDAY }
        .take(cantidad)
        .toList()

// Reservas seguidas del mismo equipo, día y curso, vistas como una sola (clase doble).
data class GrupoReserva(
    val ids: List<Int>,
    val equipoId: Int,
    val fecha: LocalDate,
    val desde: Int,
    val hasta: Int,
    val curso: String,
)

fun agruparReservas(reservas: List<Reserva>): List<GrupoReserva> =
    reservas.groupBy { Triple(it.equipoId, it.fecha, it.curso) }
        .flatMap { (clave, lista) ->
            val tramos = mutableListOf<MutableList<Reserva>>()
            for (r in lista.sortedBy { it.bloque }) {
                val ultimo = tramos.lastOrNull()
                if (ultimo != null && ultimo.last().bloque + 1 == r.bloque) ultimo += r
                else tramos += mutableListOf(r)
            }
            tramos.map {
                GrupoReserva(it.map { r -> r.id }, clave.first, clave.second, it.first().bloque, it.last().bloque, clave.third)
            }
        }
        .sortedWith(compareBy({ it.fecha }, { it.desde }))
