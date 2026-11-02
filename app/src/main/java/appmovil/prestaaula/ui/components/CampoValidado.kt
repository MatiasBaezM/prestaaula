package appmovil.prestaaula.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

// Campo de formulario con retroalimentación visual:
// - ícono a la izquierda que representa el dato,
// - ícono de error (rojo) o de válido (verde) a la derecha,
// - mensaje del error bajo el campo, que aparece con animación,
// - una sacudida corta cuando se intenta enviar con el campo inválido.
// `error` lo calcula la lógica (Validaciones); este componente solo decide cuándo mostrarlo:
// después de que el usuario pasó por el campo, o cuando ya intentó enviar (`intentos` > 0).
@Composable
fun CampoValidado(
    valor: String,
    onValorChange: (String) -> Unit,
    etiqueta: String,
    icono: ImageVector,
    error: String?,
    intentos: Int,
    modifier: Modifier = Modifier,
    ayuda: String? = null,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    iconoFinal: (@Composable () -> Unit)? = null,
) {
    var tocado by rememberSaveable { mutableStateOf(false) }
    var tuvoFoco by remember { mutableStateOf(false) }
    val mostrarError = (tocado || intentos > 0) && error != null
    val esValido = tocado && error == null && valor.isNotBlank()

    val desplazamiento = remember { Animatable(0f) }
    LaunchedEffect(intentos) {
        if (intentos > 0 && error != null) {
            for (destino in listOf(-10f, 10f, -7f, 7f, -3f, 0f)) {
                desplazamiento.animateTo(destino, tween(durationMillis = 45))
            }
        }
    }

    OutlinedTextField(
        value = valor,
        onValueChange = onValorChange,
        label = { Text(etiqueta) },
        leadingIcon = { Icon(icono, contentDescription = null) },
        trailingIcon = when {
            // Si el campo trae su propio ícono (p. ej. el ojo de la contraseña) se conserva junto al de error.
            mostrarError && iconoFinal != null -> {
                {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.ErrorOutline, contentDescription = "Campo con error", tint = MaterialTheme.colorScheme.error)
                        iconoFinal()
                    }
                }
            }
            mostrarError -> {
                { Icon(Icons.Filled.ErrorOutline, contentDescription = "Campo con error", tint = MaterialTheme.colorScheme.error) }
            }
            iconoFinal != null -> iconoFinal
            esValido -> {
                { Icon(Icons.Filled.CheckCircle, contentDescription = "Campo válido", tint = MaterialTheme.colorScheme.primary) }
            }
            else -> null
        },
        isError = mostrarError,
        supportingText = {
            val texto = if (mostrarError) error else ayuda
            if (texto != null) Text(texto)
        },
        singleLine = singleLine,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        modifier = modifier
            .offset { IntOffset(desplazamiento.value.roundToInt(), 0) }
            .onFocusChanged {
                // "Tocado" = el usuario entró al campo y salió: recién ahí se le marca un error.
                if (tuvoFoco && !it.isFocused) tocado = true
                tuvoFoco = it.isFocused
            },
    )
}

// Error que no pertenece a un campo (p. ej. "El equipo ya está prestado"): entra y sale animado.
@Composable
fun MensajeError(texto: String?, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = texto != null,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier,
    ) {
        var ultimo by remember { mutableStateOf(texto.orEmpty()) }
        if (texto != null) ultimo = texto
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
            Icon(
                Icons.Filled.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp),
            )
            Text(
                ultimo,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}
