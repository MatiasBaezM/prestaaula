package appmovil.prestaaula.ui.prestamos

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import appmovil.prestaaula.model.Bloques
import appmovil.prestaaula.model.Equipo
import appmovil.prestaaula.model.EstadoEquipo
import appmovil.prestaaula.model.Reserva
import appmovil.prestaaula.ui.components.Etiqueta
import appmovil.prestaaula.ui.components.TonoEtiqueta
import androidx.compose.material.icons.filled.Groups
import androidx.compose.runtime.mutableIntStateOf
import appmovil.prestaaula.model.Validaciones
import appmovil.prestaaula.ui.components.CampoValidado
import appmovil.prestaaula.ui.components.MensajeError
import appmovil.prestaaula.ui.components.rememberVibrador
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrarPrestamoScreen(vm: RegistrarPrestamoViewModel, onVolver: () -> Unit) {
    val equipos by vm.equipos.collectAsStateWithLifecycle()
    val reservas by vm.reservas.collectAsStateWithLifecycle()

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val vibrador = rememberVibrador()
    var equipoElegidoId by rememberSaveable { mutableStateOf<Int?>(null) }
    val equipoElegido = equipos.firstOrNull { it.id == equipoElegidoId }

    fun reservasDeHoy(equipo: Equipo): List<Reserva> =
        reservas.filter { it.equipoId == equipo.id && it.fecha == vm.hoy }.sortedBy { it.bloque }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Registrar préstamo") },
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
            Text(
                "${equipos.count { it.estado == EstadoEquipo.DISPONIBLE }} equipos para entregar — toca uno",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(equipos, key = { it.id }) { equipo ->
                    val libre = equipo.estado == EstadoEquipo.DISPONIBLE
                    val deHoy = reservasDeHoy(equipo)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 64.dp)
                            .alpha(if (libre) 1f else 0.6f)
                            .then(if (libre) Modifier.clickable { equipoElegidoId = equipo.id } else Modifier),
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
                                deHoy.firstOrNull()?.let { primera ->
                                    val bloques = deHoy.filter { it.docenteId == primera.docenteId }.map { it.bloque }
                                    Text(
                                        "Reservado hoy: ${primera.docente} · ${primera.curso} · " +
                                            Bloques.etiquetaRango(bloques.min(), bloques.max()),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            when (equipo.estado) {
                                EstadoEquipo.DISPONIBLE -> Etiqueta("Entregar", TonoEtiqueta.VERDE)
                                EstadoEquipo.PRESTADO -> Etiqueta("Prestado", TonoEtiqueta.AMBAR)
                                EstadoEquipo.EN_MANTENCION -> Etiqueta("En mantención", TonoEtiqueta.ROJO)
                            }
                        }
                    }
                }
            }
        }
    }

    if (equipoElegido != null) {
        val reserva = reservasDeHoy(equipoElegido).firstOrNull()
        EntregarDialogo(
            equipo = equipoElegido,
            docentes = vm.docentes.map { it.id to it.nombre },
            docenteInicial = reserva?.docenteId,
            cursoInicial = reserva?.curso.orEmpty(),
            onEntregar = { docenteId, curso, alTerminar ->
                val docente = vm.docentes.first { it.id == docenteId }
                vm.registrar(equipoElegido.id, docente, curso) { error ->
                    alTerminar(error)
                    if (error == null) {
                        vibrador.exito()
                        equipoElegidoId = null
                        scope.launch { snackbar.showSnackbar("Entregado: ${equipoElegido.codigo} → ${docente.nombre}") }
                    } else {
                        vibrador.error()
                    }
                }
            },
            onCerrar = { equipoElegidoId = null },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EntregarDialogo(
    equipo: Equipo,
    docentes: List<Pair<Int, String>>,
    docenteInicial: Int?,
    cursoInicial: String,
    // alTerminar recibe el resultado cuando termina de guardarse: null = guardado, o el error.
    onEntregar: (docenteId: Int, curso: String, alTerminar: (String?) -> Unit) -> Unit,
    onCerrar: () -> Unit,
) {
    var docenteId by rememberSaveable { mutableStateOf(docenteInicial ?: docentes.first().first) }
    var curso by rememberSaveable { mutableStateOf(cursoInicial) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var intentos by rememberSaveable { mutableIntStateOf(0) }
    var guardando by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Entregar ${equipo.codigo}") },
        text = {
            Column {
                Text("${equipo.tipo.etiqueta} · ${equipo.marca}", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                Text("Docente", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    docentes.forEach { (id, nombre) ->
                        FilterChip(
                            selected = docenteId == id,
                            onClick = { docenteId = id; error = null },
                            label = { Text(nombre) },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
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
            TextButton(
                enabled = !guardando,
                onClick = {
                    intentos++
                    if (Validaciones.curso(curso) == null) {
                        guardando = true
                        onEntregar(docenteId, curso) { resultado ->
                            guardando = false
                            error = resultado
                        }
                    }
                },
            ) { Text(if (guardando) "Entregando…" else "Entregar") }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar") } },
    )
}
