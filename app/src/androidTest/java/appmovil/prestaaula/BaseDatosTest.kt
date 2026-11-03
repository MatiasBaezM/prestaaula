package appmovil.prestaaula

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import appmovil.prestaaula.data.db.AlmacenDao
import appmovil.prestaaula.data.db.PrestaAulaDb
import appmovil.prestaaula.data.db.aEntidad
import appmovil.prestaaula.data.db.aModelo
import appmovil.prestaaula.model.Equipo
import appmovil.prestaaula.model.EstadoDevolucion
import appmovil.prestaaula.model.Prestamo
import appmovil.prestaaula.model.Reserva
import appmovil.prestaaula.model.TipoEquipo
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
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
}
