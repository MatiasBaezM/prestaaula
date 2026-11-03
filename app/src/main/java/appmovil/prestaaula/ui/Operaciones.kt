package appmovil.prestaaula.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

// Ejecuta una operación del repositorio (guardar en Room) en una corrutina del ViewModel y avisa
// el resultado a la pantalla: null = se guardó, texto = mensaje de error para mostrar.
// viewModelScope: la operación sigue aunque la pantalla rote, y se cancela si el ViewModel se destruye.
// Si SQLite falla (por ejemplo, sin espacio), se informa como error en vez de cerrar la app.
fun ViewModel.ejecutar(alTerminar: (String?) -> Unit, operacion: suspend () -> String?): Job =
    viewModelScope.launch {
        val error = try {
            operacion()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("PrestaAula", "Falló una operación de la base de datos", e)
            "No se pudo guardar. Intenta de nuevo."
        }
        alTerminar(error)
    }
