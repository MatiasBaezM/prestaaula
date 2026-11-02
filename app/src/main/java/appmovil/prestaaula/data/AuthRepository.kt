package appmovil.prestaaula.data

import appmovil.prestaaula.model.Rol
import appmovil.prestaaula.model.Usuario

sealed interface ResultadoLogin {
    data class Exito(val usuario: Usuario) : ResultadoLogin
    data class Error(val mensaje: String) : ResultadoLogin
}

// Contrato del login. Hoy lo implementa AuthRepository (demo local); mañana, un cliente REST
// que valide contra el servidor sin que cambie el ViewModel ni la pantalla.
interface Autenticador {
    fun iniciarSesion(correo: String, clave: String): ResultadoLogin
}

class AuthRepository : Autenticador {

    private class Credencial(val clave: String, val usuario: Usuario)

    // SOLO DEMO: en producción las credenciales se validan contra una API
    // (con hash en el servidor) y nunca van en el código de la app.
    private val usuariosDemo: Map<String, Credencial> = mapOf(
        "docente@marcelapaz.cl" to Credencial(
            "docente123", Usuario(1, "Prof. Muñoz", "docente@marcelapaz.cl", Rol.DOCENTE),
        ),
        "cra@marcelapaz.cl" to Credencial(
            "cra123", Usuario(2, "Patricia Vega", "cra@marcelapaz.cl", Rol.ENCARGADO_CRA),
        ),
        "utp@marcelapaz.cl" to Credencial(
            "utp123", Usuario(3, "Andrés Silva", "utp@marcelapaz.cl", Rol.JEFE_UTP),
        ),
        "finanzas@marcelapaz.cl" to Credencial(
            "admin123",
            Usuario(4, "Carolina Núñez", "finanzas@marcelapaz.cl", Rol.JEFE_ADMIN_FINANZAS),
        ),
    )

    // Misma firma que tendrá la llamada REST: el cuerpo se reemplaza después.
    override fun iniciarSesion(correo: String, clave: String): ResultadoLogin {
        val correoNormalizado = correo.trim().lowercase()
        if (correoNormalizado.isEmpty() || clave.isEmpty()) {
            return ResultadoLogin.Error("Ingresa tu correo y contraseña")
        }
        val credencial = usuariosDemo[correoNormalizado]
        return if (credencial != null && credencial.clave == clave) {
            ResultadoLogin.Exito(credencial.usuario)
        } else {
            ResultadoLogin.Error("Correo o contraseña incorrectos")
        }
    }
}
