package dugsolutions.leaf.main

import androidx.compose.material.MaterialTheme
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import dugsolutions.leaf.di.gameModule
import dugsolutions.leaf.main.ui.MainScreen
import dugsolutions.leaf.main.ui.MainScreenArgs
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
                            state = mainController.state,
                            onNumPlayersChanged = { mainController.setNumPlayers(it) }
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
