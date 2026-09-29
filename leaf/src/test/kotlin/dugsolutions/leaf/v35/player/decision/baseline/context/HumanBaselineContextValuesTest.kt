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
import dugsolutions.leaf.v35.player.decision.context.DieView
import dugsolutions.leaf.v35.round.domain.RoundCardType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HumanBaselineContextValuesTest {
    @Test
    fun `phase proximity exposes public next phase without a card identity`() {
        val context = DecisionContext.EMPTY.copy(
            progress = DecisionContext.EMPTY.progress.copy(
                upcomingRoundTypes = listOf(RoundCardType.BATTLE, RoundCardType.CULTIVATION)
            )
        )
        assertTrue(PhaseProximity.battleIsNext(context))
        assertEquals(1, PhaseProximity.roundsUntilBattle(context))
    }

    @Test
    fun `refresh observation distinguishes immediate and two-action opportunities`() {
        val root = card(1, "Root_05_02", CreatureCard.Facing.FACE_UP)
        val queen = card(2, "Flower_17_04", CreatureCard.Facing.FACE_DOWN)
        val immediate = context(root, queen)
        val observation = CreatureRefreshValue().observe(immediate, root)
        assertTrue(observation.immediateRefresh)
        assertFalse(observation.twoActionRefreshOpportunity)
        assertTrue("Flower_17_04" in observation.restoredCards)

        val second = card(3, "Root_09_02", CreatureCard.Facing.FACE_UP)
        val twoAction = context(root, second, queen)
        val twoObservation = CreatureRefreshValue().observe(twoAction, root)
        assertFalse(twoObservation.immediateRefresh)
        assertTrue(twoObservation.twoActionRefreshOpportunity)
    }

    @Test
    fun `future availability counts supply and lower-sided discard dice ahead`() {
        val discard = listOf(DieView(0, 8, 3), DieView(1, 20, 4), DieView(2, 12, 5))
        val base = DecisionContext.EMPTY
        val context = base.copy(
            progress = base.progress.copy(upcomingRoundTypes = listOf(RoundCardType.BATTLE)),
            self = base.self.copy(board = base.self.board.copy(
                supply = listOf(DieView(0, 4, 2), DieView(1, 6, 3)),
                discard = discard
            ))
        )
        val observed = FutureDiceAvailability.forDiscardDie(context, discard[1])
        assertEquals(2, observed.supplyDiceBeforeRecycle)
        assertEquals(2, observed.lowerSidedDiscardDiceAhead)
        assertEquals(5, observed.approximateDrawsUntilAvailable)
        assertTrue(observed.battleIsNext)
    }

    @Test
    fun `pool quality reports weak-die dilution without making a random cleanup decision`() {
        val base = DecisionContext.EMPTY
        val context = base.copy(self = base.self.copy(board = base.self.board.copy(
            supply = listOf(DieView(0, 4, 2), DieView(1, 6, 3), DieView(2, 20, 10))
        )))
        val observed = DicePoolQuality.observe(context)
        assertEquals(3, observed.ownedDiceCount)
        assertEquals(1, observed.d4Count)
        assertEquals(1, observed.d6Count)
        assertEquals(2.0 / 3.0, observed.weakDieFraction)
    }

    private fun context(vararg cards: CreatureCardView): DecisionContext {
        val base = DecisionContext.EMPTY
        return base.copy(
            progress = base.progress.copy(upcomingRoundTypes = listOf(RoundCardType.BATTLE)),
            self = base.self.copy(board = base.self.board.copy(creature = cards.toList()))
        )
    }

    private fun card(id: Int, name: String, facing: CreatureCard.Facing) = CreatureCardView(
        id = CreatureCardId(id),
        name = name,
        title = name,
        type = if (name.startsWith("Flower")) PlantType.FLOWER else PlantType.ROOT,
        cost = if (name.startsWith("Flower")) 17 else 9,
        effect = when (name) {
            "Flower_17_04" -> GameEffect.DRAW_TWO_DICE
            "Root_09_02" -> GameEffect.FLIP_OWN_DIE_TO_OPPOSITE_FACE
            else -> GameEffect.RAISE_DIE_PLUS_4
        },
        scoringRule = PlantScoringRule.Fixed(1),
        side = CreatureSide.LEFT,
        position = CreaturePosition(-id, 0),
        facing = facing,
        isSnippable = true
    )
}
