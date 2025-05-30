package dugsolutions.leaf.main.gather

import dugsolutions.leaf.game.Game
import dugsolutions.leaf.game.domain.GameTurn
import dugsolutions.leaf.main.domain.MainDomain
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MainDomainManager(
    private val game: Game,
    private val gameTurn: GameTurn,
    private val gatherPlayerInfo: GatherPlayerInfo,
    private val gatherGroveInfo: GatherGroveInfo,
) {
    private val _state = MutableStateFlow(MainDomain())

    // region public

    val state: StateFlow<MainDomain> = _state.asStateFlow()

    var showDrawCount: Boolean
        get() = state.value.showDrawCount
        set(value) {
            _state.update { currentState ->
                currentState.copy(
                    showDrawCount = value
                )
            }
        }

    var showRunButton: Boolean
        get() = state.value.showRunButton
        set(value) {
            _state.update { currentState ->
                currentState.copy(
                    showRunButton = value
                )
            }
        }

    fun update() {
        _state.update { currentState ->
            currentState.copy(
                turn = gameTurn.turn,
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

    // endregion public

}
