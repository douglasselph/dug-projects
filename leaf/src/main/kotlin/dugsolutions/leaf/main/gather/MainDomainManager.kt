package dugsolutions.leaf.main.gather

import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.game.Game
import dugsolutions.leaf.game.domain.GameTurn
import dugsolutions.leaf.game.turn.select.SelectPossibleCards
import dugsolutions.leaf.main.domain.MainDomain
import dugsolutions.leaf.player.Player
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

    fun setShowDrawCount(player: Player, value: Boolean) {
        _state.update { currentState ->
            currentState.copy(
                players = currentState.players.map { playerInfo ->
                    if (playerInfo.name == player.name) {
                        playerInfo.copy(showDrawCount = value)
                    } else {
                        playerInfo
                    }
                }
            )
        }
    }

    fun clearShowDrawCount() {
        _state.update { currentState ->
            currentState.copy(
                players = currentState.players.map { playerInfo ->
                    playerInfo.copy(showDrawCount = false)
                }
            )
        }
    }

    fun setShowRunButton(value: Boolean) {
        _state.update { currentState ->
            currentState.copy(
                showRunButton = value
            )
        }
    }

    fun clearShowRunButton() {
        _state.update { currentState ->
            currentState.copy(
                showRunButton = false
            )
        }
    }

    fun setShowNextButton(value: Boolean) {
        _state.update { currentState ->
            currentState.copy(
                showNextButton = value
            )
        }
    }

    fun clearShowNextButton() {
        _state.update { currentState ->
            currentState.copy(
                showNextButton = false
            )
        }
    }

    fun setStepMode(value: Boolean) {
        _state.update { currentState ->
            currentState.copy(
                stepModeEnabled = value
            )
        }
    }

    fun setHighlightGroveCardsForSelection(possibleCards: List<GameCard>, player: Player) {
        _state.update { currentState ->
            currentState.copy(
                turn = gameTurn.turn,
                groveInfo = gatherGroveInfo(possibleCards, player),
            )
        }
    }

    fun clearGroveCardHighlights() {
        update()
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
