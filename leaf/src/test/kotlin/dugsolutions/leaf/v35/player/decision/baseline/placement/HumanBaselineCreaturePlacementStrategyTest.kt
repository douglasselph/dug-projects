package dugsolutions.leaf.v35.player.decision.baseline.placement

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.plant.domain.PlantScoringRule
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.creature.CreaturePosition
import dugsolutions.leaf.v35.player.creature.CreatureSide
import dugsolutions.leaf.v35.player.creature.GraftPlacement
import dugsolutions.leaf.v35.player.decision.baseline.scoring.BaselineScoreEngine
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.placement.ChooseCreaturePlacementRequest
import dugsolutions.leaf.v35.player.decision.random.StrategyRandomizer
import dugsolutions.leaf.v35.round.domain.RoundCardType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class HumanBaselineCreaturePlacementStrategyTest {
    @Test
    fun `equally scored legal graft positions use seeded strategy tie breaker`() {
        val left = GraftPlacement(CreatureSide.LEFT, CreaturePosition(-1, -1))
        val right = GraftPlacement(CreatureSide.RIGHT, CreaturePosition(1, -1))
        val strategy = HumanBaselineCreaturePlacementStrategy(
            scoreEngine = BaselineScoreEngine(FixedRandomizer(1))
        )

        val chosen = strategy.choose(
            ChooseCreaturePlacementRequest(
                card = rootCard(),
                legalPlacements = listOf(left, right),
                context = DecisionContext.EMPTY.copy(phase = RoundCardType.CULTIVATION)
            )
        )

        assertEquals(right, chosen)
    }

    private fun rootCard() = PlantCard(
        6, "Root_05_01", "Test Root", PlantType.ROOT, 5, null,
        "", "", "", "", "", "", "",
        GameEffect.RAISE_DIE_PLUS_4, PlantScoringRule.Fixed(1)
    )

    private class FixedRandomizer(private val result: Int) : StrategyRandomizer {
        override fun nextInt(until: Int): Int = result
    }
}
