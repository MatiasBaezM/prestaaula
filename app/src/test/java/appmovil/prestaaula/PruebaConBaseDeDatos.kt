package appmovil.prestaaula

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import appmovil.prestaaula.data.db.PrestaAulaDb
import appmovil.prestaaula.data.db.aEntidad
import appmovil.prestaaula.data.db.aModelo
import appmovil.prestaaula.model.Equipo
import appmovil.prestaaula.model.Prestamo
import appmovil.prestaaula.model.Reserva
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// Base de los tests que usan la base de datos real: cada test parte con una base Room nueva en
// memoria (Robolectric la corre en la JVM, sin emulador) y se borra al terminar.
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
abstract class PruebaConBaseDeDatos {

    protected lateinit var db: PrestaAulaDb

    @Before
    fun abrirBase() {
        // viewModelScope usa el hilo principal de Android; en la JVM se reemplaza por uno de prueba.
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val contexto = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(contexto, PrestaAulaDb::class.java).build()
    }

    @After
    fun cerrarBase() {
        db.close()
        Dispatchers.resetMain()
    }

    // Corre un test con funciones suspend. Devuelve Unit, como exige JUnit.
    protected fun prueba(cuerpo: suspend CoroutineScope.() -> Unit) = runBlocking { cuerpo() }

    // Datos iniciales del test, escritos directo en las tablas (equipos primero por la clave foránea).
    protected fun guardar(
        equipos: List<Equipo> = emptyList(),
        reservas: List<Reserva> = emptyList(),
        prestamos: List<Prestamo> = emptyList(),
    ) = runBlocking {
        db.equipoDao().insertarTodos(equipos.map { it.aEntidad() })
        db.reservaDao().insertar(reservas.map { it.aEntidad() })
        db.prestamoDao().insertarTodos(prestamos.map { it.aEntidad() })
    }

    // Lo que quedó guardado en cada tabla.
    protected fun equiposEnBase(): List<Equipo> = runBlocking { db.equipoDao().obtenerTodos().map { it.aModelo() } }
    protected fun reservasEnBase(): List<Reserva> = runBlocking { db.reservaDao().observarTodas().first().map { it.aModelo() } }
    protected fun prestamosEnBase(): List<Prestamo> = runBlocking { db.prestamoDao().observarTodos().first().map { it.aModelo() } }
    protected fun estadoDe(equipoId: Int) = equiposEnBase().first { it.id == equipoId }.estado

    // Llama a una acción del ViewModel, espera a que termine de guardarse y devuelve lo que avisó
    // (null = se guardó, o el mensaje de error).
    protected fun resultado(accion: (alTerminar: (String?) -> Unit) -> Job): String? = runBlocking {
        var respuesta: String? = "sin respuesta"
        accion { respuesta = it }.join()
        respuesta
    }

    // Los StateFlow de los ViewModels se llenan cuando Room termina de leer: espera a que se cumpla.
    protected fun esperarQue(condicion: () -> Boolean) = runBlocking {
        withTimeout(5_000) { while (!condicion()) delay(10) }
    }
}
