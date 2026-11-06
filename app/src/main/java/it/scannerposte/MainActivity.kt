package it.scannerposte

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import it.scannerposte.ui.AggregatoScreen
import it.scannerposte.ui.HomeScreen
import it.scannerposte.ui.ScannerScreen
import it.scannerposte.ui.ScannerTheme
import it.scannerposte.ui.ViewerScreen

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ScannerTheme { AppNav(vm) } }
    }
}

@Composable
fun AppNav(vm: AppViewModel) {
    val nav = rememberNavController()
    val idArg = listOf(navArgument("id") { type = NavType.LongType })

    NavHost(nav, startDestination = "home") {
        composable("home") {
            HomeScreen(vm, onApri = { nav.navigate("agg/$it") })
        }
        composable("agg/{id}", arguments = idArg) { entry ->
            val id = entry.arguments!!.getLong("id")
            AggregatoScreen(
                vm, id,
                onScan = { nav.navigate("scan/$id") },
                onPlay = { nav.navigate("view/$id") },
                onSalva = { nav.popBackStack("home", false) },
                onBack = { nav.popBackStack() }
            )
        }
        composable("scan/{id}", arguments = idArg) { entry ->
            val id = entry.arguments!!.getLong("id")
            ScannerScreen(
                vm, id,
                onConferma = { nav.popBackStack() },
                onCompleto = { nav.popBackStack("home", false) }
            )
        }
        composable("view/{id}", arguments = idArg) { entry ->
            val id = entry.arguments!!.getLong("id")
            ViewerScreen(vm, id, onBack = { nav.popBackStack() })
        }
    }
}
