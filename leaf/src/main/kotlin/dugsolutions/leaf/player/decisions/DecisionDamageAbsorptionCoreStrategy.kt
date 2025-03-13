package dugsolutions.leaf.player.decisions

import dugsolutions.leaf.cards.CardManager
import dugsolutions.leaf.components.CardID
import dugsolutions.leaf.components.die.Die
import dugsolutions.leaf.components.FlourishType
import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.components.HandItem
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.decisions.DecisionDamageAbsorption.*

/**
 * Strategy for determining which cards and dice to sacrifice to absorb incoming damage.
 * 
 * Algorithm:
 * 1. Generate all possible combinations of cards and dice in hand that can absorb the damage
 * 2. Score each valid combination based on:
 *    - Waste: (total absorption - damage) * 1000
 *    - Item count: number of items used * 100
 *    - Bloom cards: number of bloom cards used * 2000
 *    - Die size: sum of die sides * 10
 * 3. Select the combination with the lowest score
 * 
 * Special Rules:
 * - If all cards/dice are in hand (none in compost/supply):
 *   * Must preserve at least one card/die
 *   * Preserves the highest resilience card or highest sides die
 * - If no valid combinations exist:
 *   * Uses everything in hand except one preserved item
 *   * Returns null if nothing to sacrifice
 * 
 * Scoring Priorities (lower is better):
 * 1. Minimize waste (excess absorption)
 * 2. Minimize number of items used
 * 3. Avoid using BLOOM cards
 * 4. Prefer smaller dice over larger ones
 * 
 * Example:
 * For 6 damage with options:
 * - d6 (6) = score 60 (no waste, 1 item)
 * - d4 (4) + seedling (2) = score 160 (no waste, 2 items)
 * - vine (4) + d4 (4) = score 1080 (2 waste, 2 items)
 * Algorithm would choose d6 as it has the lowest score.
 */
