package dugsolutions.leaf.game.battle

import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.effect.AppliedEffect

class HandleDeliverDamage(
    private val chronicle: GameChronicle
) {

    operator fun invoke(players: List<Player>) {
        // Sort players by pipTotal in descending order, then by player index for ties
        val sortedPlayers = players
            .filter { !it.isDormant }
            .mapIndexed { index, player ->
                player to index
            }.sortedWith(
                compareByDescending<Pair<Player, Int>> { it.first.pipTotal }
                    .thenBy { it.second }
            ).map { it.first }

        // Group players by pipTotal to handle ties
        val playersByPipTotal = sortedPlayers.groupBy { it.pipTotal }
        val pipTotals = playersByPipTotal.keys.sortedDescending()

        // Process each pipTotal group
        for (i in 0 until pipTotals.size - 1) {
            val attackerPipTotal = pipTotals[i]
            val defenderPipTotal = pipTotals[i + 1]

            // Get the attackers (higher pip total)
            val attackers = playersByPipTotal[attackerPipTotal] ?: continue

            // Get the defenders (lower pip total)
            val defenders = playersByPipTotal[defenderPipTotal] ?: continue

            // Calculate damage to be delivered to each defender
            val damagePerDefender = attackerPipTotal - defenderPipTotal
            var totalThornDamage = 0

            // For each defender, apply damage from the attackers
            for (defender in defenders) {
                defender.incomingDamage += damagePerDefender
                totalThornDamage += defender.thornDamage
            }
            // TODO: Unit test
            for (defender in defenders) {
                defender.thornDamage = 0
            }
            // Apply thorn damage to each attacker
            for (attacker in attackers) {
                attacker.incomingDamage += totalThornDamage
            }
            // TODO: Immediately absorb the damage here so that thorn damage can be accumulated as well back onto the attacker.
            //  Note: this might mean the attacker is getting hit twice in games with 3+ players (the middle man is at a disadvantage).
            //  Is this an okay thing?
            // Chronicle the damage exchange for this portion of the chain.
            chronicle(
                GameChronicle.Moment.DELIVER_DAMAGE(
                    defenders, damagePerDefender,
                    attackers, totalThornDamage
                )
            )

        }
    }
} 
