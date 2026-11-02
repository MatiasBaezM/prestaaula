package appmovil.prestaaula.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import appmovil.prestaaula.model.Funcion
import appmovil.prestaaula.model.funcionPermitida
import appmovil.prestaaula.PrestaAulaApp
import appmovil.prestaaula.ui.disponibilidad.DisponibilidadScreen
import appmovil.prestaaula.ui.disponibilidad.DisponibilidadViewModel
import appmovil.prestaaula.ui.home.HomeScreen
import appmovil.prestaaula.ui.inventario.InventarioScreen
import appmovil.prestaaula.ui.inventario.InventarioViewModel
import appmovil.prestaaula.ui.login.AuthViewModel
import appmovil.prestaaula.ui.login.LoginScreen
import appmovil.prestaaula.ui.prestamos.RegistrarDevolucionScreen
import appmovil.prestaaula.ui.prestamos.RegistrarDevolucionViewModel
import appmovil.prestaaula.ui.prestamos.RegistrarPrestamoScreen
import appmovil.prestaaula.ui.prestamos.RegistrarPrestamoViewModel
import appmovil.prestaaula.ui.prestamos.RetiroDevolucionScreen
import appmovil.prestaaula.ui.prestamos.RetiroDevolucionViewModel
import appmovil.prestaaula.ui.reportes.IncidenciasScreen
import appmovil.prestaaula.ui.reportes.IncidenciasViewModel
import appmovil.prestaaula.ui.reportes.ReporteUsoScreen
import appmovil.prestaaula.ui.reportes.ReporteUsoViewModel
import appmovil.prestaaula.ui.reservas.MisReservasScreen
import appmovil.prestaaula.ui.reservas.MisReservasViewModel
import appmovil.prestaaula.ui.reservas.ReservarScreen
import appmovil.prestaaula.ui.reservas.ReservarViewModel

private const val DURACION_NAV = 300

private object Rutas {
    const val LOGIN = "login"
    const val HOME = "home"
    const val FUNCION = "funcion/{nombre}"
    fun funcion(f: Funcion) = "funcion/${f.name}"
}

// Limpia todo el back stack al entrar a un destino raíz (login u home).
private fun NavHostController.irARaiz(ruta: String) = navigate(ruta) {
    popUpTo(graph.id) { inclusive = true }
    launchSingleTop = true
}

// ViewModel atado a la pantalla actual, armado con los repositorios compartidos de la app.
@Composable
private inline fun <reified T : ViewModel> vmDeApp(crossinline crear: (PrestaAulaApp) -> T): T {
    val app = LocalContext.current.applicationContext as PrestaAulaApp
    return viewModel(factory = viewModelFactory { initializer { crear(app) } })
}

@Composable
fun AppNavHost(auth: AuthViewModel = viewModel()) {
    val navController = rememberNavController()
    val usuario by auth.usuarioActual.collectAsState()
    val form by auth.form.collectAsState()
    val rutaActual = navController.currentBackStackEntryAsState().value?.destination?.route

    // Tras rotar, NavHost restaura su estado e ignora este valor.
    val inicio = remember { if (usuario == null) Rutas.LOGIN else Rutas.HOME }

    // La sesión manda: sin usuario siempre login; con usuario nunca login.
    LaunchedEffect(usuario, rutaActual) {
        if (rutaActual == null) return@LaunchedEffect
        if (usuario == null && rutaActual != Rutas.LOGIN) navController.irARaiz(Rutas.LOGIN)
        if (usuario != null && rutaActual == Rutas.LOGIN) navController.irARaiz(Rutas.HOME)
    }

    // Al avanzar la pantalla nueva entra deslizándose desde la derecha; al volver, el movimiento se invierte.
    NavHost(
        navController = navController,
        startDestination = inicio,
        enterTransition = { slideInHorizontally(tween(DURACION_NAV)) { it / 4 } + fadeIn(tween(DURACION_NAV)) },
        exitTransition = { slideOutHorizontally(tween(DURACION_NAV)) { -it / 6 } + fadeOut(tween(DURACION_NAV / 2)) },
        popEnterTransition = { slideInHorizontally(tween(DURACION_NAV)) { -it / 6 } + fadeIn(tween(DURACION_NAV)) },
        popExitTransition = { slideOutHorizontally(tween(DURACION_NAV)) { it / 4 } + fadeOut(tween(DURACION_NAV / 2)) },
    ) {
        composable(Rutas.LOGIN) {
            LoginScreen(
                form = form,
                onCorreoChange = auth::onCorreoChange,
                onClaveChange = auth::onClaveChange,
                onToggleMostrarClave = auth::toggleMostrarClave,
                onIniciarSesion = auth::iniciarSesion,
            )
        }
        composable(Rutas.HOME) {
            usuario?.let {
                HomeScreen(
                    usuario = it,
                    onFuncionClick = { f -> navController.navigate(Rutas.funcion(f)) },
                    onCerrarSesion = auth::cerrarSesion,
                )
            }
        }
        composable(
            route = Rutas.FUNCION,
            arguments = listOf(navArgument("nombre") { type = NavType.StringType }),
        ) { entry ->
            val actual = usuario ?: return@composable
            val funcion = funcionPermitida(actual.rol, entry.arguments?.getString("nombre"))

            // Guardia de acceso: nombre inválido o rol sin permiso -> home.
            if (funcion == null) {
                LaunchedEffect(Unit) {
                    navController.navigate(Rutas.HOME) {
                        popUpTo(Rutas.HOME) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            } else {
                val volver: () -> Unit = { navController.popBackStack() }
                when (funcion) {
                    Funcion.INVENTARIO ->
                        InventarioScreen(vmDeApp { InventarioViewModel(it.inventario) }, volver)
                    Funcion.DISPONIBILIDAD ->
                        DisponibilidadScreen(vmDeApp { DisponibilidadViewModel(it.inventario, it.reservas) }, volver)
                    Funcion.RESERVAR_EQUIPO ->
                        ReservarScreen(vmDeApp { ReservarViewModel(it.inventario, it.reservas, actual) }, volver)
                    Funcion.MIS_RESERVAS ->
                        MisReservasScreen(vmDeApp { MisReservasViewModel(it.inventario, it.reservas, actual) }, volver)
                    Funcion.REGISTRAR_PRESTAMO ->
                        RegistrarPrestamoScreen(
                            vmDeApp { RegistrarPrestamoViewModel(it.inventario, it.reservas, it.prestamos) }, volver,
                        )
                    Funcion.REGISTRAR_DEVOLUCION ->
                        RegistrarDevolucionScreen(
                            vmDeApp { RegistrarDevolucionViewModel(it.inventario, it.prestamos) }, volver,
                        )
                    Funcion.RETIRO_DEVOLUCION ->
                        RetiroDevolucionScreen(
                            vmDeApp { RetiroDevolucionViewModel(it.inventario, it.reservas, it.prestamos, actual) }, volver,
                        )
                    Funcion.REPORTE_USO ->
                        ReporteUsoScreen(vmDeApp { ReporteUsoViewModel(it.inventario, it.prestamos) }, volver)
                    Funcion.EQUIPOS_INCIDENCIAS ->
                        IncidenciasScreen(
                            vmDeApp { IncidenciasViewModel(it.inventario, it.prestamos, actual) }, volver,
                        )
                }
            }
        }
    }
}
