package appmovil.prestaaula.data

import appmovil.prestaaula.model.Equipo
import appmovil.prestaaula.model.EstadoDevolucion
import appmovil.prestaaula.model.EstadoEquipo
import appmovil.prestaaula.model.Prestamo
import appmovil.prestaaula.model.Reserva
import appmovil.prestaaula.model.TipoEquipo
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime

// Archivo JSON en el almacenamiento privado de la app. Se escribe a un temporal y se renombra,
// así un corte a mitad de escritura nunca deja el archivo a medias.
class ArchivoJson(private val archivo: File) {

    fun escribir(texto: String) {
        val temporal = File(archivo.path + ".tmp")
        temporal.writeText(texto)
        check(temporal.renameTo(archivo)) { "No se pudo guardar ${archivo.name}" }
    }

    // null si no existe o está dañado: la app arranca con los datos de ejemplo en vez de caerse.
    fun <T> cargar(decodificar: (String) -> T): T? = try {
        if (archivo.exists()) decodificar(archivo.readText()) else null
    } catch (e: Exception) {
        null
    }
}

object Codec {

    fun equiposAJson(lista: List<Equipo>): String = arreglo(lista) {
        JSONObject()
            .put("id", it.id).put("codigo", it.codigo).put("tipo", it.tipo.name)
            .put("marca", it.marca).put("estado", it.estado.name)
    }

    fun equiposDeJson(json: String): List<Equipo> = leer(json) {
        Equipo(
            it.getInt("id"), it.getString("codigo"), TipoEquipo.valueOf(it.getString("tipo")),
            it.getString("marca"), EstadoEquipo.valueOf(it.getString("estado")),
        )
    }

    fun reservasAJson(lista: List<Reserva>): String = arreglo(lista) {
        JSONObject()
            .put("id", it.id).put("equipoId", it.equipoId).put("docenteId", it.docenteId)
            .put("docente", it.docente).put("curso", it.curso)
            .put("fecha", it.fecha.toString()).put("bloque", it.bloque)
    }

    fun reservasDeJson(json: String): List<Reserva> = leer(json) {
        Reserva(
            it.getInt("id"), it.getInt("equipoId"), it.getInt("docenteId"), it.getString("docente"),
            it.getString("curso"), LocalDate.parse(it.getString("fecha")), it.getInt("bloque"),
        )
    }

    fun prestamosAJson(lista: List<Prestamo>): String = arreglo(lista) {
        JSONObject()
            .put("id", it.id).put("equipoId", it.equipoId).put("docenteId", it.docenteId)
            .put("docente", it.docente).put("curso", it.curso).put("retiro", it.retiro.toString())
            .put("retiroConfirmado", it.retiroConfirmado).put("devolucionAvisada", it.devolucionAvisada)
            .put("devolucion", it.devolucion?.toString() ?: JSONObject.NULL)
            .put("estadoDevolucion", it.estadoDevolucion?.name ?: JSONObject.NULL)
            .put("observaciones", it.observaciones)
            .put("foto", it.foto ?: JSONObject.NULL)
    }

    fun prestamosDeJson(json: String): List<Prestamo> = leer(json) {
        Prestamo(
            id = it.getInt("id"), equipoId = it.getInt("equipoId"), docenteId = it.getInt("docenteId"),
            docente = it.getString("docente"), curso = it.getString("curso"),
            retiro = LocalDateTime.parse(it.getString("retiro")),
            retiroConfirmado = it.getBoolean("retiroConfirmado"),
            devolucionAvisada = it.getBoolean("devolucionAvisada"),
            devolucion = if (it.isNull("devolucion")) null else LocalDateTime.parse(it.getString("devolucion")),
            estadoDevolucion = if (it.isNull("estadoDevolucion")) null
            else EstadoDevolucion.valueOf(it.getString("estadoDevolucion")),
            observaciones = it.getString("observaciones"),
            // isNull también es true si la clave no existe: lee archivos guardados antes de la foto.
            foto = if (it.isNull("foto")) null else it.getString("foto"),
        )
    }

    private fun <T> arreglo(lista: List<T>, aObjeto: (T) -> JSONObject): String =
        JSONArray().also { a -> lista.forEach { a.put(aObjeto(it)) } }.toString()

    private fun <T> leer(json: String, deObjeto: (JSONObject) -> T): List<T> {
        val a = JSONArray(json)
        return List(a.length()) { deObjeto(a.getJSONObject(it)) }
    }
}
