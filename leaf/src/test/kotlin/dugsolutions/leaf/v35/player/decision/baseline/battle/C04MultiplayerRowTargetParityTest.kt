package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.baseline.effect.HumanBaselineEffectStrategy
import dugsolutions.leaf.v35.player.decision.context.*
import dugsolutions.leaf.v35.player.decision.effect.ChooseEffectStrikeRowRequest
import dugsolutions.leaf.v35.round.domain.RoundCardType
import org.junit.jupiter.api.Test
import kotlin.math.roundToInt
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Multirow, three-player acceptance case using the real downstream row selector. */
class C04MultiplayerRowTargetParityTest {
    private val actor = PlayerId(0)
    private val rival = PlayerId(1)
    private val third = PlayerId(2)
    private val effect = GameEffect.SET_ANY_DIE_TO_3_OR_REDUCE_OPPOSING_STRIKE_ROW_BY_3

    private fun battleRow(row: StrikeRow, own: Int, opponent: Int, other: Int): BattleRowView =
        BattleRowView(row, false, listOf(
            BattlePlayerRowView(actor, row, emptyList(), emptyList(), own, 0, own, false),
            BattlePlayerRowView(rival, row, emptyList(), emptyList(), opponent, 0, opponent, false),
            BattlePlayerRowView(third, row, emptyList(), emptyList(), other, 0, other, false)
        ))

    private fun context(): DecisionContext = DecisionContext.EMPTY.copy(
        phase = RoundCardType.BATTLE,
        self = DecisionContext.EMPTY.self.copy(board = DecisionContext.EMPTY.self.board.copy(id = actor)),
        battle = BattleView(playerOrder = listOf(actor, rival, third), rows = listOf(
            battleRow(StrikeRow.TOP, 6, 9, 4),
            battleRow(StrikeRow.MIDDLE, 5, 7, 9),
            battleRow(StrikeRow.BOTTOM, 8, 8, 7)
        ))
    )

    @Test fun `real row selector maximizes same complete activation projections`() {
        val state = context()
        val projections = BattleCollateralActivationPriority().targetProjections(state, effect)
        assertEquals(3, projections.size)
        assertEquals(3, projections.mapNotNull { it.strikeRow }.toSet().size)
        val bestRounded = projections.maxOf { it.tacticalValue.roundToInt() }
        val permitted = projections.filter { it.tacticalValue.roundToInt() == bestRounded }
            .mapNotNull { it.strikeRow }.toSet()
        val chosen = HumanBaselineEffectStrategy().chooseStrikeRow(
            ChooseEffectStrikeRowRequest(effect, listOf(StrikeRow.TOP, StrikeRow.MIDDLE, StrikeRow.BOTTOM), state)
        )
        assertTrue(chosen in permitted, "Selected row must maximize same complete projection as activation")
        assertEquals(projections.maxOf { it.tacticalValue },
            BattleCollateralActivationPriority().bestTacticalValue(state, effect))
    }

    @Test fun `restricted legal row selection cannot choose unavailable best row`() {
        val state = context()
        val projections = BattleCollateralActivationPriority().targetProjections(state, effect)
        val excluded = projections.maxBy { it.tacticalValue }.strikeRow!!
        val remaining = listOf(StrikeRow.TOP, StrikeRow.MIDDLE, StrikeRow.BOTTOM).filterNot { it == excluded }
        val eligible = projections.filter { it.strikeRow in remaining }
        val maximum = eligible.maxOf { it.tacticalValue.roundToInt() }
        val chosen = HumanBaselineEffectStrategy().chooseStrikeRow(
            ChooseEffectStrikeRowRequest(effect, remaining, state)
        )
        assertTrue(chosen in remaining)
        assertEquals(maximum, eligible.single { it.strikeRow == chosen }.tacticalValue.roundToInt())
    }
}
