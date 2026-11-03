package appmovil.prestaaula

import android.app.Application
import android.util.Log
import appmovil.prestaaula.data.InventarioRepository
import appmovil.prestaaula.data.PrestamoRepository
import appmovil.prestaaula.data.ReservaRepository
import appmovil.prestaaula.data.db.Instantanea
import appmovil.prestaaula.data.db.PrestaAulaDb
import appmovil.prestaaula.data.db.sincronizar
import appmovil.prestaaula.data.db.aEntidad
import appmovil.prestaaula.data.db.aModelo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class PrestaAulaApp : Application() {
    // Repositorios compartidos por todas las pantallas (misma fuente de verdad).
    lateinit var inventario: InventarioRepository
        private set
    lateinit var reservas: ReservaRepository
        private set
    lateinit var prestamos: PrestamoRepository
        private set

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        val dao = PrestaAulaDb.crear(this).almacen()

        // Lectura única al arrancar (son pocas filas). Sin equipos guardados (primera vez, o BD
        // ilegible) cada repositorio parte con sus datos de ejemplo, en vez de caerse.
        // Las fechas y enums se convierten al leer (Room + Convertidores), dentro del try: una fila
        // corrupta no debe tirar la app al abrir.
        val leida = try {
            runBlocking(Dispatchers.IO) {
                Instantanea(dao.equipos(), dao.reservas(), dao.prestamos())
            }
        } catch (e: Exception) {
            Log.w("PrestaAula", "No se pudo leer la base de datos", e)
            null
        }

        // Lo que hay en disco ahora mismo, o null si hay que reescribir todo en el primer guardado.
        var enDisco: Instantanea?
        if (leida != null && leida.equipos.isNotEmpty()) {
            inventario = InventarioRepository(leida.equipos.map { it.aModelo() })
            reservas = ReservaRepository(inventario, leida.reservas.map { it.aModelo() })
            prestamos = PrestamoRepository(inventario, reservas, leida.prestamos.map { it.aModelo() })
            enDisco = leida
        } else {
            inventario = InventarioRepository()
            reservas = ReservaRepository(inventario)
            prestamos = PrestamoRepository(inventario, reservas)
            enDisco = null
        }

        // Ante cualquier cambio se escribe en una transacción solo lo que cambió respecto del disco.
        // drop(1): lo inicial no se escribe, así los datos de ejemplo siguen relativos a "hoy"
        // hasta el primer cambio real. El collect es secuencial: enDisco no tiene carreras.
        scope.launch {
            combine(inventario.equipos, reservas.reservas, prestamos.prestamos, ::Triple)
                .drop(1)
                .collect { (equipos, listaReservas, listaPrestamos) ->
                    val nueva = Instantanea(
                        equipos.map { it.aEntidad() },
                        listaReservas.map { it.aEntidad() },
                        listaPrestamos.map { it.aEntidad() },
                    )
                    try {
                        dao.sincronizar(enDisco, nueva)
                        enDisco = nueva
                    } catch (e: Exception) {
                        // Sin espacio u otro error de disco: la transacción se revirtió, enDisco sigue
                        // valiendo y el próximo cambio reintenta también lo que faltó. No cierra la app.
                        Log.w("PrestaAula", "No se pudieron guardar los datos", e)
                    }
                }
        }
    }
}
