package dugsolutions.leaf.main

import androidx.compose.material.MaterialTheme
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import dugsolutions.leaf.di.gameModule
import org.koin.core.context.startKoin
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class MainApplication : KoinComponent {
    private val mainController: MainController by inject()
    
    fun run() {
        application {
            Window(
                onCloseRequest = ::exitApplication,
                title = "Leaf Game Setup"
            ) {
                MaterialTheme {
                    MainScreen(
                        MainScreenArgs(
                            onRunButtonClicked = { mainController.run() },
                            domain = mainController.getDomain()
                        )
                    )
                }
            }
        }
    }
}

fun main() {
    // Start Koin
    startKoin {
        modules(gameModule)
    }
    
    // Run the application
    MainApplication().run()
} 