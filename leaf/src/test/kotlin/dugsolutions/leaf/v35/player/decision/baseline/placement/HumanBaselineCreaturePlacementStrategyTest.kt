package dugsolutions.leaf.v35.player.decision.baseline.placement

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.plant.domain.PlantScoringRule
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.creature.CreatureCard
import dugsolutions.leaf.v35.player.creature.CreatureCardId
import dugsolutions.leaf.v35.player.creature.CreaturePosition
import dugsolutions.leaf.v35.player.creature.CreatureSide
import dugsolutions.leaf.v35.player.creature.GraftPlacement
import dugsolutions.leaf.v35.player.decision.baseline.HumanBaselinePolicy
import dugsolutions.leaf.v35.player.decision.baseline.scoring.BaselineScoreEngine
import dugsolutions.leaf.v35.player.decision.context.CreatureCardView
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.placement.ChooseCreaturePlacementRequest
import dugsolutions.leaf.v35.player.decision.random.StrategyRandomizer
import dugsolutions.leaf.v35.round.domain.RoundCardType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HumanBaselineCreaturePlacementStrategyTest {

    // Human Baseline Graft Placement Behavior Contract

    @Test
    fun `strategy always returns one of the engine supplied legal placements`() {
        val left = GraftPlacement(CreatureSide.LEFT, CreaturePosition(-2, 0))
        val right = GraftPlacement(CreatureSide.RIGHT, CreaturePosition(1, 0))
        val legal = listOf(left, right)

        val chosen = strategy().choose(
            request(vineCard(), legal, context(creature = listOf(vine(1, CreatureSide.LEFT, -1, 0))))
        )

        assertTrue(chosen in legal)
    }

    @Test
    fun `adequate topology beats constrained topology even when constrained side is less developed`() {
        val lessDevelopedRight = GraftPlacement(CreatureSide.RIGHT, CreaturePosition(1, 0)) // 4 future slots
        val adequateLeft = GraftPlacement(CreatureSide.LEFT, CreaturePosition(-1, 1)) // 5 future slots
        val creature = listOf(vine(1, CreatureSide.LEFT, -1, 0))
        val policy = HumanBaselinePolicy(graftAdequateGrowthSlotsValue = 5)

        val chosen = strategy(policy = policy).choose(
            request(vineCard(), listOf(lessDevelopedRight, adequateLeft), context(creature))
        )

        assertEquals(adequateLeft, chosen)
    }

    @Test
    fun `extra raw slots within adequate do not beat weak less-developed-side preference`() {
        val lessDevelopedRight = GraftPlacement(CreatureSide.RIGHT, CreaturePosition(1, 0)) // 4 future slots
        val moreSlotsLeft = GraftPlacement(CreatureSide.LEFT, CreaturePosition(-1, 1)) // 5 future slots
        val creature = listOf(vine(1, CreatureSide.LEFT, -1, 0))

        val chosen = strategy().choose(
            request(vineCard(), listOf(lessDevelopedRight, moreSlotsLeft), context(creature))
        )

        assertEquals(lessDevelopedRight, chosen)
    }

    @Test
    fun `final Cultivation ignores future growth bands and uses weak balance`() {
        val lessDevelopedRight = GraftPlacement(CreatureSide.RIGHT, CreaturePosition(1, 0)) // constrained at threshold 5
        val adequateLeft = GraftPlacement(CreatureSide.LEFT, CreaturePosition(-1, 1))
        val creature = listOf(vine(1, CreatureSide.LEFT, -1, 0))
        val policy = HumanBaselinePolicy(graftAdequateGrowthSlotsValue = 5)

        val chosen = strategy(policy = policy).choose(
            request(
                vineCard(),
                listOf(lessDevelopedRight, adequateLeft),
                context(creature, isFinalCultivationRound = true)
            )
        )

        assertEquals(lessDevelopedRight, chosen)
    }

    @Test
    fun `pre-final Flower placement records strong penalty for consuming final growth slot`() {
        val creature = lastGrowthSlotCreature()
        val finalSlot = GraftPlacement(CreatureSide.LEFT, CreaturePosition(-2, 0))

        val beforeFinal = GraftPlacementPriority.score(
            context(creature), PlantType.FLOWER, finalSlot
        )
        val finalRound = GraftPlacementPriority.score(
            context(creature, isFinalCultivationRound = true), PlantType.FLOWER, finalSlot
        )

        assertTrue(beforeFinal.total < finalRound.total)
        assertTrue(beforeFinal.adjustments.any { it.reason == "Do not consume the last growth slot" })
        assertTrue(finalRound.adjustments.none { it.reason == "Do not consume the last growth slot" })
    }

    @Test
    fun `equally scored legal graft positions use seeded strategy tie breaker`() {
        val left = GraftPlacement(CreatureSide.LEFT, CreaturePosition(-1, -1))
        val right = GraftPlacement(CreatureSide.RIGHT, CreaturePosition(1, -1))
        val strategy = strategy(randomizer = FixedRandomizer(1))

        val chosen = strategy.choose(
            request(rootCard(), listOf(left, right), context())
        )

        assertEquals(right, chosen)
    }

    @Test
    fun `growth bands are bounded categories rather than linear slot scores`() {
        assertEquals(
            GraftPlacementPriority.GrowthBand.BOXED_IN,
            GraftPlacementPriority.growthBand(futureSlots = 0, adequateThreshold = 3)
        )
        assertEquals(
            GraftPlacementPriority.GrowthBand.CONSTRAINED,
            GraftPlacementPriority.growthBand(futureSlots = 1, adequateThreshold = 3)
        )
        assertEquals(
            GraftPlacementPriority.GrowthBand.CONSTRAINED,
            GraftPlacementPriority.growthBand(futureSlots = 2, adequateThreshold = 3)
        )
        assertEquals(
            GraftPlacementPriority.GrowthBand.ADEQUATE,
            GraftPlacementPriority.growthBand(futureSlots = 3, adequateThreshold = 3)
        )
        assertEquals(
            GraftPlacementPriority.GrowthBand.ADEQUATE,
            GraftPlacementPriority.growthBand(futureSlots = 8, adequateThreshold = 3)
        )
    }

    private fun strategy(
        policy: HumanBaselinePolicy = HumanBaselinePolicy(),
        randomizer: StrategyRandomizer = FixedRandomizer(0)
    ) = HumanBaselineCreaturePlacementStrategy(
        scoreEngine = BaselineScoreEngine(randomizer),
        policy = policy
    )

    private fun request(
        card: PlantCard,
        legal: List<GraftPlacement>,
        context: DecisionContext
    ) = ChooseCreaturePlacementRequest(card, legal, context)

    private fun context(
        creature: List<CreatureCardView> = emptyList(),
        isFinalCultivationRound: Boolean = false
    ): DecisionContext = DecisionContext.EMPTY.copy(
        phase = RoundCardType.CULTIVATION,
        progress = DecisionContext.EMPTY.progress.copy(
            isFinalCultivationRound = isFinalCultivationRound
        ),
        self = DecisionContext.EMPTY.self.copy(
            board = DecisionContext.EMPTY.self.board.copy(creature = creature)
        )
    )

    private fun lastGrowthSlotCreature() = listOf(
        vine(1, CreatureSide.LEFT, -1, 0),
        vine(2, CreatureSide.RIGHT, 1, 0),
        card(3, PlantType.FLOWER, CreatureSide.LEFT, -1, 1),
        card(4, PlantType.FLOWER, CreatureSide.RIGHT, 1, 1),
        card(5, PlantType.FLOWER, CreatureSide.RIGHT, 2, 0)
    )

    private fun vine(id: Int, side: CreatureSide, x: Int, y: Int) =
        card(id, PlantType.VINE, side, x, y)

    private fun card(
        id: Int,
        type: PlantType,
        side: CreatureSide,
        x: Int,
        y: Int
    ) = CreatureCardView(
        id = CreatureCardId(id),
        name = "Card_$id",
        title = "Card $id",
        type = type,
        cost = 7,
        effect = GameEffect.UNKNOWN,
        scoringRule = PlantScoringRule.Fixed(0),
        side = side,
        position = CreaturePosition(x, y),
        facing = CreatureCard.Facing.FACE_UP,
        isSnippable = true
    )

    private fun rootCard() = plantCard(PlantType.ROOT)
    private fun vineCard() = plantCard(PlantType.VINE)

    private fun plantCard(type: PlantType) = PlantCard(
        6, "Test_${type.name}", "Test ${type.name}", type, 5, null,
        "", "", "", "", "", "", "",
        GameEffect.RAISE_DIE_PLUS_4, PlantScoringRule.Fixed(1)
    )

    private class FixedRandomizer(private val result: Int) : StrategyRandomizer {
        override fun nextInt(until: Int): Int = result
    }
}
