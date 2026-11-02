package appmovil.prestaaula.data

import appmovil.prestaaula.model.Rol
import appmovil.prestaaula.model.Usuario

// Nómina de docentes para elegir a quién se entrega un equipo.
// Demo: en producción vendría de la misma API que valida el login.
object Docentes {
    val demo: List<Usuario> = listOf(
        Usuario(1, "Prof. Muñoz", "docente@marcelapaz.cl", Rol.DOCENTE),
        Usuario(101, "Prof. Rojas", "rojas@marcelapaz.cl", Rol.DOCENTE),
        Usuario(102, "Prof. Soto", "soto@marcelapaz.cl", Rol.DOCENTE),
        Usuario(103, "Prof. Díaz", "diaz@marcelapaz.cl", Rol.DOCENTE),
    )
}
