package appmovil.prestaaula.data

import androidx.room.withTransaction
import appmovil.prestaaula.data.db.PrestaAulaDb
import appmovil.prestaaula.data.db.aEntidad
import appmovil.prestaaula.model.Equipo
import appmovil.prestaaula.model.EstadoDevolucion
import appmovil.prestaaula.model.EstadoEquipo
import appmovil.prestaaula.model.Prestamo
import appmovil.prestaaula.model.Reserva
import appmovil.prestaaula.model.TipoEquipo
import appmovil.prestaaula.model.diasHabiles
import java.time.LocalDate
import java.time.LocalDateTime

// Datos de ejemplo que se guardan la primera vez que se abre la app (base vacía), para que cada
// pantalla tenga algo que mostrar. Las fechas son relativas al día de instalación.
object DatosDemo {

    // Solo si la tabla de equipos está vacía; en una transacción para no cargarlos a medias.
    suspend fun cargarSiEstaVacia(db: PrestaAulaDb, ahora: LocalDateTime = LocalDateTime.now()) {
        db.withTransaction {
            if (db.equipoDao().contar() > 0) return@withTransaction
            // Primero los equipos: reservas y préstamos los referencian (clave foránea).
            db.equipoDao().insertarTodos(equipos().map { it.aEntidad() })
            db.reservaDao().insertar(reservas(ahora.toLocalDate()).map { it.aEntidad() })
            db.prestamoDao().insertarTodos(prestamos(ahora).map { it.aEntidad() })
        }
    }

    fun equipos(): List<Equipo> = listOf(
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

    fun reservas(hoy: LocalDate): List<Reserva> {
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

    // Historial coherente con el inventario: TB-002 está prestado y NB-003 volvió dañado.
    fun prestamos(ahora: LocalDateTime): List<Prestamo> {
        fun devuelto(
            id: Int, equipoId: Int, docenteId: Int, docente: String, curso: String, diasAtras: Long,
            estado: EstadoDevolucion = EstadoDevolucion.BUEN_ESTADO, observaciones: String = "",
        ) = Prestamo(
            id, equipoId, docenteId, docente, curso,
            retiro = ahora.minusDays(diasAtras), retiroConfirmado = true,
            devolucion = ahora.minusDays(diasAtras).plusHours(1),
            estadoDevolucion = estado, observaciones = observaciones,
        )

        return listOf(
            Prestamo(
                1, equipoId = 5, docenteId = 102, docente = "Prof. Soto", curso = "6°A",
                retiro = ahora.minusHours(1), retiroConfirmado = true,
            ),
            devuelto(2, 3, 103, "Prof. Díaz", "2°M", 2, EstadoDevolucion.CON_DANOS, "Pantalla trizada"),
            devuelto(3, 1, 101, "Prof. Rojas", "7°A", 1),
            devuelto(4, 1, 102, "Prof. Soto", "8°B", 3),
            devuelto(5, 2, 103, "Prof. Díaz", "2°M", 5),
            devuelto(6, 6, 101, "Prof. Rojas", "7°A", 2, EstadoDevolucion.FALTAN_ACCESORIOS, "Falta el cable de poder"),
            devuelto(7, 6, 102, "Prof. Soto", "8°B", 9, EstadoDevolucion.CON_DANOS, "Lente rayado"),
            devuelto(8, 6, 103, "Prof. Díaz", "2°M", 15),
            devuelto(9, 4, 102, "Prof. Soto", "6°A", 4),
            devuelto(10, 8, 101, "Prof. Rojas", "7°A", 6),
            devuelto(11, 1, 101, "Prof. Rojas", "7°A", 12),
            devuelto(12, 3, 103, "Prof. Díaz", "2°M", 20, EstadoDevolucion.CON_DANOS, "Teclado sin una tecla"),
        )
    }
}
