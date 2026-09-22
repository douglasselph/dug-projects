package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.battle.BattleSupportAction
import dugsolutions.leaf.v35.player.decision.battle.BattleTurnAction
import dugsolutions.leaf.v35.player.decision.context.BattlePlayerRowView
import dugsolutions.leaf.v35.player.decision.context.BattleRowView
import dugsolutions.leaf.v35.player.decision.context.BattleView
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.tokens.Critter
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BattleContinuationAssessorTest {
    private val actor = PlayerId(0)
    private val opponent = PlayerId(1)
    private val assessor = BattleContinuationAssessor()

    @Test
    fun `all relevant rows secured commits Final Main even when Support remains`() {
        val result = assessor(
            context = context(actorTotal = 20, opponentTotal = 1, bees = 2),
            legalChoices = listOf(beeChoice()),
            individuallyWorthwhileSupportExists = true
        )

        assertTrue(result.allRelevantRowsSecured)
        assertFalse(result.shouldContinue)
        assertEquals(
            BattleContinuationReason.ALL_RELEVANT_ROWS_SECURED,
            result.reason
        )
    }

    @Test
    fun `no remaining Support commits Final Main`() {
        val result = assessor(
            context = context(actorTotal = 2, opponentTotal = 3),
            legalChoices = emptyList()
        )

        assertFalse(result.hasSupportMoves)
        assertFalse(result.shouldContinue)
        assertEquals(BattleContinuationReason.NO_SUPPORT_MOVES, result.reason)
    }

    @Test
    fun `individually worthwhile Support continues without cumulative meaningful VP`() {
        val result = assessor(
            context = context(actorTotal = 1, opponentTotal = 20, bees = 1),
            legalChoices = listOf(beeChoice()),
            individuallyWorthwhileSupportExists = true
        )

        assertFalse(result.hasMeaningfulCumulativePath)
        assertTrue(result.shouldContinue)
        assertEquals(
            BattleContinuationReason.INDIVIDUALLY_WORTHWHILE_SUPPORT,
            result.reason
        )
    }

    @Test
    fun `meaningful cumulative Bee path continues one Support at a time`() {
        val result = assessor(
            context = context(
                actorTotal = 4,
                opponentTotal = 9,
                bees = 3,
                beeValue = 2
            ),
            legalChoices = listOf(beeChoice())
        )

        assertEquals(3, result.supportCapacity.beeMoves)
        assertTrue(result.hasMeaningfulCumulativePath)
        assertTrue(result.shouldContinue)
        assertEquals(
            BattleContinuationReason.MEANINGFUL_CUMULATIVE_PATH,
            result.reason
        )
    }

    @Test
    fun `remaining Support with no worthwhile immediate or cumulative path commits Final Main`() {
        val result = assessor(
            context = context(actorTotal = 1, opponentTotal = 20, bees = 1),
            legalChoices = listOf(beeChoice())
        )

        assertTrue(result.hasSupportMoves)
        assertFalse(result.hasMeaningfulCumulativePath)
        assertFalse(result.shouldContinue)
        assertEquals(BattleContinuationReason.NO_WORTHWHILE_PATH, result.reason)
    }

    private fun context(
        actorTotal: Int,
        opponentTotal: Int,
        bees: Int = 0,
        beeValue: Int = 1
    ): DecisionContext {
        val board = DecisionContext.EMPTY.self.board.copy(
            id = actor,
            bees = bees,
            beeValue = beeValue
        )
        return DecisionContext.EMPTY.copy(
            self = DecisionContext.EMPTY.self.copy(board = board),
            battle = BattleView(
                playerOrder = listOf(actor, opponent),
                rows = listOf(
                    BattleRowView(
                        row = StrikeRow.TOP,
                        closed = false,
                        players = listOf(
                            playerRow(actor, actorTotal),
                            playerRow(opponent, opponentTotal)
                        )
                    )
                )
            )
        )
    }

    private fun playerRow(playerId: PlayerId, total: Int): BattlePlayerRowView =
        BattlePlayerRowView(
            playerId = playerId,
            row = StrikeRow.TOP,
            dice = emptyList(),
            critters = emptyList(),
            dieTotal = total,
            critterTotal = 0,
            total = total,
            withdrawn = false
        )

    private fun beeChoice(): BattleTurnAction =
        BattleTurnAction.Support(
            BattleSupportAction.PlaceCritter(Critter.BEE, StrikeRow.TOP)
        )
}
