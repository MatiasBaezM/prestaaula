package appmovil.prestaaula

import android.app.Application
import android.util.Log
import appmovil.prestaaula.data.InventarioRepository
import appmovil.prestaaula.data.PrestamoRepository
import appmovil.prestaaula.data.ReservaRepository
import appmovil.prestaaula.data.db.PrestaAulaDb
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
        // El mapeo a modelo también va dentro del try: una fila corrupta (fecha o enum inválidos)
        // no debe tirar la app al abrir.
        val guardados = try {
            runBlocking(Dispatchers.IO) {
                Triple(
                    dao.equipos().map { it.aModelo() },
                    dao.reservas().map { it.aModelo() },
                    dao.prestamos().map { it.aModelo() },
                )
            }
        } catch (e: Exception) {
            Log.w("PrestaAula", "No se pudo leer la base de datos", e)
            null
        }

        if (guardados != null && guardados.first.isNotEmpty()) {
            val (e, r, p) = guardados
            inventario = InventarioRepository(e)
            reservas = ReservaRepository(inventario, r)
            prestamos = PrestamoRepository(inventario, reservas, p)
        } else {
            inventario = InventarioRepository()
            reservas = ReservaRepository(inventario)
            prestamos = PrestamoRepository(inventario, reservas)
        }

        // Ante cualquier cambio se guardan los tres juntos, en una transacción.
        // drop(1): lo inicial no se escribe, así los datos de ejemplo siguen relativos a "hoy"
        // hasta el primer cambio real.
        scope.launch {
            combine(inventario.equipos, reservas.reservas, prestamos.prestamos, ::Triple)
                .drop(1)
                .collect { (equipos, listaReservas, listaPrestamos) ->
                    try {
                        dao.reemplazarTodo(
                            equipos.map { it.aEntidad() },
                            listaReservas.map { it.aEntidad() },
                            listaPrestamos.map { it.aEntidad() },
                        )
                    } catch (e: Exception) {
                        // Sin espacio u otro error de disco: se reintenta en el próximo cambio, sin cerrar la app.
                        Log.w("PrestaAula", "No se pudieron guardar los datos", e)
                    }
                }
        }
    }
}
