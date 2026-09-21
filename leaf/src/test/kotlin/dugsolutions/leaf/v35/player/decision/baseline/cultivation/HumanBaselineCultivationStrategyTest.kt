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
import dugsolutions.leaf.v35.player.decision.support.HandDieChoice
import dugsolutions.leaf.v35.player.decision.support.SupportAction
import dugsolutions.leaf.v35.random.die.DieSides
import dugsolutions.leaf.v35.round.domain.RoundCard
import dugsolutions.leaf.v35.round.domain.RoundCardEffect
import dugsolutions.leaf.v35.round.domain.RoundCardType
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class HumanBaselineCultivationStrategyTest {

    /**
     * Executable statement of the approved ordinary-human Cultivation behavior.
     *
     * These tests intentionally assert externally visible choices rather than
     * exact internal scores. Scorer-specific arithmetic and policy plumbing live
     * in the lower-level priority/policy tests.
     */
    @Nested
    inner class `Human Baseline Behavior Contract` {

        @Test
        fun `high value Plant activation can beat an early Draw`() {
            val queen = creature("Flower_17_04", GameEffect.DRAW_TWO_DICE, PlantType.FLOWER, 17)
            val context = context(
                supply = listOf(DieView(0, 4, 1)),
                hand = listOf(DieView(0, 4, 2))
            )

            val chosen = choose(
                context = context,
                choices = listOf(
                    CultivationAction.Main(CultivationMainAction.Draw),
                    CultivationAction.Main(CultivationMainAction.ActivatePlant(queen))
                )
            )

            assertIs<CultivationMainAction.ActivatePlant>(assertIs<CultivationAction.Main>(chosen).action)
        }

        @Test
        fun `strong Draw beats a weak Plant activation`() {
            val berryImportant = creature("Vine_07_01", GameEffect.RAISE_ANY_DIE_PLUS_1, PlantType.VINE, 7)
            val context = context(
                supply = listOf(DieView(0, 20, 1)),
                hand = listOf(DieView(0, 4, 3))
            )

            val chosen = choose(
                context = context,
                choices = listOf(
                    CultivationAction.Main(CultivationMainAction.Draw),
                    CultivationAction.Main(CultivationMainAction.ActivatePlant(berryImportant))
                )
            )

            assertEquals(CultivationMainAction.Draw, assertIs<CultivationAction.Main>(chosen).action)
        }

        @Test
        fun `same Plant activation can lose or beat Draw based on current effect value`() {
            val rootFourMore = creature("Root_05_02", GameEffect.RAISE_DIE_PLUS_4, PlantType.ROOT, 5)
            val choices = listOf(
                CultivationAction.Main(CultivationMainAction.Draw),
                CultivationAction.Main(CultivationMainAction.ActivatePlant(rootFourMore))
            )
            val weakContext = context(
                supply = listOf(DieView(0, 12, 1)),
                hand = listOf(DieView(0, 8, 8))
            )
            val usefulContext = context(
                supply = listOf(DieView(0, 12, 1)),
                hand = listOf(DieView(0, 8, 1))
            )

            val weakChoice = choose(context = weakContext, choices = choices)
            val usefulChoice = choose(context = usefulContext, choices = choices)

            assertEquals(CultivationMainAction.Draw, assertIs<CultivationAction.Main>(weakChoice).action)
            assertIs<CultivationMainAction.ActivatePlant>(assertIs<CultivationAction.Main>(usefulChoice).action)
        }

        @Test
        fun `meaningful permanent Compost upgrade can beat Draw while no upgrade target cannot`() {
            val choices = listOf(
                CultivationAction.Main(CultivationMainAction.Draw),
                CultivationAction.Main(CultivationMainAction.RoundEffect1)
            )
            val round = roundWithEffects(GameEffect.UPGRADE_DIE_FROM_HAND)
            val usefulContext = context(
                supply = listOf(DieView(0, 20, 1)),
                hand = listOf(DieView(0, 4, 4)),
                graftBed = mapOf(DieSides.D12 to 1)
            )
            val noTargetContext = context(
                supply = listOf(DieView(0, 4, 1)),
                hand = listOf(DieView(0, 4, 4))
            )

            val usefulChoice = choose(round = round, context = usefulContext, choices = choices)
            val noTargetChoice = choose(round = round, context = noTargetContext, choices = choices)

            assertEquals(CultivationMainAction.RoundEffect1, assertIs<CultivationAction.Main>(usefulChoice).action)
            assertEquals(CultivationMainAction.Draw, assertIs<CultivationAction.Main>(noTargetChoice).action)
        }

        @Test
        fun `Mulch favors storing a poor roll but can lose to Draw when the roll is already useful`() {
            val choices = listOf(
                CultivationAction.Main(CultivationMainAction.Draw),
                CultivationAction.Main(CultivationMainAction.RoundEffect1)
            )
            val round = roundWithEffects(GameEffect.MULCH_DIE_FROM_HAND)
            val poorRollContext = context(
                supply = listOf(DieView(0, 12, 1)),
                hand = listOf(DieView(0, 12, 1))
            )
            val usefulRollContext = context(
                supply = listOf(DieView(0, 12, 1)),
                hand = listOf(DieView(0, 12, 12))
            )

            val poorRollChoice = choose(round = round, context = poorRollContext, choices = choices)
            val usefulRollChoice = choose(round = round, context = usefulRollContext, choices = choices)

            assertEquals(CultivationMainAction.RoundEffect1, assertIs<CultivationAction.Main>(poorRollChoice).action)
            assertEquals(CultivationMainAction.Draw, assertIs<CultivationAction.Main>(usefulRollChoice).action)
        }

        @Test
        fun `Sunlight can beat Draw when its visible raise crosses a Buy threshold`() {
            val choices = listOf(
                CultivationAction.Main(CultivationMainAction.Draw),
                CultivationAction.Main(CultivationMainAction.RoundEffect1)
            )
            val round = roundWithEffects(GameEffect.RAISE_DIE_PLUS_3)
            val withoutThreshold = context(
                supply = listOf(DieView(0, 10, 1)),
                hand = listOf(DieView(0, 10, 6))
            )
            val withThreshold = context(
                supply = listOf(DieView(0, 10, 1)),
                hand = listOf(DieView(0, 10, 6)),
                graftBed = mapOf(DieSides.D8 to 1)
            )

            val ordinaryChoice = choose(round = round, context = withoutThreshold, choices = choices)
            val thresholdChoice = choose(round = round, context = withThreshold, choices = choices)

            assertEquals(CultivationMainAction.Draw, assertIs<CultivationAction.Main>(ordinaryChoice).action)
            assertEquals(CultivationMainAction.RoundEffect1, assertIs<CultivationAction.Main>(thresholdChoice).action)
        }

        @Test
        fun `Root Well influence can make Water acquisition beat a strong Draw`() {
            val plainContext = context(
                supply = listOf(DieView(0, 20, 1)),
                water = 0
            )
            val influencedContext = context(
                supply = listOf(DieView(0, 20, 1)),
                water = 0,
                creatureViews = listOf(rootWellView())
            )
            val round = roundWithEffects(GameEffect.GAIN_WATER_TOKEN)
            val choices = listOf(
                CultivationAction.Main(CultivationMainAction.Draw),
                CultivationAction.Main(CultivationMainAction.RoundEffect1)
            )

            val plain = choose(round = round, context = plainContext, choices = choices)
            val influenced = choose(round = round, context = influencedContext, choices = choices)

            assertEquals(CultivationMainAction.Draw, assertIs<CultivationAction.Main>(plain).action)
            assertEquals(CultivationMainAction.RoundEffect1, assertIs<CultivationAction.Main>(influenced).action)
        }

        @Test
        fun `clear Support value can beat Done after both Main Actions`() {
            val action = SupportAction.UseWaterReroll(HandDieChoice(0, 20, 1))
            val chosen = choose(
                context = context(water = 1),
                mainActionsRemaining = 0,
                choices = listOf(
                    CultivationAction.Support(action),
                    CultivationAction.Done
                )
            )

            assertEquals(CultivationAction.Support(action), chosen)
        }

        @Test
        fun `marginal Support value loses to Done after both Main Actions`() {
            val action = SupportAction.UseWaterReroll(HandDieChoice(0, 4, 4))
            val chosen = choose(
                context = context(water = 2),
                mainActionsRemaining = 0,
                choices = listOf(
                    CultivationAction.Support(action),
                    CultivationAction.Done
                )
            )

            assertEquals(CultivationAction.Done, chosen)
        }

        @Test
        fun `protected Support reserve is a soft preference rather than a prohibition`() {
            val action = SupportAction.UseWaterReroll(HandDieChoice(0, 12, 1))
            val choices = listOf(
                CultivationAction.Support(action),
                CultivationAction.Done
            )

            val protectedChoice = choose(
                context = context(water = 1),
                mainActionsRemaining = 0,
                choices = choices
            )
            val surplusChoice = choose(
                context = context(water = 2),
                mainActionsRemaining = 0,
                choices = choices
            )

            assertEquals(CultivationAction.Done, protectedChoice)
            assertEquals(CultivationAction.Support(action), surplusChoice)
        }
    }

    @Nested
    inner class `Policy wiring and implementation seams` {

        @Test
        fun `Plant activation Buy-threshold scoring uses policy normal purchasing power`() {
            val rootFourMore = creature("Root_05_02", GameEffect.RAISE_DIE_PLUS_4, PlantType.ROOT, 5)
            val context = context(
                supply = listOf(DieView(0, 20, 1)),
                hand = listOf(DieView(0, 10, 5)),
                bees = 2,
                worms = 1,
                graftBed = mapOf(DieSides.D8 to 1)
            )
            val choices = listOf(
                CultivationAction.Main(CultivationMainAction.Draw),
                CultivationAction.Main(CultivationMainAction.ActivatePlant(rootFourMore))
            )

            val reserveAware = choose(context = context, choices = choices)
            val allCrittersSpendable = choose(
                context = context,
                choices = choices,
                strategy = HumanBaselineCultivationStrategy(
                    policy = object : HumanBaselinePolicy() {
                        override fun normalPurchasingPower(context: DecisionContext): Int = 10
                    }
                )
            )

            // Root Four More is worth 70 before threshold effects. With normal power
            // 5, its visible +4 crosses the available cost-8 tier and reaches 80,
            // beating the D20 Draw (~77). If protected Critters are counted as cash,
            // power already starts at 10, the threshold bonus disappears, and Draw wins.
            assertIs<CultivationMainAction.ActivatePlant>(assertIs<CultivationAction.Main>(reserveAware).action)
            assertEquals(CultivationMainAction.Draw, assertIs<CultivationAction.Main>(allCrittersSpendable).action)
        }

        @Test
        fun `Cultivation Done benchmark comes from the injected Human Baseline policy`() {
            val weakReroll = SupportAction.UseWaterReroll(HandDieChoice(0, 4, 1))
            val choices = listOf(
                CultivationAction.Support(weakReroll),
                CultivationAction.Done
            )
            val context = context(water = 2)

            val defaultChoice = choose(
                context = context,
                mainActionsRemaining = 0,
                choices = choices
            )
            val supportFriendlyChoice = choose(
                context = context,
                mainActionsRemaining = 0,
                choices = choices,
                strategy = HumanBaselineCultivationStrategy(
                    policy = HumanBaselinePolicy(cultivationDoneScoreValue = 20)
                )
            )

            assertEquals(CultivationAction.Done, defaultChoice)
            assertEquals(CultivationAction.Support(weakReroll), supportFriendlyChoice)
        }
    }

    private fun choose(
        context: DecisionContext,
        choices: List<CultivationAction>,
        round: RoundCard = round(RoundCardType.CULTIVATION),
        mainActionsRemaining: Int = 2,
        strategy: HumanBaselineCultivationStrategy = HumanBaselineCultivationStrategy()
    ): CultivationAction = strategy.chooseAction(
        ChooseCultivationActionRequest(
            roundCard = round,
            mainActionsRemaining = mainActionsRemaining,
            legalChoices = choices,
            context = context
        )
    )

    private fun context(
        supply: List<DieView> = emptyList(),
        hand: List<DieView> = emptyList(),
        bees: Int = 0,
        worms: Int = 0,
        water: Int = 0,
        creatureViews: List<CreatureCardView> = emptyList(),
        graftBed: Map<DieSides, Int> = emptyMap()
    ): DecisionContext = DecisionContext.EMPTY.copy(
        phase = RoundCardType.CULTIVATION,
        self = DecisionContext.EMPTY.self.copy(
            board = DecisionContext.EMPTY.self.board.copy(
                supply = supply,
                hand = hand,
                bees = bees,
                worms = worms,
                water = water,
                creature = creatureViews
            )
        ),
        grove = DecisionContext.EMPTY.grove.copy(graftBed = graftBed)
    )

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

    private fun roundWithEffects(
        first: GameEffect,
        second: GameEffect = GameEffect.GAIN_ONE_VP
    ) = RoundCard(
        quantity = 1,
        name = "effect test",
        type = RoundCardType.CULTIVATION,
        firstEffect = RoundCardEffect("first", "", "", "", null, first),
        secondEffect = RoundCardEffect("second", "", "", "", null, second),
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
