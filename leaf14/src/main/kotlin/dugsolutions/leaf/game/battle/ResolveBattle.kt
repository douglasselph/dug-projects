package dugsolutions.leaf.game.battle

import dugsolutions.leaf.game.battle.domain.BattleLine
import dugsolutions.leaf.game.battle.domain.PlayerValue
import dugsolutions.leaf.game.battle.domain.PlayerValues
import dugsolutions.leaf.game.battle.domain.fill
import dugsolutions.leaf.game.battle.domain.sort
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.random.di.DieFactory
import dugsolutions.leaf.random.die.Die
import dugsolutions.leaf.random.die.DieSides

class ResolveBattle(
    private val dieFactory: DieFactory
) {

    /**
     * With all the lines finalized, resolve all pairings.
     * Each index in the values list represents a battle round.
     * Players battle at each index, with lowest attack losing to higher attacks.
     */
    operator fun invoke(grid: List<PlayerValues>) {
        if (grid.isEmpty()) return

        // Normalize the grid: sort and fill all PlayerValues to the same length
        val maxLength = grid.maxOfOrNull { it.values.size } ?: 0
        val normalizedGrid = grid.map { it.sort().fill(maxLength) }

        // Remove all committed dice from players' hands before battle resolution
        removeDiceFromHand(normalizedGrid)

        // Process each index (battle round)
        for (index in 0 until maxLength) {
            resolveBattleAtIndex(normalizedGrid, index)
        }
    }

    /**
     * Removes all dice committed to battle from players' hands.
     * This is done upfront before any battle resolution occurs.
     */
    private fun removeDiceFromHand(grid: List<PlayerValues>) {
        for (playerValues in grid) {
            for (dieBoosted in playerValues.values) {
                val dieValue = dieBoosted.dieValue
                // Only remove valid dice (not empty placeholders)
                if (dieValue.sides > 0 && dieValue.value > 0) {
                    val die = findMatchingDie(playerValues.player, dieValue)
                    if (die != null) {
                        playerValues.player.removeDieFromHand(die)
                    }
                }
            }
        }
    }

    /**
     * Resolves battle at a specific index across all players.
     * Assumes all players have been normalized (sorted and filled to same length).
     */
    private fun resolveBattleAtIndex(grid: List<PlayerValues>, index: Int) {
        // Extract PlayerValue for each player at this index
        val playerValues = grid.map { playerValues ->
            PlayerValue(playerValues.player, playerValues.values[index])
        }

        // Filter out empty participants (zero sides or zero value)
        val validPlayerValues = playerValues.filter { playerValue ->
            playerValue.dieValue.dieValue.sides > 0 && playerValue.dieValue.dieValue.value > 0
        }

        // If no valid participants, nothing to resolve
        if (validPlayerValues.isEmpty()) return

        // Build BattleLine and sort it
        val battleLine = BattleLine(validPlayerValues.toMutableList())
        battleLine.sort()

        // Get the first losers (lowest attack)
        var losers = battleLine.next() ?: return

        // If there are no more players (all tied at lowest), nothing happens
        var winners = battleLine.next() ?: return

        // Resolve pairings: losers vs winners, then winners become losers for next round
        while (losers.isNotEmpty() && winners.isNotEmpty()) {
            // Resolve each loser against each winner
            for (loser in losers) {
                for (winner in winners) {
                    resolvePairing(winner, loser)
                }
            }

            // Winners become losers for the next round
            losers = winners
            winners = battleLine.next() ?: break
        }

        // If there are remaining losers with no more winners, they just discard their dice
        for (remaining in losers) {
            resolvePairing(remaining)
        }
    }

    /**
     * Resolves a single pairing between a winner and a loser.
     */
    private fun resolvePairing(winner: PlayerValue, loser: PlayerValue) {
        val winnerPlayer = winner.player
        val loserPlayer = loser.player
        val winnerAttack = winner.attack
        val loserDieValue = loser.dieValue.dieValue
        val winnerDieValue = winner.dieValue.dieValue

        // Check if loser had a valid die (not empty placeholder)
        val loserHadValidDie = loserDieValue.sides > 0 && loserDieValue.value > 0

        if (loserHadValidDie) {
            // Create winner's die from DieSides and add to discard
            val winnerDieSides = DieSides.from(winnerDieValue.sides)
            val winnerDie = dieFactory(winnerDieSides).adjustTo(winnerDieValue.value)
            winnerPlayer.addDieToDiscard(winnerDie)

            // Loser's die is either trashed or downgraded
            if (winnerAttack >= loserDieValue.sides) {
                // Die is trashed - nothing added to discard
            } else if (winnerAttack > loserDieValue.value) {
                // Die is downgraded (attack > value but < sides)
                val loserDieSides = DieSides.from(loserDieValue.sides)
                val downgradedDieSides = loserDieSides.downgrade
                if (downgradedDieSides != null) {
                    val downgradedDie = dieFactory(downgradedDieSides).adjustTo(loserDieValue.value)
                    loserPlayer.addDieToDiscard(downgradedDie)
                }
            }
            // If winnerAttack <= loserDieValue.value, nothing happens to the loser's die
        } else {
            // Loser had no die (empty placeholder) - they take full damage
            // Create winner's die from DieSides and add to discard
            val winnerDieSides = DieSides.from(winnerDieValue.sides)
            val winnerDie = dieFactory(winnerDieSides).adjustTo(winnerDieValue.value)
            winnerPlayer.addDieToDiscard(winnerDie)
            
            // Loser takes damage
            loserPlayer.incomingDamage += winnerAttack
        }
    }

    /**
     * Resolves a single player value (when there are no more opponents).
     * Simply adds the die to discard (already removed from hand).
     */
    private fun resolvePairing(playerValue: PlayerValue) {
        val player = playerValue.player
        val dieValue = playerValue.dieValue.dieValue

        // Check if it's a valid die (not empty placeholder)
        if (dieValue.sides > 0 && dieValue.value > 0) {
            val dieSides = DieSides.from(dieValue.sides)
            val die = dieFactory(dieSides).adjustTo(dieValue.value)
            player.addDieToDiscard(die)
        }
    }

    /**
     * Finds the matching Die in the player's hand based on DieValue.
     */
    private fun findMatchingDie(player: Player, dieValue: dugsolutions.leaf.random.die.DieValue): Die? {
        return player.diceInHand.dice.firstOrNull { die ->
            die.sides == dieValue.sides && die.value == dieValue.value
        }
    }


}
