package appmovil.prestaaula.ui.reservas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Groups
import androidx.compose.runtime.mutableIntStateOf
import appmovil.prestaaula.model.Validaciones
import appmovil.prestaaula.ui.components.CampoValidado
import appmovil.prestaaula.ui.components.MensajeError
import appmovil.prestaaula.ui.components.rememberVibrador
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import appmovil.prestaaula.data.ResultadoReserva
import appmovil.prestaaula.model.Bloques
import appmovil.prestaaula.model.Equipo
import appmovil.prestaaula.model.Ocupacion
import appmovil.prestaaula.model.TipoEquipo
import appmovil.prestaaula.model.ocupacionEnBloques
import appmovil.prestaaula.ui.components.Etiqueta
import appmovil.prestaaula.ui.components.FilaChips
import appmovil.prestaaula.ui.components.TonoEtiqueta
import appmovil.prestaaula.ui.components.etiquetaDia
import appmovil.prestaaula.ui.components.fechaLarga
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReservarScreen(vm: ReservarViewModel, onVolver: () -> Unit) {
    val equipos by vm.equipos.collectAsState()
    val reservas by vm.reservas.collectAsState()
    val fecha by vm.fecha.collectAsState()
    val bloques by vm.bloques.collectAsState()
    val filtro by vm.filtroTipo.collectAsState()

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val vibrador = rememberVibrador()
    var equipoElegidoId by rememberSaveable { mutableStateOf<Int?>(null) }

    val desde = bloques.minOrNull()
    val hasta = bloques.maxOrNull()
    val filas = equipos
        .filter { filtro == null || it.tipo == filtro }
        .map { it to ocupacionEnBloques(it, reservas, fecha, bloques, vm.hoy) }
    val equipoElegido = equipos.firstOrNull { it.id == equipoElegidoId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reservar equipo") },
                navigationIcon = {
                    IconButton(onClick = onVolver, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            FilaChips {
                vm.dias.forEach { dia ->
                    FilterChip(
                        selected = dia == fecha,
                        onClick = { vm.elegirFecha(dia) },
                        label = { Text(etiquetaDia(dia, vm.hoy)) },
                    )
                }
            }
            FilaChips {
                Bloques.todos.forEach { b ->
                    FilterChip(
                        selected = b.numero in bloques,
                        onClick = { vm.alternarBloque(b.numero) },
                        enabled = !vm.bloqueTerminado(fecha, b.numero),
                        label = { Text("${b.numero} · ${b.inicio}") },
                    )
                }
            }
            FilaChips {
                FilterChip(selected = filtro == null, onClick = { vm.filtrarPor(null) }, label = { Text("Todos") })
                TipoEquipo.entries.forEach { tipo ->
                    FilterChip(
                        selected = filtro == tipo,
                        onClick = { vm.filtrarPor(tipo) },
                        label = { Text(tipo.etiqueta) },
                    )
                }
            }
            if (desde == null || hasta == null) {
                Text(
                    "La jornada de este día ya terminó. Elige otro día.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
                return@Column
            }
            Text(
                "${Bloques.etiquetaRango(desde, hasta)} — toca un equipo libre para reservarlo",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(filas, key = { it.first.id }) { (equipo, ocupacion) ->
                    EquipoReservable(equipo, ocupacion) { equipoElegidoId = equipo.id }
                }
            }
        }
    }

    if (equipoElegido != null && desde != null && hasta != null) {
        ConfirmarReservaDialogo(
            equipo = equipoElegido,
            resumen = "${fechaLarga(fecha)}\n${Bloques.etiquetaRango(desde, hasta)}",
            onConfirmar = { curso ->
                val resultado = vm.reservar(equipoElegido.id, curso)
                if (resultado is ResultadoReserva.Exito) {
                    vibrador.exito()
                    equipoElegidoId = null
                    scope.launch { snackbar.showSnackbar("Reserva confirmada: ${equipoElegido.codigo}") }
                }
                (resultado as? ResultadoReserva.Error)?.mensaje?.also { vibrador.error() }
            },
            onCerrar = { equipoElegidoId = null },
        )
    }
}

@Composable
private fun EquipoReservable(equipo: Equipo, ocupacion: Ocupacion, onClick: () -> Unit) {
    val libre = ocupacion is Ocupacion.Libre
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .alpha(if (libre) 1f else 0.6f)
            .then(if (libre) Modifier.clickable(onClick = onClick) else Modifier),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text(equipo.codigo, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${equipo.tipo.etiqueta} · ${equipo.marca}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            when (ocupacion) {
                Ocupacion.Libre -> Etiqueta("Reservar", TonoEtiqueta.VERDE)
                is Ocupacion.Reservado -> Etiqueta("Reservado", TonoEtiqueta.AMBAR)
                Ocupacion.Prestado -> Etiqueta("Prestado", TonoEtiqueta.AMBAR)
                Ocupacion.EnMantencion -> Etiqueta("En mantención", TonoEtiqueta.ROJO)
            }
        }
    }
}

@Composable
private fun ConfirmarReservaDialogo(
    equipo: Equipo,
    resumen: String,
    onConfirmar: (String) -> String?,
    onCerrar: () -> Unit,
) {
    var curso by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var intentos by rememberSaveable { mutableIntStateOf(0) }

    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Reservar ${equipo.codigo}") },
        text = {
            Column {
                Text("${equipo.tipo.etiqueta} · ${equipo.marca}", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(4.dp))
                Text(resumen, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
                CampoValidado(
                    valor = curso,
                    onValorChange = { curso = it; error = null },
                    etiqueta = "Curso (ej. 7°A)",
                    icono = Icons.Filled.Groups,
                    error = Validaciones.curso(curso),
                    intentos = intentos,
                    modifier = Modifier.fillMaxWidth(),
                )
                MensajeError(error)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                intentos++
                if (Validaciones.curso(curso) == null) error = onConfirmar(curso)
            }) { Text("Confirmar") }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar") } },
    )
}
