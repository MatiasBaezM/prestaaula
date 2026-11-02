package appmovil.prestaaula.ui.login

import androidx.lifecycle.ViewModel
import appmovil.prestaaula.data.AuthRepository
import appmovil.prestaaula.data.Autenticador
import appmovil.prestaaula.data.ResultadoLogin
import appmovil.prestaaula.model.Usuario
import appmovil.prestaaula.model.Validaciones
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class LoginFormState(
    val correo: String = "",
    val clave: String = "",
    val mostrarClave: Boolean = false,
    // Error general (credenciales rechazadas). Los errores de cada campo salen de Validaciones.
    val error: String? = null,
    // Veces que se intentó enviar con datos inválidos: la pantalla lo usa para mostrar y animar los errores.
    val intentos: Int = 0,
) {
    val correoError: String? get() = Validaciones.correo(correo)
    val claveError: String? get() = Validaciones.clave(clave)
    val valido: Boolean get() = correoError == null && claveError == null
}

class AuthViewModel(
    private val repositorio: Autenticador = AuthRepository(),
) : ViewModel() {

    private val _form = MutableStateFlow(LoginFormState())
    val form: StateFlow<LoginFormState> = _form.asStateFlow()

    // Sesión viva mientras viva el ViewModel: sobrevive a la rotación.
    private val _usuarioActual = MutableStateFlow<Usuario?>(null)
    val usuarioActual: StateFlow<Usuario?> = _usuarioActual.asStateFlow()

    fun onCorreoChange(correo: String) = _form.update { it.copy(correo = correo, error = null) }

    fun onClaveChange(clave: String) = _form.update { it.copy(clave = clave, error = null) }

    fun toggleMostrarClave() = _form.update { it.copy(mostrarClave = !it.mostrarClave) }

    fun iniciarSesion() {
        val f = _form.value
        if (!f.valido) {
            _form.update { it.copy(intentos = it.intentos + 1, error = null) }
            return
        }
        when (val resultado = repositorio.iniciarSesion(f.correo, f.clave)) {
            is ResultadoLogin.Exito -> {
                _usuarioActual.value = resultado.usuario
                _form.value = LoginFormState()
            }
            is ResultadoLogin.Error -> _form.update { it.copy(error = resultado.mensaje) }
        }
    }

    fun cerrarSesion() {
        _usuarioActual.value = null
        _form.value = LoginFormState()
    }
}
