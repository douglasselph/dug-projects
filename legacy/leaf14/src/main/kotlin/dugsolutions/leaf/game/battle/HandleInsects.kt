package dugsolutions.leaf.game.battle

import dugsolutions.leaf.game.battle.domain.DieBoosted
import dugsolutions.leaf.game.battle.domain.PlayerValues
import dugsolutions.leaf.game.battle.domain.fill
import dugsolutions.leaf.game.battle.domain.sort
import dugsolutions.leaf.player.decisions.core.DecisionBattleInsect

class HandleInsects {

    suspend operator fun invoke(grid: List<PlayerValues>): List<PlayerValues> {
        if (grid.isEmpty()) return grid

        // Normalize the grid: sort and fill all PlayerValues to the same length
        val maxLength = grid.maxOfOrNull { it.values.size } ?: 0
        val normalizedGrid = grid.map { it.sort().fill(maxLength) }.toMutableList()

        // Process each player in order, updating the grid as we go
        for (playerIndex in normalizedGrid.indices) {
            val updatedPlayerValues = evaluateBoostings(normalizedGrid, playerIndex)
            normalizedGrid[playerIndex] = updatedPlayerValues
        }

        return normalizedGrid
    }

    /**
     * Evaluates insect boostings for a single player.
     * Processes each value index, determining attackers and calling battleInsect decision.
     * Returns updated PlayerValues with boosted dice.
     */
    private suspend fun evaluateBoostings(
        grid: List<PlayerValues>,
        playerIndex: Int
    ): PlayerValues {
        val playerValues = grid[playerIndex]
        val player = playerValues.player
        val updatedValues = playerValues.values.mapIndexed { valueIndex, dieBoosted ->
            // Find the attacker (player with die value just above in the order)
            findAttacker(grid, playerIndex, valueIndex) ?: return@mapIndexed dieBoosted // If no attacker, no need to boost

            // Morph entire grid to DecisionBattleInsect format (all values for all players)
            val battleLines = morphGridToBattleLines(grid)
            
            // Call decision director
            val result = player.decisionDirector.battleInsect(battleLines)
            
            // Find the adjustment for this specific die
            val adjustment = result.adjustments.find { adjustment ->
                adjustment.die.sides == dieBoosted.dieValue.sides &&
                adjustment.die.value == dieBoosted.dieValue.value
            }
            
            // Update DieBoosted with insects if adjustment found
            if (adjustment != null && adjustment.insects.isNotEmpty()) {
                DieBoosted(dieBoosted.dieValue, adjustment.insects)
            } else {
                dieBoosted
            }
        }
        return PlayerValues(player, updatedValues)
    }

    /**
     * Finds the attacker for a specific player at a specific value index.
     * The attacker is the player with the die value just above in the order (next higher attack).
     */
    private fun findAttacker(
        grid: List<PlayerValues>,
        playerIndex: Int,
        valueIndex: Int
    ): PlayerValues? {
        val currentPlayerValues = grid[playerIndex]
        
        // Check if current player has a valid die at this index
        if (valueIndex >= currentPlayerValues.values.size ||
            currentPlayerValues.values[valueIndex].dieValue.sides == 0 ||
            currentPlayerValues.values[valueIndex].dieValue.value == 0) {
            return null
        }
        val currentAttack = currentPlayerValues.values[valueIndex].attack
        
        // Find all players with valid dice and higher attack at this index
        val potentialAttackers = grid.filterIndexed { index, otherPlayerValues ->
            // Don't include self
            index != playerIndex &&
            // Only include players with valid dice at this index
            valueIndex < otherPlayerValues.values.size &&
            otherPlayerValues.values[valueIndex].dieValue.sides > 0 &&
            otherPlayerValues.values[valueIndex].dieValue.value > 0 &&
            // Only include players with higher attack
            otherPlayerValues.values[valueIndex].attack > currentAttack
        }
        // Return the one with the lowest attack (just above current)
        return potentialAttackers.minByOrNull { it.values[valueIndex].attack }
    }

    /**
     * Morphs the entire grid to DecisionBattleInsect.BattleLine format.
     * Creates a list of BattleLines where each BattleLine represents one player's complete line of dice.
     */
    private fun morphGridToBattleLines(
        grid: List<PlayerValues>
    ): List<DecisionBattleInsect.BattleLine> {
        return grid.map { playerValues ->
            // Extract all DieValues for this player (convert DieBoosted to DieValue)
            val dieValues = playerValues.values.map { it.dieValue }
            
            DecisionBattleInsect.BattleLine(
                player = playerValues.player,
                line = dieValues
            )
        }
    }

}
