package dugsolutions.leaf.player.decisions.ui

import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.game.acquire.domain.ChoiceCard
import dugsolutions.leaf.game.acquire.domain.ChoiceDie
import dugsolutions.leaf.player.decisions.core.DecisionAcquireSelect
import dugsolutions.leaf.player.decisions.local.monitor.DecisionID
import dugsolutions.leaf.player.decisions.local.monitor.DecisionMonitor
import dugsolutions.leaf.player.decisions.local.monitor.DecisionMonitorReport
import dugsolutions.leaf.player.decisions.local.monitor.DecisionTaskQueue
import dugsolutions.leaf.random.die.Die

class DecisionAcquireSelectSuspend(
    monitor: DecisionMonitor,
    report: DecisionMonitorReport
) : DecisionAcquireSelect {

    private val taskQueue = DecisionTaskQueue<DecisionAcquireSelect.BuyItem>(monitor, report)
    private var possibleCardsStash: List<ChoiceCard> = emptyList()
    private var possibleDiceStash: List<ChoiceDie> = emptyList()

    // region DecisionBestCardPurchase

    override suspend operator fun invoke(
        possibleCards: List<ChoiceCard>,
        possibleDice: List<ChoiceDie>
    ): DecisionAcquireSelect.BuyItem {
        possibleCardsStash = possibleCards
        possibleDiceStash = possibleDice
        val cards = possibleCards.map { it.card }
        val dice = possibleDice.map { it.die }
        return taskQueue.waitForDecision(DecisionID.ACQUIRE_SELECT(cards, dice))
    }

    // endregion DecisionBestCardPurchase

    // region public

    fun provide(card: GameCard) {
        val choice = possibleCardsStash.find { it.card == card }
        if (choice == null) {
            throw Exception("Called provide() before invoke() function.")
        } else {
            taskQueue.provideDecision(DecisionAcquireSelect.BuyItem.Card(choice))
        }
    }

    fun provide(die: Die) {
        val choice = possibleDiceStash.find { it.die == die }
        if (choice == null) {
            throw Exception("Called provide() before invoke() function.")
        } else {
            taskQueue.provideDecision(DecisionAcquireSelect.BuyItem.Die(choice))
        }
    }

    // endregion public
}
