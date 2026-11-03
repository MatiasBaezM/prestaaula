package appmovil.prestaaula

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import appmovil.prestaaula.data.db.AlmacenDao
import appmovil.prestaaula.data.db.Instantanea
import appmovil.prestaaula.data.db.PrestaAulaDb
import appmovil.prestaaula.data.db.aEntidad
import appmovil.prestaaula.data.db.aModelo
import appmovil.prestaaula.data.db.sincronizar
import appmovil.prestaaula.model.Equipo
import appmovil.prestaaula.model.EstadoDevolucion
import appmovil.prestaaula.model.EstadoEquipo
import appmovil.prestaaula.model.Prestamo
import appmovil.prestaaula.model.Reserva
import appmovil.prestaaula.model.TipoEquipo
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.LocalDateTime

@RunWith(AndroidJUnit4::class)
class BaseDatosTest {

    private lateinit var db: PrestaAulaDb
    private lateinit var dao: AlmacenDao

    @Before
    fun abrir() {
        val contexto = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(contexto, PrestaAulaDb::class.java).build()
        dao = db.almacen()
    }

    @After
    fun cerrar() = db.close()

    @Test
    fun guardaYRecuperaLasTresTablas() = runBlocking {
        val equipos = listOf(Equipo(1, "NB-001", TipoEquipo.NOTEBOOK, "Lenovo"))
        val reservas = listOf(Reserva(1, 1, 2, "Prof. Díaz", "7°A", LocalDate.of(2026, 10, 30), 3))
        val prestamos = listOf(
            Prestamo(
                1, 1, 2, "Prof. Díaz", "7°A", LocalDateTime.of(2026, 10, 29, 8, 0),
                devolucion = LocalDateTime.of(2026, 10, 29, 9, 0),
                estadoDevolucion = EstadoDevolucion.BUEN_ESTADO,
            ),
        )

        dao.reemplazarTodo(equipos.map { it.aEntidad() }, reservas.map { it.aEntidad() }, prestamos.map { it.aEntidad() })

        assertEquals(equipos, dao.equipos().map { it.aModelo() })
        assertEquals(reservas, dao.reservas().map { it.aModelo() })
        assertEquals(prestamos, dao.prestamos().map { it.aModelo() })
    }

    @Test
    fun reemplazarTodoSobrescribeLoAnterior() = runBlocking {
        dao.reemplazarTodo(listOf(Equipo(1, "NB-001", TipoEquipo.NOTEBOOK, "Lenovo").aEntidad()), emptyList(), emptyList())
        dao.reemplazarTodo(listOf(Equipo(2, "TB-001", TipoEquipo.TABLET, "Samsung").aEntidad()), emptyList(), emptyList())

        assertEquals(listOf(2), dao.equipos().map { it.id })
    }

    @Test
    fun reservaDeEquipoInexistenteSeRechaza() = runBlocking {
        val equipo = Equipo(1, "NB-001", TipoEquipo.NOTEBOOK, "Lenovo").aEntidad()
        val huerfana = Reserva(1, 99, 2, "Prof. Díaz", "7°A", LocalDate.of(2026, 10, 30), 3).aEntidad()

        try {
            dao.reemplazarTodo(listOf(equipo), listOf(huerfana), emptyList())
            fail("Debió rechazar una reserva de un equipo que no existe")
        } catch (_: SQLiteConstraintException) {
        }
    }

    @Test
    fun prestamoDeEquipoInexistenteSeRechaza() = runBlocking {
        val equipo = Equipo(1, "NB-001", TipoEquipo.NOTEBOOK, "Lenovo").aEntidad()
        val huerfano = Prestamo(1, 99, 2, "Prof. Díaz", "7°A", LocalDateTime.of(2026, 10, 29, 8, 0)).aEntidad()

        try {
            dao.reemplazarTodo(listOf(equipo), emptyList(), listOf(huerfano))
            fail("Debió rechazar un préstamo de un equipo que no existe")
        } catch (_: SQLiteConstraintException) {
        }
    }

