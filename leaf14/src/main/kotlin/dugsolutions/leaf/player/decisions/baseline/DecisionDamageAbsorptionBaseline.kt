package dugsolutions.leaf.player.decisions.baseline

import dugsolutions.leaf.cards.CardManager
import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.decisions.core.DecisionDamageAbsorption
import dugsolutions.leaf.player.decisions.core.DecisionDamageAbsorption.*


/**
 * Strategy for determining which cards sacrifice to absorb incoming damage.
 *
 * Algorithm Overview
 *
 * Two-Phase Approach:
 *  1. Hand Cards First: Always try to use hand cards before creature cards
 *  2. Creature Cards Second: Only use creature cards if hand cards can't absorb all damage
 *
 * Optimal Card Selection Strategy
 * The selectOptimalCards method uses a simple exhaustive approach:
 *  1. Try 1 card first: Look for a single card with resilience ≥ damage
 *  2. Try 2 cards: Look for any combination of 2 cards with combined resilience ≥ damage  
 *  3. Try 3 cards: Look for any combination of 3 cards with combined resilience ≥ damage
 *  4. Continue: Keep increasing until damage is covered or all cards are used
 *
 * Key Features
 *  1. Minimum Cards: Always tries to use the fewest number of cards possible
 *  2. Hand Cards First: Always uses hand cards before creature cards
 *  3. Exhaustive Search: For each card count, tries all possible combinations
 *  4. Fallback: If no combination can cover the damage, returns all available cards
 *
 * Example Scenarios
 *  1. 7 damage, cards [3, 4, 5]: Tries 1 card (none ≥ 7), then 2 cards (finds 4+5=9 ≥ 7)
 *  2. 5 damage, cards [2, 3, 4]: Tries 1 card (finds 4 ≥ 5), returns [4]
 *  3. 10 damage, cards [2, 2, 2, 2]: Tries 1-3 cards (none work), then 4 cards (returns all)
 */
class DecisionDamageAbsorptionBaseline(
    private val player: Player,
    val cardManager: CardManager
) : DecisionDamageAbsorption {


    override suspend operator fun invoke(): Result {
        val amount = player.incomingDamage
        if (amount <= 0) {
            return Result()
        }
        
        val handCards = player.cardsInHand()
        val creatureCards = player.creatureLeafCards
        
        var remainingDamage = amount
        val selectedHandCards = mutableListOf<GameCard>()
        val selectedCreatureCards = mutableListOf<GameCard>()
        
        // First, try to absorb damage with hand cards
        if (handCards.isNotEmpty()) {
            val handSelection = selectOptimalCards(handCards, remainingDamage)
            selectedHandCards.addAll(handSelection)
            remainingDamage -= handSelection.sumOf { it.resilience }
        }
        
        // If there's still damage left, use creature cards
        if (remainingDamage > 0 && creatureCards.isNotEmpty()) {
            val creatureSelection = selectOptimalCards(creatureCards, remainingDamage)
            selectedCreatureCards.addAll(creatureSelection)
            remainingDamage -= creatureSelection.sumOf { it.resilience }
        }
        
        val totalDamageAbsorbed = selectedHandCards.sumOf { it.resilience } + selectedCreatureCards.sumOf { it.resilience }
        
        return Result(
            handCards = selectedHandCards,
            creatureCards = selectedCreatureCards,
            damageAbsorbed = totalDamageAbsorbed,
            damageStillLeftToAbsorb = maxOf(0, remainingDamage)
        )
    }
    
    /**
     * Selects the optimal cards to absorb the given damage amount.
     * Uses a simple approach: try to find the minimum number of cards needed.
     * First tries 1 card, then 2 cards, then 3 cards, etc. until damage is covered.
     */
    private fun selectOptimalCards(availableCards: List<GameCard>, damageToAbsorb: Int): List<GameCard> {
        if (damageToAbsorb <= 0 || availableCards.isEmpty()) {
            return emptyList()
        }
        
        val sortedCards = availableCards.sortedBy { it.resilience }
        
        // Try to find the minimum number of cards needed
        for (numCards in 1..sortedCards.size) {
            val combinations = generateCombinations(sortedCards, numCards)
            for (combination in combinations) {
                val totalResilience = combination.sumOf { it.resilience }
                if (totalResilience >= damageToAbsorb) {
                    return combination
                }
            }
        }
        
        // If no combination can cover the damage, return all cards
        return sortedCards
    }
    
    /**
     * Generates all combinations of the specified size from the given cards.
     */
    private fun generateCombinations(cards: List<GameCard>, size: Int): List<List<GameCard>> {
        if (size == 0) return listOf(emptyList())
        if (size > cards.size) return emptyList()
        if (size == cards.size) return listOf(cards)
        
        val result = mutableListOf<List<GameCard>>()
        
        fun generate(current: List<GameCard>, remaining: List<GameCard>, targetSize: Int) {
            if (current.size == targetSize) {
                result.add(current.toList())
                return
            }
            
            for (i in remaining.indices) {
                generate(
                    current + remaining[i],
                    remaining.drop(i + 1),
                    targetSize
                )
            }
        }
        
        generate(emptyList(), cards, size)
        return result
    }

} 
