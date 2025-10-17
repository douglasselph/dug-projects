package dugsolutions.leaf.game.turn.cost

import dugsolutions.leaf.components.Cost
import dugsolutions.leaf.components.CostElement
import dugsolutions.leaf.components.FlourishType
import dugsolutions.leaf.game.market.domain.Credit
import dugsolutions.leaf.game.market.domain.Credits
import dugsolutions.leaf.player.Player

class CoverCost {

    /**
     * Will check for existing dice values.
     * For adjust die considerations, will only apply this toward exact matches.
     * Everything else is ignored in favor of higher level adjustments.
     */
    operator fun invoke(player: Player, cost: Cost, credits: Credits): Credits {
        // Empty cost is always satisfied
        if (cost.elements.isEmpty()) {
            return Credits()
        }

        // Check for flourish type requirements first (early exit)
        val flourishTypeElements = cost.elements.filterIsInstance<CostElement.FlourishTypePresent>()
        for (element in flourishTypeElements) {
            if (!hasFlourishTypeCard(player, element.flourishType)) {
                return Credits() // Player doesn't have required card type
            }
        }

        // Check for exact die requirements (early exit)
        val exactDieElements = cost.elements.filterIsInstance<CostElement.SingleDieExact>()
        val creditsUsed = mutableListOf<Credit>()
        for (element in exactDieElements) {
            if (!hasExactMatch(player, element.exact)) {
                checkPossibleFor(credits) { value -> value == element.exact }?.let {
                    creditsUsed.add(it)
                } ?: run {
                    return Credits() // Player doesn't have required exact die value
                }
            }
        }

        // Now handle the remaining cost elements that involve combinations
        val remainingElements = cost.elements.filter {
            it !is CostElement.FlourishTypePresent && it !is CostElement.SingleDieExact
        }

        // If no remaining elements after simple checks, we just need to gather the dice used
        if (remainingElements.isEmpty()) {
            // If any die needed to be adjusted we return that as a type of "spent".
            // Otherwise any exact die is not "spent" at all but simply allows the purchase.
            return Credits(creditsUsed)
        }

        // Process remaining elements that require evaluating combinations
        return findBestCombination(remainingElements, credits)?.let { credits ->
            creditsUsed.addAll(credits)
            Credits(creditsUsed)
        } ?: run {
            Credits() // Not possible to do.
        }
    }

    /**
     * Look for the best combinations ignoring rerolls or die adjustments.
     */
    private fun findBestCombination(
        remainingElements: List<CostElement>,
        credits: Credits
    ): List<Credit>? {
        val usedCredits = mutableListOf<Credit>()

        // Find requirements for minimum single die values
        val singleDieMinElements =
            remainingElements.filterIsInstance<CostElement.SingleDieMinimum>()
        for (element in singleDieMinElements) {
            // Find the smallest die that meets the minimum
            val die = credits.list
                .filterIsInstance<Credit.CredDie>()
                .filter { it.die.value >= element.minimum }
                .minByOrNull { it.die.value }
            if (die != null) {
                usedCredits.add(die)
            } else {
                // Try and see if it is possible to adjust a die to this value
                checkPossibleFor(credits) { value -> value >= element.minimum }?.let {
                    usedCredits.add(it)
                } ?: run {
                    return null // Not possible to do then.
                }
            }
        }

        // Handle total minimum requirements
        val totalMinElements = remainingElements.filterIsInstance<CostElement.TotalDiceMinimum>()
        if (totalMinElements.isEmpty()) {
            return usedCredits
        }
        // Find the highest minimum total required
        val highestMinTotal = totalMinElements.maxOf { it.minimum }
        val currentTotal1 = usedCredits
            .filterIsInstance<Credit.CredDie>()
            .sumOf { it.die.value }
        val currentTotal2 = usedCredits
            .filterIsInstance<Credit.CredAddToTotal>()
            .sumOf { it.amount }
        val currentTotal = currentTotal1 + currentTotal2
        if (currentTotal >= highestMinTotal) {
            // Already satisfied the minimum total
            return usedCredits
        }

        // Need to find more dice to reach the minimum total
        val missingAmount = highestMinTotal - currentTotal

        // Acquire remaining credits
        val remainingCredits = credits.list.filter { it !in usedCredits }

        // Just check raw dice credits first -- if we can find enough to satisfy needed amount
        val sortedCredits = remainingCredits.sortedBy { valueOf(it) }

        // Try combinations of increasing size
        var located = listOf<Credit>()
        var bestTotal = 0

        for (size in 1..sortedCredits.size) {
            val combinations = generateCombinations(sortedCredits, size)
            for (combo in combinations) {
                val total = combo.sumOf { valueOf(it) }
                if (total >= missingAmount && (located.isEmpty() || total < bestTotal)) {
                    located = combo
                    bestTotal = total
                }
            }
            if (located.isNotEmpty()) {
                break // Found a combination that works
            }
        }
        if (located.isEmpty()) {
            return null // Couldn't reach the minimum
        }
        usedCredits.addAll(located)
        return usedCredits
    }

    private fun generateCombinations(credits: List<Credit>, k: Int): List<List<Credit>> {
        if (k == 0) return listOf(emptyList())
        if (credits.isEmpty()) return emptyList()

        val withFirst =
            generateCombinations(credits.drop(1), k - 1).map { listOf(credits.first()) + it }
        val withoutFirst = generateCombinations(credits.drop(1), k)

        return withFirst + withoutFirst
    }

    private fun hasFlourishTypeCard(player: Player, type: FlourishType): Boolean {
        // Check if player has a card of the specified flourish type in the player's hand.
        return player.cardsInHand.any { it.type == type }
    }

    private fun valueOf(credit: Credit): Int {
        return if (credit is Credit.CredDie) {
            credit.die.value
        } else if (credit is Credit.CredAddToTotal) {
            credit.amount
        } else {
            0
        }
    }

    /**
     * Return true if is possible to adjust a die such that the needed value can be acquired.
     */
    private fun checkPossibleFor(
        credits: Credits,
        check: (value: Int) -> Boolean
    ): Credit.CredDieAdjusted? {
        val adjustments = mutableListOf<Int>()
        for (credit in credits.list) {
            if (credit is Credit.CredAdjustDie) {
                adjustments.add(credit.value)
            }
        }
        if (adjustments.isEmpty()) {
            return null
        }
        for (credit in credits.list) {
            if (credit is Credit.CredDie) {
                for (adjustment in adjustments) {
                    if (check(credit.die.value + adjustment)) {
                        return Credit.CredDieAdjusted(credit.die, adjustment)
                    }
                }
            }
        }
        return null
    }

    /**
     * Return true if there is a die in the credit stream with the exact die value needed.
     */
    private fun hasExactMatch(player: Player, valueRequired: Int): Boolean {
        return player.diceInHand.dice.any { it.value == valueRequired }
    }

} 
