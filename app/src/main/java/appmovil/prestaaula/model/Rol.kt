package appmovil.prestaaula.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MoveToInbox
import androidx.compose.material.icons.filled.Outbox
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.ui.graphics.vector.ImageVector

enum class Funcion(val titulo: String, val descripcion: String, val icono: ImageVector) {
    INVENTARIO("Inventario", "Equipos del colegio y su estado", Icons.Filled.Inventory2),
    DISPONIBILIDAD("Disponibilidad", "Equipos libres por bloque; el docente reserva desde aquí", Icons.Filled.EventAvailable),
    MIS_RESERVAS("Mis reservas", "Revisa o cancela tus reservas", Icons.Filled.Checklist),
    RETIRO_DEVOLUCION("Retiro y devolución", "Confirma el retiro y la devolución", Icons.Filled.SwapHoriz),
    REGISTRAR_PRESTAMO("Registrar préstamo", "Entrega un equipo a un docente", Icons.Filled.Outbox),
    REGISTRAR_DEVOLUCION("Registrar devolución", "Recibe el equipo y anota su estado", Icons.Filled.MoveToInbox),
    REPORTE_USO("Reporte de uso", "Uso de los recursos por período", Icons.Filled.BarChart),
    EQUIPOS_INCIDENCIAS("Equipos con incidencias", "Equipos dañados o con fallas frecuentes", Icons.Filled.ReportProblem),
}

enum class Rol(val etiqueta: String, val funciones: List<Funcion>) {
    DOCENTE(
        "Docente",
        listOf(
            Funcion.DISPONIBILIDAD, Funcion.MIS_RESERVAS, Funcion.RETIRO_DEVOLUCION,
        ),
    ),
    ENCARGADO_CRA(
        "Encargado CRA",
        listOf(
            Funcion.INVENTARIO, Funcion.DISPONIBILIDAD, Funcion.REGISTRAR_PRESTAMO,
            Funcion.REGISTRAR_DEVOLUCION, Funcion.REPORTE_USO, Funcion.EQUIPOS_INCIDENCIAS,
        ),
    ),
    JEFE_UTP(
        "Jefe de UTP",
        listOf(Funcion.REPORTE_USO, Funcion.EQUIPOS_INCIDENCIAS),
    ),
    JEFE_ADMIN_FINANZAS(
        "Jefe de Administración y Finanzas",
        listOf(Funcion.REPORTE_USO, Funcion.EQUIPOS_INCIDENCIAS),
    );

    fun puede(funcion: Funcion) = funcion in funciones

    // Dentro de Disponibilidad solo el docente puede convertir un equipo libre en una reserva.
    val puedeReservar: Boolean get() = this == DOCENTE
}

// Guardia de acceso: la función solo se resuelve si el nombre es válido Y el rol la tiene.
// Cualquier otro caso (nombre inventado, función ajena) devuelve null y la navegación va a home.
fun funcionPermitida(rol: Rol, nombre: String?): Funcion? =
    Funcion.entries.firstOrNull { it.name == nombre }?.takeIf { rol.puede(it) }
