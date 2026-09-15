package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.plant.domain.PlantScoringRule
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.creature.CreatureCard
import dugsolutions.leaf.v35.player.creature.CreatureCardId
import dugsolutions.leaf.v35.player.creature.CreaturePosition
import dugsolutions.leaf.v35.player.creature.CreatureSide
import dugsolutions.leaf.v35.player.decision.battle.BattleMainAction
import dugsolutions.leaf.v35.player.decision.battle.ChooseBattleFirstMainActionRequest
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.context.DieView
import dugsolutions.leaf.v35.round.domain.RoundCard
import dugsolutions.leaf.v35.round.domain.RoundCardEffect
import dugsolutions.leaf.v35.round.domain.RoundCardType
import org.junit.jupiter.api.Test
import kotlin.test.assertIs

class HumanBaselineBattleStrategyTest {
    @Test
    fun `strong Battle Plant can beat Draw`() {
        val thorn = CreatureCard(
            id = CreatureCardId(2),
            card = PlantCard(6, "Vine_09_02", "Parting Thorn", PlantType.VINE, 9, null, "", "", "", "", "", "", "", GameEffect.FLIP_OWN_PLANT_OR_WOUND_EACH_OPPONENT_IN_BATTLE, PlantScoringRule.Fixed(1)),
            side = CreatureSide.LEFT,
            position = CreaturePosition(-1, 0),
            facing = CreatureCard.Facing.FACE_UP
        )
        val context = DecisionContext.EMPTY.copy(
            phase = RoundCardType.BATTLE,
            self = DecisionContext.EMPTY.self.copy(
                board = DecisionContext.EMPTY.self.board.copy(supply = listOf(DieView(0, 4, 1)))
            )
        )
        val chosen = HumanBaselineBattleStrategy().chooseFirstMainAction(
            ChooseBattleFirstMainActionRequest(
                roundCard = round(),
                legalChoices = listOf(BattleMainAction.Draw, BattleMainAction.ActivatePlant(thorn)),
                context = context
            )
        )
        assertIs<BattleMainAction.ActivatePlant>(chosen)
    }

    private fun round() = RoundCard(
        quantity = 1,
        name = "battle",
        type = RoundCardType.BATTLE,
        firstEffect = RoundCardEffect("x", "", "", "", null, GameEffect.GAIN_ONE_VP),
        secondEffect = RoundCardEffect("y", "", "", "", null, GameEffect.GAIN_ONE_VP),
        backImage = ""
    )
}
