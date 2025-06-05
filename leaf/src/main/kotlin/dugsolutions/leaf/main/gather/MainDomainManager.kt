package dugsolutions.leaf.main.gather

import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.game.Game
import dugsolutions.leaf.game.domain.GameTime
import dugsolutions.leaf.main.domain.ActionButton
import dugsolutions.leaf.main.domain.CardInfo
import dugsolutions.leaf.main.domain.DieInfo
import dugsolutions.leaf.main.domain.MainDomain
import dugsolutions.leaf.main.domain.PlayerInfo
import dugsolutions.leaf.main.local.ItemSelected
import dugsolutions.leaf.player.Player
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MainDomainManager(
    private val game: Game,
    private val gameTime: GameTime,
    private val gatherPlayerInfo: GatherPlayerInfo,
    private val gatherGroveInfo: GatherGroveInfo,
    private val itemSelected: ItemSelected
) {
    private val _state = MutableStateFlow(MainDomain())

    // region public

    val state: StateFlow<MainDomain> = _state.asStateFlow()

    fun initialize() {
        _state.value = MainDomain(
            turn = gameTime.turn,
            players = game.players.map { gatherPlayerInfo(it) },
            groveInfo = gatherGroveInfo()
        )
    }

    fun setShowDrawCount(player: Player, value: Boolean) {
        _state.update { currentState ->
            currentState.copy(
                turn = gameTime.turn,
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
                turn = gameTime.turn,
                players = currentState.players.map { playerInfo ->
                    playerInfo.copy(showDrawCount = false)
                }
            )
        }
    }

    fun setActionButton(value: ActionButton) {
        _state.update { currentState ->
            currentState.copy(
                turn = gameTime.turn,
                actionButton = value
            )
        }
    }

    fun setStepMode(value: Boolean) {
        _state.update { currentState ->
            currentState.copy(
                turn = gameTime.turn,
                stepModeEnabled = value
            )
        }
    }

    fun setHighlightGroveCardsForSelection(possibleCards: List<GameCard>, player: Player) {
        _state.update { currentState ->
            currentState.copy(
                turn = gameTime.turn,
                groveInfo = gatherGroveInfo(possibleCards, player),
            )
        }
    }

    fun clearGroveCardHighlights() {
        _state.update { currentState ->
            currentState.copy(
                turn = gameTime.turn,
                groveInfo = gatherGroveInfo(),
            )
        }
    }

    /**
     * Allows the user to select cards and dice from their hand.
     */
    fun setAllowPlayerItemSelect(player: Player) {
        _state.update { currentState ->
            currentState.copy(
                turn = gameTime.turn,
                players = currentState.players.map { playerInfo ->
                    if (playerInfo.name == player.name) {
                        playerInfo.copyForItemSelect()
                    } else {
                        playerInfo
                    }
                }
            )
        }
    }

    fun clearAllowPlayerItemSelect() {
        _state.update { currentState ->
            currentState.copy(
                turn = gameTime.turn,
                players = game.players.map { gatherPlayerInfo(it) }
            )
        }
    }

    fun setHandCardSelected(player: PlayerInfo, cardInfo: CardInfo) {
        _state.update { currentState ->
            currentState.copy(
                turn = gameTime.turn,
                players = currentState.players.map { playerInfo ->
                    if (playerInfo.name == player.name) {
                        itemSelected.handCard(playerInfo, cardInfo)
                    } else {
                        playerInfo
                    }
                }
            )
        }
    }

    fun setFloralCardSelected(player: PlayerInfo, cardInfo: CardInfo) {
        _state.update { currentState ->
            currentState.copy(
                turn = gameTime.turn,
                players = currentState.players.map { playerInfo ->
                    if (playerInfo.name == player.name) {
                        itemSelected.floralCard(playerInfo, cardInfo)
                    } else {
                        playerInfo
                    }
                }
            )
        }
    }

    fun setDieSelected(player: PlayerInfo, dieInfo: DieInfo) {
        _state.update { currentState ->
            currentState.copy(
                turn = gameTime.turn,
                players = currentState.players.map { playerInfo ->
                    if (playerInfo.name == player.name) {
                        itemSelected.die(playerInfo, dieInfo)
                    } else {
                        playerInfo
                    }
                }
            )
        }
    }

    fun addSimulationOutput(message: String) {
        _state.update { currentState ->
            currentState.copy(
                turn = gameTime.turn,
                simulationOutput = currentState.simulationOutput + message
            )
        }
    }

    // endregion public

}
