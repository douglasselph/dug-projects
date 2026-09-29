package dugsolutions.leaf.v35.player.decision.baseline.cultivation.resource

import dugsolutions.leaf.v35.player.decision.baseline.HumanBaselinePolicy
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.context.DieView
import dugsolutions.leaf.v35.player.decision.context.GameProgressView
import dugsolutions.leaf.v35.player.decision.context.MulchView
import dugsolutions.leaf.v35.random.die.DieSides
import dugsolutions.leaf.v35.round.domain.RoundCardType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MulchCompostDecisionQualityTest {
    @Test
    fun `Mulch grades current rolls without a hard five cutoff`() {
        val low = die(0, 12, 2)
        val five = die(1, 12, 5)
        val six = die(2, 12, 6)
        val context = context(hand = listOf(low, five, six))

        val lowScore = MulchPriority.targetScore(context, low, 20).total
        val fiveScore = MulchPriority.targetScore(context, five, 20).total
        val sixScore = MulchPriority.targetScore(context, six, 20).total

        assertTrue(lowScore > fiveScore)
        assertTrue(fiveScore > sixScore)
        assertTrue(sixScore > -1000)
    }

    @Test
    fun `Mulch values a high sided die more strongly when Battle is next`() {
        val d6 = die(0, 6, 5)
        val d20 = die(1, 20, 5)
        val battleNext = context(hand = listOf(d6, d20), upcoming = listOf(RoundCardType.BATTLE))
        val laterBattle = context(hand = listOf(d6, d20), upcoming = listOf(RoundCardType.CULTIVATION, RoundCardType.BATTLE))

        val d20Battle = MulchPriority.targetScore(battleNext, d20, 20).total
        val d20Later = MulchPriority.targetScore(laterBattle, d20, 20).total
        val d6Battle = MulchPriority.targetScore(battleNext, d6, 20).total

        assertTrue(d20Battle > d20Later)
        assertTrue(d20Battle > d6Battle)
    }

    @Test
    fun `Mulch Battle value increases when naturally upcoming dice are weak`() {
        val target = die(0, 20, 5)
        val weakUpcoming = context(
            hand = listOf(target),
            supply = listOf(die(1, 4, 2), die(2, 6, 3)),
            upcoming = listOf(RoundCardType.BATTLE)
        )
        val strongUpcoming = context(
            hand = listOf(target),
            supply = listOf(die(1, 20, 10), die(2, 20, 11)),
            upcoming = listOf(RoundCardType.BATTLE)
        )

        assertTrue(
            MulchPriority.targetScore(weakUpcoming, target, 20).total >
                MulchPriority.targetScore(strongUpcoming, target, 20).total
        )
    }

    @Test
    fun `Mulch score rewards having little prepared Mulch`() {
        val target = die(0, 12, 3)
        val empty = context(hand = listOf(target))
        val prepared = context(
            hand = listOf(target),
            mulch = listOf(MulchView(0, DieSides.D12, false), MulchView(1, DieSides.D10, false))
        )

        assertEquals(MulchPriority.score(prepared, 20).total + 10, MulchPriority.score(empty, 20).total)
    }

    @Test
    fun `Mulch exposes approximate natural recycle timing`() {
        val target = die(4, 12, 3)
        val context = context(
            hand = listOf(target),
            supply = listOf(die(0, 4, 1), die(1, 8, 4)),
            discard = listOf(die(2, 6, 3), die(3, 20, 7))
        )

        assertEquals(4, MulchPriority.timingObservation(context, target).approximateNaturalRecycleDraws)
    }

    @Test
    fun `Mulch retains low probability weak die cleanup instead of forbidding it`() {
        val policy = HumanBaselinePolicy()
        val weak = die(0, 4, 6)
        val context = context(hand = listOf(weak))

        assertTrue(policy.mulchUsePercentage(context, weak) in 1..20)
    }

    @Test
    fun `Compost certification exposes existing permanent upgrade timing and guardrail concepts`() {
        val target = die(0, 4, 2)
        val context = context(
            hand = listOf(target, die(1, 20, 2)),
            upcoming = listOf(RoundCardType.BATTLE),
            cultivationRoundsRemaining = 3,
            graftBed = mapOf(DieSides.D6 to 1)
        )
        val observation = CompostPriority.certificationObservation(context, 4, developmentBonus = 7)

        assertEquals(target, observation.target)
        assertEquals(2, observation.permanentUpgradeSides)
        assertEquals(2, observation.currentRollOpportunityCost)
        assertEquals(3, observation.cultivationRoundsRemaining)
        assertTrue(observation.battleIsNext)
        assertTrue(observation.belowBuyPowerGuardrail)
        assertEquals(7, observation.developmentBonus)
        assertTrue(observation.score.adjustments.any { it.reason == "Current-round Buy threshold impact" })
    }

    private fun die(index: Int, sides: Int, value: Int) = DieView(index, sides, value)

    private fun context(
        hand: List<DieView> = emptyList(),
        supply: List<DieView> = emptyList(),
        discard: List<DieView> = emptyList(),
        mulch: List<MulchView> = emptyList(),
        upcoming: List<RoundCardType> = emptyList(),
        cultivationRoundsRemaining: Int? = null,
        graftBed: Map<DieSides, Int> = emptyMap()
    ): DecisionContext = DecisionContext.EMPTY.copy(
        phase = RoundCardType.CULTIVATION,
        progress = GameProgressView.EMPTY.copy(
            cultivationRoundsRemaining = cultivationRoundsRemaining,
            upcomingRoundTypes = upcoming
        ),
        self = DecisionContext.EMPTY.self.copy(
            board = DecisionContext.EMPTY.self.board.copy(
                hand = hand,
                supply = supply,
                discard = discard,
                mulch = mulch
            )
        ),
        grove = DecisionContext.EMPTY.grove.copy(graftBed = graftBed)
    )
}
