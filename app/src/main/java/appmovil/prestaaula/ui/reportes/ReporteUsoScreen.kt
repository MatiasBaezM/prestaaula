package appmovil.prestaaula.ui.reportes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import appmovil.prestaaula.model.Conteo
import appmovil.prestaaula.model.Periodo
import appmovil.prestaaula.model.filtrarPorPeriodo
import appmovil.prestaaula.model.resumenUso
import appmovil.prestaaula.ui.components.FilaChips

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReporteUsoScreen(vm: ReporteUsoViewModel, onVolver: () -> Unit) {
    val equipos by vm.equipos.collectAsStateWithLifecycle()
    val registro by vm.registro.collectAsStateWithLifecycle()
    val periodo by vm.periodo.collectAsStateWithLifecycle()
    val resumen = resumenUso(filtrarPorPeriodo(registro, periodo, vm.hoy), equipos)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reporte de uso") },
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
                Periodo.entries.forEach {
                    FilterChip(
                        selected = it == periodo,
                        onClick = { vm.elegirPeriodo(it) },
                        label = { Text(it.etiqueta) },
                    )
                }
            }
            if (resumen.total == 0) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Sin préstamos en este período",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Indicador("Préstamos", resumen.total, Modifier.weight(1f))
                            Indicador("En curso", resumen.enCurso, Modifier.weight(1f))
                            Indicador("Con problemas", resumen.conProblemas, Modifier.weight(1f))
                        }
                    }
                    item { SeccionBarras("Por tipo de equipo", resumen.porTipo) }
                    item { SeccionBarras("Equipos más usados", resumen.masUsados) }
                    item { SeccionBarras("Por docente", resumen.porDocente) }
                }
            }
        }
    }
}

@Composable
private fun Indicador(titulo: String, valor: Int, modifier: Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Text("$valor", style = MaterialTheme.typography.headlineMedium)
            Text(
                titulo,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// Ranking de una sola serie: barra fina de un tono, valor escrito en cada fila.
@Composable
private fun SeccionBarras(titulo: String, datos: List<Conteo>) {
    val maximo = datos.maxOfOrNull { it.valor } ?: 1
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(titulo, style = MaterialTheme.typography.titleMedium)
        datos.forEach { BarraRanking(it, maximo) }
    }
}

@Composable
private fun BarraRanking(dato: Conteo, maximo: Int) {
    val fraccion = (dato.valor.toFloat() / maximo).coerceIn(0.02f, 1f)
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(dato.etiqueta, style = MaterialTheme.typography.bodyMedium)
            Text("${dato.valor}", style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraccion)
                    .fillMaxHeight()
                    .background(
                        MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp),
                    ),
            )
        }
    }
}
