package dugsolutions.leaf.v35.player.decision.baseline.effect

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.plant.domain.PlantScoringRule
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.creature.CreatureCard
import dugsolutions.leaf.v35.player.creature.CreatureCardId
import dugsolutions.leaf.v35.player.creature.CreaturePosition
import dugsolutions.leaf.v35.player.creature.CreatureSide
import dugsolutions.leaf.v35.player.decision.context.CreatureCardView
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.context.DieView
import dugsolutions.leaf.v35.player.decision.context.OpponentView
import dugsolutions.leaf.v35.player.decision.effect.ChooseEffectDieSizeRequest
import dugsolutions.leaf.v35.player.decision.effect.ChooseEffectPlayerRequest
import dugsolutions.leaf.v35.random.die.DieSides
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class HumanBaselineEffectStrategyGeneralChoiceTest {

    @Test
    fun `gain any die chooses the largest legal die size`() {
        val chosen = HumanBaselineEffectStrategy().chooseDieSize(
            ChooseEffectDieSizeRequest(
                effect = GameEffect.GAIN_ANY_DIE_TO_DISCARD,
                legalChoices = listOf(DieSides.D6, DieSides.D20, DieSides.D10),
                context = context(opponent(2), opponent(3))
            )
        )

        assertEquals(DieSides.D20, chosen)
    }

    @Test
    fun `player target follows visible Plant investment rather than VP or Wisps`() {
        val visiblyDeveloped = opponent(
            id = 2,
            vp = 0,
            wisps = 0,
            plants = listOf(plant(1, cost = 17), plant(2, cost = 14))
        )
        val scoreboardLeader = opponent(
            id = 3,
            vp = 40,
            wisps = 5,
            plants = listOf(plant(3, cost = 5))
        )

        val chosen = choosePlayer(visiblyDeveloped, scoreboardLeader)

        assertEquals(PlayerId(2), chosen)
    }

    @Test
    fun `player target counts extra owned dice above the normal starting pool`() {
        val extraDice = opponent(
            id = 2,
            dice = listOf(20, 6, 4, 4, 4), // 38 power: 8 above the normal 30.
            plants = listOf(plant(1, cost = 7))
        )
        val morePlantButStartingDice = opponent(
            id = 3,
            dice = listOf(6, 6, 6, 4, 4, 4), // normal 30-side starting pool.
            plants = listOf(plant(2, cost = 14))
        )

        val chosen = choosePlayer(extraDice, morePlantButStartingDice)

        // 7 + 8 extra dice investment = 15, narrowly above 14 Plant investment.
        assertEquals(PlayerId(2), chosen)
    }

    @Test
    fun `starting dice power itself is not counted as development investment`() {
        val normalPool = opponent(
            id = 2,
            dice = listOf(6, 6, 6, 4, 4, 4),
            plants = listOf(plant(1, cost = 7))
        )
        val noDiceFixture = opponent(
            id = 3,
            plants = listOf(plant(2, cost = 8))
        )

        val chosen = choosePlayer(normalPool, noDiceFixture)

        assertEquals(PlayerId(3), chosen)
    }

    private fun choosePlayer(vararg opponents: OpponentView): PlayerId =
        HumanBaselineEffectStrategy().choosePlayer(
            ChooseEffectPlayerRequest(
                effect = GameEffect.STEAL_RANDOM_WISP_FROM_ONE_OPPONENT,
                legalChoices = opponents.map { it.id },
                context = context(*opponents)
            )
        )

    private fun context(vararg opponents: OpponentView): DecisionContext =
        DecisionContext.EMPTY.copy(opponents = opponents.toList())

    private fun opponent(
        id: Int,
        vp: Int = 0,
        wisps: Int = 0,
        dice: List<Int> = emptyList(),
        plants: List<CreatureCardView> = emptyList()
    ): OpponentView = OpponentView(
        board = DecisionContext.EMPTY.self.board.copy(
            id = PlayerId(id),
            vp = vp,
            supply = dice.mapIndexed { index, sides -> DieView(index, sides, 1) },
            creature = plants
        ),
        wispCount = wisps
    )

    private fun plant(id: Int, cost: Int): CreatureCardView = CreatureCardView(
        id = CreatureCardId(id),
        name = "Plant $id",
        title = "Plant $id",
        type = PlantType.ROOT,
        cost = cost,
        effect = GameEffect.GAIN_ONE_VP,
        scoringRule = PlantScoringRule.Fixed(0),
        side = CreatureSide.LEFT,
        position = CreaturePosition(-id, 0),
        facing = CreatureCard.Facing.FACE_UP,
        isSnippable = true
    )
}
