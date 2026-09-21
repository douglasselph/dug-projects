package dugsolutions.leaf.v35.player.decision.baseline

import dugsolutions.leaf.v35.player.decision.baseline.common.ReserveResource
import dugsolutions.leaf.v35.player.decision.baseline.common.ResourceReserveTargets
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.context.DieView
import dugsolutions.leaf.v35.player.decision.context.GameProgressView
import dugsolutions.leaf.v35.round.domain.RoundCardType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class HumanBaselinePolicyTest {

    @Test
    fun `default protected Critter reserve is centralized at two Bees and one Worm`() {
        val reserve = HumanBaselinePolicy().protectedCritterReserve(context())

        assertEquals(2, reserve.bees)
        assertEquals(1, reserve.worms)
    }

    @Test
    fun `normal purchasing power excludes protected Critters and includes only surplus`() {
        val context = context(
            hand = listOf(DieView(0, 6, 6)),
            bees = 3,
            worms = 2
        )

        // 6 dice + one surplus Bee worth 2 + one surplus Worm worth 1.
        assertEquals(9, HumanBaselinePolicy().normalPurchasingPower(context))
    }

    @Test
    fun `normal purchasing power honors an overridden reserve policy`() {
        val context = context(
            hand = listOf(DieView(0, 6, 6)),
            bees = 2,
            worms = 1
        )
        val policy = object : HumanBaselinePolicy() {
            override fun protectedCritterReserve(context: DecisionContext) =
                ResourceReserveTargets(bees = 1, worms = 0)
        }

        // The base implementation calls the overridable reserve method rather
        // than reading companion defaults directly: 6 + Bee(2) + Worm(1) = 9.
        assertEquals(9, policy.normalPurchasingPower(context))
    }

    @Test
    fun `Cultivation dice development bonus is a modest capped nudge`() {
        val policy = HumanBaselinePolicy()
        val context = context(
            progress = GameProgressView.EMPTY.copy(
                currentCultivationRoundNumber = 1,
                totalCultivationRounds = 8
            )
        )

        assertEquals(9, policy.cultivationDiceDevelopmentBonus(context))
    }

    @Test
    fun `constructor knobs can tune Cultivation policy without changing production constants`() {
        val policy = HumanBaselinePolicy(
            cultivationDiceDeficitMaxBonusValue = 5,
            cultivationDoneScoreValue = 63,
            cultivationReserveSpendPenaltyPerUnitValue = 12
        )
        val context = context(
            progress = GameProgressView.EMPTY.copy(currentCultivationRoundNumber = 1)
        )

        assertEquals(5, policy.cultivationDiceDevelopmentBonus(context))
        assertEquals(63, policy.cultivationDoneScore(context))
        assertEquals(12, policy.cultivationReserveSpendPenaltyPerUnit(context, ReserveResource.WATER))
    }

    private fun context(
        hand: List<DieView> = emptyList(),
        bees: Int = 0,
        worms: Int = 0,
        progress: GameProgressView = GameProgressView.EMPTY
    ): DecisionContext = DecisionContext.EMPTY.copy(
        phase = RoundCardType.CULTIVATION,
        progress = progress,
        self = DecisionContext.EMPTY.self.copy(
            board = DecisionContext.EMPTY.self.board.copy(
                hand = hand,
                bees = bees,
                worms = worms
            )
        )
    )
}
