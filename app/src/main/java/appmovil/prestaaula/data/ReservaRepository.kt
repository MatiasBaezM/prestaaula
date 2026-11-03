package appmovil.prestaaula.data

import androidx.room.withTransaction
import appmovil.prestaaula.data.db.PrestaAulaDb
import appmovil.prestaaula.data.db.aEntidad
import appmovil.prestaaula.data.db.aModelo
import appmovil.prestaaula.model.Bloques
import appmovil.prestaaula.model.Ocupacion
import appmovil.prestaaula.model.Reserva
import appmovil.prestaaula.model.ocupacionEnBloque
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalDateTime

sealed interface ResultadoReserva {
    data class Exito(val reservas: List<Reserva>) : ResultadoReserva
    data class Error(val mensaje: String) : ResultadoReserva
}

// Repositorio de reservas (tabla "reservas"). `ahora` se inyecta para poder probar con una hora fija.
class ReservaRepository(
    private val db: PrestaAulaDb,
    private val ahora: () -> LocalDateTime = { LocalDateTime.now() },
) {
    private val dao = db.reservaDao()

    val reservas: Flow<List<Reserva>> = dao.observarTodas().map { filas -> filas.map { it.aModelo() } }

    // Valida y guarda todo o nada. La revisión de choques y el guardado van en la misma transacción:
    // SQLite atiende una escritura a la vez, así dos docentes no pueden tomar el mismo bloque.
    suspend fun reservar(
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

        return db.withTransaction {
            val equipo = db.equipoDao().obtener(equipoId)?.aModelo()
                ?: return@withTransaction ResultadoReserva.Error("El equipo no existe")
            val actuales = dao.obtenerDeEquipo(equipoId).map { it.aModelo() }
            for (b in ordenados) {
                val error = when (ocupacionEnBloque(equipo, actuales, fecha, b, hoy)) {
                    Ocupacion.Libre -> null
                    is Ocupacion.Reservado -> "El equipo ya está reservado en el bloque $b"
                    Ocupacion.Prestado -> "El equipo está prestado en este momento"
                    Ocupacion.EnMantencion -> "El equipo está en mantención"
                }
                if (error != null) return@withTransaction ResultadoReserva.Error(error)
            }

            // Una fila por bloque (una clase doble son 2 reservas seguidas); id 0 = lo asigna SQLite.
            val nuevas = ordenados.map { Reserva(0, equipoId, docenteId, docente, curso.trim(), fecha, it) }
            val ids = dao.insertar(nuevas.map { it.aEntidad() })
            ResultadoReserva.Exito(nuevas.zip(ids) { r, id -> r.copy(id = id.toInt()) })
        }
    }

    // Solo el dueño puede cancelar, y no reservas de días pasados.
    suspend fun cancelar(ids: List<Int>, docenteId: Int): Boolean {
        if (ids.isEmpty()) return false
        val hoy = ahora().toLocalDate()
        return db.withTransaction {
            val objetivo = dao.obtenerPorIds(ids).filter { it.docenteId == docenteId && !it.fecha.isBefore(hoy) }
            if (objetivo.size != ids.size) return@withTransaction false
            dao.borrar(ids)
            true
        }
    }
}
