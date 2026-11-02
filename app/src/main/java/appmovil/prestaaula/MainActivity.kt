package appmovil.prestaaula

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import appmovil.prestaaula.ui.navigation.AppNavHost
import appmovil.prestaaula.ui.theme.PrestaaulaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PrestaaulaTheme(dynamicColor = false) {
                AppNavHost()
            }
        }
    }
}
