package appmovil.prestaaula.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import appmovil.prestaaula.model.Equipo
import appmovil.prestaaula.model.EstadoDevolucion
import appmovil.prestaaula.model.EstadoEquipo
import appmovil.prestaaula.model.Prestamo
import appmovil.prestaaula.model.Reserva
import appmovil.prestaaula.model.TipoEquipo
import java.time.LocalDate
import java.time.LocalDateTime

// Filas de SQLite. Los enums se guardan por nombre y las fechas en ISO-8601 (texto), igual que
// hacía el JSON: así el modelo de dominio no depende de Room.

@Entity(tableName = "equipos")
data class EquipoEntity(
    @PrimaryKey val id: Int,
    val codigo: String,
    val tipo: String,
    val marca: String,
    val estado: String,
)

@Entity(tableName = "reservas")
data class ReservaEntity(
    @PrimaryKey val id: Int,
    val equipoId: Int,
    val docenteId: Int,
    val docente: String,
    val curso: String,
    val fecha: String,
    val bloque: Int,
)

@Entity(tableName = "prestamos")
data class PrestamoEntity(
    @PrimaryKey val id: Int,
    val equipoId: Int,
    val docenteId: Int,
    val docente: String,
    val curso: String,
    val retiro: String,
    val retiroConfirmado: Boolean,
    val devolucionAvisada: Boolean,
    val devolucion: String?,
    val estadoDevolucion: String?,
    val observaciones: String,
    val foto: String?,
)

fun Equipo.aEntidad() = EquipoEntity(id, codigo, tipo.name, marca, estado.name)

fun EquipoEntity.aModelo() =
    Equipo(id, codigo, TipoEquipo.valueOf(tipo), marca, EstadoEquipo.valueOf(estado))

fun Reserva.aEntidad() = ReservaEntity(id, equipoId, docenteId, docente, curso, fecha.toString(), bloque)

fun ReservaEntity.aModelo() =
    Reserva(id, equipoId, docenteId, docente, curso, LocalDate.parse(fecha), bloque)

fun Prestamo.aEntidad() = PrestamoEntity(
    id, equipoId, docenteId, docente, curso, retiro.toString(), retiroConfirmado, devolucionAvisada,
    devolucion?.toString(), estadoDevolucion?.name, observaciones, foto,
)

fun PrestamoEntity.aModelo() = Prestamo(
    id = id, equipoId = equipoId, docenteId = docenteId, docente = docente, curso = curso,
    retiro = LocalDateTime.parse(retiro),
    retiroConfirmado = retiroConfirmado,
    devolucionAvisada = devolucionAvisada,
    devolucion = devolucion?.let(LocalDateTime::parse),
    estadoDevolucion = estadoDevolucion?.let(EstadoDevolucion::valueOf),
    observaciones = observaciones,
    foto = foto,
)
