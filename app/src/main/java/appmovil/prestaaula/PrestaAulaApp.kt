package appmovil.prestaaula

import android.app.Application
import android.util.Log
import appmovil.prestaaula.data.ArchivoJson
import appmovil.prestaaula.data.Codec
import appmovil.prestaaula.data.InventarioRepository
import appmovil.prestaaula.data.PrestamoRepository
import appmovil.prestaaula.data.ReservaRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import java.io.File

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
        val archivoEquipos = ArchivoJson(File(filesDir, "equipos.json"))
        val archivoReservas = ArchivoJson(File(filesDir, "reservas.json"))
        val archivoPrestamos = ArchivoJson(File(filesDir, "prestamos.json"))

        // Sin archivo (primera vez) cada repositorio parte con sus datos de ejemplo.
        inventario = archivoEquipos.cargar(Codec::equiposDeJson)
            ?.let { InventarioRepository(it) } ?: InventarioRepository()
        reservas = archivoReservas.cargar(Codec::reservasDeJson)
            ?.let { ReservaRepository(inventario, it) } ?: ReservaRepository(inventario)
        prestamos = archivoPrestamos.cargar(Codec::prestamosDeJson)
            ?.let { PrestamoRepository(inventario, reservas, it) } ?: PrestamoRepository(inventario, reservas)

        // Ante cualquier cambio se guardan los tres juntos: en disco nunca queda el inventario de una
        // versión con los préstamos de otra (p. ej. un préstamo activo con su equipo "Disponible").
        // drop(1): lo inicial no se escribe, así los datos de ejemplo siguen relativos a "hoy"
        // hasta el primer cambio real.
        scope.launch {
            combine(inventario.equipos, reservas.reservas, prestamos.prestamos, ::Triple)
                .drop(1)
                .collect { (equipos, listaReservas, listaPrestamos) ->
                    try {
                        archivoEquipos.escribir(Codec.equiposAJson(equipos))
                        archivoReservas.escribir(Codec.reservasAJson(listaReservas))
                        archivoPrestamos.escribir(Codec.prestamosAJson(listaPrestamos))
                    } catch (e: Exception) {
                        // Sin espacio u otro error de disco: se reintenta en el próximo cambio, sin cerrar la app.
                        Log.w("PrestaAula", "No se pudieron guardar los datos", e)
                    }
                }
        }
    }
}
