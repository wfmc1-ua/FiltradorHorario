package com.wesley.cuadrantes

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.wesley.cuadrantes.ui.import.ImportScreen
import com.wesley.cuadrantes.ui.import.ImportViewModel
import com.wesley.cuadrantes.ui.review.ReviewScreen
import com.wesley.cuadrantes.ui.review.ReviewViewModel
import com.wesley.cuadrantes.ui.selector.EmployeeSelectorScreen
import com.wesley.cuadrantes.ui.selector.SelectorViewModel
import com.wesley.cuadrantes.ui.theme.CuadrantesTheme

class MainActivity : ComponentActivity() {

    private var pendingUri by mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PDFBoxResourceLoader.init(applicationContext)
        pendingUri = resolveUri(intent)
        enableEdgeToEdge()
        val container = (application as CuadrantesApp).container
        setContent {
            CuadrantesTheme {
                AppContent(
                    container = container,
                    pendingUri = pendingUri,
                    contentResolver = contentResolver,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        pendingUri = resolveUri(intent)
    }

    private fun resolveUri(intent: Intent?): Uri? = when (intent?.action) {
        Intent.ACTION_SEND -> @Suppress("DEPRECATION") intent.getParcelableExtra(Intent.EXTRA_STREAM)
        Intent.ACTION_VIEW -> intent.data
        else -> null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppContent(
    container: AppContainer,
    pendingUri: Uri?,
    contentResolver: android.content.ContentResolver,
) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()

    val title = when {
        backStack?.destination?.route == "selector" -> "Elegir empleado"
        backStack?.destination?.route?.startsWith("review/") == true -> "Revisión de horario"
        else -> "Cuadrantes"
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text(title) }) },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "import",
            modifier = Modifier.padding(innerPadding),
        ) {
            composable("import") {
                val vm: ImportViewModel = viewModel(
                    factory = ImportViewModel.Factory(container, contentResolver),
                )
                ImportScreen(
                    viewModel = vm,
                    pendingUri = pendingUri,
                    onDone = { navController.navigate("selector") },
                )
            }

            composable("selector") {
                val vm: SelectorViewModel = viewModel(
                    factory = SelectorViewModel.Factory(container),
                )
                EmployeeSelectorScreen(
                    viewModel = vm,
                    onEmployeeSelected = { index ->
                        navController.navigate("review/$index")
                    },
                )
            }

            composable(
                route = "review/{index}",
                arguments = listOf(navArgument("index") { type = NavType.IntType }),
            ) { entry ->
                val index = entry.arguments!!.getInt("index")
                val vm: ReviewViewModel = viewModel(
                    factory = ReviewViewModel.Factory(container, index),
                )
                ReviewScreen(viewModel = vm)
            }
        }
    }
}
