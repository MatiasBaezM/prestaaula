package appmovil.prestaaula

import appmovil.prestaaula.data.ArchivoJson
import appmovil.prestaaula.data.Codec
import appmovil.prestaaula.model.Equipo
import appmovil.prestaaula.model.EstadoDevolucion
import appmovil.prestaaula.model.EstadoEquipo
import appmovil.prestaaula.model.Prestamo
import appmovil.prestaaula.model.Reserva
import appmovil.prestaaula.model.TipoEquipo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime

class PersistenciaTest {

    @get:Rule
    val carpeta = TemporaryFolder()

    @Test
    fun `equipos sobreviven ida y vuelta`() {
        val lista = listOf(
            Equipo(1, "NB-001", TipoEquipo.NOTEBOOK, "Lenovo"),
            Equipo(2, "PY-001", TipoEquipo.PROYECTOR, "Epson", EstadoEquipo.EN_MANTENCION),
        )
        assertEquals(lista, Codec.equiposDeJson(Codec.equiposAJson(lista)))
    }

    @Test
    fun `reservas sobreviven ida y vuelta`() {
        val lista = listOf(Reserva(1, 2, 3, "Prof. Díaz", "7°A", LocalDate.of(2026, 10, 30), 4))
        assertEquals(lista, Codec.reservasDeJson(Codec.reservasAJson(lista)))
    }

    @Test
    fun `prestamos activos y cerrados sobreviven ida y vuelta`() {
        val activo = Prestamo(1, 5, 102, "Prof. Soto", "6°A", LocalDateTime.of(2026, 10, 29, 8, 5), retiroConfirmado = true)
        val cerrado = Prestamo(
            2, 3, 103, "Prof. Núñez", "2°M", LocalDateTime.of(2026, 10, 27, 9, 0),
            devolucion = LocalDateTime.of(2026, 10, 27, 10, 0),
            estadoDevolucion = EstadoDevolucion.CON_DANOS, observaciones = "Pantalla \"trizada\" ñandú",
        )
        val lista = listOf(activo, cerrado)

        val vuelta = Codec.prestamosDeJson(Codec.prestamosAJson(lista))

        assertEquals(lista, vuelta)
        assertNull(vuelta.first().devolucion)
        assertNull(vuelta.first().estadoDevolucion)
    }

    @Test
    fun `lista vacia tambien se guarda`() {
        assertEquals(emptyList<Equipo>(), Codec.equiposDeJson(Codec.equiposAJson(emptyList())))
    }

    @Test
    fun `el archivo guarda y recupera`() {
        val archivo = ArchivoJson(File(carpeta.root, "equipos.json"))
        val lista = listOf(Equipo(1, "NB-001", TipoEquipo.NOTEBOOK, "Lenovo"))

        archivo.escribir(Codec.equiposAJson(lista))
        assertEquals(lista, archivo.cargar(Codec::equiposDeJson))

        // Sobrescribir reemplaza el contenido anterior.
        archivo.escribir(Codec.equiposAJson(emptyList()))
        assertEquals(emptyList<Equipo>(), archivo.cargar(Codec::equiposDeJson))
    }

    @Test
    fun `archivo inexistente o danado devuelve null en vez de fallar`() {
        assertNull(ArchivoJson(File(carpeta.root, "no-existe.json")).cargar(Codec::equiposDeJson))

        val danado = File(carpeta.root, "danado.json").apply { writeText("{esto no es json") }
        assertNull(ArchivoJson(danado).cargar(Codec::equiposDeJson))

        val incompleto = File(carpeta.root, "incompleto.json").apply { writeText("""[{"id":1}]""") }
        assertNull(ArchivoJson(incompleto).cargar(Codec::equiposDeJson))
    }
}
