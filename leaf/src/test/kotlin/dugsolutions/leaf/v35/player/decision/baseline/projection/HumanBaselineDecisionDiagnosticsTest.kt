package dugsolutions.leaf.v35.player.decision.baseline.projection

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.context.DieView
import dugsolutions.leaf.v35.player.decision.context.OpponentView
import dugsolutions.leaf.v35.player.decision.effect.EffectOwnedDieZone
import dugsolutions.leaf.v35.player.decision.effect.PetalD4SourceChoice
import dugsolutions.leaf.v35.random.die.DieSides
import dugsolutions.leaf.v35.round.domain.RoundCardType
import org.junit.jupiter.api.Test
import kotlin.test.*

/** Diagnostic positions are deliberately asymmetric: a correct decision changes when its state changes. */
class HumanBaselineDecisionDiagnosticsTest {
    private val p1 = GameEffect.GAIN_OR_STEAL_D4_TO_DISCARD_FROM_HAND_ONLY_THEN_CYCLE_D4S
    private val p2 = GameEffect.GAIN_OR_STEAL_D4_TO_HAND_FROM_HAND_ONLY_THEN_CYCLE_D4S
    private val p3 = GameEffect.GAIN_OR_STEAL_D4_TO_DISCARD_FROM_HAND_OR_DISCARD_THEN_CYCLE_D4S
    private val p4 = GameEffect.GAIN_OR_STEAL_D4_TO_HAND_FROM_HAND_OR_DISCARD_THEN_CYCLE_D4S

    private fun petalState(groveD4: Int, handD4: Boolean, discardD4: Boolean): DecisionContext {
        val enemy = DecisionContext.EMPTY.self.board.copy(
            id = PlayerId(2),
            hand = if (handD4) listOf(DieView(0, 4, 4)) else emptyList(),
            discard = if (discardD4) listOf(DieView(1, 4, 1)) else emptyList()
        )
        return DecisionContext.EMPTY.copy(
            phase = RoundCardType.CULTIVATION,
            grove = DecisionContext.EMPTY.grove.copy(graftBed = if (groveD4 > 0) mapOf(DieSides.D4 to groveD4) else emptyMap()),
            opponents = listOf(OpponentView(enemy, 0)),
            self = DecisionContext.EMPTY.self.copy(board = DecisionContext.EMPTY.self.board.copy(
                hand = listOf(DieView(0, 8, 4)), supply = listOf(DieView(0, 6, 2))
            ))
        )
    }

    @Test fun `P1 and P2 forbid discard theft even when opponent discard exists`() {
        val state = petalState(1, true, true)
        for (effect in listOf(p1, p2)) {
            assertFalse(PetalAcquisitionProjection.legalSources(state, effect).any {
                it is PetalD4SourceChoice.Opponent && it.zone == EffectOwnedDieZone.DISCARD
            }, "Illegal discard theft offered for $effect")
        }
        for (effect in listOf(p3, p4)) {
            assertTrue(PetalAcquisitionProjection.legalSources(state, effect).any {
                it is PetalD4SourceChoice.Opponent && it.zone == EffectOwnedDieZone.DISCARD
            }, "Legal discard theft omitted for $effect")
        }
    }

    @Test fun `all Petal variants remove sources that no longer exist`() {
        val absent = petalState(0, false, false)
        for (effect in listOf(p1, p2, p3, p4)) {
            assertTrue(PetalAcquisitionProjection.legalSources(absent, effect).isEmpty(), "$effect offered a phantom D4")
        }
        assertEquals(listOf(PetalD4SourceChoice.Grove), PetalAcquisitionProjection.legalSources(petalState(1, false, false), p4))
    }

    @Test fun `all Petal activation values equal the highest complete legal sequence`() {
        val state = petalState(1, true, true)
        val snapshot = state.toString()
        for (effect in listOf(p1, p2, p3, p4)) {
            val options = PetalAcquisitionProjection.legalSources(state, effect)
            val candidates = options.map { PetalAcquisitionProjection.scoreAcquisitionSequence(state, effect, it).total }
            assertEquals(candidates.maxOrNull(), PetalAcquisitionProjection.activationScore(state, effect).total,
                "Activation and target valuations diverge for $effect: $candidates")
        }
        assertEquals(snapshot, state.toString(), "Decision predictions modified their input")
    }

    @Test fun `future die mean is fractional and a capped Raise cannot manufacture power`() {
        val initial = ProjectedState(
            dice = listOf(ProjectedDie("owned", 6, 5.0)),
            buyingPower = 5.0, availableCosts = listOf(6, 7, 9)
        )
        val first = HumanStateProjection.apply(initial, ProjectionStep.RaiseDie("owned", 4.0))
        val second = HumanStateProjection.apply(first, ProjectionStep.RaiseDie("owned", 4.0))
        assertEquals(6.0, first.buyingPower)
        assertEquals(first, second, "A second capped Raise must not be credited again")
        val drawn = HumanStateProjection.apply(second, ProjectionStep.ExpectedDraw("unknown", 6))
        assertEquals(3.5, drawn.die("unknown").value)
        assertEquals(listOf(7, 9), HumanProjectionEvaluator().evaluate(DecisionContext.EMPTY, second, drawn).crossedBuyTiersUp)
    }

    @Test fun `a one-point Strike improvement can prevent a wound without modifying the other row`() {
        val before = ProjectedState(strikes = listOf(
            ProjectedStrike(StrikeRow.TOP, 5.0, mapOf("rival" to 10.0)),
            ProjectedStrike(StrikeRow.MIDDLE, 5.0, mapOf("rival" to 5.0))
        ))
        val after = HumanStateProjection.apply(before, ProjectionStep.OwnStrikeDelta(StrikeRow.TOP, 1.0))
        assertEquals(before.strike(StrikeRow.MIDDLE), after.strike(StrikeRow.MIDDLE))
        val swing = assertNotNull(HumanProjectionEvaluator().evaluate(DecisionContext.EMPTY, before, after).battle)
        assertEquals(listOf(StrikeRow.TOP), swing.rowSwings.map { it.row })
        assertTrue(swing.totalValue > 0.0, "Wound prevention should have positive tactical value")
    }

    @Test fun `an alternate projection branch cannot leak into its sibling`() {
        val origin = ProjectedState(dice = listOf(ProjectedDie("d", 8, 2.0)), buyingPower = 2.0)
        val mild = HumanStateProjection.apply(origin, ProjectionStep.RaiseDie("d", 1.0))
        val strong = HumanStateProjection.apply(origin, ProjectionStep.RaiseDie("d", 5.0))
        assertEquals(3.0, mild.die("d").value)
        assertEquals(7.0, strong.die("d").value)
        assertEquals(2.0, origin.die("d").value)
    }
}
