package dugsolutions.leaf.game.battle

import dugsolutions.leaf.game.battle.domain.DieBoosted
import dugsolutions.leaf.game.battle.domain.PlayerValues
import dugsolutions.leaf.game.battle.domain.fill
import dugsolutions.leaf.game.battle.domain.sort
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.random.di.DieFactory
import dugsolutions.leaf.random.die.Die

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

        // Process each index (battle round)
        for (index in 0 until maxLength) {
            resolveBattleAtIndex(normalizedGrid, index)
        }
    }

    private data class PlayerValue(
        val player: Player,
        val dieValue: DieBoosted
    )

    /**
     * Resolves battle at a specific index across all players.
     * Assumes all players have been normalized (sorted and filled to same length).
     */
    private fun resolveBattleAtIndex(grid: List<PlayerValues>, index: Int) {
        // Get DieBoosted for each player at this index (all players have same length now)
        val battleParticipants = grid.map { playerValues ->
            Pair(playerValues, playerValues.values[index])
        }

        // Filter out empty participants (zero sides or zero value)
        val validParticipants = battleParticipants.filter { (_, dieBoosted) ->
            dieBoosted.dieValue.sides > 0 && dieBoosted.dieValue.value > 0
        }

        // If no valid participants, nothing to resolve
        if (validParticipants.isEmpty()) return

        // Sort by attack value (lowest to highest)
        val sortedParticipants = validParticipants.sortedBy { (_, dieBoosted) -> dieBoosted.attack }

        // Find the lowest attack value
        val lowestAttack = sortedParticipants.first().second.attack

        // Find all participants with the lowest attack (losers)
        val losers = sortedParticipants.filter { (_, dieBoosted) -> dieBoosted.attack == lowestAttack }

        // Find all participants with higher attack (winners)
        val winners = sortedParticipants.filter { (_, dieBoosted) -> dieBoosted.attack > lowestAttack }

        // If there are no winners (all tied at lowest), nothing happens
        if (winners.isEmpty()) return

        // Resolve each loser against the winners
        for ((loserPlayerValues, loserDieBoosted) in losers) {
            // Find the highest attack value among winners (or any winner if tied)
            val winnerAttack = winners.maxOf { (_, dieBoosted) -> dieBoosted.attack }
            val winnerParticipants = winners.filter { (_, dieBoosted) -> dieBoosted.attack == winnerAttack }

            // Resolve against each winner (in case of ties)
            for ((winnerPlayerValues, winnerDieBoosted) in winnerParticipants) {
                resolvePairing(
                    winnerPlayerValues,
                    winnerDieBoosted,
                    loserPlayerValues,
                    loserDieBoosted
                )
            }
        }
    }

    /**
     * Resolves a single pairing between a winner and a loser.
     */
    private fun resolvePairing(
        winnerPlayerValues: PlayerValues,
        winnerDieBoosted: DieBoosted,
        loserPlayerValues: PlayerValues,
        loserDieBoosted: DieBoosted
    ) {
        val winnerPlayer = winnerPlayerValues.player
        val loserPlayer = loserPlayerValues.player
        val winnerAttack = winnerDieBoosted.attack
        val loserDieValue = loserDieBoosted.dieValue

        // Check if loser had a valid die (not empty placeholder)
        val loserHadValidDie = loserDieValue.sides > 0 && loserDieValue.value > 0

        if (loserHadValidDie) {
            // Find the actual Die in both players' hands that match the DieValues
            val winnerDie = findMatchingDie(winnerPlayer, winnerDieBoosted.dieValue)
            val loserDie = findMatchingDie(loserPlayer, loserDieValue)
            
            if (winnerDie != null && loserDie != null) {
                // Both players remove the die from their hand
                winnerPlayer.removeDieFromHand(winnerDie)
                loserPlayer.removeDieFromHand(loserDie)

                // Winner adds their die to discard
                winnerPlayer.addDieToDiscard(winnerDie)

                // Loser's die is either trashed or downgraded
                if (winnerAttack >= loserDie.sides) {
                    // Die is trashed - nothing added to discard
                } else if (winnerAttack > loserDieValue.value) {
                    // Die is downgraded (attack > value but < sides)
                    val downgradedDie = downgrade(loserDie)
                    if (downgradedDie != null) {
                        loserPlayer.addDieToDiscard(downgradedDie)
                    }
                }
                // If winnerAttack <= loserDieValue.value, nothing happens to the loser's die
            }
        } else {
            // Loser had no die (empty placeholder) - they take full damage
            // Find the winner's Die and discard it
            val winnerDie = findMatchingDie(winnerPlayer, winnerDieBoosted.dieValue)
            if (winnerDie != null) {
                winnerPlayer.discard(winnerDie)
                // Winner adds their die to discard
                winnerPlayer.addDieToDiscard(winnerDie)
            }
            
            // Loser takes damage
            loserPlayer.incomingDamage += winnerAttack
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

    private fun downgrade(die: Die): Die? {
        return die.dieSides.downgrade?.let { dieFactory(it) }
    }

}
