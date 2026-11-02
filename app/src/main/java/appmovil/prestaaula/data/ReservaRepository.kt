package appmovil.prestaaula.data

import appmovil.prestaaula.model.Bloques
import appmovil.prestaaula.model.Ocupacion
import appmovil.prestaaula.model.Reserva
import appmovil.prestaaula.model.diasHabiles
import appmovil.prestaaula.model.ocupacionEnBloque
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import java.time.LocalDateTime

sealed interface ResultadoReserva {
    data class Exito(val reservas: List<Reserva>) : ResultadoReserva
    data class Error(val mensaje: String) : ResultadoReserva
}

// En memoria por ahora; mismo criterio que InventarioRepository (se cambia por Room).
class ReservaRepository(
    private val inventario: InventarioRepository,
    inicial: List<Reserva> = reservasDemo(LocalDate.now()),
    private val ahora: () -> LocalDateTime = { LocalDateTime.now() },
) {
    private val _reservas = MutableStateFlow(inicial)
    val reservas: StateFlow<List<Reserva>> = _reservas.asStateFlow()

    // Valida y guarda todo o nada; sincronizado para que dos reservas no choquen.
    @Synchronized
    fun reservar(
        equipoId: Int,
        docenteId: Int,
        docente: String,
        curso: String,
        fecha: LocalDate,
        bloques: Set<Int>,
    ): ResultadoReserva {
        val ahoraActual = ahora()
        val hoy = ahoraActual.toLocalDate()
        val ordenados = bloques.sorted()

        if (curso.isBlank()) return ResultadoReserva.Error("Ingresa el curso")
        if (ordenados.isEmpty() || ordenados.any { Bloques.porNumero(it) == null }) {
            return ResultadoReserva.Error("Elige al menos un bloque")
        }
        if (ordenados.zipWithNext().any { (a, b) -> b != a + 1 }) {
            return ResultadoReserva.Error("Los bloques deben ser seguidos")
        }
        if (fecha.isBefore(hoy)) return ResultadoReserva.Error("No puedes reservar en una fecha pasada")
        if (fecha == hoy) {
            val minutos = ahoraActual.hour * 60 + ahoraActual.minute
            if (ordenados.any { Bloques.porNumero(it)!!.finMin <= minutos }) {
                return ResultadoReserva.Error("Uno de los bloques elegidos ya terminó")
            }
        }

        val equipo = inventario.equipos.value.firstOrNull { it.id == equipoId }
            ?: return ResultadoReserva.Error("El equipo no existe")
        val actuales = _reservas.value
        for (b in ordenados) {
            when (ocupacionEnBloque(equipo, actuales, fecha, b, hoy)) {
                Ocupacion.Libre -> Unit
                is Ocupacion.Reservado -> return ResultadoReserva.Error("El equipo ya está reservado en el bloque $b")
                Ocupacion.Prestado -> return ResultadoReserva.Error("El equipo está prestado en este momento")
                Ocupacion.EnMantencion -> return ResultadoReserva.Error("El equipo está en mantención")
            }
        }

        var ultimoId = actuales.maxOfOrNull { it.id } ?: 0
        val nuevas = ordenados.map { Reserva(++ultimoId, equipoId, docenteId, docente, curso.trim(), fecha, it) }
        _reservas.value = actuales + nuevas
        return ResultadoReserva.Exito(nuevas)
    }

    // Una reserva cumplida (el equipo ya se entregó) deja de ocupar el horario.
    @Synchronized
    fun consumir(ids: Collection<Int>) {
        _reservas.value = _reservas.value.filterNot { it.id in ids }
    }

    // Solo el dueño puede cancelar, y no reservas de días pasados.
    @Synchronized
    fun cancelar(ids: List<Int>, docenteId: Int): Boolean {
        val hoy = ahora().toLocalDate()
        val objetivo = _reservas.value.filter { it.id in ids && it.docenteId == docenteId && !it.fecha.isBefore(hoy) }
        if (ids.isEmpty() || objetivo.size != ids.size) return false
        _reservas.value = _reservas.value.filterNot { it in objetivo }
        return true
    }
}

private fun reservasDemo(hoy: LocalDate): List<Reserva> {
    // Próximos días hábiles, para que los datos de ejemplo nunca caigan en fin de semana.
    val manana = diasHabiles(hoy.plusDays(1), 1).first()
    val pasado = diasHabiles(manana.plusDays(1), 1).first()
    return listOf(
        Reserva(1, equipoId = 6, docenteId = 101, docente = "Prof. Rojas", curso = "7°A", fecha = hoy, bloque = 2),
        Reserva(2, equipoId = 6, docenteId = 101, docente = "Prof. Rojas", curso = "7°A", fecha = hoy, bloque = 3),
        Reserva(3, equipoId = 1, docenteId = 102, docente = "Prof. Soto", curso = "8°B", fecha = hoy, bloque = 4),
        Reserva(4, equipoId = 7, docenteId = 103, docente = "Prof. Díaz", curso = "2°M", fecha = hoy, bloque = 2),
        Reserva(5, equipoId = 4, docenteId = 102, docente = "Prof. Soto", curso = "6°A", fecha = manana, bloque = 1),
        // Reservas del usuario de prueba "Prof. Muñoz" (id 1), para "Mis reservas".
        Reserva(6, equipoId = 2, docenteId = 1, docente = "Prof. Muñoz", curso = "7°A", fecha = manana, bloque = 3),
        Reserva(7, equipoId = 2, docenteId = 1, docente = "Prof. Muñoz", curso = "7°A", fecha = manana, bloque = 4),
        Reserva(8, equipoId = 8, docenteId = 1, docente = "Prof. Muñoz", curso = "8°B", fecha = pasado, bloque = 2),
    )
}
