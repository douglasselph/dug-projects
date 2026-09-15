package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.plant.domain.PlantScoringRule
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.creature.CreatureCard
import dugsolutions.leaf.v35.player.creature.CreatureCardId
import dugsolutions.leaf.v35.player.creature.CreaturePosition
import dugsolutions.leaf.v35.player.creature.CreatureSide
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.battle.BattleMainAction
import dugsolutions.leaf.v35.player.decision.battle.BattleSupportAction
import dugsolutions.leaf.v35.player.decision.battle.BattleTurnAction
import dugsolutions.leaf.v35.player.decision.battle.ChooseBattleTurnActionRequest
import dugsolutions.leaf.v35.player.decision.battle.ChooseBattleFirstMainActionRequest
import dugsolutions.leaf.v35.player.decision.context.BattlePlayerRowView
import dugsolutions.leaf.v35.player.decision.context.BattleRowView
import dugsolutions.leaf.v35.player.decision.context.BattleView
import dugsolutions.leaf.v35.player.decision.context.CreatureCardView
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.context.DieView
import dugsolutions.leaf.v35.round.domain.RoundCard
import dugsolutions.leaf.v35.round.domain.RoundCardEffect
import dugsolutions.leaf.v35.round.domain.RoundCardType
import dugsolutions.leaf.v35.tokens.Critter
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
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


    @Test
    fun `Bee-loved Bloom influence can preserve Bee during Battle Support`() {
        val actorId = PlayerId(0)
        val opponentId = PlayerId(1)
        val row = BattleRowView(
            row = StrikeRow.TOP,
            closed = false,
            players = listOf(
                BattlePlayerRowView(actorId, StrikeRow.TOP, emptyList(), emptyList(), 0, 0, 0, false),
                BattlePlayerRowView(opponentId, StrikeRow.TOP, emptyList(), emptyList(), 0, 0, 0, false)
            )
        )
        val context = DecisionContext.EMPTY.copy(
            phase = RoundCardType.BATTLE,
            self = DecisionContext.EMPTY.self.copy(
                board = DecisionContext.EMPTY.self.board.copy(
                    id = actorId,
                    bees = 1,
                    worms = 1,
                    creature = listOf(beeLovedView())
                )
            ),
            battle = BattleView(
                playerOrder = listOf(actorId, opponentId),
                rows = listOf(row)
            )
        )
        val chosen = HumanBaselineBattleStrategy().chooseTurnAction(
            ChooseBattleTurnActionRequest(
                roundCard = round(),
                passNumber = 1,
                legalChoices = listOf(
                    BattleTurnAction.Support(BattleSupportAction.PlaceCritter(Critter.BEE, StrikeRow.TOP)),
                    BattleTurnAction.Support(BattleSupportAction.PlaceCritter(Critter.WORM, StrikeRow.TOP))
                ),
                context = context
            )
        )

        val support = assertIs<BattleTurnAction.Support>(chosen)
        val placement = assertIs<BattleSupportAction.PlaceCritter>(support.action)
        assertEquals(Critter.WORM, placement.critter)
    }

    private fun beeLovedView() = CreatureCardView(
        id = CreatureCardId(99),
        name = "Flower_14_01",
        title = "Bee-loved Bloom",
        type = PlantType.FLOWER,
        cost = 14,
        effect = GameEffect.GAIN_OR_STEAL_BEE_AND_BOOST_BEES_THIS_ROUND,
        scoringRule = PlantScoringRule.Fixed(1),
        side = CreatureSide.LEFT,
        position = CreaturePosition(-1, 0),
        facing = CreatureCard.Facing.FACE_UP,
        isSnippable = true
    )

    private fun round() = RoundCard(
        quantity = 1,
        name = "battle",
        type = RoundCardType.BATTLE,
        firstEffect = RoundCardEffect("x", "", "", "", null, GameEffect.GAIN_ONE_VP),
        secondEffect = RoundCardEffect("y", "", "", "", null, GameEffect.GAIN_ONE_VP),
        backImage = ""
    )
}
