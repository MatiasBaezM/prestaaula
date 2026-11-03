package appmovil.prestaaula.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import appmovil.prestaaula.model.EstadoEquipo
import kotlinx.coroutines.flow.Flow

// Acceso a la tabla "equipos". Room genera el SQL de @Insert/@Update y valida el de @Query al compilar.
// Las funciones suspend corren fuera del hilo principal; el Flow vuelve a emitir cada vez que la
// tabla cambia, así la UI se actualiza sola.
@Dao
interface EquipoDao {

    // Recuperar (observando): la lista que muestran las pantallas.
    @Query("SELECT * FROM equipos ORDER BY id")
    fun observarTodos(): Flow<List<EquipoEntity>>

    // Recuperar (una vez): para validar dentro de una operación.
    @Query("SELECT * FROM equipos ORDER BY id")
    suspend fun obtenerTodos(): List<EquipoEntity>

    @Query("SELECT * FROM equipos WHERE id = :id")
    suspend fun obtener(id: Int): EquipoEntity?

    @Query("SELECT COUNT(*) FROM equipos")
    suspend fun contar(): Int

    // Insertar: devuelve el id que SQLite le asignó.
    @Insert
    suspend fun insertar(equipo: EquipoEntity): Long

    @Insert
    suspend fun insertarTodos(equipos: List<EquipoEntity>)

    // Actualizar: reemplaza la fila con el mismo id.
    @Update
    suspend fun actualizar(equipo: EquipoEntity)

    @Query("UPDATE equipos SET estado = :estado WHERE id = :id")
    suspend fun cambiarEstado(id: Int, estado: EstadoEquipo)
}
