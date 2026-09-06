package dugsolutions.leaf.v35.player.decision.baseline.common

import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.context.GameProgressView
import dugsolutions.leaf.v35.round.domain.RoundCardType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ResourceReserveHeuristicsTest {

    @Test
    fun status_reportsDeficitAndSurplus() {
        val status = ResourceReserveStatus(
            resource = ReserveResource.WATER,
            current = 0,
            target = 2
        )

        assertEquals(2, status.deficit)
        assertEquals(0, status.surplus)
        assertEquals(2, status.unitsBelowReserveAfterSpend(0))
    }

    @Test
    fun acquireBonus_onlyRewardsFillingReserveDeficit() {
        val missingTwo = ResourceReserveStatus(ReserveResource.WORM, 0, 2)
        val stocked = ResourceReserveStatus(ReserveResource.WORM, 3, 2)

        assertEquals(10, ResourceReserveHeuristics.acquireBonus(missingTwo, amount = 1))
        assertEquals(0, ResourceReserveHeuristics.acquireBonus(stocked, amount = 1))
    }

    @Test
    fun spendPenalty_appliesOnlyWhenSpendWouldLeaveResourceBelowReserve() {
        val atReserve = ResourceReserveStatus(ReserveResource.BEE, current = 1, target = 1)
        val aboveReserve = ResourceReserveStatus(ReserveResource.BEE, current = 2, target = 1)

        assertEquals(-10, ResourceReserveHeuristics.spendPenalty(atReserve))
        assertEquals(0, ResourceReserveHeuristics.spendPenalty(aboveReserve))
    }

    @Test
    fun finalBattle_reservesNothing() {
        val context = DecisionContext.EMPTY.copy(
            phase = RoundCardType.BATTLE,
            progress = GameProgressView.EMPTY.copy(
                currentBattleRoundNumber = 4,
                totalBattleRounds = 4,
                isFinalBattleRound = true
            )
        )

        assertEquals(
            ResourceReserveTargets(),
            ResourceReserveHeuristics.targets(context)
        )
    }
}
