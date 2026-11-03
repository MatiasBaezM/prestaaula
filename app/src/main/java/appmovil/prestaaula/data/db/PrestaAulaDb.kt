package appmovil.prestaaula.data.db

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction

@Dao
abstract class AlmacenDao {

    @Query("SELECT * FROM equipos ORDER BY id")
    abstract suspend fun equipos(): List<EquipoEntity>

    @Query("SELECT * FROM reservas ORDER BY id")
    abstract suspend fun reservas(): List<ReservaEntity>

    @Query("SELECT * FROM prestamos ORDER BY id")
    abstract suspend fun prestamos(): List<PrestamoEntity>

    @Insert
    abstract suspend fun insertarEquipos(filas: List<EquipoEntity>)

    @Insert
    abstract suspend fun insertarReservas(filas: List<ReservaEntity>)

    @Insert
    abstract suspend fun insertarPrestamos(filas: List<PrestamoEntity>)

    @Query("DELETE FROM equipos")
    abstract suspend fun borrarEquipos()

    @Query("DELETE FROM reservas")
    abstract suspend fun borrarReservas()

    @Query("DELETE FROM prestamos")
    abstract suspend fun borrarPrestamos()

    // Las tres tablas se reemplazan en una sola transacción: si algo falla a medias, SQLite
    // revierte y en disco nunca queda el inventario de una versión con los préstamos de otra.
    @Transaction
    open suspend fun reemplazarTodo(
        equipos: List<EquipoEntity>,
        reservas: List<ReservaEntity>,
        prestamos: List<PrestamoEntity>,
    ) {
        borrarEquipos()
        borrarReservas()
        borrarPrestamos()
        insertarEquipos(equipos)
        insertarReservas(reservas)
        insertarPrestamos(prestamos)
    }
}

@Database(
    entities = [EquipoEntity::class, ReservaEntity::class, PrestamoEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class PrestaAulaDb : RoomDatabase() {
    abstract fun almacen(): AlmacenDao

    companion object {
        fun crear(contexto: Context): PrestaAulaDb =
            Room.databaseBuilder(contexto.applicationContext, PrestaAulaDb::class.java, "prestaaula.db")
                // Sin migraciones: si cambia el esquema se recrea la base (los datos se pierden y la
                // app vuelve a partir con los de ejemplo).
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
    }
}
