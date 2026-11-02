package appmovil.prestaaula.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.TextStyle
import java.util.Locale

private val ES = Locale.forLanguageTag("es-CL")

// Fila de chips con scroll horizontal.
@Composable
fun FilaChips(contenido: @Composable () -> Unit) {
    Row(
        Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) { contenido() }
}

fun etiquetaDia(dia: LocalDate, hoy: LocalDate): String {
    if (dia == hoy) return "Hoy"
    val nombre = dia.dayOfWeek.getDisplayName(TextStyle.SHORT, ES).replace(".", "")
    return "${nombre.replaceFirstChar { it.uppercase() }} ${dia.dayOfMonth}"
}

// "hoy 08:05" si es del día, o "29/10 08:05" si no.
fun fechaHoraCorta(momento: LocalDateTime, hoy: LocalDate): String {
    val hora = "%02d:%02d".format(momento.hour, momento.minute)
    return if (momento.toLocalDate() == hoy) "hoy $hora"
    else "%02d/%02d %s".format(momento.dayOfMonth, momento.monthValue, hora)
}

fun haceTexto(momento: LocalDateTime, hoy: LocalDate): String {
    val dias = java.time.temporal.ChronoUnit.DAYS.between(momento.toLocalDate(), hoy)
    return when {
        dias <= 0 -> "hoy"
        dias == 1L -> "ayer"
        else -> "hace $dias días"
    }
}

// Ej.: "Jueves 29 de octubre"
fun fechaLarga(dia: LocalDate): String {
    val semana = dia.dayOfWeek.getDisplayName(TextStyle.FULL, ES).replaceFirstChar { it.uppercase() }
    val mes = dia.month.getDisplayName(TextStyle.FULL, ES)
    return "$semana ${dia.dayOfMonth} de $mes"
}
