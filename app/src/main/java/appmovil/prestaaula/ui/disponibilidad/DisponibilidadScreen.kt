package appmovil.prestaaula.ui.disponibilidad

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import appmovil.prestaaula.model.Bloques
import appmovil.prestaaula.model.Equipo
import appmovil.prestaaula.model.Ocupacion
import appmovil.prestaaula.model.TipoEquipo
import appmovil.prestaaula.model.ocupacionEnBloque
import appmovil.prestaaula.ui.components.Etiqueta
import appmovil.prestaaula.ui.components.FilaChips
import appmovil.prestaaula.ui.components.TonoEtiqueta
import appmovil.prestaaula.ui.components.etiquetaDia

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisponibilidadScreen(vm: DisponibilidadViewModel, onVolver: () -> Unit) {
    val equipos by vm.equipos.collectAsState()
    val reservas by vm.reservas.collectAsState()
    val fecha by vm.fecha.collectAsState()
    val numeroBloque by vm.bloque.collectAsState()
    val filtro by vm.filtroTipo.collectAsState()

    val bloque = Bloques.porNumero(numeroBloque) ?: Bloques.todos.first()
    val filas = equipos
        .filter { filtro == null || it.tipo == filtro }
        .map { it to ocupacionEnBloque(it, reservas, fecha, bloque.numero, vm.hoy) }
    val libres = filas.count { it.second is Ocupacion.Libre }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Disponibilidad") },
                navigationIcon = {
                    IconButton(onClick = onVolver, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
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
                        selected = b.numero == numeroBloque,
                        onClick = { vm.elegirBloque(b.numero) },
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
            Text(
                "Bloque ${bloque.numero} · ${bloque.etiqueta} — $libres de ${filas.size} libres",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(filas, key = { it.first.id }) { (equipo, ocupacion) ->
                    FilaDisponibilidad(equipo, ocupacion)
                }
            }
        }
    }
}

@Composable
private fun FilaDisponibilidad(equipo: Equipo, ocupacion: Ocupacion) {
    Card(
        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
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
                if (ocupacion is Ocupacion.Reservado) {
                    Text(
                        "${ocupacion.reserva.docente} · ${ocupacion.reserva.curso}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            when (ocupacion) {
                Ocupacion.Libre -> Etiqueta("Libre", TonoEtiqueta.VERDE)
                is Ocupacion.Reservado -> Etiqueta("Reservado", TonoEtiqueta.AMBAR)
                Ocupacion.Prestado -> Etiqueta("Prestado", TonoEtiqueta.AMBAR)
                Ocupacion.EnMantencion -> Etiqueta("En mantención", TonoEtiqueta.ROJO)
            }
        }
    }
}
