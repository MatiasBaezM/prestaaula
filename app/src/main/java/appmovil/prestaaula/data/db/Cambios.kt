package appmovil.prestaaula.data.db

// Lo que hay que escribir en una tabla para pasar de una versión de la lista a otra:
// filas nuevas o modificadas (guardar) e ids que ya no están (borrar).
data class Cambios<T>(val guardar: List<T>, val borrar: List<Int>) {
    val vacio: Boolean get() = guardar.isEmpty() && borrar.isEmpty()
}

// Compara por id y, dentro del mismo id, por igualdad de data class: una fila sin cambios no se escribe.
fun <T> diferencias(antes: List<T>, ahora: List<T>, id: (T) -> Int): Cambios<T> {
    val previos = antes.associateBy(id)
    val idsActuales = ahora.mapTo(HashSet(), id)
    return Cambios(
        guardar = ahora.filter { previos[id(it)] != it },
        borrar = antes.map(id).filterNot { it in idsActuales },
    )
}

// Las tres tablas tal como quedaron en disco tras la última escritura exitosa.
data class Instantanea(
    val equipos: List<EquipoEntity>,
    val reservas: List<ReservaEntity>,
    val prestamos: List<PrestamoEntity>,
)

// Lleva el disco de `enDisco` a `nueva`. Sin instantánea previa (primera vez, o la base no se pudo
// leer) se reemplaza todo, lo que además limpia filas corruptas; si no, se escribe solo lo cambiado.
suspend fun AlmacenDao.sincronizar(enDisco: Instantanea?, nueva: Instantanea) {
    if (enDisco == null) {
        reemplazarTodo(nueva.equipos, nueva.reservas, nueva.prestamos)
        return
    }
    val equipos = diferencias(enDisco.equipos, nueva.equipos) { it.id }
    val reservas = diferencias(enDisco.reservas, nueva.reservas) { it.id }
    val prestamos = diferencias(enDisco.prestamos, nueva.prestamos) { it.id }
    if (equipos.vacio && reservas.vacio && prestamos.vacio) return
    aplicar(equipos, reservas, prestamos)
}
