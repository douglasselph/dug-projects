package dugsolutions.leaf.v35.player.decision.baseline.projection

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HumanStateProjectionTest {
    @Test fun `unknown draw uses exact mean without mutating original`() {
        val initial = ProjectedState(buyingPower = 6.0, availableCosts = listOf(7, 9, 11))
        val projected = HumanStateProjection.apply(initial, ProjectionStep.ExpectedDraw("new", 6))
        assertEquals(3.5, projected.die("new").value)
        assertEquals(9.5, projected.buyingPower)
        assertTrue(initial.dice.isEmpty())
        assertEquals(6.0, initial.buyingPower)
        val outcome = HumanProjectionEvaluator().evaluate(DecisionContext.EMPTY, initial, projected)
        assertEquals(listOf(7, 9), outcome.crossedBuyTiersUp)
    }

    @Test fun `sequential projection respects capped raises and incremental tier scoring`() {
        val initial = ProjectedState(
            dice = listOf(ProjectedDie("first", 6, 3.5)),
            buyingPower = 3.5,
            availableCosts = listOf(5, 6, 7)
        )
        val after = HumanStateProjection.apply(initial, listOf(
            ProjectionStep.RaiseDie("first", 2.0),
            ProjectionStep.RaiseDie("first", 3.0)
        ))
        assertEquals(6.0, after.die("first").value)
        assertEquals(6.0, after.buyingPower)
        assertEquals(listOf(5, 6), HumanProjectionEvaluator().evaluate(DecisionContext.EMPTY, initial, after).crossedBuyTiersUp)
    }

    @Test fun `battle projection scores changed row only and leaves input unchanged`() {
        val before = ProjectedState(strikes = listOf(
            ProjectedStrike(StrikeRow.TOP, 7.0, mapOf("opponent" to 9.0)),
            ProjectedStrike(StrikeRow.MIDDLE, 3.0, mapOf("opponent" to 10.0))
        ))
        val after = HumanStateProjection.apply(before, listOf(
            ProjectionStep.OwnStrikeDelta(StrikeRow.TOP, 2.0),
            ProjectionStep.OpponentStrikeDelta(StrikeRow.TOP, "opponent", -2.0)
        ))
        assertEquals(7.0, before.strike(StrikeRow.TOP).ownTotal)
        assertEquals(9.0, before.strike(StrikeRow.TOP).opponentTotals["opponent"])
        val outcome = HumanProjectionEvaluator().evaluate(DecisionContext.EMPTY, before, after)
        assertEquals(1, outcome.battle?.rowSwings?.size)
        assertTrue(requireNotNull(outcome.battle).totalValue > 0.0)
    }

    @Test fun `two independently evaluated options cannot mutate one another`() {
        val original = ProjectedState(dice = listOf(ProjectedDie("d", 8, 4.5)), buyingPower = 4.5)
        val a = HumanStateProjection.apply(original, ProjectionStep.RaiseDie("d", 1.0))
        val b = HumanStateProjection.apply(original, ProjectionStep.RaiseDie("d", 3.0))
        assertEquals(5.5, a.die("d").value)
        assertEquals(7.5, b.die("d").value)
        assertEquals(4.5, original.die("d").value)
    }
}
