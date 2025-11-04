package dugsolutions.leaf.game

import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.chronicle.domain.Moment
import dugsolutions.leaf.game.acquire.AcquireItems
import dugsolutions.leaf.game.select.SelectCardsToExecute
import dugsolutions.leaf.game.select.SelectGraftedDice
import dugsolutions.leaf.game.select.SelectItemsToAcquire
import dugsolutions.leaf.game.turn.PlayerOrder
import dugsolutions.leaf.game.turn.handle.HandleCards
import dugsolutions.leaf.game.turn.handle.HandleCleanup
import dugsolutions.leaf.player.Player

class GameCultivationTurn(
    private val playerOrder: PlayerOrder,
    private val selectGraftedDice: SelectGraftedDice,
    private val selectCardsToExecute: SelectCardsToExecute,
    private val handleCards: HandleCards,
    private val selectItemsToAcquire: SelectItemsToAcquire,
    private val acquireItems: AcquireItems,
    private val handleCleanup: HandleCleanup,
    private val chronicle: GameChronicle
) {

    suspend operator fun invoke(players: List<Player>) {
        reportHand(players)
        val orderedPlayers = playerOrder(players)
        orderedPlayers.forEach { player ->
            player.addDiceToHand(selectGraftedDice(player))
            handleCards(player, selectCardsToExecute(player))
            acquireItems(player, selectItemsToAcquire(player))
        }
        orderedPlayers.forEach { player -> handleCleanup(player) }
    }

    private fun reportHand(players: List<Player>) {
        players.forEach { chronicle(Moment.REPORT_HAND(it)) }
    }
}
