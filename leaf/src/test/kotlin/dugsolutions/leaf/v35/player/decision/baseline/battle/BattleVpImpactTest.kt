package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.player.PlayerId
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class BattleVpImpactTest {
    private val evaluator = BattleVpImpact()

    @Nested
    inner class `Strike VP semantics` {
        @Test
        fun `non-winner earns zero VP`() {
            val impact = evaluator(
                state(winning = false, own = 8.0, opponents = mapOf(2 to 10.0)),
                state(winning = false, own = 9.0, opponents = mapOf(2 to 10.0))
            )

            assertEquals(0, impact.beforeVp)
            assertEquals(0, impact.afterVp)
            assertEquals(0, impact.gain)
        }

        @Test
        fun `winner earns base two VP with no wounds`() {
            val impact = evaluator(
                state(winning = false, own = 9.0, opponents = mapOf(2 to 10.0)),
                state(winning = true, own = 11.0, opponents = mapOf(2 to 10.0))
            )

            assertEquals(0, impact.beforeVp)
            assertEquals(2, impact.afterVp)
            assertEquals(2, impact.gain)
        }

        @Test
        fun `winner gains one additional VP for each wounded opponent`() {
            val impact = evaluator(
                state(winning = true, own = 10.0, opponents = mapOf(2 to 8.0, 3 to 6.0)),
                state(winning = true, own = 12.0, opponents = mapOf(2 to 7.0, 3 to 6.0))
            )

            assertEquals(2, impact.beforeVp)
            assertEquals(4, impact.afterVp)
            assertEquals(2, impact.gain)
        }

        @Test
        fun `shared winner still receives wound bonus from lower opponents`() {
            val impact = evaluator(
                state(winning = false, own = 9.0, opponents = mapOf(2 to 10.0, 3 to 4.0)),
                state(winning = true, own = 10.0, opponents = mapOf(2 to 10.0, 3 to 4.0))
            )

            assertEquals(3, impact.afterVp)
            assertEquals(3, impact.gain)
        }

        @Test
        fun `expected half-point totals use the real five-point wound boundary`() {
            val impact = evaluator(
                state(winning = true, own = 10.0, opponents = mapOf(2 to 6.0)),
                state(winning = true, own = 10.5, opponents = mapOf(2 to 5.5))
            )

            assertEquals(2, impact.beforeVp)
            assertEquals(3, impact.afterVp)
            assertEquals(1, impact.gain)
        }
    }

    @Test
    fun `multi-row impact sums before after and gain`() {
        val top = rowChange(
            row = StrikeRow.TOP,
            before = actionState(StrikeRow.TOP, winning = false, own = 8.0, opponents = mapOf(2 to 10.0)),
            after = actionState(StrikeRow.TOP, winning = true, own = 11.0, opponents = mapOf(2 to 10.0))
        )
        val middle = rowChange(
            row = StrikeRow.MIDDLE,
            before = actionState(StrikeRow.MIDDLE, winning = true, own = 10.0, opponents = mapOf(2 to 8.0)),
            after = actionState(StrikeRow.MIDDLE, winning = true, own = 13.0, opponents = mapOf(2 to 8.0))
        )

        val impact = evaluator(listOf(top, middle))

        assertEquals(2, impact.beforeVp)
        assertEquals(5, impact.afterVp)
        assertEquals(3, impact.gain)
        assertEquals(listOf(2, 1), impact.rowImpacts.map { it.gain })
    }

    private fun state(
        winning: Boolean,
        own: Double,
        opponents: Map<Int, Double>,
        row: StrikeRow = StrikeRow.TOP
    ): BattleVpState =
        BattleVpState(
            row = row,
            available = true,
            currentlyWinning = winning,
            ownTotal = own,
            opponentTotals = opponents.mapKeys { PlayerId(it.key) }
        )

    private fun actionState(
        row: StrikeRow,
        winning: Boolean,
        own: Double,
        opponents: Map<Int, Double>
    ): BattleActionRowState {
        val benchmark = opponents.values.maxOrNull()
        return BattleActionRowState(
            row = row,
            available = true,
            currentlyWinning = winning,
            woundRisk = false,
            securedForNow = false,
            scoreMargin = benchmark?.let { own - it },
            liveThreatMargin = benchmark?.let { own - it },
            ownTotal = own,
            opponentTotals = opponents.mapKeys { PlayerId(it.key) }
        )
    }

    private fun rowChange(
        row: StrikeRow,
        before: BattleActionRowState,
        after: BattleActionRowState
    ): BattleActionRowChange {
        require(before.row == row && after.row == row)
        return BattleActionRowChange(before, after)
    }
}
