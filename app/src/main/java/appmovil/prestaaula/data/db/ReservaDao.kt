package appmovil.prestaaula.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

// Acceso a la tabla "reservas".
@Dao
interface ReservaDao {

    @Query("SELECT * FROM reservas ORDER BY id")
    fun observarTodas(): Flow<List<ReservaEntity>>

    // Reservas de un equipo: lo único que hace falta para revisar choques de horario.
    @Query("SELECT * FROM reservas WHERE equipoId = :equipoId")
    suspend fun obtenerDeEquipo(equipoId: Int): List<ReservaEntity>

    @Query("SELECT * FROM reservas WHERE id IN (:ids)")
    suspend fun obtenerPorIds(ids: List<Int>): List<ReservaEntity>

    // Devuelve los ids asignados, en el mismo orden de la lista.
    @Insert
    suspend fun insertar(reservas: List<ReservaEntity>): List<Long>

    @Query("DELETE FROM reservas WHERE id IN (:ids)")
    suspend fun borrar(ids: List<Int>)
}
