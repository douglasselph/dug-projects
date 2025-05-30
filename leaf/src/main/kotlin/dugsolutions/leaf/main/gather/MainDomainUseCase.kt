package dugsolutions.leaf.main.gather

import dugsolutions.leaf.game.Game
import dugsolutions.leaf.main.domain.MainDomain
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MainDomainUseCase(
    private val game: Game,
    private val gatherPlayerInfo: GatherPlayerInfo,
    private val gatherGroveInfo: GatherGroveInfo
) {
    private val _state = MutableStateFlow(MainDomain())
    val state: StateFlow<MainDomain> = _state.asStateFlow()

    fun update() {
        _state.update { currentState ->
            currentState.copy(
                players = game.players.map { gatherPlayerInfo(it) },
                groveInfo = gatherGroveInfo()
            )
        }
    }

    fun addSimulationOutput(message: String) {
        _state.update { currentState ->
            currentState.copy(
                simulationOutput = currentState.simulationOutput + message
            )
        }
    }
}
