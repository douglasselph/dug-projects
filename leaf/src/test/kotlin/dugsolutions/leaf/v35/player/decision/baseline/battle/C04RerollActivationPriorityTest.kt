package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.context.*
import dugsolutions.leaf.v35.round.domain.RoundCardType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class C04RerollActivationPriorityTest {
    private val actor = PlayerId(0)
    private val opponent = PlayerId(1)
    private val effect = GameEffect.REROLL_DIE_UNTIL_3_PLUS_IGNORE_ROLL_REWARDS

    @Test
    fun `reroll tactical value averages outcomes after evaluating each face`() {
        val context = context(face = 1, sides = 6)
        val analyzer = BattleOwnTotalChangeAnalyzer()
        val expected = (3..6).map { face ->
            assertNotNull(
                analyzer(context, 0, StrikeRow.TOP, (face - 1).toDouble(), BattleAnalysisMode.EXPECTED)
            ).tacticalValue
        }.average()
        assertEquals(expected, assertNotNull(BattleRerollActivationPriority().bestTacticalValue(context, effect)), 1e-9)
    }

    @Test
    fun `unsupported compound rerolls never receive a partial tactical value`() {
        val context = context(face = 1, sides = 6)
        assertNull(BattleRerollActivationPriority().bestTacticalValue(
            context, GameEffect.REROLL_ONE_DIE_AND_REROLL_HIGHER_OPPOSING_DICE_IN_STRIKE_ROW
        ))
    }

    @Test
    fun `empty Battle board has no invented reroll target`() {
        assertNull(BattleRerollActivationPriority().bestTacticalValue(DecisionContext.EMPTY, effect))
    }

    private fun context(face: Int, sides: Int) = DecisionContext.EMPTY.copy(
        phase = RoundCardType.BATTLE,
        self = DecisionContext.EMPTY.self.copy(board = DecisionContext.EMPTY.self.board.copy(id = actor)),
        battle = BattleView(
            playerOrder = listOf(actor, opponent),
            donePlayerIds = emptySet(),
            rows = listOf(BattleRowView(StrikeRow.TOP, false, listOf(
                BattlePlayerRowView(actor, StrikeRow.TOP,
                    listOf(BattleDieView(0, sides, face)), emptyList(), face, 0, face, false),
                BattlePlayerRowView(opponent, StrikeRow.TOP,
                    emptyList(), emptyList(), 4, 0, 4, false)
            )))
        )
    )
}
