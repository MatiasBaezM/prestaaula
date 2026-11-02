package appmovil.prestaaula.model

data class Usuario(
    val id: Int,
    val nombre: String,
    val correo: String,
    val rol: Rol,
)
