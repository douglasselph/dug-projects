package dugsolutions.leaf.main.gather

import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.chronicle.domain.Moment
import dugsolutions.leaf.random.die.Die
import dugsolutions.leaf.game.Game
import dugsolutions.leaf.game.domain.GameTime
import dugsolutions.leaf.main.domain.ActionButton
import dugsolutions.leaf.main.domain.CardInfo
import dugsolutions.leaf.main.domain.DieInfo
import dugsolutions.leaf.main.domain.MainGameDomain
import dugsolutions.leaf.main.domain.PlayerInfo
import dugsolutions.leaf.main.domain.SelectedItems
import dugsolutions.leaf.main.local.SelectGather
import dugsolutions.leaf.main.local.SelectItem
import dugsolutions.leaf.player.Player
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MainGameManager(
    private val game: Game,
    private val gameTime: GameTime,
    private val gatherPlayerInfo: GatherPlayerInfo,
    private val gatherGroveInfo: GatherGroveInfo,
    private val selectItem: SelectItem,
    private val selectGather: SelectGather
) {
    private val _state = MutableStateFlow(MainGameDomain())

    // region public

    val state: StateFlow<MainGameDomain> = _state.asStateFlow()

    fun initialize() {
        _state.value = MainGameDomain(
            turn = gameTime.turn,
            players = game.players.map { gatherPlayerInfo(it) },
            groveInfo = gatherGroveInfo()
        )
    }

    fun setStepMode(value: Boolean) {
        _state.update { currentState ->
            currentState.copy(
                turn = gameTime.turn,
                stepModeEnabled = value
            )
        }
    }

    fun setHighlightGroveItemsForSelection(
        possibleCards: List<GameCard>,
        possibleDice: List<Die>,
        player: Player
    ) {
        _state.update { currentState ->
            currentState.copy(
                turn = gameTime.turn,
                groveInfo = gatherGroveInfo(possibleCards, possibleDice, player),
            )
        }
    }

    fun clearGroveCardHighlights() {
        _state.update { currentState ->
            currentState.copy(
                turn = gameTime.turn,
                groveInfo = gatherGroveInfo()
            )
        }
    }

    fun resetData(selectForPlayer: Player? = null) {
        _state.update { currentState ->
            currentState.copy(
                turn = gameTime.turn,
                players = game.players.map { gatherPlayerInfo(it) },
                groveInfo = gatherGroveInfo(selectForPlayer = selectForPlayer)
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
                players = game.players.map { p ->
                    if (p.name == player.name) {
                        gatherPlayerInfo(p).copyForItemSelect()
                    } else {
                        gatherPlayerInfo(p)
                    }
                }
            )
        }
    }

    /**
     * Allows the user to select flower cards from their hand.
     */
    fun setAllowPlayerFlowerSelect(player: Player) {
        _state.update { currentState ->
            currentState.copy(
                turn = gameTime.turn,
                players = game.players.map { p ->
                    if (p.name == player.name) {
                        gatherPlayerInfo(p).copyForFlowerSelect()
                    } else {
                        gatherPlayerInfo(p)
                    }
                }
            )
        }
    }

    fun clearPlayerSelect() {
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
                        selectItem.handCard(playerInfo, cardInfo)
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
                        selectItem.floralCard(playerInfo, cardInfo)
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
                        selectItem.die(playerInfo, dieInfo)
                    } else {
                        playerInfo
                    }
                }
            )
        }
    }

    fun gatherSelected(): SelectedItems {
        return selectGather(_state.value)
    }

    fun setHighlightPlayerCard(player: Player, card: GameCard) {
        _state.update { currentState ->
            currentState.copy(
                turn = gameTime.turn,
                players = currentState.players.map { playerInfo ->
                    if (playerInfo.name == player.name) {
                        playerInfo.copyForCardSelect(card)
                    } else {
                        playerInfo
                    }
                }
            )
        }
    }

    fun setAskTrash(value: Boolean) {
        _state.update { currentState ->
            currentState.copy(
                askTrashEnabled = value
            )
        }
    }

    // endregion public

}
