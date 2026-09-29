package dugsolutions.leaf.v35.player.decision.baseline.context

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.plant.domain.PlantScoringRule
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.creature.CreatureCard
import dugsolutions.leaf.v35.player.creature.CreatureCardId
import dugsolutions.leaf.v35.player.creature.CreaturePosition
import dugsolutions.leaf.v35.player.creature.CreatureSide
import dugsolutions.leaf.v35.player.decision.context.CreatureCardView
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.round.domain.RoundCardType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FirstPlantCalibrationSharedValuesTest {
    @Test
    fun `immediate refresh cancels Battle preservation and creates positive shared value`() {
        val root = card(1, "Root_05_02", GameEffect.RAISE_DIE_PLUS_4, CreatureCard.Facing.FACE_UP)
        val queen = card(2, "Flower_17_04", GameEffect.DRAW_TWO_DICE, CreatureCard.Facing.FACE_DOWN)
        val context = context(root, queen)
        val refreshValue = CreatureRefreshValue()
        val refresh = refreshValue.observe(context, root)
        val preservation = PhasePreservationValue().observe(context, root)

        assertTrue(refresh.immediateRefresh)
        assertTrue(refreshValue.priorityAdjustment(refresh) > 0)
        assertEquals(0, PhasePreservationValue().priorityAdjustment(preservation))
    }

    @Test
    fun `Queen has stronger Battle preservation pressure than Root Four More`() {
        val root = card(1, "Root_05_02", GameEffect.RAISE_DIE_PLUS_4, CreatureCard.Facing.FACE_UP)
        val queen = card(2, "Flower_17_04", GameEffect.DRAW_TWO_DICE, CreatureCard.Facing.FACE_UP)
        val third = card(3, "Root_09_02", GameEffect.FLIP_OWN_DIE_TO_OPPOSITE_FACE, CreatureCard.Facing.FACE_UP)
        val context = context(root, queen, third)
        val preservation = PhasePreservationValue()

        val rootPenalty = preservation.priorityAdjustment(preservation.observe(context, root))
        val queenPenalty = preservation.priorityAdjustment(preservation.observe(context, queen))
        assertTrue(rootPenalty < 0)
        assertTrue(queenPenalty < rootPenalty)
    }

    private fun context(vararg cards: CreatureCardView): DecisionContext {
        val base = DecisionContext.EMPTY
        return base.copy(
            phase = RoundCardType.CULTIVATION,
            progress = base.progress.copy(upcomingRoundTypes = listOf(RoundCardType.BATTLE)),
            self = base.self.copy(board = base.self.board.copy(creature = cards.toList()))
        )
    }

    private fun card(id: Int, name: String, effect: GameEffect, facing: CreatureCard.Facing) = CreatureCardView(
        id = CreatureCardId(id), name = name, title = name,
        type = if (name.startsWith("Flower")) PlantType.FLOWER else PlantType.ROOT,
        cost = if (name.startsWith("Flower")) 17 else if (name.startsWith("Root_09")) 9 else 5,
        effect = effect, scoringRule = PlantScoringRule.Fixed(1), side = CreatureSide.LEFT,
        position = CreaturePosition(-id, 0), facing = facing, isSnippable = true
    )
}
