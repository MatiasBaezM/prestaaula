package appmovil.prestaaula

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import appmovil.prestaaula.data.db.PrestaAulaDb
import appmovil.prestaaula.data.db.aEntidad
import appmovil.prestaaula.data.db.aModelo
import appmovil.prestaaula.model.Equipo
import appmovil.prestaaula.model.EstadoDevolucion
import appmovil.prestaaula.model.EstadoEquipo
import appmovil.prestaaula.model.Prestamo
import appmovil.prestaaula.model.Reserva
import appmovil.prestaaula.model.TipoEquipo
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.LocalDateTime

// Los DAOs contra el SQLite real del dispositivo (base en memoria, se borra al terminar cada test).
@RunWith(AndroidJUnit4::class)
class BaseDatosTest {

    private lateinit var db: PrestaAulaDb

    @Before
    fun abrir() {
        val contexto = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(contexto, PrestaAulaDb::class.java).build()
    }

    @After
    fun cerrar() = db.close()

    private val notebook = Equipo(0, "NB-001", TipoEquipo.NOTEBOOK, "Lenovo")
    private val fecha = LocalDate.of(2026, 10, 30)
    private val retiro = LocalDateTime.of(2026, 10, 29, 8, 0)

    @Test
    fun insertarAsignaIdYSeRecuperaIgual() = runBlocking {
        val id = db.equipoDao().insertar(notebook.aEntidad()).toInt()

        assertEquals(1, id)
        assertEquals(notebook.copy(id = id), db.equipoDao().obtener(id)?.aModelo())
        assertNull(db.equipoDao().obtener(99))
    }

    @Test
    fun actualizarModificaLaFila() = runBlocking {
        val id = db.equipoDao().insertar(notebook.aEntidad()).toInt()

        db.equipoDao().actualizar(notebook.copy(id = id, marca = "Dell").aEntidad())
        db.equipoDao().cambiarEstado(id, EstadoEquipo.EN_MANTENCION)

        val guardado = db.equipoDao().obtener(id)!!.aModelo()
        assertEquals("Dell", guardado.marca)
        assertEquals(EstadoEquipo.EN_MANTENCION, guardado.estado)
    }

    @Test
    fun reservasYPrestamosSeGuardanConSusFechas() = runBlocking {
        val equipoId = db.equipoDao().insertar(notebook.aEntidad()).toInt()
        val reserva = Reserva(0, equipoId, 2, "Prof. Díaz", "7°A", fecha, 3)
        val prestamo = Prestamo(0, equipoId, 2, "Prof. Díaz", "7°A", retiro)

        val idReserva = db.reservaDao().insertar(listOf(reserva.aEntidad())).single().toInt()
        val idPrestamo = db.prestamoDao().insertar(prestamo.aEntidad()).toInt()

        assertEquals(listOf(reserva.copy(id = idReserva)), db.reservaDao().obtenerDeEquipo(equipoId).map { it.aModelo() })
        assertEquals(prestamo.copy(id = idPrestamo), db.prestamoDao().obtener(idPrestamo)?.aModelo())
    }

    @Test
    fun registrarLaDevolucionActualizaElPrestamo() = runBlocking {
        val equipoId = db.equipoDao().insertar(notebook.aEntidad()).toInt()
        val id = db.prestamoDao().insertar(Prestamo(0, equipoId, 2, "Prof. Díaz", "7°A", retiro).aEntidad()).toInt()

        val devuelto = db.prestamoDao().obtener(id)!!.aModelo().copy(
            devolucion = retiro.plusHours(1), estadoDevolucion = EstadoDevolucion.CON_DANOS,
            observaciones = "Pantalla trizada", foto = "dev_1.jpg",
        )
        db.prestamoDao().actualizar(devuelto.aEntidad())

        assertEquals(devuelto, db.prestamoDao().obtener(id)?.aModelo())
    }

    @Test
    fun laListaObservadaTraeLoUltimoGuardado() = runBlocking {
        db.equipoDao().insertar(notebook.aEntidad())
        db.equipoDao().insertar(Equipo(0, "TB-001", TipoEquipo.TABLET, "Samsung").aEntidad())

        assertEquals(listOf("NB-001", "TB-001"), db.equipoDao().observarTodos().first().map { it.codigo })
    }

    @Test
    fun borrarReservasPorId() = runBlocking {
        val equipoId = db.equipoDao().insertar(notebook.aEntidad()).toInt()
        val ids = db.reservaDao().insertar(
            listOf(1, 2, 3).map { Reserva(0, equipoId, 2, "Prof. Díaz", "7°A", fecha, it).aEntidad() },
        ).map { it.toInt() }

        db.reservaDao().borrar(ids.take(2))

        assertEquals(listOf(ids[2]), db.reservaDao().observarTodas().first().map { it.id })
    }

    @Test
    fun codigoRepetidoSeRechaza() = runBlocking {
        db.equipoDao().insertar(notebook.aEntidad())
        try {
            db.equipoDao().insertar(notebook.copy(marca = "HP").aEntidad())
            fail("Debió rechazar un segundo equipo con el código NB-001")
        } catch (_: SQLiteConstraintException) {
        }
        assertEquals(1, db.equipoDao().contar())
    }

    @Test
    fun reservaDeEquipoInexistenteSeRechaza() = runBlocking {
        try {
            db.reservaDao().insertar(listOf(Reserva(0, 99, 2, "Prof. Díaz", "7°A", fecha, 3).aEntidad()))
            fail("Debió rechazar una reserva de un equipo que no existe")
        } catch (_: SQLiteConstraintException) {
        }
    }

    @Test
    fun prestamoDeEquipoInexistenteSeRechaza() = runBlocking {
        try {
            db.prestamoDao().insertar(Prestamo(0, 99, 2, "Prof. Díaz", "7°A", retiro).aEntidad())
            fail("Debió rechazar un préstamo de un equipo que no existe")
        } catch (_: SQLiteConstraintException) {
        }
    }

    @Test
    fun siFallaLaTransaccionNoQuedaNadaAMedias() = runBlocking {
        try {
            db.withTransaction {
                db.equipoDao().insertar(notebook.aEntidad())
                db.reservaDao().insertar(listOf(Reserva(0, 99, 2, "Prof. Díaz", "7°A", fecha, 3).aEntidad()))
            }
            fail("Debió rechazar la reserva huérfana")
        } catch (_: SQLiteConstraintException) {
        }
        // El equipo se insertó antes del error, pero la transacción se revirtió completa.
        assertEquals(0, db.equipoDao().contar())
        assertTrue(db.reservaDao().observarTodas().first().isEmpty())
    }
}
