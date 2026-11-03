package appmovil.prestaaula.data

import appmovil.prestaaula.model.Bloques
import appmovil.prestaaula.model.EstadoDevolucion
import appmovil.prestaaula.model.EstadoEquipo
import appmovil.prestaaula.model.Prestamo
import appmovil.prestaaula.model.Usuario
import appmovil.prestaaula.model.agruparReservas
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDateTime

// Cada movimiento actualiza de inmediato el inventario (estado) y las reservas.
// En memoria por ahora; mismo criterio que los otros repositorios.
class PrestamoRepository(
    private val inventario: InventarioRepository,
    private val reservas: ReservaRepository,
    inicial: List<Prestamo> = prestamosDemo(LocalDateTime.now()),
    private val ahora: () -> LocalDateTime = { LocalDateTime.now() },
) {
    private val _prestamos = MutableStateFlow(inicial)
    val prestamos: StateFlow<List<Prestamo>> = _prestamos.asStateFlow()

    // Devuelve el mensaje de error, o null si se registró.
    @Synchronized
    fun registrarPrestamo(equipoId: Int, docente: Usuario, curso: String): String? {
        val ahoraActual = ahora()
        val minutos = ahoraActual.hour * 60 + ahoraActual.minute
        val equipo = inventario.equipos.value.firstOrNull { it.id == equipoId } ?: return "El equipo no existe"

        if (curso.isBlank()) return "Ingresa el curso"
        when (equipo.estado) {
            EstadoEquipo.PRESTADO -> return "El equipo ya está prestado"
            EstadoEquipo.EN_MANTENCION -> return "El equipo está en mantención"
            EstadoEquipo.DISPONIBLE -> Unit
        }

        val deHoy = reservas.reservas.value.filter { it.equipoId == equipoId && it.fecha == ahoraActual.toLocalDate() }
        val bloqueActual = Bloques.deLaHora(minutos)?.numero
        deHoy.firstOrNull { it.bloque == bloqueActual && it.docenteId != docente.id }?.let {
            return "Está reservado en este bloque por ${it.docente}"
        }

        // Se cumple solo el tramo que el docente usa ahora (el primero aún vigente). Si tiene otra
        // clase más tarde con este mismo equipo, esa reserva se mantiene y sigue protegiendo su horario.
        agruparReservas(
            deHoy.filter { it.docenteId == docente.id && (Bloques.porNumero(it.bloque)?.finMin ?: 0) > minutos },
        ).firstOrNull()?.let { reservas.consumir(it.ids) }
        inventario.cambiarEstado(equipoId, EstadoEquipo.PRESTADO)
        val id = (_prestamos.value.maxOfOrNull { it.id } ?: 0) + 1
        _prestamos.update { it + Prestamo(id, equipoId, docente.id, docente.nombre, curso.trim(), ahoraActual) }
        return null
    }

    // Un equipo dañado o incompleto pasa a mantención; si no, vuelve a estar disponible.
    @Synchronized
    fun registrarDevolucion(
        prestamoId: Int,
        estado: EstadoDevolucion,
        observaciones: String,
        foto: String? = null,
    ): String? {
        val prestamo = _prestamos.value.firstOrNull { it.id == prestamoId } ?: return "El préstamo no existe"
        if (!prestamo.activo) return "El equipo ya fue devuelto"
        if (estado != EstadoDevolucion.BUEN_ESTADO && observaciones.isBlank()) {
            return "Describe el problema en las observaciones"
        }

        inventario.cambiarEstado(
            prestamo.equipoId,
            if (estado == EstadoDevolucion.BUEN_ESTADO) EstadoEquipo.DISPONIBLE else EstadoEquipo.EN_MANTENCION,
        )
        actualizar(prestamoId) {
            it.copy(devolucion = ahora(), estadoDevolucion = estado, observaciones = observaciones.trim(), foto = foto)
        }
        return null
    }

    // El docente confirma que recibió el equipo.
    @Synchronized
    fun confirmarRetiro(prestamoId: Int, docenteId: Int): Boolean = actualizarPropio(prestamoId, docenteId) {
        it.copy(retiroConfirmado = true)
    }

    // El docente avisa que ya devolvió el equipo; el CRA igual debe recibirlo y revisarlo.
    @Synchronized
    fun avisarDevolucion(prestamoId: Int, docenteId: Int): Boolean = actualizarPropio(prestamoId, docenteId) {
        it.copy(devolucionAvisada = true)
    }

    private fun actualizarPropio(id: Int, docenteId: Int, cambio: (Prestamo) -> Prestamo): Boolean {
        val p = _prestamos.value.firstOrNull { it.id == id } ?: return false
        if (p.docenteId != docenteId || !p.activo) return false
        actualizar(id, cambio)
        return true
    }

    private fun actualizar(id: Int, cambio: (Prestamo) -> Prestamo) {
        _prestamos.update { lista -> lista.map { if (it.id == id) cambio(it) else it } }
    }
}

// Historial de ejemplo, coherente con el inventario: TB-002 está prestado y NB-003 volvió dañado.
private fun prestamosDemo(ahora: LocalDateTime): List<Prestamo> {
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