    @Test
    fun siFallaElReemplazoSeConservaLoAnterior() = runBlocking {
        val anterior = Equipo(1, "NB-001", TipoEquipo.NOTEBOOK, "Lenovo").aEntidad()
        dao.reemplazarTodo(listOf(anterior), emptyList(), emptyList())

        val huerfana = Reserva(1, 99, 2, "Prof. Díaz", "7°A", LocalDate.of(2026, 10, 30), 3).aEntidad()
        try {
            dao.reemplazarTodo(listOf(Equipo(2, "TB-001", TipoEquipo.TABLET, "Samsung").aEntidad()), listOf(huerfana), emptyList())
        } catch (_: SQLiteConstraintException) {
        }

        // La transacción se revirtió completa: sigue el equipo 1, no el 2.
        assertEquals(listOf(1), dao.equipos().map { it.id })
    }

    private val nb1 = Equipo(1, "NB-001", TipoEquipo.NOTEBOOK, "Lenovo").aEntidad()
    private val tb1 = Equipo(2, "TB-001", TipoEquipo.TABLET, "Samsung").aEntidad()
    private val reserva = Reserva(1, 1, 2, "Prof. Díaz", "7°A", LocalDate.of(2026, 10, 30), 3).aEntidad()
    private val prestamo = Prestamo(1, 1, 2, "Prof. Díaz", "7°A", LocalDateTime.of(2026, 10, 29, 8, 0)).aEntidad()

    private suspend fun enDisco() = Instantanea(dao.equipos(), dao.reservas(), dao.prestamos())

    @Test
    fun sincronizarSinInstantaneaReemplazaTodo() = runBlocking {
        dao.reemplazarTodo(listOf(tb1), emptyList(), emptyList())

        val nueva = Instantanea(listOf(nb1), listOf(reserva), listOf(prestamo))
        dao.sincronizar(null, nueva)

        assertEquals(nueva, enDisco())
    }

    @Test
    fun sincronizarAplicaAltasCambiosYBajas() = runBlocking {
        val inicial = Instantanea(listOf(nb1), listOf(reserva), emptyList())
        dao.sincronizar(null, inicial)

        // Se presta NB-001 (cambia de estado, teniendo una reserva que lo referencia: el upsert
        // actualiza en el lugar y no choca con la clave foránea), se consume la reserva y se
        // agrega un equipo.
        val nueva = Instantanea(
            listOf(nb1.copy(estado = EstadoEquipo.PRESTADO), tb1),
            emptyList(),
            listOf(prestamo),
        )
        dao.sincronizar(inicial, nueva)

        assertEquals(nueva, enDisco())
    }

    @Test
    fun sincronizarSoloTocaLoQueCambio() = runBlocking {
        val inicial = Instantanea(listOf(nb1, tb1), emptyList(), emptyList())
        dao.sincronizar(null, inicial)

        // Simula una fila que la app no conoce: si sincronizar reescribiera todo, desaparecería.
        dao.insertarReservas(listOf(reserva))

        dao.sincronizar(inicial, inicial.copy(equipos = listOf(nb1, tb1.copy(marca = "Lenovo"))))

        assertEquals(listOf(reserva), dao.reservas())
        assertEquals("Lenovo", dao.equipos().single { it.id == 2 }.marca)
    }

    @Test
    fun siFallaSincronizarNoQuedaNadaAMedias() = runBlocking {
        val inicial = Instantanea(listOf(nb1), emptyList(), emptyList())
        dao.sincronizar(null, inicial)

        val huerfana = reserva.copy(id = 2, equipoId = 99)
        try {
            dao.sincronizar(inicial, Instantanea(listOf(nb1, tb1), listOf(huerfana), emptyList()))
            fail("Debió rechazar una reserva de un equipo que no existe")
        } catch (_: SQLiteConstraintException) {
        }

        assertEquals(inicial, enDisco())
    }
}
