package dugsolutions.leaf.v35.player.decision.baseline.buy

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.plant.domain.PlantScoringRule
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.creature.CreatureCard
import dugsolutions.leaf.v35.player.creature.CreatureCardId
import dugsolutions.leaf.v35.player.creature.CreaturePosition
import dugsolutions.leaf.v35.player.creature.CreatureSide
import dugsolutions.leaf.v35.player.decision.buy.BuyCritterResource
import dugsolutions.leaf.v35.player.decision.buy.BuyItem
import dugsolutions.leaf.v35.player.decision.buy.ChoosePaymentRequest
import dugsolutions.leaf.v35.player.decision.context.CreatureCardView
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.round.domain.RoundCardType
import dugsolutions.leaf.v35.tokens.Critter
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class PaymentPriorityTest {
    @Test
    fun `Root Appreciation preserves an unboosted Worm when Bee can pay instead`() {
        val strategy = HumanBaselineBuyStrategy()
        val plain = strategy.choosePayment(request(context(emptyList())))
        val influenced = strategy.choosePayment(request(context(listOf(rootAppreciation()))))

        assertEquals(Critter.WORM, plain.critters.single().critter)
        assertEquals(Critter.BEE, influenced.critters.single().critter)
    }

    private fun request(context: DecisionContext) = ChoosePaymentRequest(
        item = BuyItem.Plant(
            PlantCard(
                1,
                "Test_1",
                "Test",
                PlantType.ROOT,
                1,
                null,
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                GameEffect.GAIN_ONE_VP,
                PlantScoringRule.Fixed(0)
            )
        ),
        availableDice = emptyList(),
        availableCritters = listOf(
            BuyCritterResource(Critter.BEE, 2),
            BuyCritterResource(Critter.WORM, 1)
        ),
        context = context
    )

    private fun context(cards: List<CreatureCardView>) = DecisionContext.EMPTY.copy(
        phase = RoundCardType.CULTIVATION,
        self = DecisionContext.EMPTY.self.copy(
            board = DecisionContext.EMPTY.self.board.copy(
                bees = 1,
                worms = 1,
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
