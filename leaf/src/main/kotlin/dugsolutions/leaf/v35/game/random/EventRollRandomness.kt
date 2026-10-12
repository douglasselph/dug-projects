package dugsolutions.leaf.v35.game.random

import dugsolutions.leaf.v35.player.Player
import dugsolutions.leaf.v35.random.die.Die

/**
 * Optional counterfactual roll coupling. Each die is keyed by a physical,
 * player-local identity and by its occurrence within a specific game round.
 * Events that are skipped in one replay do not advance other dice's outcomes.
 * This is deliberately *not* an RNG reset or a shared stream.
 *
 * Future work: stable identity across upgrades/acquisitions and explicit event
 * types for non-die mechanics (Wisp deck, shuffles, tie-breakers).
 */
class EventRollRandomness(
    private val masterSeed: Long,
    private val sharedPercentile: Boolean = false
) {
    private data class Key(val round: Int, val player: Int, val dieId: Long)
    private val occurrences = mutableMapOf<Key, Int>()
    private val recordedEvents = mutableListOf<ReplayRollEvent>()
    val events: List<ReplayRollEvent> get() = recordedEvents.toList()

    fun roll(player: Player, die: Die, roundNumber: Int): Int {
        val key = Key(roundNumber, player.id.value, player.dice.stableDieId(die))
        val occurrence = (occurrences[key] ?: 0) + 1
        occurrences[key] = occurrence
        val percentile = if (sharedPercentile) {
            percentileFor(masterSeed, roundNumber, key.player, key.dieId, occurrence)
        } else null
        val face = if (percentile != null) (percentile * die.sides).toInt() + 1
            else faceFor(masterSeed, roundNumber, key.player, key.dieId, occurrence, die.sides)
        recordedEvents += ReplayRollEvent(recordedEvents.size + 1, roundNumber,
            key.player, key.dieId, occurrence, die.sides, face, percentile)
        return face
    }

    companion object {
        /** Stable mixing independent of JVM hashCode, map iteration, and call order. */
        fun faceFor(seed: Long, round: Int, player: Int, dieId: Long, occurrence: Int, sides: Int): Int {
            require(round >= 0 && player > 0 && dieId > 0 && occurrence > 0 && sides > 0)
            var x = seed xor (round.toLong() * -7046029254386353131L)
            x = mix(x xor (player.toLong() * -4658895280553007687L))
            x = mix(x xor (dieId * -7723592293110705685L))
            x = mix(x xor occurrence.toLong())
            val unsigned = x ushr 1
            return (unsigned % sides).toInt() + 1
        }

        /**
         * Use the same uniformly distributed underlying 53-bit fraction for a
         * matching die event regardless of its current number of sides.
         * A high percentile stays relatively high after D4 -> D6, but each
         * individual die size retains its uniform marginal distribution.
         */
        fun percentileFaceFor(seed: Long, round: Int, player: Int, dieId: Long, occurrence: Int, sides: Int): Int {
            val fraction = percentileFor(seed, round, player, dieId, occurrence)
            return (fraction * sides).toInt() + 1
        }

        fun percentileFor(seed: Long, round: Int, player: Int, dieId: Long, occurrence: Int): Double {
            require(round >= 0 && player > 0 && dieId > 0 && occurrence > 0)
            var x = seed xor (round.toLong() * -7046029254386353131L)
            x = mix(x xor (player.toLong() * -4658895280553007687L))
            x = mix(x xor (dieId * -7723592293110705685L))
            x = mix(x xor occurrence.toLong())
            // The upper 53 bits map exactly into [0,1), including zero.
            return (x ushr 11).toDouble() * (1.0 / 9007199254740992.0)
        }

        private fun mix(value: Long): Long {
            var x = value
            x = (x xor (x ushr 30)) * -4658895280553007687L
            x = (x xor (x ushr 27)) * -7723592293110705685L
            return x xor (x ushr 31)
        }
    }
}
