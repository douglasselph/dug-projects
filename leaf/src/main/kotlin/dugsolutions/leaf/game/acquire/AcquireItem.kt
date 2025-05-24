package dugsolutions.leaf.game.acquire

import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.components.FlourishType
import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.game.acquire.cost.ApplyCost
import dugsolutions.leaf.game.acquire.credit.CombinationGenerator
import dugsolutions.leaf.game.acquire.evaluator.AcquireCardEvaluator
import dugsolutions.leaf.game.acquire.evaluator.AcquireDieEvaluator
import dugsolutions.leaf.grove.Grove
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.decisions.DecisionAcquireSelect

class AcquireItem(
    private val combinationGenerator: CombinationGenerator,
    private val acquireCardEvaluator: AcquireCardEvaluator,
    private val acquireDieEvaluator: AcquireDieEvaluator,
    private val manageAcquiredFloralTypes: ManageAcquiredFloralTypes,
    private val applyCost: ApplyCost,
    private val grove: Grove,
    private val chronicle: GameChronicle
) {

    operator fun invoke(player: Player, marketCards: List<GameCard>) {
        val combinations = combinationGenerator(player)
        val bestCardChoice = acquireCardEvaluator(player, combinations, marketCards)
        val bestDieChoice = acquireDieEvaluator(combinations)
        val bestChoice = player.decisionDirector.acquireSelectDecision(bestCardChoice, bestDieChoice)
        when (bestChoice) {
            is DecisionAcquireSelect.BuyItem.Card -> {
                val card = bestChoice.item.card
                val combination = bestChoice.item.combination
                applyCost(player, combination) {
                    if (card.type == FlourishType.FLOWER) {
                        player.addCardToFloralArray(card.id)
                    } else {
                        player.addCardToCompost(card.id)
                    }
                    grove.removeCard(card.id)
                }
                manageAcquiredFloralTypes.add(card.type)
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
