package dugsolutions.leaf.player.decisions.baseline

import dugsolutions.leaf.components.CardID
import dugsolutions.leaf.components.FlourishType
import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.decisions.core.DecisionBestCardPurchase

/**
 * Strategy for determining the best card to purchase from a list of available cards.
 * 
 * Decision Rules (in order of priority):
 * 1. Highest evaluation score wins
 * 2. If tied, least owned card wins
 * 3. If still tied, lowest flourish type priority wins (ROOT=0, CANOPY=1, VINE=2, FLOWER=3)
 * 
 * Evaluation Score Rules:
 * - Each card can have specific evaluation rules based on how many copies are owned
 * - Rules are defined in evaluationMap using CountsInHand:
 *   ```kotlin
 *   evaluationMap[cardId] = CountsInHand(
 *       listOf(
 *           CountInHand(0, 1),  // 0-1 copies: score = 1
 *           CountInHand(2, 3)   // 2+ copies: score = 3
 *       )
 *   )
 *   ```
 * - CountsInHand list must be ordered by countInHand (ascending)
 * - The first CountInHand where countInHand <= actual count is used
 * - If no specific rules exist, generalEvaluation is used
 * 
 * Example Usage:
 * ```kotlin
 * val strategy = DecisionBestCardPurchaseCoreStrategy(player)
 * 
 * // Set general evaluation (used when no specific rules exist)
 * strategy.generalEvaluation = 1
 * 
 * // Set specific evaluation rules for a card
 * strategy.evaluationMap[cardId] = CountsInHand(
 *     listOf(
 *         CountInHand(0, 5),  // First copy is worth 5
 *         CountInHand(1, 3),  // Second copy is worth 3
 *         CountInHand(3, 1)   // Fourth+ copy is worth 1
 *     )
 * )
 * ```
 */
class DecisionBestCardPurchaseBaseline(
    private val player: Player
) : DecisionBestCardPurchase {

    data class CountInHand(
        val countInHand: Int,
        val score: Int
    )

    data class CountsInHand(
        val list: List<CountInHand>
    )

    val evaluationMap = mutableMapOf<CardID, CountsInHand>()
    var generalEvaluation: Int = 0
    
    private val flourishTypePriority = mapOf(
        FlourishType.ROOT to 0,
        FlourishType.CANOPY to 1,
        FlourishType.VINE to 2,
        FlourishType.FLOWER to 3
    )

    private data class Score(
        val card: GameCard,
        val evaluation: Int,
        val count: Int
    )
    
    override suspend operator fun invoke(possibleCards: List<GameCard>): GameCard {
        if (possibleCards.isEmpty()) {
            throw IllegalArgumentException("Cannot decide best purchase from empty list")
        }

        // Get counts of each card in player's deck
        val cardCounts = player.allCardsInDeck.groupBy { it.id }
            .mapValues { it.value.size }

        // Calculate evaluation scores for each card
        val cardScores = possibleCards.map { card ->
            val count = cardCounts[card.id] ?: 0
            val evaluation = evaluationMap[card.id]?.let { countsInHand ->
                // Find the first CountInHand where countInHand >= actual count
                countsInHand.list.findLast { it.countInHand <= count }?.score
            } ?: generalEvaluation

            Score(card, evaluation, count)
        }

        // Find the highest evaluation score
        val maxScore = cardScores.maxOf { it.evaluation }

        // Filter to cards with the highest score
        val highestScoringCards = cardScores.filter { it.evaluation == maxScore }
            .map { Score(it.card, it.evaluation, it.count) }

        // If only one card has the highest score, return it
        if (highestScoringCards.size == 1) {
            return highestScoringCards[0].card
        }

        // Find the minimum count among highest scoring cards
        val minCount = highestScoringCards.minOf { it.count }

        // Filter to cards with the minimum count
        val leastOwnedCards = highestScoringCards.filter { it.count == minCount }
            .map { it.card }

        // If only one card has the minimum count, return it
        if (leastOwnedCards.size == 1) {
            return leastOwnedCards[0]
        }

        // Sort by flourish type priority and return the first one
        return leastOwnedCards.minByOrNull { flourishTypePriority[it.type] ?: Int.MAX_VALUE }
            ?: leastOwnedCards.first()
    }
}
