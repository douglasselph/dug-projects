package dugsolutions.leaf.game.purchase

import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.game.purchase.cost.ApplyCost
import dugsolutions.leaf.game.purchase.credit.CombinationGenerator
import dugsolutions.leaf.game.purchase.evaluator.PurchaseCardEvaluator
import dugsolutions.leaf.game.purchase.evaluator.PurchaseDieEvaluator
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.decisions.DecisionAcquireSelect

class PurchaseItem(
    private val combinationGenerator: CombinationGenerator,
    private val purchaseCardEvaluator: PurchaseCardEvaluator,
    private val purchaseDieEvaluator: PurchaseDieEvaluator,
    private val managePurchasedFloralTypes: ManagePurchasedFloralTypes,
    private val applyCost: ApplyCost,
    private val chronicle: GameChronicle
) {

    operator fun invoke(player: Player, marketCards: List<GameCard>) {
        val combinations = combinationGenerator(player)
        val bestCardChoice = purchaseCardEvaluator(player, combinations, marketCards)
        val bestDieChoice = purchaseDieEvaluator(combinations)
        val bestChoice = player.decisionDirector.acquireSelectDecision(bestCardChoice, bestDieChoice)
        when (bestChoice) {
            is DecisionAcquireSelect.BuyItem.Card -> {
                val card = bestChoice.item.card
                val combination = bestChoice.item.combination
                applyCost(player, combination) {
                    player.addCardToCompost(card.id)
                }
                managePurchasedFloralTypes.add(card.type)
                chronicle(GameChronicle.Moment.ACQUIRE_CARD(player, card, combination))
            }
            is DecisionAcquireSelect.BuyItem.Die -> {
                val die = bestChoice.item.die
                val combination = bestChoice.item.combination
                applyCost(player, combination) {
                    player.addDieToCompost(die)
                }
                chronicle(GameChronicle.Moment.ACQUIRE_DIE(player, die, combination))
            }
            else -> {
                player.discardHand()
            }
        }
    }

}
