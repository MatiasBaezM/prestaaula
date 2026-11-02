package appmovil.prestaaula.ui.prestamos

import android.content.ActivityNotFoundException
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.runtime.mutableIntStateOf
import appmovil.prestaaula.model.Validaciones
import appmovil.prestaaula.ui.components.CampoValidado
import appmovil.prestaaula.ui.components.MensajeError
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import appmovil.prestaaula.ui.components.FotoEquipo
import appmovil.prestaaula.ui.components.Fotos
import appmovil.prestaaula.ui.components.rememberVibrador
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import appmovil.prestaaula.model.Equipo
import appmovil.prestaaula.model.EstadoDevolucion
import appmovil.prestaaula.model.Prestamo
import appmovil.prestaaula.ui.components.Etiqueta
import appmovil.prestaaula.ui.components.TonoEtiqueta
import appmovil.prestaaula.ui.components.fechaHoraCorta
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrarDevolucionScreen(vm: RegistrarDevolucionViewModel, onVolver: () -> Unit) {
    val equipos by vm.equipos.collectAsState()
    val registro by vm.registro.collectAsState()
    val activos = registro.filter { it.activo }.sortedBy { it.retiro }

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val vibrador = rememberVibrador()
    var elegidoId by rememberSaveable { mutableStateOf<Int?>(null) }
    val elegido = activos.firstOrNull { it.id == elegidoId }
    val equipoElegido = equipos.firstOrNull { it.id == elegido?.equipoId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Registrar devolución") },
                navigationIcon = {
                    IconButton(onClick = onVolver, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        if (activos.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(
                    "No hay equipos prestados",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Column(Modifier.fillMaxSize().padding(padding)) {
                Text(
                    "${activos.size} equipos por recibir — toca uno",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(activos, key = { it.id }) { prestamo ->
                        TarjetaPrestamo(
                            prestamo, equipos.firstOrNull { it.id == prestamo.equipoId }, vm.hoy,
                            Modifier.animateItem(),
                        ) { elegidoId = prestamo.id }
                    }
                }
            }
        }
    }

    if (elegido != null && equipoElegido != null) {
        RecibirDialogo(
            equipo = equipoElegido,
            prestamo = elegido,
            onRecibir = { estado, obs, foto ->
                val error = vm.recibir(elegido.id, estado, obs, foto)
                if (error == null) {
                    vibrador.exito()
                    elegidoId = null
                    val destino = if (estado == EstadoDevolucion.BUEN_ESTADO) "disponible" else "en mantención"
                    scope.launch { snackbar.showSnackbar("${equipoElegido.codigo} recibido: queda $destino") }
                } else {
                    vibrador.error()
                }
                error
            },
            onCerrar = { elegidoId = null },
        )
    }
}

@Composable
private fun TarjetaPrestamo(
    prestamo: Prestamo,
    equipo: Equipo?,
    hoy: java.time.LocalDate,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth().heightIn(min = 72.dp).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                "${equipo?.codigo ?: "Equipo"} · ${equipo?.tipo?.etiqueta.orEmpty()} ${equipo?.marca.orEmpty()}".trim(),
                style = MaterialTheme.typography.titleMedium,
            )
            Text("${prestamo.docente} · ${prestamo.curso}", style = MaterialTheme.typography.bodyMedium)
            Text(
                "Retirado ${fechaHoraCorta(prestamo.retiro, hoy)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (prestamo.retiroConfirmado || prestamo.devolucionAvisada) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (prestamo.retiroConfirmado) Etiqueta("Retiro confirmado", TonoEtiqueta.VERDE)
                    if (prestamo.devolucionAvisada) Etiqueta("Docente avisó devolución", TonoEtiqueta.AMBAR)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecibirDialogo(
    equipo: Equipo,
    prestamo: Prestamo,
    onRecibir: (EstadoDevolucion, String, String?) -> String?,
    onCerrar: () -> Unit,
) {
    val contexto = LocalContext.current
    var estado by rememberSaveable { mutableStateOf(EstadoDevolucion.BUEN_ESTADO) }
    var observaciones by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    // Foto ya tomada, y nombre reservado mientras la cámara del sistema está abierta.
    var foto by rememberSaveable { mutableStateOf<String?>(null) }
    var pendiente by rememberSaveable { mutableStateOf<String?>(null) }
    var errorCamara by rememberSaveable { mutableStateOf<String?>(null) }
    var intentos by rememberSaveable { mutableIntStateOf(0) }

    val camara = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { tomada ->
        val nombre = pendiente
        pendiente = null
        if (nombre == null) return@rememberLauncherForActivityResult
        if (tomada && Fotos.existe(contexto, nombre)) {
            foto?.let { Fotos.borrar(contexto, it) }
            foto = nombre
            errorCamara = null
        } else {
            Fotos.borrar(contexto, nombre)
        }
    }

    // Cancelar o tocar fuera descarta la foto: no queda un archivo sin préstamo asociado.
    val cerrar = {
        foto?.let { Fotos.borrar(contexto, it) }
        onCerrar()
    }

    AlertDialog(
        onDismissRequest = cerrar,
        title = { Text("Recibir ${equipo.codigo}") },
        text = {
            Column {
                Text("${prestamo.docente} · ${prestamo.curso}", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                Text("Estado del equipo", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    EstadoDevolucion.entries.forEach {
                        FilterChip(
                            selected = estado == it,
                            onClick = { estado = it; error = null },
                            label = { Text(it.etiqueta) },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                CampoValidado(
                    valor = observaciones,
                    onValorChange = { observaciones = it; error = null },
                    etiqueta = if (estado == EstadoDevolucion.BUEN_ESTADO) "Observaciones (opcional)" else "Observaciones",
                    icono = Icons.Filled.EditNote,
                    error = Validaciones.observaciones(estado, observaciones),
                    intentos = intentos,
                    ayuda = "${observaciones.length}/${Validaciones.MAX_OBSERVACIONES}",
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth(),
                )
                MensajeError(error)
                Spacer(Modifier.height(8.dp))
                val actual = foto
                if (actual != null) {
                    FotoEquipo(
                        nombre = actual,
                        descripcion = "Foto del equipo ${equipo.codigo} al devolverlo",
                        modifier = Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(12.dp)),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = {
                            val nombre = Fotos.nuevoNombre()
                            try {
                                pendiente = nombre
                                camara.launch(Fotos.uri(contexto, nombre))
                            } catch (e: ActivityNotFoundException) {
                                pendiente = null
                                errorCamara = "Este dispositivo no tiene una app de cámara"
                            }
                        },
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Icon(Icons.Filled.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (actual == null) "Tomar foto" else "Repetir foto")
                    }
                    if (actual != null) {
                        TextButton(onClick = { Fotos.borrar(contexto, actual); foto = null }) { Text("Quitar") }
                    }
                }
                errorCamara?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                intentos++
                if (Validaciones.observaciones(estado, observaciones) == null) {
                    val resultado = onRecibir(estado, observaciones, foto)
                    // Con la devolución registrada la foto pertenece al préstamo y ya no se borra al cerrar.
                    if (resultado == null) foto = null
                    error = resultado
                }
            }) { Text("Recibir equipo") }
        },
        dismissButton = { TextButton(onClick = cerrar) { Text("Cancelar") } },
    )
}
