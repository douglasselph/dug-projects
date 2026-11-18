package dugsolutions.leaf.v35.player.decision.baseline

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.plant.domain.PlantScoringRule
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.creature.CreatureCard
import dugsolutions.leaf.v35.player.creature.CreatureCardId
import dugsolutions.leaf.v35.player.creature.CreaturePosition
import dugsolutions.leaf.v35.player.creature.CreatureSide
import dugsolutions.leaf.v35.player.decision.baseline.battle.BattleDirectSupportAnalyzer
import dugsolutions.leaf.v35.player.decision.baseline.battle.BattleSupportPriority
import dugsolutions.leaf.v35.player.decision.baseline.battle.HumanBaselineBattleStrategy
import dugsolutions.leaf.v35.player.decision.baseline.card.HumanBaselineCardScorerRegistry
import dugsolutions.leaf.v35.player.decision.baseline.cultivation.CultivationSupportPriority
import dugsolutions.leaf.v35.player.decision.baseline.cultivation.HumanBaselineCultivationStrategy
import dugsolutions.leaf.v35.player.decision.baseline.scoring.BaselineScoreEngine
import dugsolutions.leaf.v35.player.decision.battle.BattleMainAction
import dugsolutions.leaf.v35.player.decision.battle.BattleSupportAction
import dugsolutions.leaf.v35.player.decision.battle.BattleTurnAction
import dugsolutions.leaf.v35.player.decision.battle.ChooseBattleTurnActionRequest
import dugsolutions.leaf.v35.player.decision.context.BattleDieView
import dugsolutions.leaf.v35.player.decision.context.BattlePlayerRowView
import dugsolutions.leaf.v35.player.decision.context.BattleRowView
import dugsolutions.leaf.v35.player.decision.context.BattleView
import dugsolutions.leaf.v35.player.decision.context.ButterflyView
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.context.CreatureCardView
import dugsolutions.leaf.v35.player.decision.context.DieView
import dugsolutions.leaf.v35.player.decision.context.MulchView
import dugsolutions.leaf.v35.player.decision.cultivation.ChooseCultivationActionRequest
import dugsolutions.leaf.v35.player.decision.cultivation.CultivationAction
import dugsolutions.leaf.v35.player.decision.random.StrategyRandomizer
import dugsolutions.leaf.v35.player.decision.support.HandDieChoice
import dugsolutions.leaf.v35.player.decision.support.SupportAction
import dugsolutions.leaf.v35.random.die.DieSides
import dugsolutions.leaf.v35.round.domain.RoundCard
import dugsolutions.leaf.v35.round.domain.RoundCardEffect
import dugsolutions.leaf.v35.round.domain.RoundCardType
import dugsolutions.leaf.v35.tokens.Butterfly
import dugsolutions.leaf.v35.tokens.Token
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Designer contract: resource personalities change ACQUISITION, not how a player
 * decides to spend an already-held Water, Mulch or Sunlight. All profiles see
 * the exact same decision snapshot, legal offers and seeded strategy RNG.
 *
 * The engine still owns legality, token movement and Support/Main limits; these
 * tests intentionally exercise the real Human Baseline selection paths rather
 * than duplicate game-rule mutations in test code.
 */
class ResourceSpendingPersonalityIndependenceTest {
    private val profiles = ResourcePersonality.NAMED
    private val actor = PlayerId(0)
    private val opponent = PlayerId(1)
    private val scorers = HumanBaselineCardScorerRegistry()

    @Test
    fun `Cultivation Water reroll is used when worthwhile and saved when not for every personality`() {
        val rerollLow = SupportAction.UseWaterReroll(HandDieChoice(0, 20, 1))
        val rerollHigh = SupportAction.UseWaterReroll(HandDieChoice(0, 4, 4))

        assertCultivationSpend(
            cultivationContext(water = 1, hand = listOf(DieView(0, 20, 1))),
            rerollLow,
            CultivationAction.Support(rerollLow)
        )
        assertCultivationSpend(
            cultivationContext(water = 1, hand = listOf(DieView(0, 4, 4))),
            rerollHigh,
            CultivationAction.Done
        )
    }

    @Test
    fun `Cultivation Water Refresh is used for spent opportunities and conserved otherwise`() {
        val spentButterflies = Butterfly.entries.map { ButterflyView(it, isFaceUp = false) }
        assertCultivationSpend(
            cultivationContext(
                water = 2,
                butterflies = spentButterflies,
                creature = listOf(spentPlant())
            ),
            SupportAction.UseWaterRefresh,
            CultivationAction.Support(SupportAction.UseWaterRefresh)
        )
        assertCultivationSpend(
            cultivationContext(water = 1),
            SupportAction.UseWaterRefresh,
            CultivationAction.Done
        )
    }

