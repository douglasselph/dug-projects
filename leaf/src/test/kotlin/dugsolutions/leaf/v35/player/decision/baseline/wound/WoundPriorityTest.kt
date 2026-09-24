package dugsolutions.leaf.v35.player.decision.baseline.wound

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.plant.domain.PlantScoringRule
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.creature.*
import dugsolutions.leaf.v35.player.decision.baseline.battle.BattleEnabledPlantAnalyzer
import dugsolutions.leaf.v35.player.decision.baseline.card.PlantPreservationEvaluator
import dugsolutions.leaf.v35.player.decision.context.CreatureCardView
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.wound.WoundChoice
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class WoundPriorityTest {
    @Test
    fun `Snip priority is higher for the Plant with lower permanent preservation value`() {
        val root = creature(1, "Root_05_01", GameEffect.DOUBLE_ONE_DIE, 5, PlantType.ROOT)
        val flower = creature(2, "Flower_17_04", GameEffect.DRAW_TWO_DICE, 17, PlantType.FLOWER)
        val context = context(listOf(view(root), view(flower)))
        val analyzer = BattleEnabledPlantAnalyzer()
        val preservation = PlantPreservationEvaluator()

        val rootScore = WoundPriority.score(context, WoundChoice.Snip(root), analyzer, preservation).total
        val flowerScore = WoundPriority.score(context, WoundChoice.Snip(flower), analyzer, preservation).total

        assertTrue(rootScore > flowerScore)
    }

    private fun context(cards: List<CreatureCardView>) = DecisionContext.EMPTY.copy(
        self = DecisionContext.EMPTY.self.copy(
            board = DecisionContext.EMPTY.self.board.copy(creature = cards)
        )
    )

    private fun creature(id: Int, name: String, effect: GameEffect, cost: Int, type: PlantType) = CreatureCard(
        CreatureCardId(id),
        PlantCard(6, name, name, type, cost, null, "", "", "", "", "", "", "", effect, PlantScoringRule.Fixed(1)),
        CreatureSide.LEFT,
        CreaturePosition(-id, 0),
        CreatureCard.Facing.FACE_DOWN
    )

    private fun view(card: CreatureCard) = CreatureCardView(
        card.id, card.card.name, card.card.title, card.card.type, card.card.cost,
        card.card.effect, card.card.scoringRule, card.side, card.position, card.facing, true
    )
}
