package dugsolutions.leaf.v35.player.decision.baseline.cultivation

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.plant.domain.PlantScoringRule
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.creature.CreatureCard
import dugsolutions.leaf.v35.player.creature.CreatureCardId
import dugsolutions.leaf.v35.player.creature.CreaturePosition
import dugsolutions.leaf.v35.player.creature.CreatureSide
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.context.DieView
import dugsolutions.leaf.v35.player.decision.cultivation.ChooseCultivationActionRequest
import dugsolutions.leaf.v35.player.decision.cultivation.CultivationAction
import dugsolutions.leaf.v35.player.decision.cultivation.CultivationMainAction
import dugsolutions.leaf.v35.round.domain.RoundCard
import dugsolutions.leaf.v35.round.domain.RoundCardEffect
import dugsolutions.leaf.v35.round.domain.RoundCardType
import org.junit.jupiter.api.Test
import kotlin.test.assertIs

class HumanBaselineCultivationStrategyTest {
    @Test
    fun `high value Plant activation can beat an early Draw`() {
        val queen = creature("Flower_17_04", GameEffect.DRAW_TWO_DICE, PlantType.FLOWER, 17)
        val context = DecisionContext.EMPTY.copy(
            phase = RoundCardType.CULTIVATION,
            self = DecisionContext.EMPTY.self.copy(
                board = DecisionContext.EMPTY.self.board.copy(
                    supply = listOf(DieView(0, 4, 1)),
                    hand = listOf(DieView(0, 4, 2))
                )
            )
        )
        val chosen = HumanBaselineCultivationStrategy().chooseAction(
            ChooseCultivationActionRequest(
                roundCard = round(RoundCardType.CULTIVATION),
                mainActionsRemaining = 2,
                legalChoices = listOf(
                    CultivationAction.Main(CultivationMainAction.Draw),
                    CultivationAction.Main(CultivationMainAction.ActivatePlant(queen))
                ),
                context = context
            )
        )
        assertIs<CultivationAction.Main>(chosen)
        assertIs<CultivationMainAction.ActivatePlant>(chosen.action)
    }

    private fun creature(name: String, effect: GameEffect, type: PlantType, cost: Int) = CreatureCard(
        id = CreatureCardId(1),
        card = PlantCard(6, name, name, type, cost, null, "", "", "", "", "", "", "", effect, PlantScoringRule.Fixed(1)),
        side = CreatureSide.LEFT,
        position = CreaturePosition(-1, 0),
        facing = CreatureCard.Facing.FACE_UP
    )

    private fun round(type: RoundCardType) = RoundCard(
        quantity = 1,
        name = "test",
        type = type,
        firstEffect = RoundCardEffect("x", "", "", "", null, GameEffect.GAIN_ONE_VP),
        secondEffect = RoundCardEffect("y", "", "", "", null, GameEffect.GAIN_ONE_VP),
        backImage = ""
    )
}
