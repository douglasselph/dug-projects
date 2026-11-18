package dugsolutions.leaf.v35.player.decision.baseline.projection

import dugsolutions.leaf.v35.player.decision.baseline.HumanBaselinePolicy
import dugsolutions.leaf.v35.player.decision.baseline.ResourcePersonality
import dugsolutions.leaf.v35.player.decision.baseline.cultivation.resource.CompostLifetimePriority
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.context.DieView
import dugsolutions.leaf.v35.random.die.DieSides
import dugsolutions.leaf.v35.round.domain.RoundCardType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CompostLifetimeValuationTest {
    private val battle = listOf(RoundCardType.BATTLE)

    @Test fun `early D12 to D20 trades one showing pip for four future pips`() {
        val ctx = context(hand = listOf(die(0, 12, 1)), supply = listOf(die(1, 4, 1)),
            upcoming = battle, bed = mapOf(DieSides.D20 to 1))
        val result = CompostLifetimeValuation.compare(ctx, 1)
        val target = result.targets.single()
        assertEquals(20, target.replacementSides)
        assertEquals(1, target.currentSpendingPipsLost)
        assertEquals(4.0, target.futureSpendingPipsChange, 1e-12)
        assertEquals(0.0, target.futureRewardEquivalentChange, 1e-12)
        assertEquals(3.0, target.modeledPipEquivalent, 1e-12)
        assertEquals(2.5, result.drawReference, 1e-12)
        assertTrue(CompostLifetimePriority.score(ctx, 1).total >
            dugsolutions.leaf.v35.player.decision.baseline.cultivation.DrawPriority.score(ctx).total)
    }

    @Test fun `late upgrade has no imaginary future value`() {
        val ctx = context(hand = listOf(die(0, 12, 1)), supply = listOf(die(1, 4, 1)),
            bed = mapOf(DieSides.D20 to 1))
        val target = CompostLifetimeValuation.compare(ctx, 1).targets.single()
        assertEquals(0.0, target.futureSpendingPipsChange, 1e-12)
        assertEquals(-1.0, target.modeledPipEquivalent, 1e-12)
        assertTrue(CompostLifetimePriority.score(ctx, 1).total <
            dugsolutions.leaf.v35.player.decision.baseline.cultivation.DrawPriority.score(ctx).total)
    }

    @Test fun `replacement waiting behind Supply is not credited as a future encounter`() {
        val ctx = context(hand = listOf(die(0, 12, 1)),
            supply = (1..4).map { die(it, 4, 1) }, upcoming = battle,
            bed = mapOf(DieSides.D20 to 1))
        assertEquals(0.0, CompostLifetimeValuation.compare(ctx, 1).targets.single().futureSpendingPipsChange, 1e-12)
    }

    @Test fun `a midgame repeated encounter uses the actual projected cycling`() {
        val ctx = context(hand = listOf(die(0, 12, 1)),
            supply = listOf(die(1, 4, 1)), discard = listOf(die(2, 6, 1)),
            upcoming = listOf(RoundCardType.CULTIVATION, RoundCardType.BATTLE, RoundCardType.CULTIVATION),
            bed = mapOf(DieSides.D20 to 1))
        val result = CompostLifetimeValuation.compare(ctx, 1)
        val original = FutureDiceForecast.fromContext(ctx)
        val changed = FutureDiceForecast.project(
            FutureDiceZones.fromBoard(ctx.self.board).withHandUpgradeIntoDiscard("h0", 20),
            ctx.progress.upcomingRoundTypes,
            rewards = RollRewardOpportunities.fromVisibleContext(ctx)
        )
        assertEquals(changed.expectedSpendingPips - original.expectedSpendingPips,
            result.targets.single().futureSpendingPipsChange, 1e-12)
        assertTrue(result.targets.single().futureSpendingPipsChange > 4.0)
    }

    @Test fun `available next size only no skipping and legal Trash fallback`() {
        val ctx = context(hand = listOf(die(0, 4, 1), die(1, 6, 2), die(2, 20, 1)),
            bed = mapOf(DieSides.D8 to 1), upcoming = battle)
        val targets = CompostLifetimeValuation.compare(ctx, 4).targets
        assertNull(targets[0].replacementSides) // Do not skip missing D6 to reach D8.
        assertEquals(8, targets[1].replacementSides)
        assertTrue(targets[2].isTrash) // D20 has no next normal size.
        assertEquals(3, targets.size) // All Hand choices remain legal.
    }

    @Test fun `separate target scorer and top level select the same die`() {
        val ctx = context(hand = listOf(die(0, 4, 1), die(1, 12, 1)),
            upcoming = battle, bed = mapOf(DieSides.D6 to 1, DieSides.D20 to 1))
        val best = requireNotNull(CompostLifetimePriority.bestTarget(ctx, 2))
        assertEquals(1, best.value.die.index)
        assertEquals(best.score,
            CompostLifetimePriority.targetScore(ctx, best.value.die, 2))
        assertEquals(best.score, CompostLifetimePriority.score(ctx, 2))
    }

    @Test fun `Buy tier loss is explicit and separate from the pip projection`() {
        val ctx = context(hand = listOf(die(0, 12, 5), die(1, 20, 5)),
            upcoming = battle, bed = mapOf(DieSides.D20 to 1, DieSides.D10 to 1))
        val target = CompostLifetimeValuation.compare(ctx, 10).targets[0]
        assertEquals(listOf(10), target.lostAffordableBuyTiers)
        assertEquals(5, target.currentSpendingPipsLost)
        val score = requireNotNull(CompostLifetimePriority.targetScore(ctx, ctx.self.board.hand[0], 10))
        assertTrue(score.adjustments.any { it.reason.contains("Buy tiers lost: 10") })
    }

    @Test fun `identical states have identical math independent of resource personality`() {
        val ctx = context(hand = listOf(die(0, 12, 1)), supply = listOf(die(1, 4, 2)),
            upcoming = battle, bed = mapOf(DieSides.D20 to 1))
        val expected = CompostLifetimeValuation.compare(ctx, HumanBaselinePolicy().normalPurchasingPower(ctx))
        // Every named profile supplies exactly the same math and buy power.
        ResourcePersonality.NAMED.forEach { profile ->
            val policy = HumanBaselinePolicy(resourcePersonality = profile)
            assertEquals(expected, CompostLifetimeValuation.compare(ctx,
                policy.normalPurchasingPower(ctx)))
            assertEquals(CompostLifetimePriority.score(ctx, HumanBaselinePolicy().normalPurchasingPower(ctx)),
                CompostLifetimePriority.score(ctx, policy.normalPurchasingPower(ctx)))
        }
    }

    @Test fun `Draw refill from Discard is modeled without drawing current Hand early`() {
        val ctx = context(hand = listOf(die(0, 12, 1)), discard = listOf(die(1, 4, 2)),
            upcoming = battle, bed = mapOf(DieSides.D20 to 1))
        val result = CompostLifetimeValuation.compare(ctx, 1)
        assertEquals(2.5, result.drawReference, 1e-12)
        assertEquals(4.0, result.targets.single().futureSpendingPipsChange, 1e-12)
    }

    private fun die(index: Int, sides: Int, value: Int) = DieView(index, sides, value)
    private fun context(
        hand: List<DieView> = emptyList(),
        supply: List<DieView> = emptyList(),
        discard: List<DieView> = emptyList(),
        upcoming: List<RoundCardType> = emptyList(),
        bed: Map<DieSides, Int> = emptyMap()
    ): DecisionContext = DecisionContext.EMPTY.copy(
        phase = RoundCardType.CULTIVATION,
        progress = DecisionContext.EMPTY.progress.copy(upcomingRoundTypes = upcoming),
        self = DecisionContext.EMPTY.self.copy(board = DecisionContext.EMPTY.self.board.copy(
            hand = hand, supply = supply, discard = discard
        )),
        grove = DecisionContext.EMPTY.grove.copy(graftBed = bed)
    )
}
