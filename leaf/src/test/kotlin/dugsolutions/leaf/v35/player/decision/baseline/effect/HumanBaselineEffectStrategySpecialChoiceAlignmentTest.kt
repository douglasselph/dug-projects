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
import dugsolutions.leaf.v35.player.decision.context.WispView
import dugsolutions.leaf.v35.player.decision.effect.ChooseOEdelweissRequest
import dugsolutions.leaf.v35.player.decision.effect.ChooseWispsToKeepRequest
import dugsolutions.leaf.v35.player.decision.effect.EffectPlantChoice
import dugsolutions.leaf.v35.player.decision.effect.EffectWispChoice
import dugsolutions.leaf.v35.player.decision.effect.OEdelweissChoice
import dugsolutions.leaf.v35.round.domain.RoundCardType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class HumanBaselineEffectStrategySpecialChoiceAlignmentTest {
    @Test
    fun `Wisp Reckoning keeps the best complete set rather than the first legal Wisps`() {
        val low = wisp(0, "Unknown_Low", 0)
        val best = wisp(1, "Unknown_Best", 2)
        val second = wisp(2, "Unknown_Second", 1)
        val legal = listOf(low.toChoice(), best.toChoice(), second.toChoice())
        val context = cultivationContext(wisps = listOf(low, best, second))

        val chosen = HumanBaselineEffectStrategy().chooseWispsToKeep(
            ChooseWispsToKeepRequest(
                effect = GameEffect.LIMIT_WISPS_AND_TRASH_EXCESS,
                playerId = ACTOR,
                keepLimit = 2,
                legalChoices = legal,
                context = context
            )
        )

        assertEquals(setOf(1, 2), chosen.selected.map { it.index }.toSet())
    }

    @Test
    fun `Cultivation O Edelweiss flips up the more useful spent Plant`() {
        val modest = plant(10, "Vine_07_04", GameEffect.RAISE_ANY_DIE_PLUS_1, faceUp = false)
        val useful = plant(11, "Vine_11_01", GameEffect.DISCARD_ONE_DIE_DRAW_TWO, faceUp = false)
        val context = cultivationContext(creature = listOf(modest, useful))
        val modestChoice = OEdelweissChoice.Flip(modest.toChoice())
        val usefulChoice = OEdelweissChoice.Flip(useful.toChoice())

        val chosen = HumanBaselineEffectStrategy().chooseOEdelweiss(
            ChooseOEdelweissRequest(
                effect = GameEffect.PLAY_OR_FLIP_ANOTHER_CARD_TWICE,
                choiceNumber = 1,
                legalChoices = listOf(modestChoice, usefulChoice, OEdelweissChoice.Done),
                context = context
            )
        )

        assertEquals(usefulChoice, chosen)
    }

    @Test
    fun `Cultivation O Edelweiss chooses Done rather than flip down a ready Plant`() {
        val ready = plant(12, "Vine_11_01", GameEffect.DISCARD_ONE_DIE_DRAW_TWO, faceUp = true)
        val context = cultivationContext(creature = listOf(ready))

        val chosen = HumanBaselineEffectStrategy().chooseOEdelweiss(
            ChooseOEdelweissRequest(
                effect = GameEffect.PLAY_OR_FLIP_ANOTHER_CARD_TWICE,
                choiceNumber = 2,
                legalChoices = listOf(OEdelweissChoice.Flip(ready.toChoice()), OEdelweissChoice.Done),
                context = context
            )
        )

        assertEquals(OEdelweissChoice.Done, chosen)
    }

    private fun cultivationContext(
        creature: List<CreatureCardView> = emptyList(),
        wisps: List<WispView> = emptyList()
    ): DecisionContext = DecisionContext.EMPTY.copy(
        phase = RoundCardType.CULTIVATION,
        self = DecisionContext.EMPTY.self.copy(
            board = DecisionContext.EMPTY.self.board.copy(id = ACTOR, creature = creature),
            wisps = wisps
        )
    )

    private fun plant(
        id: Int,
        name: String,
        effect: GameEffect,
        faceUp: Boolean
    ) = CreatureCardView(
        id = CreatureCardId(id),
        name = name,
        title = name,
        type = PlantType.VINE,
        cost = if (id == 11 || id == 12) 11 else 7,
        effect = effect,
        scoringRule = PlantScoringRule.Fixed(1),
        side = CreatureSide.LEFT,
        position = CreaturePosition(-id, 0),
        facing = if (faceUp) CreatureCard.Facing.FACE_UP else CreatureCard.Facing.FACE_DOWN,
        isSnippable = true
    )

    private fun CreatureCardView.toChoice() = EffectPlantChoice(id, name, isFaceUp)

    private fun wisp(index: Int, name: String, endGameVp: Int) = WispView(
        index = index,
        name = name,
        title = name,
        effect = GameEffect.GAIN_ONE_VP,
        playImmediately = false,
        battleOnly = false,
        endGameVp = endGameVp
    )

    private fun WispView.toChoice() = EffectWispChoice(index, name, title, effect)

    private companion object {
        val ACTOR = PlayerId(0)
    }
}
