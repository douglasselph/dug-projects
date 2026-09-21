package dugsolutions.leaf.v35.player.decision.baseline.cultivation

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.plant.domain.PlantScoringRule
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.creature.CreatureCard
import dugsolutions.leaf.v35.player.creature.CreatureCardId
import dugsolutions.leaf.v35.player.creature.CreaturePosition
import dugsolutions.leaf.v35.player.creature.CreatureSide
import dugsolutions.leaf.v35.player.decision.baseline.HumanBaselinePolicy
import dugsolutions.leaf.v35.player.decision.baseline.card.HumanBaselineCardScorerRegistry
import dugsolutions.leaf.v35.player.decision.context.CreatureCardView
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.context.MulchView
import dugsolutions.leaf.v35.player.decision.support.HandDieChoice
import dugsolutions.leaf.v35.player.decision.support.SupportAction
import dugsolutions.leaf.v35.random.die.DieSides
import dugsolutions.leaf.v35.round.domain.RoundCardType
import dugsolutions.leaf.v35.tokens.Butterfly
import dugsolutions.leaf.v35.tokens.Token
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class CultivationSupportPriorityTest {
    private val scorers = HumanBaselineCardScorerRegistry()
    private val policy = HumanBaselinePolicy()

    @Test
    fun `Water reserve is a soft penalty rather than a prohibition`() {
        val weakContext = context(water = 1)
        val stockedContext = context(water = 2)
        val action = SupportAction.UseWaterReroll(HandDieChoice(0, 12, 1))

        val protected = score(weakContext, action)
        val surplus = score(stockedContext, action)

        assertTrue(surplus.total > protected.total)
        assertTrue(protected.total > 0)
    }

    @Test
    fun `strong Water reroll can still beat Done while spending reserve`() {
        val context = context(water = 1)
        val action = SupportAction.UseWaterReroll(HandDieChoice(0, 20, 1))

        assertTrue(score(context, action).total > policy.cultivationDoneScore(context))
    }

    @Test
    fun `stored high-sided Mulch becomes attractive when another Mulch remains protected`() {
        val action = SupportAction.UseMulch(Token.MULCH(DieSides.D12))
        val protected = context(mulch = listOf(MulchView(0, DieSides.D12, false)))
        val surplus = context(
            mulch = listOf(
                MulchView(0, DieSides.D12, false),
                MulchView(1, DieSides.D4, false)
            )
        )

        assertTrue(score(protected, action).total < policy.cultivationDoneScore(protected))
        assertTrue(score(surplus, action).total > policy.cultivationDoneScore(surplus))
    }

    @Test
    fun `Worm does not flip an already face-up Plant`() {
        val card = plantView(faceUp = true)
        val context = context(worms = 2, creature = listOf(card))

        val result = score(context, SupportAction.UseWormFlip(card.id))

        assertTrue(result.total < 0)
    }

    @Test
    fun `Butterfly values the actual reroll opportunity and spends no reserve`() {
        val context = context()
        val strong = score(
            context,
            SupportAction.UseButterfly(Butterfly.entries.first(), HandDieChoice(0, 20, 1))
        )
        val weak = score(
            context,
            SupportAction.UseButterfly(Butterfly.entries.first(), HandDieChoice(0, 4, 4))
        )

        assertTrue(strong.total > policy.cultivationDoneScore(context))
        assertTrue(weak.total < policy.cultivationDoneScore(context))
    }

    private fun score(context: DecisionContext, action: SupportAction) =
        CultivationSupportPriority.score(context, action, scorers, policy)

    private fun context(
        water: Int = 0,
        worms: Int = 0,
        mulch: List<MulchView> = emptyList(),
        creature: List<CreatureCardView> = emptyList()
    ) = DecisionContext.EMPTY.copy(
        phase = RoundCardType.CULTIVATION,
        self = DecisionContext.EMPTY.self.copy(
            board = DecisionContext.EMPTY.self.board.copy(
                water = water,
                worms = worms,
                mulch = mulch,
                creature = creature
            )
        )
    )

    private fun plantView(faceUp: Boolean) = CreatureCardView(
        id = CreatureCardId(7),
        name = "Root_05_02",
        title = "Root Four More",
        type = PlantType.ROOT,
        cost = 5,
        effect = GameEffect.RAISE_DIE_PLUS_4,
        scoringRule = PlantScoringRule.Fixed(1),
        side = CreatureSide.LEFT,
        position = CreaturePosition(-1, 0),
        facing = if (faceUp) CreatureCard.Facing.FACE_UP else CreatureCard.Facing.FACE_DOWN,
        isSnippable = true
    )
}
