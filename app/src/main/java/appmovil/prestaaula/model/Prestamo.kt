package appmovil.prestaaula.model

import java.time.LocalDateTime

enum class EstadoDevolucion(val etiqueta: String) {
    BUEN_ESTADO("Buen estado"),
    CON_DANOS("Con daños"),
    FALTAN_ACCESORIOS("Faltan accesorios"),
}

// Registro de cada movimiento: es el historial que identifica al responsable de un equipo.
data class Prestamo(
    val id: Int,
    val equipoId: Int,
    val docenteId: Int,
    val docente: String,
    val curso: String,
    val retiro: LocalDateTime,
    val retiroConfirmado: Boolean = false,
    val devolucionAvisada: Boolean = false,
    val devolucion: LocalDateTime? = null,
    val estadoDevolucion: EstadoDevolucion? = null,
    val observaciones: String = "",
    // Nombre del archivo de la foto tomada al recibir el equipo (carpeta privada de la app), si hay.
    val foto: String? = null,
) {
    val activo: Boolean get() = devolucion == null
}

// Último docente que retiró el equipo, o null si nunca se ha prestado.
fun ultimoResponsable(equipoId: Int, prestamos: List<Prestamo>): Prestamo? =
    prestamos.filter { it.equipoId == equipoId }.maxByOrNull { it.retiro }
