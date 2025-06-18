package dugsolutions.leaf.player.decisions.baseline

import dugsolutions.leaf.cards.CardManager
import dugsolutions.leaf.player.di.CardEffectBattleScoreFactory
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.decisions.core.DecisionDamageAbsorption
import dugsolutions.leaf.player.decisions.core.DecisionDamageAbsorption.*
import dugsolutions.leaf.player.domain.ExtendedHandItem
import dugsolutions.leaf.random.die.DieSides

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
class DecisionDamageAbsorptionBaseline(
    private val player: Player,
    cardEffectBattleScoreFactory: CardEffectBattleScoreFactory,
    val cardManager: CardManager
) : DecisionDamageAbsorption {

    companion object {
        private const val BALANCE_TRIGGER = 3
    }

    private data class ItemCombination(
        val items: List<ExtendedHandItem> = emptyList(),
        val total: Int = 0,
        val overallScore: Int = 0 // Lower is better
    ) {
        val hasOnlyFlowerCards: Boolean
            get() = items.any { it is ExtendedHandItem.FloralArray } &&
                    items.none { it is ExtendedHandItem.Card }

        val hasOnlyDice: Boolean
            get() = items.all { it is ExtendedHandItem.Dice }

        val hasD20: Boolean
            get() = items.any { it is ExtendedHandItem.Dice && it.die.sides == DieSides.D20.value }

        val hasOnlyCards: Boolean
            get() = items.all { it is ExtendedHandItem.Card || it is ExtendedHandItem.FloralArray }
    }

    private val cardEffectBattleScore = cardEffectBattleScoreFactory(player)

    override suspend operator fun invoke(): Result {
        val amount = player.incomingDamage
        if (amount <= 0) {
            return Result()
        }
        val handItems = player.getExtendedHandItems()

        // Check if cards/dice exist only in hand (none in compost/supply)
        val onlyCardsInHand = player.cardsInBed.isEmpty() && player.cardsInSupply.isEmpty()
        val onlyDiceInHand = player.diceInBed.isEmpty() && player.diceInSupply.isEmpty()

        // Generate all valid combinations that can absorb damage
        val allCombinations = generateCombinations(handItems, amount)
        val validCombinations = allCombinations.filter { combination ->
            // Filter out combinations where the card is only the Flower card -- as this is not a legal play
            if (combination.hasOnlyFlowerCards) {
                false
                // Filter out combinations that would remove all cards if we only have cards in hand
            } else if (onlyCardsInHand) {
                val cardItemsInCombo = combination.items.filterIsInstance<ExtendedHandItem.Card>()
                val handCardItems = handItems.filterIsInstance<ExtendedHandItem.Card>()
                cardItemsInCombo.size < handCardItems.size
                // Filter out combinations that would remove all dice if we only have dice in hand
            } else if (onlyDiceInHand) {
                val diceItemsInCombo = combination.items.filterIsInstance<ExtendedHandItem.Dice>()
                val handDiceItems = handItems.filterIsInstance<ExtendedHandItem.Dice>()
                diceItemsInCombo.size < handDiceItems.size
            } else {
                // No restrictions if we have cards/dice elsewhere
                true
            }
        }

        // There is no combination we like -- so we need to get rid of everything.
        if (validCombinations.isEmpty()) {
            // If no valid combinations, must sacrifice everything in hand except one item if needed
            if (onlyCardsInHand && handItems.any { it is ExtendedHandItem.Card }) {
                // Keep one card if that's all we have
                val cards = handItems.filterIsInstance<ExtendedHandItem.Card>()
                val floralCards = handItems.filterIsInstance<ExtendedHandItem.FloralArray>()
                val dice = handItems.filterIsInstance<ExtendedHandItem.Dice>()

                // Only have one card left so remove it
                if (cards.size == 1) {
                    return Result(
                        cards = cards.map { it.card },
                        dice = dice.map { it.die },
                        floralCards = floralCards.map { it.card }
                    )
                }
                // Keep the most valuable card (highest resilience)
                val cardToKeep = cards.maxByOrNull { it.card.resilience }
                val cardsToRemove = if (cardToKeep != null) cards - cardToKeep else cards

                return Result(
                    cards = cardsToRemove.map { it.card },
                    dice = dice.map { it.die },
                    floralCards = floralCards.map { it.card }
                )
            } else if (onlyDiceInHand && handItems.any { it is ExtendedHandItem.Dice }) {
                // Keep one die if that's all we have
                val cards = handItems.filterIsInstance<ExtendedHandItem.Card>()
                val floralCards = handItems.filterIsInstance<ExtendedHandItem.FloralArray>()
                val dice = handItems.filterIsInstance<ExtendedHandItem.Dice>()

                // Only have one die left, so remove it
                if (dice.size == 1) {
                    return Result(
                        cards = cards.map { it.card },
                        dice = dice.map { it.die },
                        floralCards = floralCards.map { it.card }
                    )
                }
                // Keep the most valuable die (highest sides)
                val dieToKeep = dice.maxByOrNull { it.die.sides }
                val diceToRemove = if (dieToKeep != null) dice - dieToKeep else dice

                return Result(
                    cards = cards.map { it.card },
                    dice = diceToRemove.map { it.die },
                    floralCards = floralCards.map { it.card }
                )
            } else {
                // No special preservation needed, remove everything
                val cards = handItems.filterIsInstance<ExtendedHandItem.Card>()
                val dice = handItems.filterIsInstance<ExtendedHandItem.Dice>()
                val floralCards = handItems.filterIsInstance<ExtendedHandItem.FloralArray>()

                return Result(
                    cards = cards.map { it.card },
                    dice = dice.map { it.die },
                    floralCards = floralCards.map { it.card }
                )
            }
        }
        val bestCombination = selectBestCombination(player, validCombinations) ?: return Result()

        // Convert the best combination back to cards and dice
        val selectedCards = bestCombination.items.filterIsInstance<ExtendedHandItem.Card>()
        val selectedDice = bestCombination.items.filterIsInstance<ExtendedHandItem.Dice>()
        val floralCards = bestCombination.items.filterIsInstance<ExtendedHandItem.FloralArray>()

        return Result(
            cards = selectedCards.map { it.card },
            dice = selectedDice.map { it.die },
            floralCards = floralCards.map { it.card }
        )
    }

    private fun generateCombinations(
        items: List<ExtendedHandItem>,
        targetDamage: Int
    ): List<ItemCombination> {
        val results = mutableListOf<ItemCombination>()
        val allCombinations = generateItemCombinations(items)

        for (combo in allCombinations) {
            if (combo.isEmpty()) continue

            // Calculate the total resilience/sides of this combination
            val totalValue = combo.sumOf {
                when (it) {
                    is ExtendedHandItem.Card -> it.card.resilience
                    is ExtendedHandItem.Dice -> it.die.sides
                    is ExtendedHandItem.FloralArray -> it.card.resilience
                }
            }

            // Only consider combinations that meet or exceed the damage
            if (totalValue >= targetDamage) {
                // Score this combination (lower is more likely to be used)
                val waste = totalValue - targetDamage
                val itemScore = scoreItemCombination(combo)
                val overallScore = itemScore + waste // Only slight consideration of waste value

                results.add(
                    ItemCombination(
                        items = combo,
                        total = totalValue,
                        overallScore = overallScore
                    )
                )
            }
        }

        return results
    }

    private fun generateItemCombinations(items: List<ExtendedHandItem>): List<List<ExtendedHandItem>> {
        val result = mutableListOf<List<ExtendedHandItem>>()

        // Empty combination
        result.add(emptyList())

        // Generate all possible item combinations (power set)
        for (i in items.indices) {
            val currentItem = items[i]
            val newCombinations = mutableListOf<List<ExtendedHandItem>>()

            for (combo in result) {
                newCombinations.add(combo + currentItem)
            }
            result.addAll(newCombinations)
        }

        // Filter out empty list
        return result.filter { it.isNotEmpty() }
    }

    private fun scoreItemCombination(
        items: List<ExtendedHandItem>
    ): Int {
        // Lower score means more likely to be used to absorb damage.
        var score = 0

        // Extract cards and dice for scoring
        val cards = items.filterIsInstance<ExtendedHandItem.Card>()
        val dice = items.filterIsInstance<ExtendedHandItem.Dice>()

        score += dice.sumOf { it.die.sides }
        score += cards.sumOf { cardEffectBattleScore(it.card) }

        return score
    }

    private fun selectBestCombination(player: Player, combinations: List<ItemCombination>): ItemCombination? {
        val balanceDice = player.allDice.size - player.allCardsInDeck.size

        // If we have many more dice than cards, then select the best combination with dice
        if (balanceDice > BALANCE_TRIGGER) {
            // Select best dice combination
            combinations
                .filter { it.hasOnlyDice }
                .filter { !it.hasD20 }
                .minByOrNull { it.overallScore }?.let {
                    return it
                }
        } else if (balanceDice < -BALANCE_TRIGGER) {
            // Select best card combination
            combinations
                .filter { it.hasOnlyCards }
                .minByOrNull { it.overallScore }?.let {
                    return it
                }
        }
        if (player.allDice.size <= 2) {
            // If we have almost no dice left, then choose cards.
            // Select best card combination
            combinations
                .filter { it.hasOnlyCards }
                .minByOrNull { it.overallScore }?.let {
                    return it
                }
        } else if (player.allCardsInDeck.size <= 2) {
            // If we have almost no cards left, then choose dice.
            combinations
                .filter { it.hasOnlyDice }
                .minByOrNull { it.overallScore }?.let {
                    return it
                }
        } else if (player.allDice.size <= 1) {
            // If we have one dice left, then choose cards.
            // Select best card combination
            combinations
                .filter { it.hasOnlyCards }
                .minByOrNull { it.overallScore }?.let {
                    return it
                }
        } else if (player.allCardsInDeck.size <= 1) {
            // If we have one card left, then choose dice.
            combinations
                .filter { it.hasOnlyDice }
                .minByOrNull { it.overallScore }?.let {
                    return it
                }
        }
        // Select the best combination based on scoring
        return combinations.minByOrNull { it.overallScore }

    }

} 
