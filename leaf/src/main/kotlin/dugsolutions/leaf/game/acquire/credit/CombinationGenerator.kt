package dugsolutions.leaf.game.acquire.credit

import dugsolutions.leaf.components.die.DieValues
import dugsolutions.leaf.game.acquire.domain.Adjusted
import dugsolutions.leaf.game.acquire.domain.Combination
import dugsolutions.leaf.game.acquire.domain.Combinations
import dugsolutions.leaf.player.Player

class CombinationGenerator(
    private val effectToCredits: EffectToCredits
) {

    operator fun invoke(player: Player): Combinations {
        val credits = effectToCredits(player)
        val dieList = credits.dieList
        val addToTotal = credits.addToTotal
        val adjustList = credits.adjustList
        val numSetToMax = credits.numSetToMax
        val dieValues = DieValues.from(dieList)
        val combinations = mutableListOf<Combination>()
        
        // Add the base combination with no adjustments
        combinations.add(Combination(dieValues, addToTotal))
        
        // Generate all combinations with dice adjustments
        generateAdjustmentCombinations(dieValues, adjustList, addToTotal, combinations)
        
        // Handle setToMax adjustments
        if (numSetToMax > 0) {
            generateSetToMaxCombinations(dieValues, numSetToMax, addToTotal, combinations)
        }
        return Combinations(combinations)
    }
    
    private fun generateAdjustmentCombinations(
        dieValues: DieValues,
        adjustList: List<Int>,
        addToTotal: Int,
        combinations: MutableList<Combination>
    ) {
        // If no adjustments or no dice, return
        if (adjustList.isEmpty() || dieValues.dice.isEmpty()) {
            return
        }
        
        // For each die, try each adjustment
        for (dieIndex in dieValues.dice.indices) {
            for (adjustment in adjustList) {
                // Create a new copy of the dice values
                val newDieValues = dieValues.copy
                
                // Apply the adjustment to the specific die
                val adjustedDie = newDieValues.dice[dieIndex]
                adjustedDie.adjustBy(adjustment)
                
                // Track the adjustment
                val adjusted = listOf(Adjusted.ByAmount(adjustedDie, adjustment))
                
                // Add this combination
                combinations.add(Combination(newDieValues, addToTotal, adjusted))
                
                // If there are multiple adjustments, recursively apply remaining adjustments
                if (adjustList.size > 1) {
                    val remainingAdjustments = adjustList.filter { it != adjustment }
                    // For each additional die, try remaining adjustments
                    for (nextDieIndex in dieValues.dice.indices) {
                        if (nextDieIndex != dieIndex) {
                            applyAdditionalAdjustments(
                                newDieValues.copy, 
                                remainingAdjustments, 
                                addToTotal, 
                                adjusted.toMutableList(), 
                                nextDieIndex, 
                                combinations
                            )
                        }
                    }
                }
            }
        }
    }
    
    private fun applyAdditionalAdjustments(
        dieValues: DieValues,
        adjustments: List<Int>,
        addToTotal: Int,
        currentAdjustments: MutableList<Adjusted>,
        startDieIndex: Int,
        combinations: MutableList<Combination>
    ) {
        for (dieIndex in startDieIndex until dieValues.dice.size) {
            for (adjustment in adjustments) {
                // Create a copy with the new adjustment
                val newDieValues = dieValues.copy
                val adjustedDie = newDieValues.dice[dieIndex]
                adjustedDie.adjustBy(adjustment)
                
                // Add to the tracked adjustments
                val newAdjustments = currentAdjustments.toMutableList()
                newAdjustments.add(Adjusted.ByAmount(adjustedDie, adjustment))
                
                // Add this combination
                combinations.add(Combination(newDieValues, addToTotal, newAdjustments))
            }
        }
    }
    
    private fun generateSetToMaxCombinations(
        dieValues: DieValues,
        numSetToMax: Int,
        addToTotal: Int,
        combinations: MutableList<Combination>
    ) {
        // Handle single setToMax
        for (dieIndex in dieValues.dice.indices) {
            val newDieValues = dieValues.copy
            val adjustedDie = newDieValues.dice[dieIndex]
            adjustedDie.adjustToMax()
            
            val adjusted = listOf(Adjusted.ToMax(adjustedDie))
            combinations.add(Combination(newDieValues, addToTotal, adjusted))
        }
        
        // If multiple setToMax are available, handle combinations
        if (numSetToMax > 1) {
            // Generate combinations of dice indices to set to max
            for (i in dieValues.dice.indices) {
                for (j in i+1 until dieValues.dice.size) {
                    val newDieValues = dieValues.copy
                    
                    val adjustedDie1 = newDieValues.dice[i]
                    adjustedDie1.adjustToMax()
                    
                    val adjustedDie2 = newDieValues.dice[j]
                    adjustedDie2.adjustToMax()
                    
                    val adjusted = listOf(
                        Adjusted.ToMax(adjustedDie1),
                        Adjusted.ToMax(adjustedDie2)
                    )
                    combinations.add(Combination(newDieValues, addToTotal, adjusted))
                }
            }
        }
    }
}
