package appmovil.prestaaula.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

// Base de datos SQLite del dispositivo ("prestaaula.db"). Room crea las tablas a partir de las
// entidades y genera la implementación de cada DAO.
@Database(
    entities = [EquipoEntity::class, ReservaEntity::class, PrestamoEntity::class],
    // v2: claves foráneas e índices en equipoId. v3: ids autogenerados y un DAO por tabla.
    version = 3,
    exportSchema = false,
)
@TypeConverters(Convertidores::class)
abstract class PrestaAulaDb : RoomDatabase() {
    abstract fun equipoDao(): EquipoDao
    abstract fun reservaDao(): ReservaDao
    abstract fun prestamoDao(): PrestamoDao

    companion object {
        fun crear(contexto: Context): PrestaAulaDb =
            Room.databaseBuilder(contexto.applicationContext, PrestaAulaDb::class.java, "prestaaula.db")
                // Sin migraciones: si cambia el esquema se recrea la base (los datos se pierden y la
                // app vuelve a partir con los de ejemplo).
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
    }
}
