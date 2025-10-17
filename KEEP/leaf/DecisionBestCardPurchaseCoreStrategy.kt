package dugsolutions.leaf.player.decisions

import dugsolutions.leaf.common.Commons
import dugsolutions.leaf.components.CardID
import dugsolutions.leaf.components.CostScore
import dugsolutions.leaf.components.FlourishType
import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.player.Player

/** TODO: Unit tests **/
/**
 * Rules of choice:
 *   - HERE
 */
class DecisionBestCardPurchaseCoreStrategy(
    private val player: Player
) : DecisionBestCardPurchase {

    companion object {
        private const val HAS_AT_LEAST = 2
    }

    data class CountInHand(
        val countInHand: Int,
        val score: Int
    )

    data class CountsInHand(
        val list: List<CountInHand>
    )

    val evaluationMap = mutableMapOf<CardID, CountsInHand>()
    val generalEvaluation: Int = 0
    
    override operator fun invoke(possibleCards: List<GameCard>): GameCard {
        if (possibleCards.isEmpty()) {
            throw IllegalArgumentException("Cannot decide best purchase from empty list")
        }
        
        // Get counts of flourish types the player currently has
        val rootCount = player.cardsInHand.count { it.type == FlourishType.ROOT }
        val canopyCount = player.cardsInHand.count { it.type == FlourishType.CANOPY }
        val vineCount = player.cardsInHand.count { it.type == FlourishType.VINE }
        val bloomCount = player.cardsInHand.count { it.type == FlourishType.BLOOM }
        
        // Check if player is about to get their last bloom card
        val isLastBloomCard = bloomCount == Commons.BLOOM_COUNT - 1
        
        // Define flourish type priorities (lowest to highest)
        val priorityOrder = listOf(
            FlourishType.ROOT,
            FlourishType.CANOPY,
            FlourishType.VINE,
            FlourishType.BLOOM
        )
        
        // If this would be the last bloom card, ensure other types meet minimum requirements first
        if (isLastBloomCard) {
            // Check if any flourish type is below minimum count
            val belowMinimumTypes = mutableListOf<FlourishType>()
            if (rootCount < HAS_AT_LEAST) belowMinimumTypes.add(FlourishType.ROOT)
            if (canopyCount < HAS_AT_LEAST) belowMinimumTypes.add(FlourishType.CANOPY)
            if (vineCount < HAS_AT_LEAST) belowMinimumTypes.add(FlourishType.VINE)
            
            // If any type is below minimum, prioritize those cards
            if (belowMinimumTypes.isNotEmpty()) {
                // Sort by priority order
                val sortedBelowMinimumTypes = belowMinimumTypes.sortedBy { priorityOrder.indexOf(it) }
                
                // Try to find cards of the highest priority below-minimum type
                for (type in sortedBelowMinimumTypes) {
                    val cardsOfType = possibleCards.filter { it.type == type }
                    if (cardsOfType.isNotEmpty()) {
                        // Return the best card of this type based on cost score
                        return cardsOfType.maxByOrNull { costScore(it.cost) } ?: cardsOfType.first()
                    }
                }
            }
        }
        
        // Normal priority logic for flourish types
        val typeCounts = mapOf(
            FlourishType.ROOT to rootCount,
            FlourishType.CANOPY to canopyCount,
            FlourishType.VINE to vineCount,
            FlourishType.BLOOM to bloomCount
        )
        
        // First, try to find types below minimum count
        val belowMinimumTypes = priorityOrder.filter { typeCounts[it]!! < HAS_AT_LEAST }
        
        if (belowMinimumTypes.isNotEmpty()) {
            // Try each type in priority order
            for (type in belowMinimumTypes) {
                val cardsOfType = possibleCards.filter { it.type == type }
                if (cardsOfType.isNotEmpty()) {
                    // Return the best card of this type based on cost score
                    return cardsOfType.maxByOrNull { costScore(it.cost) } ?: cardsOfType.first()
                }
            }
        }
        
        // If all types meet minimum requirements or no cards of below-minimum types are available
        // Choose by priority order
        for (type in priorityOrder) {
            // For BLOOM, check if we're already at the limit
            if (type == FlourishType.BLOOM && bloomCount >= Commons.BLOOM_COUNT) {
                continue
            }
            
            val cardsOfType = possibleCards.filter { it.type == type }
            if (cardsOfType.isNotEmpty()) {
                // Return the best card of this type based on cost score
                return cardsOfType.maxByOrNull { costScore(it.cost) } ?: cardsOfType.first()
            }
        }
        
        // If we get here, just return the highest cost score card
        return possibleCards.maxByOrNull { costScore(it.cost) } ?: possibleCards.first()
    }
}
