package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class BattleActionAnalyzerTest {
    private val analyzer = BattleActionAnalyzer()
    private val context = DecisionContext.EMPTY

    @Nested
    inner class `Shared action metrics` {
        @Test
        fun `win flip carries Battle Swing VP gain and crossed-boundary steps`() {
            val analysis = analyze(
                mode = BattleAnalysisMode.DETERMINISTIC,
                before = state(margin = -2.0, winning = false, own = 8.0, opponent = 10.0),
                after = state(margin = 2.0, winning = true, own = 12.0, opponent = 10.0)
            )

            assertEquals(504.0, analysis.tacticalValue)
            assertEquals(2, analysis.vpImpact.gain)
            assertEquals(2, analysis.improvementStepCount)
        }

        @Test
        fun `wounded loss to win counts wound tie and win boundaries but only one Swing base`() {
            val analysis = analyze(
                before = state(
                    margin = -6.0,
                    winning = false,
                    woundRisk = true,
                    own = 4.0,
                    opponent = 10.0
                ),
                after = state(
                    margin = 2.0,
                    winning = true,
                    woundRisk = false,
                    own = 12.0,
                    opponent = 10.0
                )
            )

            assertEquals(BattleTransition.WIN_FLIPPED, analysis.swing.rowSwings.single().transition)
            assertEquals(508.0, analysis.tacticalValue)
            assertEquals(3, analysis.improvementStepCount)
            assertEquals(2, analysis.vpImpact.gain)
        }

        @Test
        fun `creating a wound is one improvement step and one VP of added Strike value`() {
            val analysis = analyze(
                before = state(margin = 2.0, winning = true, own = 10.0, opponent = 8.0),
                after = state(margin = 5.0, winning = true, own = 13.0, opponent = 8.0)
            )

            assertEquals(203.0, analysis.tacticalValue)
            assertEquals(1, analysis.improvementStepCount)
            assertEquals(1, analysis.vpImpact.gain)
        }

        @Test
        fun `harmful realization has no positive improvement steps`() {
            val analysis = analyze(
                before = state(margin = 2.0, winning = true, own = 12.0, opponent = 10.0),
                after = state(margin = -2.0, winning = false, own = 8.0, opponent = 10.0)
            )

            assertEquals(-504.0, analysis.tacticalValue)
            assertEquals(-2, analysis.vpImpact.gain)
            assertEquals(0, analysis.improvementStepCount)
        }
    }

    @Nested
    inner class `Expected actual and multi-row realizations` {
        @Test
        fun `expected mode retains half-point Battle Swing and target realization`() {
            val candidate = BattleActionRealization(
                realization = "expected D8 into TOP",
                mode = BattleAnalysisMode.EXPECTED,
                rowChanges = listOf(
                    BattleActionRowChange(
                        before = state(margin = -2.0, winning = false, own = 8.0, opponent = 10.0),
                        after = state(margin = 2.5, winning = true, own = 12.5, opponent = 10.0)
                    )
                )
            )

            val analysis = analyzer(context, candidate)

            assertEquals(BattleAnalysisMode.EXPECTED, analysis.mode)
            assertEquals("expected D8 into TOP", analysis.realization)
            assertEquals(504.5, analysis.tacticalValue)
            assertEquals(2, analysis.vpImpact.gain)
        }

        @Test
        fun `actual mode is retained independently of tactical math`() {
            val analysis = analyze(
                mode = BattleAnalysisMode.ACTUAL,
                before = state(margin = -1.0, winning = false, own = 9.0, opponent = 10.0),
                after = state(margin = 3.0, winning = true, own = 13.0, opponent = 10.0)
            )

            assertEquals(BattleAnalysisMode.ACTUAL, analysis.mode)
            assertEquals(504.0, analysis.tacticalValue)
        }

        @Test
        fun `multi-row analysis includes positive swing collateral harm and net VP`() {
            val candidate = BattleActionRealization(
                realization = "swap",
                mode = BattleAnalysisMode.DETERMINISTIC,
                rowChanges = listOf(
                    BattleActionRowChange(
                        before = state(StrikeRow.TOP, -2.0, false, own = 8.0, opponent = 10.0),
                        after = state(StrikeRow.TOP, 2.0, true, own = 12.0, opponent = 10.0)
                    ),
                    BattleActionRowChange(
                        before = state(StrikeRow.MIDDLE, 2.0, true, own = 12.0, opponent = 10.0),
                        after = state(StrikeRow.MIDDLE, -2.0, false, own = 8.0, opponent = 10.0)
                    )
                )
            )

            val analysis = analyzer(context, candidate)

            assertEquals(0.0, analysis.tacticalValue)
            assertEquals(0, analysis.vpImpact.gain)
            assertEquals(2, analysis.improvementStepCount)
        }

        @Test
        fun `improvement steps add across different rows`() {
            val candidate = BattleActionRealization(
                realization = Unit,
                mode = BattleAnalysisMode.DETERMINISTIC,
                rowChanges = listOf(
                    BattleActionRowChange(
                        before = state(StrikeRow.TOP, 2.0, true, own = 10.0, opponent = 8.0),
                        after = state(StrikeRow.TOP, 5.0, true, own = 13.0, opponent = 8.0)
                    ),
                    BattleActionRowChange(
                        before = state(StrikeRow.MIDDLE, 1.0, true, own = 10.0, opponent = 9.0),
                        after = state(StrikeRow.MIDDLE, 5.0, true, own = 14.0, opponent = 9.0)
                    )
                )
            )

            val analysis = analyzer(context, candidate)

            assertEquals(2, analysis.improvementStepCount)
            assertEquals(2, analysis.vpImpact.gain)
        }
    }

    private fun analyze(
        mode: BattleAnalysisMode = BattleAnalysisMode.DETERMINISTIC,
        before: BattleActionRowState,
        after: BattleActionRowState
    ): BattleActionAnalysis<String> =
        analyzer(
            context,
            BattleActionRealization(
                realization = "target",
                mode = mode,
                rowChanges = listOf(BattleActionRowChange(before, after))
            )
        )

    private fun state(
        row: StrikeRow = StrikeRow.TOP,
        margin: Double,
        winning: Boolean,
        woundRisk: Boolean = false,
        secured: Boolean = false,
        own: Double,
        opponent: Double,
        liveMargin: Double? = margin
    ): BattleActionRowState =
        BattleActionRowState(
            row = row,
            available = true,
            currentlyWinning = winning,
            woundRisk = woundRisk,
            securedForNow = secured,
            scoreMargin = margin,
            liveThreatMargin = liveMargin,
            ownTotal = own,
            opponentTotals = mapOf(PlayerId(2) to opponent)
        )
}
