package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.baseline.HumanBaselinePolicy
import dugsolutions.leaf.v35.player.decision.context.BattleDieView
import dugsolutions.leaf.v35.player.decision.context.BattlePlayerRowView
import dugsolutions.leaf.v35.player.decision.context.BattleRowView
import dugsolutions.leaf.v35.player.decision.context.BattleView
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BattleSupportReachabilityTest {
    private val actor = PlayerId(0)
    private val opponent = PlayerId(1)

    @Test
    fun `multiple Bees use current Bee value and make a winning VP path reachable`() {
        val context = context(
            actorTotal = 4,
            opponentTotal = 9,
            beeValue = 2
        )

        val result = BattleSupportReachability()(
            context = context,
            capacity = BattleSupportCapacity(beeMoves = 3)
        ).rows.single()

        assertEquals(6.0, result.beeImprovement)
        assertEquals(0.0, result.expectedButterflyImprovement)
        assertEquals(6.0, result.cumulativeImprovement)
        assertEquals(2, result.projectedVpGain)
        assertTrue(result.meaningfulResultReachable)
        assertEquals(BattleAnalysisMode.DETERMINISTIC, result.analysis.mode)
    }

    @Test
    fun `Butterfly uses expected keep-better gain without rolling`() {
        val context = context(
            actorTotal = 1,
            opponentTotal = 3,
            actorDice = listOf(BattleDieView(handIndex = 0, sides = 6, value = 1))
        )

        val result = BattleSupportReachability()(
            context = context,
            capacity = BattleSupportCapacity(butterflyMoves = 1)
        ).rows.single()

        assertEquals(2.5, result.expectedButterflyImprovement)
        assertEquals(2.5, result.cumulativeImprovement)
        assertEquals(3.5, result.projectedOwnTotal)
        assertEquals(2, result.projectedVpGain)
        assertTrue(result.meaningfulResultReachable)
        assertEquals(BattleAnalysisMode.EXPECTED, result.analysis.mode)
    }

    @Test
    fun `multiple Butterfly estimate is capped by physical die headroom`() {
        val context = context(
            actorTotal = 1,
            opponentTotal = 5,
            actorDice = listOf(BattleDieView(handIndex = 0, sides = 4, value = 1))
        )

        val result = BattleSupportReachability()(
            context = context,
            capacity = BattleSupportCapacity(butterflyMoves = 4)
        ).rows.single()

        assertEquals(3.0, result.expectedButterflyImprovement)
        assertEquals(4.0, result.projectedOwnTotal)
    }

    @Test
    fun `ordinary reachability does not generically add Worm Water Mulch or Wisp capacity`() {
        val result = BattleSupportReachability()(
            context = context(actorTotal = 1, opponentTotal = 10),
            capacity = BattleSupportCapacity(
                wispMoves = 3,
                waterMoves = 3,
                mulchMoves = 3,
                wormMoves = 3
            )
        ).rows.single()

        assertEquals(0.0, result.cumulativeImprovement)
        assertEquals(0, result.projectedVpGain)
        assertFalse(result.meaningfulResultReachable)
    }

    @Test
    fun `policy threshold controls whether projected VP gain is meaningful`() {
        val policy = HumanBaselinePolicy(battleMinimumMeaningfulVpGainValue = 3)
        val assessment = BattleSupportReachability(policy)(
            context = context(actorTotal = 4, opponentTotal = 9, beeValue = 2),
            capacity = BattleSupportCapacity(beeMoves = 3)
        )

        assertEquals(3, assessment.minimumMeaningfulVpGain)
        assertEquals(2, assessment.rows.single().projectedVpGain)
        assertFalse(assessment.hasMeaningfulPath)
    }

    @Test
    fun `secured rows are excluded from cumulative commitment projection`() {
        val assessment = BattleSupportReachability()(
            context = context(actorTotal = 20, opponentTotal = 1),
            capacity = BattleSupportCapacity(beeMoves = 4, butterflyMoves = 2)
        )

        assertTrue(assessment.rows.isEmpty())
        assertFalse(assessment.hasMeaningfulPath)
    }

    private fun context(
        actorTotal: Int,
        opponentTotal: Int,
        beeValue: Int = 1,
        actorDice: List<BattleDieView> = emptyList()
    ): DecisionContext {
        val board = DecisionContext.EMPTY.self.board.copy(
            id = actor,
            beeValue = beeValue
        )
        return DecisionContext.EMPTY.copy(
            self = DecisionContext.EMPTY.self.copy(board = board),
            battle = BattleView(
                playerOrder = listOf(actor, opponent),
                rows = listOf(
                    row(actorTotal, opponentTotal, actorDice)
                )
            )
        )
    }

    private fun row(
        actorTotal: Int,
        opponentTotal: Int,
        actorDice: List<BattleDieView>
    ): BattleRowView =
        BattleRowView(
            row = StrikeRow.TOP,
            closed = false,
            players = listOf(
                BattlePlayerRowView(
                    playerId = actor,
                    row = StrikeRow.TOP,
                    dice = actorDice,
                    critters = emptyList(),
                    dieTotal = actorTotal,
                    critterTotal = 0,
                    total = actorTotal,
                    withdrawn = false
                ),
                BattlePlayerRowView(
                    playerId = opponent,
                    row = StrikeRow.TOP,
                    dice = emptyList(),
                    critters = emptyList(),
                    dieTotal = opponentTotal,
                    critterTotal = 0,
                    total = opponentTotal,
                    withdrawn = false
                )
            )
        )
}
