package appmovil.prestaaula.model

import java.time.LocalTime

data class BloqueHorario(val numero: Int, val inicioMin: Int) {
    val finMin: Int get() = inicioMin + Bloques.DURACION_MIN
    val inicio: String get() = hora(inicioMin)
    val fin: String get() = hora(finMin)
    val etiqueta: String get() = "$inicio–$fin"

    private fun hora(min: Int) = "%02d:%02d".format(min / 60, min % 60)
}

// Jornada: 9 bloques de 45 min desde las 08:00 (último: 14:00–14:45).
object Bloques {
    const val DURACION_MIN = 45
    private const val INICIO_MIN = 8 * 60
    private const val CANTIDAD = 9

    val todos: List<BloqueHorario> = List(CANTIDAD) { BloqueHorario(it + 1, INICIO_MIN + it * DURACION_MIN) }

    fun deLaHora(minutosDelDia: Int): BloqueHorario? =
        todos.firstOrNull { minutosDelDia in it.inicioMin until it.finMin }

    fun porNumero(numero: Int): BloqueHorario? = todos.firstOrNull { it.numero == numero }

    // Bloque en curso, o el primero si estamos fuera de la jornada.
    fun actual(ahora: LocalTime = LocalTime.now()): Int =
        deLaHora(ahora.hour * 60 + ahora.minute)?.numero ?: 1

    fun etiquetaRango(desde: Int, hasta: Int): String {
        val d = porNumero(desde) ?: return ""
        val h = porNumero(hasta) ?: return ""
        return if (desde == hasta) "Bloque $desde · ${d.etiqueta}"
        else "Bloques $desde–$hasta · ${d.inicio}–${h.fin}"
    }
}
