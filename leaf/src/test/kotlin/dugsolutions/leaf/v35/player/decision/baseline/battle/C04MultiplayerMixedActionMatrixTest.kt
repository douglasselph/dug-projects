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
import kotlin.test.assertTrue

/** C04 acceptance matrix: real scoring entry point with three players and three Strike rows. */
class C04MultiplayerMixedActionMatrixTest {
    private val self = PlayerId(0)
    private val rivalOne = PlayerId(1)
    private val rivalTwo = PlayerId(2)
    private val plant = CreatureCard(
        id = CreatureCardId(845),
        card = PlantCard(6, "Root_05_02", "Root Four More", PlantType.ROOT, 5,
            null, "", "", "", "", "", "", "", GameEffect.RAISE_DIE_PLUS_4,
            PlantScoringRule.Fixed(1)),
        side = CreatureSide.LEFT, position = CreaturePosition(-1, 0),
        facing = CreatureCard.Facing.FACE_UP
    )
    private val round = RoundCard(
        quantity = 1, name = "C04 three-player matrix", type = RoundCardType.BATTLE,
        firstEffect = RoundCardEffect("Wisp", "", "", "", null, GameEffect.GAIN_ONE_WISP),
        secondEffect = RoundCardEffect("VP", "", "", "", null, GameEffect.GAIN_ONE_VP),
        backImage = ""
    )
    private fun row(row: StrikeRow, ownFace: Int, opponentA: Int, opponentB: Int) =
        BattleRowView(row, false, listOf(
            BattlePlayerRowView(self, row,
                listOf(BattleDieView(0, 6, ownFace)), emptyList(), ownFace, 0, ownFace, false),
            BattlePlayerRowView(rivalOne, row,
                emptyList(), emptyList(), opponentA, 0, opponentA, false),
            BattlePlayerRowView(rivalTwo, row,
                emptyList(), emptyList(), opponentB, 0, opponentB, false)
        ))
    private fun context(wisps: Int, face: Int, rivalTop: Int) = DecisionContext.EMPTY.copy(
        phase = RoundCardType.BATTLE,
        grove = DecisionContext.EMPTY.grove.copy(wispDeckRemaining = wisps),
        self = DecisionContext.EMPTY.self.copy(board = DecisionContext.EMPTY.self.board.copy(
            id = self, supply = listOf(DieView(0, 4, 1))
        )),
        battle = BattleView(
            playerOrder = listOf(self, rivalOne, rivalTwo),
            rows = listOf(
                row(StrikeRow.TOP, face, rivalTop, rivalTop - 1),
                row(StrikeRow.MIDDLE, 5, 4, 3),
                row(StrikeRow.BOTTOM, 4, 3, 2)
            )
        )
    )
    private fun score(ctx: DecisionContext, action: BattleMainAction) =
        BattleMainPriority.score(ctx, round, action).total

    @Test fun `Wisp availability reverses preference against the same legal Draw in three-row Battle`() {
        val available = context(wisps = 2, face = 6, rivalTop = 8)
        val exhausted = context(wisps = 0, face = 6, rivalTop = 8)
        val draw = score(available, BattleMainAction.Draw)
        val availableWisp = score(available, BattleMainAction.RoundEffect1)
        val exhaustedWisp = score(exhausted, BattleMainAction.RoundEffect1)
        assertEquals(0, exhaustedWisp)
        assertTrue(availableWisp > exhaustedWisp)
        assertEquals(draw, score(exhausted, BattleMainAction.Draw))
        assertTrue(draw > exhaustedWisp)
    }

    @Test fun `positive capped die effect changes Plant valuation while Round immediate VP remains stable`() {
        val effective = context(wisps = 0, face = 2, rivalTop = 5)
        val capped = context(wisps = 0, face = 6, rivalTop = 9)
        val activate = BattleMainAction.ActivatePlant(plant)
        val usefulValue = score(effective, activate)
        val cappedValue = score(capped, activate)
        assertTrue(usefulValue > cappedValue,
            "The projected Raise should respond to its actual capped face change")
        assertEquals(38, score(effective, BattleMainAction.RoundEffect2))
        assertEquals(38, score(capped, BattleMainAction.RoundEffect2))
    }

    @Test fun `First and Final Main compare the same Battle Draw consequence in multirow context`() {
        val state = context(wisps = 0, face = 2, rivalTop = 5)
        val draw = BattleMainAction.Draw
        val first = BattleFirstMainPriority()(state, round, draw)
        val ordinary = BattleMainPriority.score(state, round, draw)
        val final = BattleMainPriority.finalMainScore(state, round, draw)
        assertEquals(first.total, ordinary.total)
        assertEquals(first.total, final.total)
    }
}
