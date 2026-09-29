package dugsolutions.leaf.v35.battle

import dugsolutions.leaf.v35.battle.domain.BattleGridRowSnapshot
import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.player.PlayerId

/** The concrete row asset whose current value contributes to a Strike total. */
sealed interface StrikeContributionSource {
    data class Die(val index: Int, val sides: Int) : StrikeContributionSource
    data class Critter(val index: Int, val name: String) : StrikeContributionSource
}

/** Compact causal test for one directly measurable contribution to a resolved Strike. */
data class StrikeContribution(
    val playerId: PlayerId,
    val source: StrikeContributionSource,
    val magnitude: Int,
    val used: Boolean,
    val contributesValue: Boolean,
    val individuallyWinnerDecisive: Boolean,
    val individuallyWoundDecisive: Boolean,
    val associatedBattleVp: Int
)

data class StrikeContributionLedger(
    val row: StrikeRow,
    val winnerIds: List<PlayerId>,
    val woundedPlayerIds: List<PlayerId>,
    val vpPerWinner: Int,
    val contributions: List<StrikeContribution>
)

/**
 * Replays the Strike arithmetic with one direct contribution removed at a time.
 *
 * This intentionally attributes only value visible in the final row snapshot.
 * Later provenance can connect an effect to the die/critter value it created;
 * the ledger itself does not infer that an earlier action caused a win.
 */
object StrikeContributionAnalyzer {
    private const val WOUND_MARGIN = 5

    fun analyze(
        rowSnapshot: BattleGridRowSnapshot,
        winnerIds: List<PlayerId>,
        woundedPlayerIds: List<PlayerId>,
        vpPerWinner: Int
    ): StrikeContributionLedger {
        val active = rowSnapshot.squares.filterNot { it.withdrawn }
        val originalTotals = active.associate { it.playerId to it.total }
        val originalWinners = winnerIds.toSet()
        val originalWounded = woundedPlayerIds.toSet()

        val contributions = active.flatMap { square ->
            val dice = square.dice.mapIndexed { index, die ->
                contribution(
                    playerId = square.playerId,
                    source = StrikeContributionSource.Die(index, die.sides.value),
                    magnitude = die.value,
                    originalTotals = originalTotals,
                    originalWinners = originalWinners,
                    originalWounded = originalWounded,
                    vpPerWinner = vpPerWinner
                )
            }
            val critters = square.critters.mapIndexed { index, critter ->
                contribution(
                    playerId = square.playerId,
                    source = StrikeContributionSource.Critter(index, critter.critter.name),
                    magnitude = critter.value,
                    originalTotals = originalTotals,
                    originalWinners = originalWinners,
                    originalWounded = originalWounded,
                    vpPerWinner = vpPerWinner
                )
            }
            dice + critters
        }

        return StrikeContributionLedger(
            row = rowSnapshot.row,
            winnerIds = winnerIds,
            woundedPlayerIds = woundedPlayerIds,
            vpPerWinner = vpPerWinner,
            contributions = contributions
        )
    }

    private fun contribution(
        playerId: PlayerId,
        source: StrikeContributionSource,
        magnitude: Int,
        originalTotals: Map<PlayerId, Int>,
        originalWinners: Set<PlayerId>,
        originalWounded: Set<PlayerId>,
        vpPerWinner: Int
    ): StrikeContribution {
        val without = originalTotals.toMutableMap().also {
            it[playerId] = requireNotNull(it[playerId]) - magnitude
        }
        val counterfactual = outcome(without)
        return StrikeContribution(
            playerId = playerId,
            source = source,
            magnitude = magnitude,
            used = true,
            contributesValue = magnitude != 0,
            individuallyWinnerDecisive = counterfactual.winners != originalWinners,
            individuallyWoundDecisive = counterfactual.wounded != originalWounded,
            associatedBattleVp = if (playerId in originalWinners) vpPerWinner else 0
        )
    }

    private data class Outcome(
        val winners: Set<PlayerId>,
        val wounded: Set<PlayerId>
    )

    private fun outcome(totals: Map<PlayerId, Int>): Outcome {
        val high = totals.values.maxOrNull() ?: return Outcome(emptySet(), emptySet())
        val highPlayers = totals.filterValues { it == high }.keys
        val winners = if (totals.size > 1 && highPlayers.size == totals.size) emptySet() else highPlayers
        if (winners.isEmpty()) return Outcome(emptySet(), emptySet())
        val wounded = totals.filter { (playerId, total) ->
            playerId !in winners && high - total >= WOUND_MARGIN
        }.keys
        return Outcome(winners, wounded)
    }
}
