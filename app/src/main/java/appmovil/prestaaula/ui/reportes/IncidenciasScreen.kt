package appmovil.prestaaula.ui.reportes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import appmovil.prestaaula.model.IncidenciaEquipo
import appmovil.prestaaula.model.equiposConIncidencias
import appmovil.prestaaula.ui.components.Etiqueta
import appmovil.prestaaula.ui.components.TonoEtiqueta
import appmovil.prestaaula.ui.components.fechaHoraCorta
import appmovil.prestaaula.ui.components.haceTexto
import appmovil.prestaaula.ui.components.FotoEquipo
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncidenciasScreen(vm: IncidenciasViewModel, onVolver: () -> Unit) {
    val equipos by vm.equipos.collectAsStateWithLifecycle()
    val registro by vm.registro.collectAsStateWithLifecycle()
    val lista = equiposConIncidencias(registro, equipos)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Equipos con incidencias") },
                navigationIcon = {
                    IconButton(onClick = onVolver, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
    ) { padding ->
        if (lista.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(
                    "No hay equipos con incidencias",
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
                items(lista, key = { it.equipo.id }) { item ->
                    TarjetaIncidencia(
                        item = item,
                        hoy = vm.hoy,
                        puedeReparar = vm.puedeMarcarReparado && item.enMantencion,
                        onReparado = { vm.marcarReparado(item.equipo.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TarjetaIncidencia(
    item: IncidenciaEquipo,
    hoy: LocalDate,
    puedeReparar: Boolean,
    onReparado: () -> Unit,
) {
    val ultima = item.incidencias.firstOrNull()
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "${item.equipo.codigo} · ${item.equipo.tipo.etiqueta} ${item.equipo.marca}",
                style = MaterialTheme.typography.titleMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (item.enMantencion) Etiqueta("En mantención", TonoEtiqueta.ROJO)
                if (item.incidencias.isNotEmpty()) {
                    val n = item.incidencias.size
                    Etiqueta(
                        if (item.reincidente) "Reincidente · $n incidencias" else "1 incidencia",
                        if (item.reincidente) TonoEtiqueta.ROJO else TonoEtiqueta.AMBAR,
                    )
                }
            }
            if (ultima != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Última: ${ultima.estadoDevolucion?.etiqueta} — ${ultima.observaciones} " +
                        "(${haceTexto(ultima.retiro, hoy)})",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            ultima?.foto?.let { foto ->
                Spacer(Modifier.height(4.dp))
                FotoEquipo(
                    nombre = foto,
                    descripcion = "Foto de la última incidencia de ${item.equipo.codigo}",
                    modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(12.dp)),
                )
            }
            item.ultimoResponsable?.let {
                Text(
                    "Último responsable: ${it.docente} · ${it.curso}, retiró ${fechaHoraCorta(it.retiro, hoy)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (puedeReparar) {
                OutlinedButton(
                    onClick = onReparado,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) { Text("Marcar como reparado") }
            }
        }
    }
}
