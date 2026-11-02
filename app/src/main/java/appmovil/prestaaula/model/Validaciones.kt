package appmovil.prestaaula.model

// Reglas de validación de todos los formularios en un solo lugar, sin nada de interfaz:
// cada función devuelve el mensaje de error del campo, o null si el valor es válido.
// Las pantallas solo muestran el mensaje; los ViewModels lo usan para decidir si continúan.
object Validaciones {

    private val FORMATO_CORREO = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]{2,}$")

    // 7°A, 7A, 7 A, 2°M, 12°B…
    private val FORMATO_CURSO = Regex("^\\d{1,2}\\s?°?\\s?[A-Za-z]$")

    const val MAX_MARCA = 30
    const val MAX_OBSERVACIONES = 200

    fun correo(valor: String): String? {
        val v = valor.trim()
        return when {
            v.isEmpty() -> "Ingresa tu correo"
            !FORMATO_CORREO.matches(v) -> "Escribe un correo válido (nombre@colegio.cl)"
            else -> null
        }
    }

    fun clave(valor: String): String? =
        if (valor.isEmpty()) "Ingresa tu contraseña" else null

    fun curso(valor: String): String? {
        val v = valor.trim()
        return when {
            v.isEmpty() -> "Ingresa el curso"
            !FORMATO_CURSO.matches(v) -> "Usa el formato 7°A"
            else -> null
        }
    }

    fun marca(valor: String): String? {
        val v = valor.trim()
        return when {
            v.isEmpty() -> "Ingresa la marca"
            v.length < 2 -> "La marca debe tener al menos 2 letras"
            v.length > MAX_MARCA -> "Máximo $MAX_MARCA caracteres"
            else -> null
        }
    }

    // Con el equipo en buen estado las observaciones son opcionales; con daños o faltantes, obligatorias.
    fun observaciones(estado: EstadoDevolucion, valor: String): String? {
        val v = valor.trim()
        return when {
            estado != EstadoDevolucion.BUEN_ESTADO && v.isEmpty() -> "Describe el problema en las observaciones"
            v.length > MAX_OBSERVACIONES -> "Máximo $MAX_OBSERVACIONES caracteres"
            else -> null
        }
    }
}
