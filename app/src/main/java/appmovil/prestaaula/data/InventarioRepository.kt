package appmovil.prestaaula.data

import androidx.room.withTransaction
import appmovil.prestaaula.data.db.PrestaAulaDb
import appmovil.prestaaula.data.db.aEntidad
import appmovil.prestaaula.data.db.aModelo
import appmovil.prestaaula.model.Equipo
import appmovil.prestaaula.model.EstadoEquipo
import appmovil.prestaaula.model.TipoEquipo
import appmovil.prestaaula.model.siguienteCodigo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Repositorio del inventario: única puerta de los ViewModels a la tabla "equipos".
// Traduce entre filas de Room (EquipoEntity) y el modelo (Equipo), y aplica las reglas del inventario.
class InventarioRepository(private val db: PrestaAulaDb) {

    private val dao = db.equipoDao()

    // Se actualiza solo cada vez que cambia la tabla.
    val equipos: Flow<List<Equipo>> = dao.observarTodos().map { filas -> filas.map { it.aModelo() } }

    // `codigo` null = asignarlo automáticamente. El estado inicial es Disponible o En mantención.
    // Devuelve el equipo guardado (con el id que le dio SQLite), o null si el código ya existe.
    // Lee y escribe en la misma transacción: dos altas seguidas no pueden tomar el mismo código.
    suspend fun agregar(
        tipo: TipoEquipo,
        marca: String,
        codigo: String? = null,
        estado: EstadoEquipo = EstadoEquipo.DISPONIBLE,
    ): Equipo? = db.withTransaction {
        val actuales = dao.obtenerTodos().map { it.aModelo() }
        val nuevo = Equipo(
            id = 0, // lo asigna SQLite
            codigo = codigo?.trim()?.uppercase() ?: siguienteCodigo(actuales, tipo),
            tipo = tipo,
            marca = marca.trim(),
            estado = estado,
        )
        if (actuales.any { it.codigo == nuevo.codigo }) return@withTransaction null
        val id = dao.insertar(nuevo.aEntidad())
        nuevo.copy(id = id.toInt())
    }

    // El CRA saca de circulación un equipo dañado: deja de poder reservarse y prestarse hasta repararlo.
    // Solo aplica a un equipo Disponible; uno prestado se marca al devolverlo, con sus observaciones.
    suspend fun marcarDanado(id: Int): Boolean =
        cambiarSiEsta(id, desde = EstadoEquipo.DISPONIBLE, hacia = EstadoEquipo.EN_MANTENCION)

    // Cierra una mantención: solo aplica a equipos que están en mantención.
    suspend fun marcarReparado(id: Int): Boolean =
        cambiarSiEsta(id, desde = EstadoEquipo.EN_MANTENCION, hacia = EstadoEquipo.DISPONIBLE)

    suspend fun cambiarEstado(id: Int, estado: EstadoEquipo) = dao.cambiarEstado(id, estado)

    private suspend fun cambiarSiEsta(id: Int, desde: EstadoEquipo, hacia: EstadoEquipo): Boolean =
        db.withTransaction {
            val equipo = dao.obtener(id)
            if (equipo == null || equipo.estado != desde) return@withTransaction false
            dao.actualizar(equipo.copy(estado = hacia))
            true
        }
}
