package appmovil.prestaaula.ui.inventario

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Sell
import androidx.compose.runtime.mutableIntStateOf
import appmovil.prestaaula.model.Validaciones
import appmovil.prestaaula.ui.components.CampoValidado
import appmovil.prestaaula.ui.components.MensajeError
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import appmovil.prestaaula.model.Equipo
import appmovil.prestaaula.model.EstadoEquipo
import appmovil.prestaaula.model.TipoEquipo
import appmovil.prestaaula.ui.components.Etiqueta
import appmovil.prestaaula.ui.components.TonoEtiqueta
import appmovil.prestaaula.ui.theme.PrestaaulaTheme

@Composable
fun InventarioScreen(vm: InventarioViewModel, onVolver: () -> Unit) {
    val equipos by vm.equipos.collectAsState()
    val filtro by vm.filtroTipo.collectAsState()
    InventarioContenido(
        equipos = equipos,
        filtro = filtro,
        onFiltrar = vm::filtrarPor,
        onAgregar = vm::agregar,
        onVolver = onVolver,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InventarioContenido(
    equipos: List<Equipo>,
    filtro: TipoEquipo?,
    onFiltrar: (TipoEquipo?) -> Unit,
    onAgregar: (TipoEquipo, String) -> String?,
    onVolver: () -> Unit,
) {
    var mostrarDialogo by rememberSaveable { mutableStateOf(false) }
    val visibles = if (filtro == null) equipos else equipos.filter { it.tipo == filtro }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inventario") },
                navigationIcon = {
                    IconButton(onClick = onVolver, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { mostrarDialogo = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Agregar equipo") },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Text(
                "${equipos.size} equipos · ${equipos.count { it.estado == EstadoEquipo.DISPONIBLE }} disponibles",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Row(
                Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(selected = filtro == null, onClick = { onFiltrar(null) }, label = { Text("Todos") })
                TipoEquipo.entries.forEach { tipo ->
                    FilterChip(
                        selected = filtro == tipo,
                        onClick = { onFiltrar(tipo) },
                        label = { Text(tipo.etiqueta) },
                    )
                }
            }
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // animateItem: al filtrar o agregar un equipo, las tarjetas se deslizan en vez de saltar.
                items(visibles, key = { it.id }) { EquipoCard(it, Modifier.animateItem()) }
            }
        }
    }

    if (mostrarDialogo) {
        AgregarEquipoDialogo(
            onGuardar = onAgregar,
            onCerrar = { mostrarDialogo = false },
        )
    }
}

@Composable
private fun EquipoCard(equipo: Equipo, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth().heightIn(min = 64.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(equipo.codigo, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${equipo.tipo.etiqueta} · ${equipo.marca}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            EstadoChip(equipo.estado)
        }
    }
}

@Composable
private fun EstadoChip(estado: EstadoEquipo) {
    val tono = when (estado) {
        EstadoEquipo.DISPONIBLE -> TonoEtiqueta.VERDE
        EstadoEquipo.PRESTADO -> TonoEtiqueta.AMBAR
        EstadoEquipo.EN_MANTENCION -> TonoEtiqueta.ROJO
    }
    Etiqueta(estado.etiqueta, tono)
}

@Composable
private fun AgregarEquipoDialogo(
    onGuardar: (TipoEquipo, String) -> String?,
    onCerrar: () -> Unit,
) {
    var tipo by rememberSaveable { mutableStateOf(TipoEquipo.NOTEBOOK) }
    var marca by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var intentos by rememberSaveable { mutableIntStateOf(0) }

    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Agregar equipo") },
        text = {
            Column {
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TipoEquipo.entries.forEach {
                        FilterChip(selected = tipo == it, onClick = { tipo = it }, label = { Text(it.etiqueta) })
                    }
                }
                Spacer(Modifier.height(12.dp))
                CampoValidado(
                    valor = marca,
                    onValorChange = { marca = it; error = null },
                    etiqueta = "Marca",
                    icono = Icons.Filled.Sell,
                    error = Validaciones.marca(marca),
                    intentos = intentos,
                    ayuda = "El código interno se asigna automáticamente.",
                    modifier = Modifier.fillMaxWidth(),
                )
                MensajeError(error)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                intentos++
                if (Validaciones.marca(marca) == null) {
                    error = onGuardar(tipo, marca)
                    if (error == null) onCerrar()
                }
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar") } },
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun InventarioPreview() {
    PrestaaulaTheme(dynamicColor = false) {
        InventarioContenido(
            equipos = listOf(
                Equipo(1, "NB-001", TipoEquipo.NOTEBOOK, "Lenovo"),
                Equipo(2, "TB-001", TipoEquipo.TABLET, "Samsung", EstadoEquipo.PRESTADO),
                Equipo(3, "PY-001", TipoEquipo.PROYECTOR, "Epson", EstadoEquipo.EN_MANTENCION),
            ),
            filtro = null,
            onFiltrar = {},
            onAgregar = { _, _ -> null },
            onVolver = {},
        )
    }
}
