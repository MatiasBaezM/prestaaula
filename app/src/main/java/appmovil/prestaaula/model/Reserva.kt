package appmovil.prestaaula.model

import java.time.LocalDate

// Una reserva ocupa un equipo en un bloque de un día (una clase doble son 2 reservas seguidas).
data class Reserva(
    val id: Int,
    val equipoId: Int,
    val docenteId: Int,
    val docente: String,
    val curso: String,
    val fecha: LocalDate,
    val bloque: Int,
)

sealed interface Ocupacion {
    data object Libre : Ocupacion
    data class Reservado(val reserva: Reserva) : Ocupacion
    data object Prestado : Ocupacion
    data object EnMantencion : Ocupacion
}

// "Prestado" describe el momento actual, por eso solo bloquea el día de hoy.
fun ocupacionEnBloque(
    equipo: Equipo,
    reservas: List<Reserva>,
    fecha: LocalDate,
    bloque: Int,
    hoy: LocalDate,
): Ocupacion = when {
    equipo.estado == EstadoEquipo.EN_MANTENCION -> Ocupacion.EnMantencion
    equipo.estado == EstadoEquipo.PRESTADO && fecha == hoy -> Ocupacion.Prestado
    else -> reservas
        .firstOrNull { it.equipoId == equipo.id && it.fecha == fecha && it.bloque == bloque }
        ?.let { Ocupacion.Reservado(it) }
        ?: Ocupacion.Libre
}

// Libre solo si lo está en todos los bloques; si no, devuelve el primer impedimento.
fun ocupacionEnBloques(
    equipo: Equipo,
    reservas: List<Reserva>,
    fecha: LocalDate,
    bloques: Collection<Int>,
    hoy: LocalDate,
): Ocupacion = bloques.sorted()
    .map { ocupacionEnBloque(equipo, reservas, fecha, it, hoy) }
    .firstOrNull { it !is Ocupacion.Libre }
    ?: Ocupacion.Libre
