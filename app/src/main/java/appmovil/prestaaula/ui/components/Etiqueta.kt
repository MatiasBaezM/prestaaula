package appmovil.prestaaula.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class TonoEtiqueta(val fondo: Color, val texto: Color) {
    VERDE(Color(0xFFD7F0DC), Color(0xFF1B5E20)),
    AMBAR(Color(0xFFFFE9C7), Color(0xFF8A5100)),
    ROJO(Color(0xFFFFDAD6), Color(0xFF93000A)),
}

@Composable
fun Etiqueta(texto: String, tono: TonoEtiqueta) {
    // Al cambiar de estado (p. ej. Disponible → Prestado) el color se transforma en vez de saltar.
    val fondo by animateColorAsState(tono.fondo, tween(350), label = "fondoEtiqueta")
    val colorTexto by animateColorAsState(tono.texto, tween(350), label = "textoEtiqueta")
    Surface(color = fondo, shape = RoundedCornerShape(8.dp), modifier = Modifier.animateContentSize()) {
        Text(
            texto,
            color = colorTexto,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}
