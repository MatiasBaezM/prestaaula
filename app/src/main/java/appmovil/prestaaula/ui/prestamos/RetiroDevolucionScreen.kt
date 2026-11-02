package appmovil.prestaaula.ui.prestamos

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
import androidx.compose.material3.Button
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import appmovil.prestaaula.model.Bloques
import appmovil.prestaaula.model.Equipo
import appmovil.prestaaula.model.GrupoReserva
import appmovil.prestaaula.model.Prestamo
import appmovil.prestaaula.model.agruparReservas
import appmovil.prestaaula.ui.components.fechaHoraCorta
import appmovil.prestaaula.ui.components.rememberVibrador

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RetiroDevolucionScreen(vm: RetiroDevolucionViewModel, onVolver: () -> Unit) {
    val equipos by vm.equipos.collectAsState()
    val registro by vm.registro.collectAsState()
    val reservas by vm.reservas.collectAsState()
    val vibrador = rememberVibrador()
    val prestamos = vm.misPrestamos(registro)
    // Una clase doble se muestra como un solo retiro, igual que en "Mis reservas".
    val pendientes = agruparReservas(vm.misReservasDeHoy(reservas))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Retiro y devolución") },
                navigationIcon = {
                    IconButton(onClick = onVolver, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
    ) { padding ->
        if (prestamos.isEmpty() && pendientes.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(
                    "No tienes equipos para retirar ni devolver",
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
                if (prestamos.isNotEmpty()) {
                    item { Text("Equipos en mi poder", style = MaterialTheme.typography.titleMedium) }
                    items(prestamos, key = { "p${it.id}" }) { p ->
                        TarjetaEnPoder(
                            prestamo = p,
                            equipo = equipos.firstOrNull { it.id == p.equipoId },
                            hoy = vm.hoy,
                            onConfirmarRetiro = { if (vm.confirmarRetiro(p.id)) vibrador.exito() else vibrador.error() },
                            onAvisarDevolucion = { if (vm.avisarDevolucion(p.id)) vibrador.exito() else vibrador.error() },
                        )
                    }
                }
                if (pendientes.isNotEmpty()) {
                    item { Text("Reservas de hoy por retirar", style = MaterialTheme.typography.titleMedium) }
                    items(pendientes, key = { "r${it.ids.first()}" }) { r ->
                        TarjetaPorRetirar(r, equipos.firstOrNull { it.id == r.equipoId })
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaEnPoder(
    prestamo: Prestamo,
    equipo: Equipo?,
    hoy: java.time.LocalDate,
    onConfirmarRetiro: () -> Unit,
    onAvisarDevolucion: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "${equipo?.codigo ?: "Equipo"} · ${equipo?.tipo?.etiqueta.orEmpty()} ${equipo?.marca.orEmpty()}".trim(),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                "Curso ${prestamo.curso} · retirado ${fechaHoraCorta(prestamo.retiro, hoy)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            when {
                !prestamo.retiroConfirmado -> Button(
                    onClick = onConfirmarRetiro,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) { Text("Confirmar retiro") }
                !prestamo.devolucionAvisada -> OutlinedButton(
                    onClick = onAvisarDevolucion,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) { Text("Avisar devolución") }
                else -> Text(
                    "Devolución avisada: el CRA debe recibir el equipo",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun TarjetaPorRetirar(reserva: GrupoReserva, equipo: Equipo?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                "${equipo?.codigo ?: "Equipo"} · ${equipo?.tipo?.etiqueta.orEmpty()} ${equipo?.marca.orEmpty()}".trim(),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                "${Bloques.etiquetaRango(reserva.desde, reserva.hasta)} · curso ${reserva.curso}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                "Retíralo en el CRA",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
