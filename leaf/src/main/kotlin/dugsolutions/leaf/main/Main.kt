package dugsolutions.leaf.main

import androidx.compose.material.MaterialTheme
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import dugsolutions.leaf.di.appModule
import dugsolutions.leaf.di.appModules
import dugsolutions.leaf.game.di.gameModule
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
                    width = 1800.dp,
                    height = 1800.dp
                )
            ) {
                MaterialTheme {
                    MainScreen(
                        MainScreenArgs(
                            state = mainController.state,
                            onDrawCountChosen = { player, value -> mainController.onDrawCountChosen(player, value) },
                            onActionButtonPressed = { action -> mainController.onActionPressed(action) },
                            onBooleanInstructionChosen = { choice -> mainController.onBooleanInstructionResponse(choice) },
                            onStepEnabledToggled = { mainController.onStepEnabledToggled(it) },
                            onAskTrashToggled = { mainController.onAskTrashToggled(it) },
                            onGroveItemSelected = { item -> mainController.onGroveItemSelected(item) },
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
        modules(appModules)
    }

    // Run the application
    MainApplication().run()
} 
