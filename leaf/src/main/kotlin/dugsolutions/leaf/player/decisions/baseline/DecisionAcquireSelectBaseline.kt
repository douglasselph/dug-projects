package dugsolutions.leaf.player.decisions.baseline

import dugsolutions.leaf.game.acquire.evaluator.AcquireCardEvaluator
import dugsolutions.leaf.game.acquire.evaluator.AcquireDieEvaluator
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.decisions.core.DecisionAcquireSelect

/**
 * Strategy for deciding whether to acquire a card or die during the acquire phase.
 * 
 * Decision Rules:
 * 1. Calculate total scores:
 *    - Card Score = totalCardCount + preferenceCard
 *    - Die Score = totalDiceCount + preferenceDie
 * 2. Choose the option with the lower score (to balance card/die count)
 * 3. If scores are equal, prefer die over card
 * 4. If best choice for preferred option is null, fall back to other option
 * 5. If both best choices are null, return None
 * 
 * Preference Adjustments:
 * - preferenceCard: Added to card count to bias towards/against cards
 *   - Positive values make cards less likely to be chosen
 *   - Negative values make cards more likely to be chosen
 *   - Example: preferenceCard = 2 means cards need 2 fewer than dice to be chosen
 * 
 * - preferenceDie: Added to die count to bias towards/against dice
 *   - Positive values make dice less likely to be chosen
 *   - Negative values make dice more likely to be chosen
 *   - Example: preferenceDie = -1 means dice need 1 more than cards to be chosen
 * 
 * Example Usage:
 * ```kotlin
 * val strategy = DecisionAcquireSelectCoreStrategy(player)
 * 
 * // Bias towards cards (cards need 2 more than dice to be chosen)
 * strategy.preferenceCard = -2
 * strategy.preferenceDie = 0
 * 
 * // Bias towards dice (dice need 1 more than cards to be chosen)
 * strategy.preferenceCard = 0
 * strategy.preferenceDie = -1
 * 
 * // Strong bias towards cards (cards need 3 more than dice to be chosen)
 * strategy.preferenceCard = -3
 * strategy.preferenceDie = 0
 * ```
 */
class DecisionAcquireSelectBaseline(
    private val player: Player
) : DecisionAcquireSelect {

    var preferenceCard = 0
    var preferenceDie = 0

    override fun invoke(
        bestCard: AcquireCardEvaluator.Choice?,
        bestDie: AcquireDieEvaluator.BestChoice?
    ): DecisionAcquireSelect.BuyItem {
        val scoreCards = player.totalCardCount + preferenceCard
        val scoreDice = player.totalDiceCount + preferenceDie

        return if (scoreCards < scoreDice && bestCard != null) {
            DecisionAcquireSelect.BuyItem.Card(bestCard)
        } else if (bestDie != null) {
            DecisionAcquireSelect.BuyItem.Die(bestDie)
        } else {
            DecisionAcquireSelect.BuyItem.None
        }
    }

} 