    @Test
    fun `Cultivation Mulch use depends on shared reserve and benefit rather than personality`() {
        val stored = SupportAction.UseMulch(Token.MULCH(DieSides.D12))
        val one = listOf(MulchView(0, DieSides.D12, false))
        val two = one + MulchView(1, DieSides.D4, false)

        assertCultivationSpend(cultivationContext(mulch = one), stored, CultivationAction.Done)
        assertCultivationSpend(
            cultivationContext(mulch = two), stored, CultivationAction.Support(stored)
        )
    }

    @Test
    fun `Battle Water reroll uses the same positive-transition gate for every personality`() {
        val action = BattleSupportAction.Shared(
            SupportAction.UseWaterReroll(HandDieChoice(0, 20, 1))
        )
        val dice = listOf(BattleDieView(0, 20, 1), BattleDieView(1, 6, 3))
        val useful = battleContext(ownTotal = 4, otherTotal = 7, ownDice = dice, water = 1)
        val alreadyWinning = battleContext(
            ownTotal = 13, otherTotal = 1,
            ownDice = dice + BattleDieView(2, 12, 9), water = 1
        )

        for (context in listOf(useful, alreadyWinning)) {
            val gates = profiles.map { profile ->
                requireNotNull(BattleDirectSupportAnalyzer(policy(profile))(context, action)).gate
            }
            assertEquals(List(profiles.size) { gates.first() }, gates)
            assertBattleSpendIsIdentical(context, action, battleRound(GameEffect.GAIN_ONE_VP))
        }
        assertTrue(requireNotNull(BattleDirectSupportAnalyzer()(useful, action)).passesSpendingGate)
        assertFalse(requireNotNull(BattleDirectSupportAnalyzer()(alreadyWinning, action)).passesSpendingGate)
    }

    @Test
    fun `Battle Mulch use follows the same win-flipped gate for every personality`() {
        val action = BattleSupportAction.Shared(SupportAction.UseMulch(Token.MULCH(DieSides.D12)))
        val dice = listOf(BattleDieView(0, 4, 4))
        val useful = battleContext(
            ownTotal = 4, otherTotal = 7, ownDice = dice,
            mulch = listOf(MulchView(0, DieSides.D12, false))
        )
        val alreadyWinning = battleContext(
            ownTotal = 13, otherTotal = 1,
            ownDice = dice + BattleDieView(1, 12, 9),
            mulch = listOf(MulchView(0, DieSides.D12, false))
        )

        for (context in listOf(useful, alreadyWinning)) {
            val gates = profiles.map { profile ->
                requireNotNull(BattleDirectSupportAnalyzer(policy(profile))(context, action)).gate
            }
            assertEquals(List(profiles.size) { gates.first() }, gates)
            assertBattleSpendIsIdentical(context, action, battleRound(GameEffect.GAIN_ONE_VP))
        }
        assertTrue(requireNotNull(BattleDirectSupportAnalyzer()(useful, action)).passesSpendingGate)
        assertFalse(requireNotNull(BattleDirectSupportAnalyzer()(alreadyWinning, action)).passesSpendingGate)
    }

    @Test
    fun `Battle Sunlight continuation saves a scarce token and spends surplus or final token identically`() {
        val action = BattleSupportAction.UseSunlight(BattleMainAction.RoundEffect1)
        val futureDie = battleRound(GameEffect.GAIN_D20_TO_DISCARD)
        val weak = battleRound(GameEffect.RAISE_DIE_PLUS_4)

        assertBattleSpendIsIdentical(
            battleContext(4, 7, sunlight = 1, battlesRemaining = 3),
            action,
            futureDie,
            expected = BattleTurnAction.FinalMain(BattleMainAction.RoundEffect1)
        )
        assertBattleSpendIsIdentical(
            battleContext(4, 7, sunlight = 2, battlesRemaining = 3),
            action,
            futureDie,
            expected = BattleTurnAction.Support(action)
        )
        assertBattleSpendIsIdentical(
            battleContext(4, 7, sunlight = 1, battlesRemaining = 1, finalBattle = true),
            action,
            weak,
            expected = BattleTurnAction.Support(action)
        )
    }

    private fun assertCultivationSpend(
        context: DecisionContext,
        resourceAction: SupportAction,
        expected: CultivationAction
    ) {
        val choices = listOf(CultivationAction.Support(resourceAction), CultivationAction.Done)
        val selected = profiles.map { profile ->
            val selectedByPolicy = HumanBaselineCultivationStrategy(
                policy = policy(profile),
                scoreEngine = BaselineScoreEngine(StrategyRandomizer.create(31L)),
                strategyRandomizer = StrategyRandomizer.create(31L)
            ).chooseAction(
                ChooseCultivationActionRequest(
                    roundCard = cultivationRound(),
                    mainActionsRemaining = 0,
                    legalChoices = choices,
                    context = context
                )
            )
            val score = CultivationSupportPriority.score(
                context, resourceAction, scorers, policy(profile)
            )
            selectedByPolicy to score
        }
        assertEquals(List(profiles.size) { expected }, selected.map { it.first })
        assertEquals(List(profiles.size) { selected.first().second }, selected.map { it.second })
    }

