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
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.verticalScroll
import appmovil.prestaaula.ui.components.rememberVibrador
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
    val equipos by vm.equipos.collectAsStateWithLifecycle()
    val filtro by vm.filtroTipo.collectAsStateWithLifecycle()
    val vibrador = rememberVibrador()
    // Un pulso confirma la acción; dos avisan que no se pudo (misma convención que el resto de la app).
    fun conVibracion(error: String?) {
        if (error == null) vibrador.exito() else vibrador.error()
    }
    InventarioContenido(
        equipos = equipos,
        filtro = filtro,
        onFiltrar = vm::filtrarPor,
        onAgregar = { tipo, marca, codigo, estado, alTerminar ->
            vm.agregar(tipo, marca, codigo, estado) { error ->
                conVibracion(error)
                alTerminar(error)
            }
        },
        codigoSugerido = vm::codigoSugerido,
        errorCodigo = vm::errorCodigo,
        reservasPendientes = vm::reservasPendientes,
        onMarcarDanado = { id -> vm.marcarDanado(id, ::conVibracion) },
        onVolver = onVolver,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InventarioContenido(
    equipos: List<Equipo>,
    filtro: TipoEquipo?,
    onFiltrar: (TipoEquipo?) -> Unit,
    // El último parámetro recibe el resultado cuando termina de guardarse: null = guardado, o el error.
    onAgregar: (TipoEquipo, String, String, EstadoEquipo, (String?) -> Unit) -> Unit,
    codigoSugerido: (TipoEquipo) -> String,
    errorCodigo: (TipoEquipo, String) -> String?,
    reservasPendientes: (Int) -> Int,
    onMarcarDanado: (Int) -> Unit,
    onVolver: () -> Unit,
) {
    var mostrarDialogo by rememberSaveable { mutableStateOf(false) }
    var equipoADanarId by rememberSaveable { mutableStateOf<Int?>(null) }
    val equipoADanar = equipos.firstOrNull { it.id == equipoADanarId }
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
                items(visibles, key = { it.id }) {
                    EquipoCard(it, Modifier.animateItem()) { equipoADanarId = it.id }
                }
            }
        }
    }

    if (mostrarDialogo) {
        AgregarEquipoDialogo(
            onGuardar = onAgregar,
            codigoSugerido = codigoSugerido,
            errorCodigo = errorCodigo,
            onCerrar = { mostrarDialogo = false },
        )
    }

    if (equipoADanar != null) {
        DanadoDialogo(
            equipo = equipoADanar,
            pendientes = reservasPendientes(equipoADanar.id),
            onConfirmar = {
                onMarcarDanado(equipoADanar.id)
                equipoADanarId = null
            },
            onCerrar = { equipoADanarId = null },
        )
    }
}

@Composable
private fun EquipoCard(equipo: Equipo, modifier: Modifier = Modifier, onDanado: () -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth().heightIn(min = 64.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
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
            // Un equipo prestado se marca al devolverlo; uno en mantención ya está fuera de servicio.
            if (equipo.estado == EstadoEquipo.DISPONIBLE) {
                OutlinedButton(
                    onClick = onDanado,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(top = 8.dp),
                ) {
                    Icon(Icons.Filled.ReportProblem, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("Marcar como dañado", modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun DanadoDialogo(equipo: Equipo, pendientes: Int, onConfirmar: () -> Unit, onCerrar: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Marcar ${equipo.codigo} como dañado") },
        text = {
            Column {
                Text(
                    "El equipo pasará a \"En mantención\": no se podrá reservar ni prestar hasta que " +
                        "lo marques como reparado en Equipos con incidencias.",
                )
                if (pendientes > 0) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Tiene $pendientes ${if (pendientes == 1) "reserva pendiente" else "reservas pendientes"}: " +
                            "seguirán registradas, pero el equipo no podrá entregarse.",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onConfirmar) { Text("Marcar como dañado") } },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar") } },
    )
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

// Todos los datos de un equipo según el caso: tipo, marca, número de identificación interno y estado.
@Composable
private fun AgregarEquipoDialogo(
    onGuardar: (TipoEquipo, String, String, EstadoEquipo, (String?) -> Unit) -> Unit,
    codigoSugerido: (TipoEquipo) -> String,
    errorCodigo: (TipoEquipo, String) -> String?,
    onCerrar: () -> Unit,
) {
    var tipo by rememberSaveable { mutableStateOf(TipoEquipo.NOTEBOOK) }
    var estado by rememberSaveable { mutableStateOf(EstadoEquipo.DISPONIBLE) }
    var marca by rememberSaveable { mutableStateOf("") }
    // Mientras el CRA no escribe el código, se muestra el siguiente libre del tipo elegido.
    var codigoEscrito by rememberSaveable { mutableStateOf<String?>(null) }
    val codigo = codigoEscrito ?: codigoSugerido(tipo)
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var intentos by rememberSaveable { mutableIntStateOf(0) }
    // Mientras se guarda, el botón queda deshabilitado: un doble toque no agrega el equipo dos veces.
    var guardando by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Agregar equipo") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text("Tipo", style = MaterialTheme.typography.labelLarge)
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
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(4.dp))
                CampoValidado(
                    valor = codigo,
                    onValorChange = { codigoEscrito = it.uppercase(); error = null },
                    etiqueta = "Número de identificación",
                    icono = Icons.Filled.Tag,
                    error = errorCodigo(tipo, codigo),
                    intentos = intentos,
                    ayuda = "Sugerido: ${codigoSugerido(tipo)}. Puedes cambiarlo.",
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(4.dp))
                Text("Estado inicial", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(EstadoEquipo.DISPONIBLE, EstadoEquipo.EN_MANTENCION).forEach {
                        FilterChip(selected = estado == it, onClick = { estado = it }, label = { Text(it.etiqueta) })
                    }
                }
                MensajeError(error)
            }
        },
        confirmButton = {
            TextButton(
                enabled = !guardando,
                onClick = {
                    intentos++
                    if (Validaciones.marca(marca) == null && errorCodigo(tipo, codigo) == null) {
                        guardando = true
                        onGuardar(tipo, marca, codigo, estado) { resultado ->
                            guardando = false
                            error = resultado
                            if (resultado == null) onCerrar()
                        }
                    }
                },
            ) { Text(if (guardando) "Guardando…" else "Guardar") }
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
            onAgregar = { _, _, _, _, listo -> listo(null) },
            codigoSugerido = { "NB-002" },
            errorCodigo = { _, _ -> null },
            reservasPendientes = { 0 },
            onMarcarDanado = {},
            onVolver = {},
        )
    }
}
