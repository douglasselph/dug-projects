package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.battle.BattleMainAction
import dugsolutions.leaf.v35.player.decision.context.*
import dugsolutions.leaf.v35.round.domain.RoundCard
import dugsolutions.leaf.v35.round.domain.RoundCardEffect
import dugsolutions.leaf.v35.round.domain.RoundCardType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Main stages and Sunlight-funded actions must not use a separate Draw premium. */
class C04MainStageParityTest {
    private val actor = PlayerId(0)
    private val opponent = PlayerId(1)

    @Test
    fun `first and final Main Draw use identical Strike consequence projections`() {
        val context = context(drawSides = 20)
        val round = round()
        val first = BattleFirstMainPriority()(context, round, BattleMainAction.Draw)
        val final = BattleMainPriority.score(context, round, BattleMainAction.Draw)
        assertEquals(first.total, final.total)
        assertEquals(first.adjustments, final.adjustments)
        assertEquals(first.total, BattleMainPriority.intrinsicScore(context, round, BattleMainAction.Draw).total)
    }

    @Test
    fun `without a drawable die no stage awards phantom tactical draw bonus`() {
        val context = context(drawSides = null)
        val round = round()
        val first = BattleFirstMainPriority()(context, round, BattleMainAction.Draw)
        val final = BattleMainPriority.score(context, round, BattleMainAction.Draw)
        val intrinsic = BattleMainPriority.intrinsicScore(context, round, BattleMainAction.Draw)
        assertEquals(intrinsic.total, first.total)
        assertEquals(first.total, final.total)
    }

    private fun context(drawSides: Int?): DecisionContext = DecisionContext.EMPTY.copy(
        phase = RoundCardType.BATTLE,
        self = DecisionContext.EMPTY.self.copy(
            board = DecisionContext.EMPTY.self.board.copy(
                id = actor,
                supply = drawSides?.let { listOf(DieView(0, it, 1)) } ?: emptyList()
            )
        ),
        battle = BattleView(
            playerOrder = listOf(actor, opponent),
            donePlayerIds = emptySet(),
            rows = listOf(
                BattleRowView(StrikeRow.TOP, false, listOf(
                    BattlePlayerRowView(actor, StrikeRow.TOP, emptyList(), emptyList(), 5, 0, 5, false),
                    BattlePlayerRowView(opponent, StrikeRow.TOP, emptyList(), emptyList(), 7, 0, 7, false)
                ))
            )
        )
    )

    private fun round() = RoundCard(
        quantity = 1,
        name = "C04_Stage_Parity",
        type = RoundCardType.BATTLE,
        firstEffect = RoundCardEffect("first", "", "", "", null, GameEffect.GAIN_ONE_VP),
        secondEffect = RoundCardEffect("second", "", "", "", null, GameEffect.GAIN_ONE_WISP),
        backImage = ""
    )
}
