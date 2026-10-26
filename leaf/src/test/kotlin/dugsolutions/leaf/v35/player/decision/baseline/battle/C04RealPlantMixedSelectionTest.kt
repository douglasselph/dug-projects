package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.plant.domain.PlantScoringRule
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.creature.CreatureCard
import dugsolutions.leaf.v35.player.creature.CreatureCardId
import dugsolutions.leaf.v35.player.creature.CreaturePosition
import dugsolutions.leaf.v35.player.creature.CreatureSide
import dugsolutions.leaf.v35.player.decision.battle.BattleMainAction
import dugsolutions.leaf.v35.player.decision.context.*
import dugsolutions.leaf.v35.round.domain.RoundCard
import dugsolutions.leaf.v35.round.domain.RoundCardEffect
import dugsolutions.leaf.v35.round.domain.RoundCardType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** C04: a real Plant action, Round action and Draw compete at the Main scoring boundary. */
class C04RealPlantMixedSelectionTest {
    private val actor = PlayerId(0)
    private val rival = PlayerId(1)
    private val plant = CreatureCard(
        id = CreatureCardId(45),
        card = PlantCard(6, "Root_05_02", "Root Four More", PlantType.ROOT, 5,
            null, "", "", "", "", "", "", "", GameEffect.RAISE_DIE_PLUS_4,
            PlantScoringRule.Fixed(1)),
        side = CreatureSide.LEFT,
        position = CreaturePosition(-1, 0),
        facing = CreatureCard.Facing.FACE_UP
    )
    private val round = RoundCard(
        quantity = 1, name = "C04 mixed real Plant", type = RoundCardType.BATTLE,
        firstEffect = RoundCardEffect("one VP", "", "", "", null, GameEffect.GAIN_ONE_VP),
        secondEffect = RoundCardEffect("Wisp", "", "", "", null, GameEffect.GAIN_ONE_WISP),
        backImage = ""
    )

    private fun state(face: Int, opponentTotal: Int): DecisionContext = DecisionContext.EMPTY.copy(
        phase = RoundCardType.BATTLE,
        grove = DecisionContext.EMPTY.grove.copy(wispDeckRemaining = 0),
        self = DecisionContext.EMPTY.self.copy(board = DecisionContext.EMPTY.self.board.copy(
            id = actor, supply = listOf(DieView(0, 4, 1))
        )),
        battle = BattleView(
            playerOrder = listOf(actor, rival),
            rows = listOf(BattleRowView(StrikeRow.TOP, false, listOf(
                BattlePlayerRowView(actor, StrikeRow.TOP,
                    listOf(BattleDieView(0, 6, face)), emptyList(), face, 0, face, false),
                BattlePlayerRowView(rival, StrikeRow.TOP,
                    emptyList(), emptyList(), opponentTotal, 0, opponentTotal, false)
            )))
        )
    )

    @Test fun `real Plant projection uses exactly the row consequence in mixed scoring`() {
        val context = state(2, 4)
        val projected = assertNotNull(BattleDirectDieActivationPriority()
            .bestTacticalValue(context, GameEffect.RAISE_DIE_PLUS_4))
        val actual = BattleMainPriority.score(context, round, BattleMainAction.ActivatePlant(plant))
        val expected = BattleProjectedActionScore.fromCompleteProjection(projected, "same outcome")
        assertEquals(expected.total, actual.total)
        // A non-projectable Wisp gain must not win merely because the Round card has an old priority.
        val exhausted = BattleMainPriority.score(context, round, BattleMainAction.RoundEffect2)
        assertEquals(0, exhausted.total)
        assertTrue(actual.total > exhausted.total)
    }

    @Test fun `mixed Main score ordering depends on legal row outcomes rather than Plant name`() {
        val contested = state(2, 4)
        val capped = state(6, 8)
        val activate = BattleMainAction.ActivatePlant(plant)
        val contestedScore = BattleMainPriority.score(contested, round, activate).total
        val cappedScore = BattleMainPriority.score(capped, round, activate).total
        val contestedDraw = BattleMainPriority.score(contested, round, BattleMainAction.Draw).total
        val contestedVp = BattleMainPriority.score(contested, round, BattleMainAction.RoundEffect1).total
        assertEquals(38, contestedVp)
        assertTrue(contestedDraw > 0)
        assertTrue(contestedScore > 0)
        // A capped Raise cannot generate the same improvement as a legal winning Raise.
        assertTrue(contestedScore > cappedScore)
    }
}
