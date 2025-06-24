package dugsolutions.leaf.game.acquire

import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.chronicle.domain.Moment
import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.game.acquire.cost.ApplyCost
import dugsolutions.leaf.game.acquire.evaluator.CombinationGenerator
import dugsolutions.leaf.game.acquire.evaluator.PossibleCards
import dugsolutions.leaf.game.acquire.evaluator.PossibleDice
import dugsolutions.leaf.grove.Grove
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.decisions.core.DecisionAcquireSelect

class AcquireItem(
    private val combinationGenerator: CombinationGenerator,
    private val possibleCards: PossibleCards,
    private val possibleDice: PossibleDice,
    private val manageAcquiredFloralTypes: ManageAcquiredFloralTypes,
    private val applyCost: ApplyCost,
    private val grove: Grove,
    private val chronicle: GameChronicle
) {

    suspend operator fun invoke(player: Player, marketCards: List<GameCard>): Boolean {
        val combinations = combinationGenerator(player)
        val possibleCards = possibleCards(player, combinations, marketCards)
        val possibleDice = possibleDice(combinations)
        val decisionDirector = player.decisionDirector
        if (possibleCards.isEmpty() && possibleDice.isEmpty()) {
            return false
        }
        val bestChoice = decisionDirector.acquireSelectDecision(possibleCards, possibleDice)
        var result = false
        when (bestChoice) {
            is DecisionAcquireSelect.BuyItem.Card -> {
                val card = bestChoice.item.card
                val combination = bestChoice.item.combination
                applyCost(player, combination) {
                    player.addCardToDiscard(card.id)
                    chronicle(Moment.ACQUIRE_CARD(player, card, combination))
                    grove.removeCard(card.id)
                    grove.repairWild()
                    result = true
                }
                manageAcquiredFloralTypes.add(card.type)
            }
            is DecisionAcquireSelect.BuyItem.Die -> {
                val die = bestChoice.item.die
                val combination = bestChoice.item.combination
                applyCost(player, combination) {
                    player.addDieToDiscard(die)
                    chronicle(Moment.ACQUIRE_DIE(player, die, combination))
                    grove.removeDie(die)
                    result = true
                }
            }
            DecisionAcquireSelect.BuyItem.None -> {
            }
        }
        // If result is false, is this something the chronicle should know?
        return result
    }

}
