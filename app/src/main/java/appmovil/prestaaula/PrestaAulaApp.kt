package appmovil.prestaaula

import android.app.Application
import android.util.Log
import appmovil.prestaaula.data.DatosDemo
import appmovil.prestaaula.data.InventarioRepository
import appmovil.prestaaula.data.PrestamoRepository
import appmovil.prestaaula.data.ReservaRepository
import appmovil.prestaaula.data.db.PrestaAulaDb
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

// Contenedor de dependencias de la app: crea una sola base de datos y un solo repositorio de cada
// tipo, que comparten todas las pantallas (misma fuente de verdad). Los ViewModels los reciben por
// constructor (ver vmDeApp en AppNavHost).
class PrestaAulaApp : Application() {
    lateinit var db: PrestaAulaDb
        private set
    lateinit var inventario: InventarioRepository
        private set
    lateinit var reservas: ReservaRepository
        private set
    lateinit var prestamos: PrestamoRepository
        private set

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        db = PrestaAulaDb.crear(this)
        inventario = InventarioRepository(db)
        reservas = ReservaRepository(db)
        prestamos = PrestamoRepository(db)

        // Primera vez: se guardan los datos de ejemplo en segundo plano. Las pantallas observan la base,
        // así que se actualizan solas apenas terminan de guardarse.
        scope.launch {
            try {
                DatosDemo.cargarSiEstaVacia(db)
            } catch (e: Exception) {
                Log.w("PrestaAula", "No se pudieron cargar los datos de ejemplo", e)
            }
        }
    }
}