    private fun assertBattleSpendIsIdentical(
        context: DecisionContext,
        resourceAction: BattleSupportAction,
        round: RoundCard,
        expected: BattleTurnAction? = null
    ) {
        val choices = listOf(
            BattleTurnAction.Support(resourceAction),
            BattleTurnAction.FinalMain(BattleMainAction.RoundEffect1)
        )
        val selected = profiles.map { profile ->
            HumanBaselineBattleStrategy(
                policy = policy(profile),
                scoreEngine = BaselineScoreEngine(StrategyRandomizer.create(47L)),
                strategyRandomizer = StrategyRandomizer.create(47L)
            ).chooseTurnAction(
                ChooseBattleTurnActionRequest(
                    roundCard = round,
                    passNumber = 1,
                    legalChoices = choices,
                    context = context
                )
            )
        }
        assertEquals(List(profiles.size) { selected.first() }, selected)
        if (expected != null) assertEquals(expected, selected.first())

        val scores = profiles.map { profile ->
            BattleSupportPriority(policy = policy(profile)).score(
                context = context,
                roundCard = round,
                action = resourceAction,
                currentFinalMains = listOf(BattleMainAction.RoundEffect1),
                legalSupports = listOf(resourceAction)
            )
        }
        assertEquals(List(profiles.size) { scores.first() }, scores)
    }

    private fun cultivationContext(
        water: Int = 0,
        hand: List<DieView> = emptyList(),
        mulch: List<MulchView> = emptyList(),
        butterflies: List<ButterflyView> = emptyList(),
        creature: List<CreatureCardView> = emptyList()
    ): DecisionContext = DecisionContext.EMPTY.copy(
        phase = RoundCardType.CULTIVATION,
        self = DecisionContext.EMPTY.self.copy(
            board = DecisionContext.EMPTY.self.board.copy(
                water = water,
                hand = hand,
                mulch = mulch,
                butterflies = butterflies,
                creature = creature
            )
        )
    )

    private fun battleContext(
        ownTotal: Int,
        otherTotal: Int,
        ownDice: List<BattleDieView> = emptyList(),
        water: Int = 0,
        sunlight: Int = 0,
        mulch: List<MulchView> = emptyList(),
        battlesRemaining: Int = 3,
        finalBattle: Boolean = false
    ): DecisionContext = DecisionContext.EMPTY.copy(
        phase = RoundCardType.BATTLE,
        progress = DecisionContext.EMPTY.progress.copy(
            battleRoundsRemaining = battlesRemaining,
            totalBattleRounds = 3,
            isFinalBattleRound = finalBattle,
            roundsRemainingToReveal = if (finalBattle) 0 else 2
        ),
        self = DecisionContext.EMPTY.self.copy(
            board = DecisionContext.EMPTY.self.board.copy(
                id = actor,
                hand = ownDice.map { DieView(it.handIndex, it.sides, it.value) },
                water = water,
                sunlight = sunlight,
                mulch = mulch
            )
        ),
        battle = BattleView(
            playerOrder = listOf(actor, opponent),
            rows = listOf(
                BattleRowView(
                    row = StrikeRow.TOP,
                    closed = false,
                    players = listOf(
                        BattlePlayerRowView(
                            actor, StrikeRow.TOP, ownDice, emptyList(),
                            ownTotal, 0, ownTotal, false
                        ),
                        BattlePlayerRowView(
                            opponent, StrikeRow.TOP, emptyList(), emptyList(),
                            otherTotal, 0, otherTotal, false
                        )
                    )
                )
            )
        )
    )

    private fun spentPlant() = CreatureCardView(
        id = CreatureCardId(18),
        name = "Root_05_02",
        title = "Root Four More",
        type = PlantType.ROOT,
        cost = 5,
        effect = GameEffect.RAISE_DIE_PLUS_4,
        scoringRule = PlantScoringRule.Fixed(1),
        side = CreatureSide.LEFT,
        position = CreaturePosition(-1, 0),
        facing = CreatureCard.Facing.FACE_DOWN,
        isSnippable = true
    )

    private fun policy(profile: ResourcePersonality) =
        HumanBaselinePolicy(resourcePersonality = profile)

    private fun cultivationRound(): RoundCard = round(RoundCardType.CULTIVATION, GameEffect.GAIN_ONE_VP)
    private fun battleRound(effect: GameEffect): RoundCard = round(RoundCardType.BATTLE, effect)

    private fun round(type: RoundCardType, effect: GameEffect) = RoundCard(
        quantity = 1,
        name = "Spending independence fixture",
        type = type,
        firstEffect = RoundCardEffect("effect1", "", "", "", null, effect),
        secondEffect = RoundCardEffect("effect2", "", "", "", null, GameEffect.GAIN_ONE_VP),
        backImage = ""
    )
}
