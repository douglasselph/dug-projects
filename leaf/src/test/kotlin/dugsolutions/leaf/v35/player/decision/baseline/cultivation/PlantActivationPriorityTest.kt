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
import dugsolutions.leaf.v35.player.decision.baseline.card.HumanBaselineCardScorerRegistry
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.context.DieView
import dugsolutions.leaf.v35.round.domain.RoundCardType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlantActivationPriorityTest {
    private val cardScorers = HumanBaselineCardScorerRegistry()

    @Test
    fun `permanent die upgrade Plant receives the policy development nudge`() {
        val context = contextWithHand(DieView(0, 4, 1))
        val card = creature(
            name = "Root_09_01",
            effect = GameEffect.UPGRADE_DIE_AND_USE_NOW,
            type = PlantType.ROOT,
            cost = 9
        )
        val noNudge = PlantActivationPriority.score(
            context = context,
            card = card,
            cardScorers = cardScorers,
            policy = fixedDevelopmentBonusPolicy(0)
        )
        val nudged = PlantActivationPriority.score(
            context = context,
            card = card,
            cardScorers = cardScorers,
            policy = fixedDevelopmentBonusPolicy(7)
        )

        assertEquals(noNudge.total + 7, nudged.total)
        assertTrue(
            nudged.adjustments.any {
                it.amount == 7 && it.reason == "Behind permanent dice-development target"
            }
        )
    }

    @Test
    fun `ordinary Plant activation does not receive a generic development nudge`() {
        val context = contextWithHand(DieView(0, 6, 2))
        val card = creature(
            name = "Vine_07_01",
            effect = GameEffect.RAISE_ANY_DIE_PLUS_1,
            type = PlantType.VINE,
            cost = 7
        )
        val noNudge = PlantActivationPriority.score(
            context = context,
            card = card,
            cardScorers = cardScorers,
            policy = fixedDevelopmentBonusPolicy(0)
        )
        val exaggeratedNudge = PlantActivationPriority.score(
            context = context,
            card = card,
            cardScorers = cardScorers,
            policy = fixedDevelopmentBonusPolicy(50)
        )

        assertEquals(noNudge.total, exaggeratedNudge.total)
    }

    private fun fixedDevelopmentBonusPolicy(value: Int) = object : HumanBaselinePolicy() {
        override fun cultivationDiceDevelopmentBonus(context: DecisionContext): Int = value
    }

    private fun contextWithHand(die: DieView) = DecisionContext.EMPTY.copy(
        phase = RoundCardType.CULTIVATION,
        self = DecisionContext.EMPTY.self.copy(
            board = DecisionContext.EMPTY.self.board.copy(hand = listOf(die))
        )
    )

    private fun creature(
        name: String,
        effect: GameEffect,
        type: PlantType,
        cost: Int
    ) = CreatureCard(
        id = CreatureCardId(1),
        card = PlantCard(
            6,
            name,
            name,
            type,
            cost,
            null,
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            effect,
            PlantScoringRule.Fixed(1)
        ),
        side = CreatureSide.LEFT,
        position = CreaturePosition(-1, 0),
        facing = CreatureCard.Facing.FACE_UP
    )
}
