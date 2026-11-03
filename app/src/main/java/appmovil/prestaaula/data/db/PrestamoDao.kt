package appmovil.prestaaula.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

// Acceso a la tabla "prestamos": el historial de entregas y devoluciones.
@Dao
interface PrestamoDao {

    @Query("SELECT * FROM prestamos ORDER BY id")
    fun observarTodos(): Flow<List<PrestamoEntity>>

    @Query("SELECT * FROM prestamos WHERE id = :id")
    suspend fun obtener(id: Int): PrestamoEntity?

    @Insert
    suspend fun insertar(prestamo: PrestamoEntity): Long

    @Insert
    suspend fun insertarTodos(prestamos: List<PrestamoEntity>)

    // Registrar la devolución, confirmar el retiro o avisar la devolución actualizan la misma fila.
    @Update
    suspend fun actualizar(prestamo: PrestamoEntity)
}
