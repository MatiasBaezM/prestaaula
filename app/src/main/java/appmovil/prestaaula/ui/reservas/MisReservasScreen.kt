package appmovil.prestaaula.ui.reservas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import appmovil.prestaaula.model.Bloques
import appmovil.prestaaula.model.Equipo
import appmovil.prestaaula.model.GrupoReserva
import appmovil.prestaaula.model.agruparReservas
import appmovil.prestaaula.ui.components.fechaLarga

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MisReservasScreen(vm: MisReservasViewModel, onVolver: () -> Unit) {
    val equipos by vm.equipos.collectAsState()
    val reservas by vm.reservas.collectAsState()
    val grupos = agruparReservas(vm.propias(reservas))

    // Se guarda el id de la primera reserva del grupo: sobrevive a la rotación.
    var porCancelarId by rememberSaveable { mutableStateOf<Int?>(null) }
    val porCancelar = grupos.firstOrNull { it.ids.first() == porCancelarId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis reservas") },
                navigationIcon = {
                    IconButton(onClick = onVolver, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
    ) { padding ->
        if (grupos.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(
                    "No tienes reservas próximas",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(grupos, key = { it.ids.first() }) { grupo ->
                    TarjetaReserva(grupo, equipos.firstOrNull { it.id == grupo.equipoId }) {
                        porCancelarId = grupo.ids.first()
                    }
                }
            }
        }
    }

    if (porCancelar != null) {
        val equipo = equipos.firstOrNull { it.id == porCancelar.equipoId }
        AlertDialog(
            onDismissRequest = { porCancelarId = null },
            title = { Text("¿Cancelar reserva?") },
            text = {
                Text("${equipo?.codigo ?: "Equipo"} · ${fechaLarga(porCancelar.fecha)}\n" +
                    Bloques.etiquetaRango(porCancelar.desde, porCancelar.hasta))
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.cancelar(porCancelar)
                    porCancelarId = null
                }) { Text("Sí, cancelar") }
            },
            dismissButton = { TextButton(onClick = { porCancelarId = null }) { Text("Mantener") } },
        )
    }
}

@Composable
private fun TarjetaReserva(grupo: GrupoReserva, equipo: Equipo?, onCancelar: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.fillMaxWidth().padding(start = 16.dp, top = 16.dp, end = 8.dp, bottom = 4.dp)) {
            Text(
                "${equipo?.codigo ?: "Equipo"} · ${equipo?.tipo?.etiqueta ?: ""} ${equipo?.marca ?: ""}".trim(),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(fechaLarga(grupo.fecha), style = MaterialTheme.typography.bodyMedium)
            Text(
                Bloques.etiquetaRango(grupo.desde, grupo.hasta),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "Curso ${grupo.curso}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(
                onClick = onCancelar,
                modifier = Modifier.heightIn(min = 48.dp).align(Alignment.End),
            ) { Text("Cancelar reserva", color = MaterialTheme.colorScheme.error) }
        }
    }
}
