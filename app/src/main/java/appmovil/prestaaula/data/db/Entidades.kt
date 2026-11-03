package appmovil.prestaaula.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import appmovil.prestaaula.model.Equipo
import appmovil.prestaaula.model.EstadoDevolucion
import appmovil.prestaaula.model.EstadoEquipo
import appmovil.prestaaula.model.Prestamo
import appmovil.prestaaula.model.Reserva
import appmovil.prestaaula.model.TipoEquipo
import java.time.LocalDate
import java.time.LocalDateTime

// Filas de SQLite, separadas del modelo de dominio para que este no dependa de Room.
// En disco, los enums quedan por nombre (conversión que Room trae incluida) y las fechas en
// texto ISO-8601 (Convertidores), igual que antes: el esquema no cambia.

@Entity(tableName = "equipos")
data class EquipoEntity(
    @PrimaryKey val id: Int,
    val codigo: String,
    val tipo: TipoEquipo,
    val marca: String,
    val estado: EstadoEquipo,
)

// equipoId debe apuntar a un equipo existente. RESTRICT: no se puede borrar un equipo que aún
// tiene reservas o préstamos (el historial no se pierde en cascada). El índice lo exige Room
// para claves foráneas y evita recorrer toda la tabla al validar.
@Entity(
    tableName = "reservas",
    foreignKeys = [
        ForeignKey(
            entity = EquipoEntity::class,
            parentColumns = ["id"],
            childColumns = ["equipoId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("equipoId")],
)
data class ReservaEntity(
    @PrimaryKey val id: Int,
    val equipoId: Int,
    val docenteId: Int,
    val docente: String,
    val curso: String,
    val fecha: LocalDate,
    val bloque: Int,
)

@Entity(
    tableName = "prestamos",
    foreignKeys = [
        ForeignKey(
            entity = EquipoEntity::class,
            parentColumns = ["id"],
            childColumns = ["equipoId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("equipoId")],
)
data class PrestamoEntity(
    @PrimaryKey val id: Int,
    val equipoId: Int,
    val docenteId: Int,
    val docente: String,
    val curso: String,
    val retiro: LocalDateTime,
    val retiroConfirmado: Boolean,
    val devolucionAvisada: Boolean,
    val devolucion: LocalDateTime?,
    val estadoDevolucion: EstadoDevolucion?,
    val observaciones: String,
    val foto: String?,
)

// Fechas <-> texto ISO-8601 ("2026-10-30", "2026-10-30T08:15"). Un texto inválido lanza
// excepción al leer; PrestaAulaApp la atrapa y parte con los datos de ejemplo.
class Convertidores {
    @TypeConverter fun fechaATexto(fecha: LocalDate?): String? = fecha?.toString()
    @TypeConverter fun textoAFecha(texto: String?): LocalDate? = texto?.let(LocalDate::parse)
    @TypeConverter fun fechaHoraATexto(fechaHora: LocalDateTime?): String? = fechaHora?.toString()
    @TypeConverter fun textoAFechaHora(texto: String?): LocalDateTime? = texto?.let(LocalDateTime::parse)
}

fun Equipo.aEntidad() = EquipoEntity(id, codigo, tipo, marca, estado)

fun EquipoEntity.aModelo() = Equipo(id, codigo, tipo, marca, estado)

fun Reserva.aEntidad() = ReservaEntity(id, equipoId, docenteId, docente, curso, fecha, bloque)

fun ReservaEntity.aModelo() = Reserva(id, equipoId, docenteId, docente, curso, fecha, bloque)

fun Prestamo.aEntidad() = PrestamoEntity(
    id, equipoId, docenteId, docente, curso, retiro, retiroConfirmado, devolucionAvisada,
    devolucion, estadoDevolucion, observaciones, foto,
)

fun PrestamoEntity.aModelo() = Prestamo(
    id = id, equipoId = equipoId, docenteId = docenteId, docente = docente, curso = curso,
    retiro = retiro,
    retiroConfirmado = retiroConfirmado,
    devolucionAvisada = devolucionAvisada,
    devolucion = devolucion,
    estadoDevolucion = estadoDevolucion,
    observaciones = observaciones,
    foto = foto,
)
