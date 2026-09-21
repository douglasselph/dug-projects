package dugsolutions.leaf.v35.player.decision.baseline.cultivation

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.plant.domain.PlantScoringRule
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.creature.CreatureCard
import dugsolutions.leaf.v35.player.creature.CreatureCardId
import dugsolutions.leaf.v35.player.creature.CreaturePosition
import dugsolutions.leaf.v35.player.creature.CreatureSide
import dugsolutions.leaf.v35.player.decision.baseline.HumanBaselinePolicy
import dugsolutions.leaf.v35.player.decision.context.CreatureCardView
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.context.DieView
import dugsolutions.leaf.v35.player.decision.cultivation.ChooseCultivationActionRequest
import dugsolutions.leaf.v35.player.decision.cultivation.CultivationAction
import dugsolutions.leaf.v35.player.decision.cultivation.CultivationMainAction
import dugsolutions.leaf.v35.player.decision.support.SupportAction
import dugsolutions.leaf.v35.round.domain.RoundCard
import dugsolutions.leaf.v35.round.domain.RoundCardEffect
import dugsolutions.leaf.v35.round.domain.RoundCardType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
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


    @Test
    fun `Root Well influence can make Water beat a strong Draw`() {
        val plainContext = DecisionContext.EMPTY.copy(
            phase = RoundCardType.CULTIVATION,
            self = DecisionContext.EMPTY.self.copy(
                board = DecisionContext.EMPTY.self.board.copy(
                    supply = listOf(DieView(0, 20, 1)),
                    water = 0
                )
            )
        )
        val influencedContext = plainContext.copy(
            self = plainContext.self.copy(
                board = plainContext.self.board.copy(
                    creature = listOf(rootWellView())
                )
            )
        )
        val round = roundWithWater()
        val choices = listOf(
            CultivationAction.Main(CultivationMainAction.Draw),
            CultivationAction.Main(CultivationMainAction.RoundEffect1)
        )

        val plain = HumanBaselineCultivationStrategy().chooseAction(
            ChooseCultivationActionRequest(round, 2, choices, plainContext)
        )
        val influenced = HumanBaselineCultivationStrategy().chooseAction(
            ChooseCultivationActionRequest(round, 2, choices, influencedContext)
        )

        assertEquals(CultivationMainAction.Draw, (plain as CultivationAction.Main).action)
        assertEquals(CultivationMainAction.RoundEffect1, (influenced as CultivationAction.Main).action)
    }


    @Test
    fun `Cultivation Done benchmark comes from the injected Human Baseline policy`() {
        val context = DecisionContext.EMPTY.copy(phase = RoundCardType.CULTIVATION)
        val choices = listOf(
            CultivationAction.Support(SupportAction.UseWaterRefresh),
            CultivationAction.Done
        )
        val request = ChooseCultivationActionRequest(
            roundCard = round(RoundCardType.CULTIVATION),
            mainActionsRemaining = 0,
            legalChoices = choices,
            context = context
        )

        val defaultChoice = HumanBaselineCultivationStrategy().chooseAction(request)
        val supportFriendlyChoice = HumanBaselineCultivationStrategy(
            policy = HumanBaselinePolicy(cultivationDoneScoreValue = 20)
        ).chooseAction(request)

        assertEquals(CultivationAction.Done, defaultChoice)
        assertEquals(CultivationAction.Support(SupportAction.UseWaterRefresh), supportFriendlyChoice)
    }

    private fun creature(name: String, effect: GameEffect, type: PlantType, cost: Int) = CreatureCard(
        id = CreatureCardId(1),
        card = PlantCard(6, name, name, type, cost, null, "", "", "", "", "", "", "", effect, PlantScoringRule.Fixed(1)),
        side = CreatureSide.LEFT,
        position = CreaturePosition(-1, 0),
        facing = CreatureCard.Facing.FACE_UP
    )


    private fun rootWellView() = CreatureCardView(
        id = CreatureCardId(99),
        name = "Root_05_04",
        title = "Root Well",
        type = PlantType.ROOT,
        cost = 5,
        effect = GameEffect.GAIN_WATER_AND_SPEND_1_TO_REROLL_TWO_OWN_OR_ONE_OPPONENT_BATTLE_DIE,
        scoringRule = PlantScoringRule.Fixed(1),
        side = CreatureSide.LEFT,
        position = CreaturePosition(-1, 0),
        facing = CreatureCard.Facing.FACE_UP,
        isSnippable = true
    )

    private fun roundWithWater() = RoundCard(
        quantity = 1,
        name = "water test",
        type = RoundCardType.CULTIVATION,
        firstEffect = RoundCardEffect("Water", "", "", "", null, GameEffect.GAIN_WATER_TOKEN),
        secondEffect = RoundCardEffect("VP", "", "", "", null, GameEffect.GAIN_ONE_VP),
        backImage = ""
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
