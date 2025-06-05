package dugsolutions.leaf.main

import androidx.compose.material.MaterialTheme
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
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
                title = "Leaf Game Setup",
                state = WindowState(
                    width = 1400.dp,
                    height = 1800.dp
                )
            ) {
                MaterialTheme {
                    MainScreen(
                        MainScreenArgs(
                            state = mainController.state,
                            onDrawCountChosen = { mainController.onDrawCountChosen(it) },
                            onActionButtonPressed = { mainController.onActionPressed(it) },
                            onStepEnabledToggled = { mainController.onStepEnabledToggled(it) },
                            onGroveCardSelected = { card -> mainController.onGroveCardSelected(card) },
                            onHandCardSelected = { player, card -> mainController.onHandCardSelected(player, card) },
                            onFloralCardSelected = { player, card -> mainController.onFloralCardSelected(player, card) },
                            onDieSelected = { player, card -> mainController.onDieSelected(player, card) }
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
