package appmovil.prestaaula.data

import androidx.room.withTransaction
import appmovil.prestaaula.data.db.PrestaAulaDb
import appmovil.prestaaula.data.db.aEntidad
import appmovil.prestaaula.data.db.aModelo
import appmovil.prestaaula.model.Bloques
import appmovil.prestaaula.model.EstadoDevolucion
import appmovil.prestaaula.model.EstadoEquipo
import appmovil.prestaaula.model.Prestamo
import appmovil.prestaaula.model.Usuario
import appmovil.prestaaula.model.agruparReservas
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime

// Repositorio de préstamos (tabla "prestamos"). Un préstamo o una devolución tocan además el estado
// del equipo y las reservas: todo va en una sola transacción, así nunca queda a medias.
class PrestamoRepository(
    private val db: PrestaAulaDb,
    private val ahora: () -> LocalDateTime = { LocalDateTime.now() },
) {
    private val dao = db.prestamoDao()
    private val equipos = db.equipoDao()
    private val reservas = db.reservaDao()

    val prestamos: Flow<List<Prestamo>> = dao.observarTodos().map { filas -> filas.map { it.aModelo() } }

    // Devuelve el mensaje de error, o null si se registró.
    suspend fun registrarPrestamo(equipoId: Int, docente: Usuario, curso: String): String? {
        val ahoraActual = ahora()
        val hoy = ahoraActual.toLocalDate()
        val minutos = ahoraActual.hour * 60 + ahoraActual.minute

        return db.withTransaction {
            val equipo = equipos.obtener(equipoId) ?: return@withTransaction "El equipo no existe"
            if (curso.isBlank()) return@withTransaction "Ingresa el curso"
            when (equipo.estado) {
                EstadoEquipo.PRESTADO -> return@withTransaction "El equipo ya está prestado"
                EstadoEquipo.EN_MANTENCION -> return@withTransaction "El equipo está en mantención"
                EstadoEquipo.DISPONIBLE -> Unit
            }

            val deHoy = reservas.obtenerDeEquipo(equipoId).map { it.aModelo() }.filter { it.fecha == hoy }
            val bloqueActual = Bloques.deLaHora(minutos)?.numero
            deHoy.firstOrNull { it.bloque == bloqueActual && it.docenteId != docente.id }?.let {
                return@withTransaction "Está reservado en este bloque por ${it.docente}"
            }

            // Se cumple solo el tramo que el docente usa ahora (el primero aún vigente). Si tiene otra
            // clase más tarde con este mismo equipo, esa reserva se mantiene y sigue protegiendo su horario.
            agruparReservas(
                deHoy.filter { it.docenteId == docente.id && (Bloques.porNumero(it.bloque)?.finMin ?: 0) > minutos },
            ).firstOrNull()?.let { reservas.borrar(it.ids) }

            equipos.cambiarEstado(equipoId, EstadoEquipo.PRESTADO)
            dao.insertar(Prestamo(0, equipoId, docente.id, docente.nombre, curso.trim(), ahoraActual).aEntidad())
            null
        }
    }

    // Un equipo dañado o incompleto pasa a mantención; si no, vuelve a estar disponible.
    suspend fun registrarDevolucion(
        prestamoId: Int,
        estado: EstadoDevolucion,
        observaciones: String,
        foto: String? = null,
    ): String? = db.withTransaction {
        val prestamo = dao.obtener(prestamoId)?.aModelo() ?: return@withTransaction "El préstamo no existe"
        if (!prestamo.activo) return@withTransaction "El equipo ya fue devuelto"
        if (estado != EstadoDevolucion.BUEN_ESTADO && observaciones.isBlank()) {
            return@withTransaction "Describe el problema en las observaciones"
        }

        equipos.cambiarEstado(
            prestamo.equipoId,
            if (estado == EstadoDevolucion.BUEN_ESTADO) EstadoEquipo.DISPONIBLE else EstadoEquipo.EN_MANTENCION,
        )
        val cerrado = prestamo.copy(
            devolucion = ahora(), estadoDevolucion = estado, observaciones = observaciones.trim(), foto = foto,
        )
        dao.actualizar(cerrado.aEntidad())
        null
    }

    // El docente confirma que recibió el equipo.
    suspend fun confirmarRetiro(prestamoId: Int, docenteId: Int): Boolean =
        actualizarPropio(prestamoId, docenteId) { it.copy(retiroConfirmado = true) }

    // El docente avisa que ya devolvió el equipo; el CRA igual debe recibirlo y revisarlo.
    suspend fun avisarDevolucion(prestamoId: Int, docenteId: Int): Boolean =
        actualizarPropio(prestamoId, docenteId) { it.copy(devolucionAvisada = true) }

    // Solo el docente del préstamo, y solo mientras siga activo.
    private suspend fun actualizarPropio(id: Int, docenteId: Int, cambio: (Prestamo) -> Prestamo): Boolean =
        db.withTransaction {
            val p = dao.obtener(id)?.aModelo() ?: return@withTransaction false
            if (p.docenteId != docenteId || !p.activo) return@withTransaction false
            dao.actualizar(cambio(p).aEntidad())
            true
        }
}
