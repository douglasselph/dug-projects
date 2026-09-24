package dugsolutions.leaf.v35.player.decision.baseline.reward

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.plant.domain.PlantScoringRule
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.creature.CreatureCard
import dugsolutions.leaf.v35.player.creature.CreatureCardId
import dugsolutions.leaf.v35.player.creature.CreaturePosition
import dugsolutions.leaf.v35.player.creature.CreatureSide
import dugsolutions.leaf.v35.player.decision.baseline.HumanBaselinePolicy
import dugsolutions.leaf.v35.player.decision.baseline.common.ResourceReserveTargets
import dugsolutions.leaf.v35.player.decision.context.CreatureCardView
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.random.StrategyRandomizer
import dugsolutions.leaf.v35.player.decision.reward.ChooseCritterRequest
import dugsolutions.leaf.v35.round.domain.RoundCardType
import dugsolutions.leaf.v35.tokens.Critter
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class HumanBaselineRewardStrategyTest {
    @Nested
    inner class `Human Baseline Critter Reward Behavior Contract` {
        @Test
        fun `missing Bee is preferred while establishing protected two Bee one Worm minimum`() {
            val randomizer = RecordingRandomizer(999_999)
            val strategy = strategy(randomizer = randomizer)

            val choice = strategy.chooseCritter(request(context(bees = 1, worms = 1)))

            assertEquals(Critter.BEE, choice)
            assertEquals(0, randomizer.calls)
        }

        @Test
        fun `missing Worm is preferred while establishing protected two Bee one Worm minimum`() {
            val randomizer = RecordingRandomizer(0)
            val strategy = strategy(randomizer = randomizer)

            val choice = strategy.chooseCritter(request(context(bees = 2, worms = 0)))

            assertEquals(Critter.WORM, choice)
            assertEquals(0, randomizer.calls)
        }

        @Test
        fun `only legal Critter is taken without consuming strategy randomness`() {
            val randomizer = RecordingRandomizer(0)
            val strategy = strategy(randomizer = randomizer)

            val choice = strategy.chooseCritter(
                request(
                    context = context(bees = 2, worms = 1),
                    legalChoices = listOf(Critter.WORM)
                )
            )

            assertEquals(Critter.WORM, choice)
            assertEquals(0, randomizer.calls)
        }

        @Test
        fun `post-reserve strategy randomization can select Bee`() {
            val randomizer = RecordingRandomizer(0)
            val strategy = strategy(randomizer = randomizer)

            val choice = strategy.chooseCritter(request(context(bees = 2, worms = 1)))

            assertEquals(Critter.BEE, choice)
            assertEquals(1, randomizer.calls)
        }

        @Test
        fun `post-reserve strategy randomization can select Worm`() {
            val randomizer = RecordingRandomizer(999_999)
            val strategy = strategy(randomizer = randomizer)

            val choice = strategy.chooseCritter(request(context(bees = 2, worms = 1)))

            assertEquals(Critter.WORM, choice)
            assertEquals(1, randomizer.calls)
        }

        @Test
        fun `player-specific policy can change protected minimum without changing defaults`() {
            val customPolicy = object : HumanBaselinePolicy() {
                override fun protectedCritterReserve(context: DecisionContext) =
                    ResourceReserveTargets(bees = 3, worms = 1)
            }
            val strategy = strategy(
                policy = customPolicy,
                randomizer = RecordingRandomizer(999_999)
            )

            val choice = strategy.chooseCritter(request(context(bees = 2, worms = 1)))

            assertEquals(Critter.BEE, choice)
            assertEquals(2, HumanBaselinePolicy().protectedCritterReserve(context()).bees)
        }

        @Test
        fun `player-specific policy can change post-reserve Bee probability`() {
            val customPolicy = object : HumanBaselinePolicy() {
                override fun postReserveBeeProbability(context: DecisionContext): Double = 0.0
            }
            val strategy = strategy(
                policy = customPolicy,
                randomizer = RecordingRandomizer(0)
            )

            val choice = strategy.chooseCritter(request(context(bees = 2, worms = 1)))

            assertEquals(Critter.WORM, choice)
            assertEquals(2.0 / 3.0, HumanBaselinePolicy().postReserveBeeProbability(context()))
        }

        @Test
        fun `visible Root Appreciation influence can override ordinary Bee preference`() {
            val strategy = strategy(randomizer = RecordingRandomizer(0))
            val influenced = context(
                bees = 2,
                worms = 1,
                cards = listOf(rootAppreciation())
            )

            val choice = strategy.chooseCritter(request(influenced))

            assertEquals(Critter.WORM, choice)
        }
    }

    private fun strategy(
        policy: HumanBaselinePolicy = HumanBaselinePolicy(),
        randomizer: StrategyRandomizer
    ) = HumanBaselineRewardStrategy(
        policy = policy,
        strategyRandomizer = randomizer
    )

    private fun request(
        context: DecisionContext,
        legalChoices: List<Critter> = listOf(Critter.BEE, Critter.WORM)
    ) = ChooseCritterRequest(
        legalChoices = legalChoices,
        ownedCritters = emptyList(),
        context = context
    )

    private fun context(
        bees: Int = 0,
        worms: Int = 0,
        cards: List<CreatureCardView> = emptyList()
    ) = DecisionContext.EMPTY.copy(
        phase = RoundCardType.CULTIVATION,
        self = DecisionContext.EMPTY.self.copy(
            board = DecisionContext.EMPTY.self.board.copy(
                bees = bees,
                worms = worms,
                creature = cards
            )
        )
    )

    private fun rootAppreciation() = CreatureCardView(
        id = CreatureCardId(1),
        name = "Root_07_02",
        title = "Root Appreciation",
        type = PlantType.ROOT,
        cost = 7,
        effect = GameEffect.GAIN_WORM_AND_BOOST_WORMS_THIS_ROUND,
        scoringRule = PlantScoringRule.Fixed(1),
        side = CreatureSide.LEFT,
        position = CreaturePosition(-1, 0),
        facing = CreatureCard.Facing.FACE_UP,
        isSnippable = true
    )

    private class RecordingRandomizer(private val result: Int) : StrategyRandomizer {
        var calls: Int = 0
            private set

        override fun nextInt(until: Int): Int {
            calls += 1
            require(result in 0 until until)
            return result
        }
    }
}
