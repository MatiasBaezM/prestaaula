package appmovil.prestaaula.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import appmovil.prestaaula.model.Funcion
import appmovil.prestaaula.model.Rol
import appmovil.prestaaula.model.Usuario
import appmovil.prestaaula.ui.theme.PrestaaulaTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    usuario: Usuario,
    onFuncionClick: (Funcion) -> Unit,
    onCerrarSesion: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(usuario.nombre, style = MaterialTheme.typography.titleMedium)
                        Text(
                            usuario.rol.etiqueta,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onCerrarSesion, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Cerrar sesión")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text("¿Qué necesitas hacer?", style = MaterialTheme.typography.titleLarge)
            }
            // La UI se construye solo desde las funciones del rol
            items(usuario.rol.funciones) { funcion ->
                FuncionCard(funcion, onClick = { onFuncionClick(funcion) })
            }
        }
    }
}

@Composable
private fun FuncionCard(funcion: Funcion, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                funcion.icono,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp),
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Text(funcion.titulo, style = MaterialTheme.typography.titleMedium)
                Text(
                    funcion.descripcion,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Docente")
@Composable
private fun HomeDocentePreview() {
    PrestaaulaTheme(dynamicColor = false) {
        HomeScreen(Usuario(1, "Prof. Muñoz", "docente@marcelapaz.cl", Rol.DOCENTE), {}, {})
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Encargado CRA")
@Composable
private fun HomeCraPreview() {
    PrestaaulaTheme(dynamicColor = false) {
        HomeScreen(Usuario(2, "Patricia Vega", "cra@marcelapaz.cl", Rol.ENCARGADO_CRA), {}, {})
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Jefe UTP")
@Composable
private fun HomeUtpPreview() {
    PrestaaulaTheme(dynamicColor = false) {
        HomeScreen(Usuario(3, "Andrés Silva", "utp@marcelapaz.cl", Rol.JEFE_UTP), {}, {})
    }
}
