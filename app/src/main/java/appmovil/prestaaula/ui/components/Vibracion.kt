package appmovil.prestaaula.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

// Retroalimentación háptica: un pulso corto confirma, dos pulsos avisan de un error.
class Vibrador(private val vibrador: Vibrator?) {

    fun exito() = pulsar(longArrayOf(0, 45))

    fun error() = pulsar(longArrayOf(0, 60, 80, 60))

    private fun pulsar(patron: LongArray) {
        val v = vibrador?.takeIf { it.hasVibrator() } ?: return
        try {
            v.vibrate(VibrationEffect.createWaveform(patron, -1))
        } catch (e: SecurityException) {
            // Sin el permiso VIBRATE la app sigue funcionando, solo sin vibrar.
        }
    }
}

@Composable
fun rememberVibrador(): Vibrador {
    val contexto = LocalContext.current.applicationContext
    return remember { Vibrador(vibradorDelSistema(contexto)) }
}

private fun vibradorDelSistema(c: Context): Vibrator? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        c.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        c.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
