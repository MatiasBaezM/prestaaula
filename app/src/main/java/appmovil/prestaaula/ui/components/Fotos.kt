package appmovil.prestaaula.ui.components

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

// Fotos de devolución: viven en la carpeta privada de la app y solo se comparten con la cámara
// del sistema mediante un FileProvider limitado a esta carpeta.
object Fotos {
    private fun carpeta(c: Context) = File(c.filesDir, "fotos").apply { mkdirs() }

    // File(nombre).name descarta cualquier ruta: un nombre alterado no puede salir de la carpeta.
    fun archivo(c: Context, nombre: String) = File(carpeta(c), File(nombre).name)

    fun nuevoNombre() = "dev_${System.currentTimeMillis()}.jpg"

    fun uri(c: Context, nombre: String): Uri =
        FileProvider.getUriForFile(c, "${c.packageName}.fileprovider", archivo(c, nombre))

    fun existe(c: Context, nombre: String) = archivo(c, nombre).let { it.exists() && it.length() > 0 }

    fun borrar(c: Context, nombre: String) {
        archivo(c, nombre).delete()
    }

    // Carga reducida: una foto de cámara completa en pantalla gastaría memoria sin necesidad.
    fun cargar(c: Context, nombre: String, ladoMax: Int = 800): ImageBitmap? {
        val f = archivo(c, nombre)
        if (!f.exists()) return null
        val medidas = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(f.path, medidas)
        var muestra = 1
        while (medidas.outWidth / muestra > ladoMax || medidas.outHeight / muestra > ladoMax) muestra *= 2
        val opciones = BitmapFactory.Options().apply { inSampleSize = muestra }
        return BitmapFactory.decodeFile(f.path, opciones)?.asImageBitmap()
    }
}

@Composable
fun FotoEquipo(nombre: String, descripcion: String, modifier: Modifier = Modifier) {
    val contexto = LocalContext.current
    val imagen by produceState<ImageBitmap?>(null, nombre) {
        value = withContext(Dispatchers.IO) { Fotos.cargar(contexto, nombre) }
    }
    imagen?.let {
        Image(
            bitmap = it,
            contentDescription = descripcion,
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )
    }
}