class DecisionDamageAbsorptionCoreStrategy(
    private val player: Player,
    val cardManager: CardManager
) : DecisionDamageAbsorption {

    private data class ItemCombination(
        val items: List<HandItem> = emptyList(),
        val total: Int = 0,
        val overallScore: Int = 0 // Lower is better
    )

    override operator fun invoke(): Result? {
        if (player.incomingDamage <= 0) {
            return null
        }
        val amount = player.incomingDamage
        val handItems = player.getItemsInHand()
        
        // Check if cards/dice exist only in hand (none in compost or supply)
        val onlyCardsInHand = player.cardsInCompost.isEmpty() && player.cardsInSupply.isEmpty()
        val onlyDiceInHand = player.diceInCompost.isEmpty() && player.diceInSupply.isEmpty()
        
        // Generate all valid combinations that can absorb damage
        val validCombinations = generateCombinations(handItems, amount)
            .filter { combination ->
                // Filter out combinations that would remove all cards if we only have cards in hand
                if (onlyCardsInHand) {
                    val cardItemsInCombo = combination.items.filterIsInstance<HandItem.Card>()
                    val handCardItems = handItems.filterIsInstance<HandItem.Card>() 
                    cardItemsInCombo.size < handCardItems.size
                } else if (onlyDiceInHand) {
                    // Filter out combinations that would remove all dice if we only have dice in hand
                    val diceItemsInCombo = combination.items.filterIsInstance<HandItem.Dice>()
                    val handDiceItems = handItems.filterIsInstance<HandItem.Dice>()
                    diceItemsInCombo.size < handDiceItems.size
                } else {
                    // No restrictions if we have cards/dice elsewhere
                    true
                }
            }
        
        if (validCombinations.isEmpty()) {
            // If no valid combinations, must sacrifice everything in hand except one item if needed
            if (onlyCardsInHand && handItems.any { it is HandItem.Card }) {
                // Keep one card if that's all we have
                val cards = handItems.filterIsInstance<HandItem.Card>()
                val dice = handItems.filterIsInstance<HandItem.Dice>()

                // Only have one card left so remove it
                if (cards.size == 1) {
                    return Result(
                        cardIds = cards.map { it.card.id },
                        dice = dice.map { it.die }
                    )
                }
                // Keep the most valuable card (highest resilience)
                val cardToKeep = cards.maxByOrNull { it.card.resilience }
                val cardsToRemove = if (cardToKeep != null) cards - cardToKeep else cards

                return Result(
                    cardIds = cardsToRemove.map { it.card.id },
                    dice = dice.map { it.die }
                )
            } else if (onlyDiceInHand && handItems.any { it is HandItem.Dice }) {
                // Keep one die if that's all we have
                val cards = handItems.filterIsInstance<HandItem.Card>()
                val dice = handItems.filterIsInstance<HandItem.Dice>()

                // Only have one die left, so remove it
                if (dice.size == 1) {
                    return Result(
                        cardIds = cards.map { it.card.id },
                        dice = dice.map { it.die }
                    )
                }
                // Keep the most valuable die (highest sides)
                val dieToKeep = dice.maxByOrNull { it.die.sides }
                val diceToRemove = if (dieToKeep != null) dice - dieToKeep else dice
                
                return Result(
                    cardIds = cards.map { it.card.id },
                    dice = diceToRemove.map { it.die }
                )
            } else {
                // No special preservation needed, remove everything
                val cards = handItems.filterIsInstance<HandItem.Card>()
                val dice = handItems.filterIsInstance<HandItem.Dice>()
                
                // If nothing to remove, return null
                if (cards.isEmpty() && dice.isEmpty()) {
                    return null
                }
                return Result(
                    cardIds = cards.map { it.card.id },
                    dice = dice.map { it.die }
                )
            }
        }
        
        // Select the best combination based on scoring
        val bestCombination = validCombinations.minByOrNull { it.overallScore }
            ?: return null
        
        // Convert the best combination back to cards and dice
        val selectedCards = bestCombination.items.filterIsInstance<HandItem.Card>()
        val selectedDice = bestCombination.items.filterIsInstance<HandItem.Dice>()
            
        return Result(
            cardIds = selectedCards.map { it.card.id },
            dice = selectedDice.map { it.die }
        )
    }
    
    private fun generateCombinations(
        items: List<HandItem>,
        targetDamage: Int
    ): List<ItemCombination> {
        val results = mutableListOf<ItemCombination>()
        val allCombinations = generateItemCombinations(items)
        
        for (combo in allCombinations) {
            if (combo.isEmpty()) continue
            
            // Calculate the total resilience/sides of this combination
            val totalValue = combo.sumOf { 
                when (it) {
                    is HandItem.Card -> it.card.resilience
                    is HandItem.Dice -> it.die.sides
                }
            }
            
            // Only consider combinations that meet or exceed the damage
            if (totalValue >= targetDamage) {
                // Score this combination (lower is better)
                val overallScore = scoreItemCombination(combo, totalValue, targetDamage)
                
                results.add(ItemCombination(
                    items = combo,
                    total = totalValue,
                    overallScore = overallScore
                ))
            }
        }
        
        return results
    }
    
    private fun generateItemCombinations(items: List<HandItem>): List<List<HandItem>> {
        val result = mutableListOf<List<HandItem>>()
        
        // Empty combination
        result.add(emptyList())
        
        // Generate all possible item combinations (power set)
        for (i in items.indices) {
            val currentItem = items[i]
            val newCombinations = mutableListOf<List<HandItem>>()
            
            for (combo in result) {
                newCombinations.add(combo + currentItem)
            }
            
            result.addAll(newCombinations)
        }
        
        // Filter out empty list
        return result.filter { it.isNotEmpty() }
    }
    
    private fun scoreItemCombination(
        items: List<HandItem>,
        total: Int,
        targetDamage: Int
    ): Int {
        // Lower score is better
        var score = 0
        
        // Extract cards and dice for scoring
        val cards = items.filterIsInstance<HandItem.Card>()
        val dice = items.filterIsInstance<HandItem.Dice>()
        
        // Penalize for excessive damage absorption (waste)
        score += (total - targetDamage) * 1000
        
        // Penalize for number of items used
        score += items.size * 100
        
        // Prefer not using BLOOM cards if possible
        score += cards.count { it.card.type == FlourishType.BLOOM } * 2000
        
        // Prefer using smaller dice over larger ones (less waste)
        score += dice.sumOf { it.die.sides * 10 }
        
        return score
    }
} 
