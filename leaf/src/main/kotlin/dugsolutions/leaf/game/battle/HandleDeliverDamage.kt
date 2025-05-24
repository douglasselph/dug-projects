package dugsolutions.leaf.game.battle

import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.player.Player
import kotlin.math.max

class HandleDeliverDamage(
    private val handleAbsorbDamage: HandleAbsorbDamage,
    private val chronicle: GameChronicle
) {

    operator fun invoke(players: List<Player>) {
        // Sorts players by pipTotal in descending order (highest to lowest)
        // When pipTotals are equal, uses original player index as tiebreaker
        // Returns only the sorted Player objects, discarding the indices used for sorting
        val sortedPlayers = players
            .mapIndexed { index, player ->
                player to index
            }.sortedWith(
                compareByDescending<Pair<Player, Int>> { it.first.pipTotal }
                    .thenBy { it.second }
            ).map { it.first }

        // Ensure incoming damage cleared
        players.forEach { player -> player.incomingDamage = 0 }

        // Process players from lowest to highest pip total
        val reversedPlayers = sortedPlayers.reversed()

        // Process each player in sequence, starting with lowest pips
        for (i in 0 until reversedPlayers.size - 1) {
            val defender = reversedPlayers[i]
            val attacker = reversedPlayers[i + 1]

            // Calculate damage to be delivered
            val damage = attacker.pipTotal - defender.pipTotal

            // Skip if no damage to deliver (tie of lowest versus highest)
            if (damage > 0) {
                defender.incomingDamage += max(0, damage - defender.deflectDamage)
                if (defender.incomingDamage > 0) {
                    val thornDamage = handleAbsorbDamage(defender)
                    if (thornDamage > 0) {
                        attacker.incomingDamage += thornDamage
                    }
                    chronicle(
                        GameChronicle.Moment.DELIVER_DAMAGE(
                            defender, damage,
                            attacker, thornDamage
                        )
                    )
                }
            }
        }
    }
} 
