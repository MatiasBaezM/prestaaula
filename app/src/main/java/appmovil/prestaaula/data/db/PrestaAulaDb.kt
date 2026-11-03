package appmovil.prestaaula.data.db

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.TypeConverters
import androidx.room.Upsert

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

    // @Upsert inserta o actualiza en el lugar. No usar @Insert(onConflict = REPLACE): reemplazar
    // borra la fila y la vuelve a crear, y borrar un equipo con reservas lo impide la clave foránea.
    @Upsert
    abstract suspend fun guardarEquipos(filas: List<EquipoEntity>)

    @Upsert
    abstract suspend fun guardarReservas(filas: List<ReservaEntity>)

    @Upsert
    abstract suspend fun guardarPrestamos(filas: List<PrestamoEntity>)

    @Query("DELETE FROM equipos")
    abstract suspend fun borrarEquipos()

    @Query("DELETE FROM reservas")
    abstract suspend fun borrarReservas()

    @Query("DELETE FROM prestamos")
    abstract suspend fun borrarPrestamos()

    @Query("DELETE FROM equipos WHERE id IN (:ids)")
    abstract suspend fun borrarEquipos(ids: List<Int>)

    @Query("DELETE FROM reservas WHERE id IN (:ids)")
    abstract suspend fun borrarReservas(ids: List<Int>)

    @Query("DELETE FROM prestamos WHERE id IN (:ids)")
    abstract suspend fun borrarPrestamos(ids: List<Int>)

    // Las tres tablas se reemplazan en una sola transacción: si algo falla a medias, SQLite
    // revierte y en disco nunca queda el inventario de una versión con los préstamos de otra.
    // El orden importa por las claves foráneas: se borran primero las tablas que apuntan a
    // equipos y se insertan los equipos antes que sus reservas y préstamos.
    // Se usa solo para el primer guardado (o si la base estaba ilegible); después, aplicar().
    @Transaction
    open suspend fun reemplazarTodo(
        equipos: List<EquipoEntity>,
        reservas: List<ReservaEntity>,
        prestamos: List<PrestamoEntity>,
    ) {
        borrarPrestamos()
        borrarReservas()
        borrarEquipos()
        insertarEquipos(equipos)
        insertarReservas(reservas)
        insertarPrestamos(prestamos)
    }

    // Escribe solo lo que cambió, en una transacción y con el mismo orden que reemplazarTodo.
    @Transaction
    open suspend fun aplicar(
        equipos: Cambios<EquipoEntity>,
        reservas: Cambios<ReservaEntity>,
        prestamos: Cambios<PrestamoEntity>,
    ) {
        if (prestamos.borrar.isNotEmpty()) borrarPrestamos(prestamos.borrar)
        if (reservas.borrar.isNotEmpty()) borrarReservas(reservas.borrar)
        if (equipos.borrar.isNotEmpty()) borrarEquipos(equipos.borrar)
        if (equipos.guardar.isNotEmpty()) guardarEquipos(equipos.guardar)
        if (reservas.guardar.isNotEmpty()) guardarReservas(reservas.guardar)
        if (prestamos.guardar.isNotEmpty()) guardarPrestamos(prestamos.guardar)
    }
}

@Database(
    entities = [EquipoEntity::class, ReservaEntity::class, PrestamoEntity::class],
    // v2: claves foráneas e índices en equipoId.
    version = 2,
    exportSchema = false,
)
@TypeConverters(Convertidores::class)
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
