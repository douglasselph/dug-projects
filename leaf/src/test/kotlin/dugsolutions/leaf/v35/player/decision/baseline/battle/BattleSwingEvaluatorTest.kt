package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.player.decision.baseline.HumanBaselinePolicy
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class BattleSwingEvaluatorTest {
    private val context = DecisionContext.EMPTY
    private val evaluator = BattleSwingEvaluator()

    @Nested
    inner class `Positive transition hierarchy` {
        @Test
        fun `loss to actual win is WIN_FLIPPED`() {
            val swing = evaluate(
                before = state(margin = -2.0, winning = false),
                after = state(margin = 2.0, winning = true)
            )

            assertSwing(swing, BattleTransition.WIN_FLIPPED, 500.0, 4.0, 504.0)
        }

        @Test
        fun `loss to non-winning equal score is TIE_ACHIEVED`() {
            val swing = evaluate(
                before = state(margin = -2.0, winning = false),
                after = state(margin = 0.0, winning = false)
            )

            assertSwing(swing, BattleTransition.TIE_ACHIEVED, 400.0, 2.0, 402.0)
        }

        @Test
        fun `escaping wound range is WOUND_PREVENTED`() {
            val swing = evaluate(
                before = state(margin = -6.0, winning = false, woundRisk = true),
                after = state(margin = -2.0, winning = false, woundRisk = false)
            )

            assertSwing(swing, BattleTransition.WOUND_PREVENTED, 300.0, 4.0, 304.0)
        }

        @Test
        fun `winning margin crossing five is WOUND_CREATED`() {
            val swing = evaluate(
                before = state(margin = 2.0, winning = true),
                after = state(margin = 5.0, winning = true)
            )

            assertSwing(swing, BattleTransition.WOUND_CREATED, 200.0, 3.0, 203.0)
        }

        @Test
        fun `secured transition uses Live Threat margin as raw swing`() {
            val swing = evaluate(
                before = state(
                    margin = 1.0,
                    liveMargin = 8.0,
                    winning = true,
                    secured = false
                ),
                after = state(
                    margin = 1.0,
                    liveMargin = 10.0,
                    winning = true,
                    secured = true
                )
            )

            assertSwing(swing, BattleTransition.SECURED_CREATED, 100.0, 2.0, 102.0)
        }

        @Test
        fun `strongest transition wins and does not stack crossed boundaries`() {
            val swing = evaluate(
                before = state(margin = -6.0, winning = false, woundRisk = true),
                after = state(margin = 2.0, winning = true, woundRisk = false)
            )

            assertSwing(swing, BattleTransition.WIN_FLIPPED, 500.0, 8.0, 508.0)
        }

        @Test
        fun `becoming shared winner at zero margin is WIN_FLIPPED rather than TIE_ACHIEVED`() {
            val swing = evaluate(
                before = state(margin = -2.0, winning = false),
                after = state(margin = 0.0, winning = true)
            )

            assertSwing(swing, BattleTransition.WIN_FLIPPED, 500.0, 2.0, 502.0)
        }
    }

    @Nested
    inner class `Raw and harmful movement` {
        @Test
        fun `ordinary improvement with no transition is raw margin movement`() {
            val swing = evaluate(
                before = state(margin = -4.0, winning = false),
                after = state(margin = -2.0, winning = false)
            )

            assertSwing(swing, BattleTransition.NONE, 0.0, 2.0, 2.0)
        }

        @Test
        fun `ordinary deterioration with no transition is negative raw movement`() {
            val swing = evaluate(
                before = state(margin = 4.0, winning = true),
                after = state(margin = 2.0, winning = true)
            )

            assertSwing(swing, BattleTransition.NONE, 0.0, -2.0, -2.0)
        }

        @Test
        fun `win becoming loss is negative reverse WIN_FLIPPED`() {
            val swing = evaluate(
                before = state(margin = 2.0, winning = true),
                after = state(margin = -2.0, winning = false)
            )

            assertSwing(swing, BattleTransition.WIN_FLIPPED, -500.0, -4.0, -504.0)
        }

        @Test
        fun `ordinary loss becoming wounded loss is negative reverse WOUND_PREVENTED`() {
            val swing = evaluate(
                before = state(margin = -2.0, winning = false, woundRisk = false),
                after = state(margin = -6.0, winning = false, woundRisk = true)
            )

            assertSwing(swing, BattleTransition.WOUND_PREVENTED, -300.0, -4.0, -304.0)
        }

        @Test
        fun `losing secured status is negative reverse SECURED_CREATED using Live Threat margin`() {
            val swing = evaluate(
                before = state(
                    margin = 1.0,
                    liveMargin = 10.0,
                    winning = true,
                    secured = true
                ),
                after = state(
                    margin = 1.0,
                    liveMargin = 8.0,
                    winning = true,
                    secured = false
                )
            )

            assertSwing(swing, BattleTransition.SECURED_CREATED, -100.0, -2.0, -102.0)
        }
    }

    @Nested
    inner class `Expected values and aggregation` {
        @Test
        fun `expected half-point swing remains Double`() {
            val swing = evaluate(
                before = state(margin = -2.0, winning = false),
                after = state(margin = 2.5, winning = true)
            )

            assertSwing(swing, BattleTransition.WIN_FLIPPED, 500.0, 4.5, 504.5)
        }

        @Test
        fun `multi-row action sums positive contributions across different rows`() {
            val top = change(
                row = StrikeRow.TOP,
                before = state(StrikeRow.TOP, margin = -2.0, winning = false),
                after = state(StrikeRow.TOP, margin = 1.0, winning = true)
            )
            val middle = change(
                row = StrikeRow.MIDDLE,
                before = state(StrikeRow.MIDDLE, margin = 2.0, winning = true),
                after = state(StrikeRow.MIDDLE, margin = 5.0, winning = true)
            )

            val action = evaluator(context, listOf(top, middle))

            assertEquals(503.0, action.rowSwings[0].totalValue)
            assertEquals(203.0, action.rowSwings[1].totalValue)
            assertEquals(706.0, action.totalValue)
        }

        @Test
        fun `multi-row action sums one contribution per row including collateral harm`() {
            val top = change(
                row = StrikeRow.TOP,
                before = state(StrikeRow.TOP, margin = -2.0, winning = false),
                after = state(StrikeRow.TOP, margin = 1.0, winning = true)
            )
            val middle = change(
                row = StrikeRow.MIDDLE,
                before = state(StrikeRow.MIDDLE, margin = 4.0, winning = true),
                after = state(StrikeRow.MIDDLE, margin = 1.0, winning = true)
            )

            val action = evaluator(context, listOf(top, middle))

            assertEquals(2, action.rowSwings.size)
            assertEquals(503.0, action.rowSwings[0].totalValue)
            assertEquals(-3.0, action.rowSwings[1].totalValue)
            assertEquals(500.0, action.totalValue)
        }

        @Test
        fun `policy override changes transition spacing without changing raw swing`() {
            val custom = BattleSwingEvaluator(
                HumanBaselinePolicy(battleTransitionScaleValue = 30)
            )

            val swing = custom(
                context,
                state(margin = -2.0, winning = false),
                state(margin = 2.0, winning = true)
            )

            assertSwing(swing, BattleTransition.WIN_FLIPPED, 150.0, 4.0, 154.0)
        }
    }

    private fun evaluate(
        before: BattleSwingState,
        after: BattleSwingState
    ): BattleSwing = evaluator(context, before, after)

    private fun change(
        row: StrikeRow,
        before: BattleSwingState,
        after: BattleSwingState
    ): BattleSwingChange {
        require(before.row == row && after.row == row)
        return BattleSwingChange(before, after)
    }

    private fun state(
        row: StrikeRow = StrikeRow.TOP,
        margin: Double,
        liveMargin: Double? = margin,
        winning: Boolean,
        woundRisk: Boolean = false,
        secured: Boolean = false
    ): BattleSwingState =
        BattleSwingState(
            row = row,
            available = true,
            currentlyWinning = winning,
            woundRisk = woundRisk,
            securedForNow = secured,
            scoreMargin = margin,
            liveThreatMargin = liveMargin
        )

    private fun assertSwing(
        swing: BattleSwing,
        transition: BattleTransition,
        transitionBase: Double,
        rawSwing: Double,
        total: Double
    ) {
        assertEquals(transition, swing.transition)
        assertEquals(transitionBase, swing.transitionBase)
        assertEquals(rawSwing, swing.rawSwing)
        assertEquals(total, swing.totalValue)
    }
}
