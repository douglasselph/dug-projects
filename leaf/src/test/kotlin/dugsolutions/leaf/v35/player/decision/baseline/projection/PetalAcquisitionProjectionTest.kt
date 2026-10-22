package dugsolutions.leaf.v35.player.decision.baseline.projection

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
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PetalAcquisitionProjectionTest {
    private val p1 = GameEffect.GAIN_OR_STEAL_D4_TO_DISCARD_FROM_HAND_ONLY_THEN_CYCLE_D4S
    private val p2 = GameEffect.GAIN_OR_STEAL_D4_TO_HAND_FROM_HAND_ONLY_THEN_CYCLE_D4S
    private val p3 = GameEffect.GAIN_OR_STEAL_D4_TO_DISCARD_FROM_HAND_OR_DISCARD_THEN_CYCLE_D4S
    private val p4 = GameEffect.GAIN_OR_STEAL_D4_TO_HAND_FROM_HAND_OR_DISCARD_THEN_CYCLE_D4S

    private fun context(): DecisionContext {
        val opponentBoard = DecisionContext.EMPTY.self.board.copy(
            id = PlayerId(2), hand = listOf(DieView(0, 4, 4)),
            discard = listOf(DieView(0, 4, 1))
        )
        return DecisionContext.EMPTY.copy(
            phase = RoundCardType.CULTIVATION,
            grove = DecisionContext.EMPTY.grove.copy(graftBed = mapOf(DieSides.D4 to 1)),
            opponents = listOf(OpponentView(opponentBoard, 0)),
            self = DecisionContext.EMPTY.self.copy(board = DecisionContext.EMPTY.self.board.copy(
                hand = listOf(DieView(0, 4, 1)), supply = listOf(DieView(0, 6, 2))
            ))
        )
    }

    @Test fun `four codes express two independent source and destination policies`() {
        val context = context()
        for (effect in listOf(p1, p2, p3, p4)) {
            val choices = PetalAcquisitionProjection.legalSources(context, effect)
            assertTrue(PetalD4SourceChoice.Grove in choices)
            assertEquals(if (PetalAcquisitionProjection.canStealDiscard(effect)) 3 else 2, choices.size)
            assertEquals(effect == p2 || effect == p4, PetalAcquisitionProjection.toHand(effect))
            assertEquals(effect == p3 || effect == p4,
                choices.any { it is PetalD4SourceChoice.Opponent && it.zone == EffectOwnedDieZone.DISCARD })
        }
    }

    @Test fun `no available Grove or opponent D4 means no legal acquisition`() {
        val context = context().copy(
            grove = DecisionContext.EMPTY.grove,
            opponents = emptyList()
        )
        assertTrue(PetalAcquisitionProjection.legalSources(context, p4).isEmpty())
        assertEquals(10, PetalAcquisitionProjection.activationScore(context, p4).total)
    }

    @Test fun `Hand destination contributes immediate buying value but Discard does not`() {
        val context = context().copy(opponents = emptyList())
        val grove = PetalAcquisitionProjection.scoreSource(context, p1, PetalD4SourceChoice.Grove)
        val hand = PetalAcquisitionProjection.scoreSource(context, p2, PetalD4SourceChoice.Grove)
        assertTrue(hand.total > grove.total)
        assertFalse(grove.adjustments.any { "immediate purchasing power" in it.reason })
    }

    @Test fun `stealing opponent Hand removes currently useful power relative to Discard`() {
        val context = context()
        val options = PetalAcquisitionProjection.legalSources(context, p3).filterIsInstance<PetalD4SourceChoice.Opponent>()
        val hand = options.single { it.zone == EffectOwnedDieZone.HAND }
        val discard = options.single { it.zone == EffectOwnedDieZone.DISCARD }
        assertTrue(PetalAcquisitionProjection.scoreSource(context, p3, hand).total >=
            PetalAcquisitionProjection.scoreSource(context, p3, discard).total)
    }

    @Test fun `activation uses exactly the best source sequence score`() {
        val state = context()
        listOf(p1, p2, p3, p4).forEach { effect ->
            val scores = PetalAcquisitionProjection.legalSources(state, effect).map { source ->
                PetalAcquisitionProjection.scoreAcquisitionSequence(state, effect, source).total
            }
            assertEquals(scores.maxOrNull(), PetalAcquisitionProjection.activationScore(state, effect).total)
        }
    }

    @Test fun `activation projection does not mutate observed dice or opponent zones`() {
        val context = context()
        val snapshot = context.toString()
        PetalAcquisitionProjection.activationScore(context, p4)
        assertEquals(snapshot, context.toString())
    }
}
