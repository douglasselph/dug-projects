package dugsolutions.leaf.v35.player.decision.baseline.wound

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.plant.domain.PlantScoringRule
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.creature.CreatureCard
import dugsolutions.leaf.v35.player.creature.CreatureCardId
import dugsolutions.leaf.v35.player.creature.CreaturePosition
import dugsolutions.leaf.v35.player.creature.CreatureSide
import dugsolutions.leaf.v35.player.decision.context.CreatureCardView
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.wound.WoundChoice
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class WoundPriorityTest {
    @Test
    fun `lower value Plant is preferred as wound victim`() {
        val weak = creature(1, "Vine_07_01", "Berry Important", GameEffect.RAISE_ANY_DIE_PLUS_1, 7)
        val strong = creature(2, "Flower_17_04", "Queen's Blossom", GameEffect.DRAW_TWO_DICE, 17)
        val context = DecisionContext.EMPTY.copy(
            self = DecisionContext.EMPTY.self.copy(
                board = DecisionContext.EMPTY.self.board.copy(
                    creature = listOf(view(weak), view(strong))
                )
            )
        )
        val weakScore = WoundPriority.score(context, WoundChoice.Flip(weak)).total
        val strongScore = WoundPriority.score(context, WoundChoice.Flip(strong)).total
        assertTrue(weakScore > strongScore)
    }

    private fun creature(id: Int, name: String, title: String, effect: GameEffect, cost: Int) = CreatureCard(
        id = CreatureCardId(id),
        card = PlantCard(6, name, title, if (name.startsWith("Flower")) PlantType.FLOWER else PlantType.VINE, cost, null, "", "", "", "", "", "", "", effect, PlantScoringRule.Fixed(1)),
        side = CreatureSide.LEFT,
        position = CreaturePosition(-id, 0),
        facing = CreatureCard.Facing.FACE_UP
    )

    private fun view(card: CreatureCard) = CreatureCardView(
        id = card.id,
        name = card.card.name,
        title = card.card.title,
        type = card.card.type,
        cost = card.card.cost,
        effect = card.card.effect,
        scoringRule = card.card.scoringRule,
        side = card.side,
        position = card.position,
        facing = card.facing,
        isSnippable = true
    )
}
