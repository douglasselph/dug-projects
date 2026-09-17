package dugsolutions.leaf.simulation.v35.strategy.planned

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.plant.domain.PlantScoringRule
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.creature.CreatureCard
import dugsolutions.leaf.v35.player.creature.CreatureCardId
import dugsolutions.leaf.v35.player.creature.CreaturePosition
import dugsolutions.leaf.v35.player.creature.CreatureSide
import dugsolutions.leaf.v35.player.decision.baseline.scoring.PriorityScore
import dugsolutions.leaf.v35.player.decision.buy.BuyItem
import dugsolutions.leaf.v35.player.decision.context.CreatureCardView
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.context.SelfPlayerView
import kotlin.test.Test
import kotlin.test.assertEquals

class CardFocusModifierTest {
    private val rootAppreciation = PlantCard(
        quantity = 4,
        name = "Root_07_02",
        title = "Root Appreciation",
        type = PlantType.ROOT,
        cost = 7,
        lineIcon = null,
        vpIcon = "",
        typeIcon = "",
        fgColor = "",
        textColor = "",
        fullImage = "",
        backgroundImage = "",
        cardBackgroundImage = "",
        effect = GameEffect.GAIN_WORM_AND_BOOST_WORMS_THIS_ROUND,
        scoringRule = PlantScoringRule.Fixed(0)
    )

    private val modifier = CardFocusModifier(
        CreaturePlan.of("Root_07_02" to 2)
    )

    @Test
    fun firstDesiredCopy_addsForty() {
        val result = modifier.modify(
            context = contextWithOwnedCopies(0),
            item = BuyItem.Plant(rootAppreciation),
            score = PriorityScore(100)
        )

        assertEquals(140, result.total)
        assertEquals(40, result.adjustments.single().amount)
    }

    @Test
    fun laterDesiredCopy_addsThirty() {
        val result = modifier.modify(
            context = contextWithOwnedCopies(1),
            item = BuyItem.Plant(rootAppreciation),
            score = PriorityScore(100)
        )

        assertEquals(130, result.total)
        assertEquals(30, result.adjustments.single().amount)
    }

    @Test
    fun reachedTarget_addsNothing() {
        val result = modifier.modify(
            context = contextWithOwnedCopies(2),
            item = BuyItem.Plant(rootAppreciation),
            score = PriorityScore(100)
        )

        assertEquals(PriorityScore(100), result)
    }

    private fun contextWithOwnedCopies(count: Int): DecisionContext {
        val creature = List(count) { index ->
            CreatureCardView(
                id = CreatureCardId(index + 1),
                name = rootAppreciation.name,
                title = rootAppreciation.title,
                type = rootAppreciation.type,
                cost = rootAppreciation.cost,
                effect = rootAppreciation.effect,
                scoringRule = rootAppreciation.scoringRule,
                side = CreatureSide.LEFT,
                position = CreaturePosition(-1 - index, 0),
                facing = CreatureCard.Facing.FACE_UP,
                isSnippable = true
            )
        }
        val board = SelfPlayerView.EMPTY.board.copy(creature = creature)
        return DecisionContext.EMPTY.copy(
            self = SelfPlayerView(board = board, wisps = emptyList())
        )
    }
}
