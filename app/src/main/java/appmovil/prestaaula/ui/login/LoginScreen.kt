package appmovil.prestaaula.ui.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import appmovil.prestaaula.ui.components.CampoValidado
import appmovil.prestaaula.ui.components.MensajeError
import appmovil.prestaaula.ui.components.rememberVibrador
import appmovil.prestaaula.ui.theme.PrestaaulaTheme

@Composable
fun LoginScreen(
    form: LoginFormState,
    onCorreoChange: (String) -> Unit,
    onClaveChange: (String) -> Unit,
    onToggleMostrarClave: () -> Unit,
    onIniciarSesion: () -> Unit,
) {
    val focus = LocalFocusManager.current
    val vibrador = rememberVibrador()

    // Vibra al rechazar el intento, ya sea por campos inválidos o por credenciales incorrectas.
    LaunchedEffect(form.intentos) { if (form.intentos > 0) vibrador.error() }
    LaunchedEffect(form.error) { if (form.error != null) vibrador.error() }

    // El logo entra con un pequeño rebote al abrir la app.
    var logoVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { logoVisible = true }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            AnimatedVisibility(
                visible = logoVisible,
                enter = fadeIn() + scaleIn(
                    initialScale = 0.6f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                ),
            ) {
                Icon(
                    imageVector = Icons.Filled.School,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(72.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            Text("PrestaAula", style = MaterialTheme.typography.headlineLarge)
            Text(
                "Colegio Marcela Paz",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Acceso exclusivo para personal del establecimiento",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(32.dp))

            CampoValidado(
                valor = form.correo,
                onValorChange = onCorreoChange,
                etiqueta = "Correo",
                icono = Icons.Filled.Email,
                error = form.correoError,
                intentos = form.intentos,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) }),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(4.dp))
            CampoValidado(
                valor = form.clave,
                onValorChange = onClaveChange,
                etiqueta = "Contraseña",
                icono = Icons.Filled.Lock,
                error = form.claveError,
                intentos = form.intentos,
                visualTransformation = if (form.mostrarClave) VisualTransformation.None
                else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = {
                    focus.clearFocus()
                    onIniciarSesion()
                }),
                iconoFinal = {
                    IconButton(onClick = onToggleMostrarClave) {
                        Icon(
                            imageVector = if (form.mostrarClave) Icons.Filled.VisibilityOff
                            else Icons.Filled.Visibility,
                            contentDescription = if (form.mostrarClave) "Ocultar contraseña"
                            else "Mostrar contraseña",
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )

            MensajeError(form.error, Modifier.fillMaxWidth())

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onIniciarSesion,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp),
            ) {
                Text("Iniciar sesión")
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun LoginPreview() {
    PrestaaulaTheme(dynamicColor = false) {
        LoginScreen(LoginFormState(correo = "docente@marcelapaz.cl"), {}, {}, {}, {})
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun LoginErrorPreview() {
    PrestaaulaTheme(dynamicColor = false) {
        LoginScreen(
            LoginFormState("cra@marcelapaz.cl", "xxxx", true, "Correo o contraseña incorrectos"),
            {}, {}, {}, {},
        )
    }
}
