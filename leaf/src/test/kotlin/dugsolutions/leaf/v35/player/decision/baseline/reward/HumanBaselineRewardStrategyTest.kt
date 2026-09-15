package dugsolutions.leaf.v35.player.decision.baseline.reward

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.plant.domain.PlantScoringRule
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.creature.CreatureCard
import dugsolutions.leaf.v35.player.creature.CreatureCardId
import dugsolutions.leaf.v35.player.creature.CreaturePosition
import dugsolutions.leaf.v35.player.creature.CreatureSide
import dugsolutions.leaf.v35.player.decision.context.CreatureCardView
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.reward.ChooseCritterRequest
import dugsolutions.leaf.v35.round.domain.RoundCardType
import dugsolutions.leaf.v35.tokens.Critter
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class HumanBaselineRewardStrategyTest {
    @Test
    fun `Root Appreciation shifts early equal reward choice from Bee to Worm`() {
        val plain = context(emptyList())
        val withRootAppreciation = context(listOf(rootAppreciation()))
        val strategy = HumanBaselineRewardStrategy()

        val plainChoice = strategy.chooseCritter(request(plain))
        val influencedChoice = strategy.chooseCritter(request(withRootAppreciation))

        assertEquals(Critter.BEE, plainChoice)
        assertEquals(Critter.WORM, influencedChoice)
    }

    private fun request(context: DecisionContext) = ChooseCritterRequest(
        legalChoices = listOf(Critter.BEE, Critter.WORM),
        ownedCritters = emptyList(),
        context = context
    )

    private fun context(cards: List<CreatureCardView>) = DecisionContext.EMPTY.copy(
        phase = RoundCardType.CULTIVATION,
        progress = DecisionContext.EMPTY.progress.copy(currentCultivationRoundNumber = 1),
        self = DecisionContext.EMPTY.self.copy(
            board = DecisionContext.EMPTY.self.board.copy(
                bees = 0,
                worms = 0,
                creature = cards
            )
        )
    )

    private fun rootAppreciation() = CreatureCardView(
        id = CreatureCardId(1),
        name = "Root_07_02",
        title = "Root Appreciation",
        type = PlantType.ROOT,
        cost = 7,
        effect = GameEffect.GAIN_WORM_AND_BOOST_WORMS_THIS_ROUND,
        scoringRule = PlantScoringRule.Fixed(1),
        side = CreatureSide.LEFT,
        position = CreaturePosition(-1, 0),
        facing = CreatureCard.Facing.FACE_UP,
        isSnippable = true
    )
}
