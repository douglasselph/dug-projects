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
import dugsolutions.leaf.v35.player.decision.baseline.scoring.BaselineScoreEngine
import dugsolutions.leaf.v35.player.decision.battle.BattleDiePlacementReason
import dugsolutions.leaf.v35.player.decision.battle.BattleMainAction
import dugsolutions.leaf.v35.player.decision.battle.BattleSupportAction
import dugsolutions.leaf.v35.player.decision.battle.BattleTurnAction
import dugsolutions.leaf.v35.player.decision.battle.ChooseBattleDiePlacementRequest
import dugsolutions.leaf.v35.player.decision.battle.ChooseBattleTurnActionRequest
import dugsolutions.leaf.v35.player.decision.battle.ChooseBattleFirstMainActionRequest
import dugsolutions.leaf.v35.player.decision.context.BattleDieView
import dugsolutions.leaf.v35.player.decision.context.BattlePlayerRowView
import dugsolutions.leaf.v35.player.decision.context.BattleRowView
import dugsolutions.leaf.v35.player.decision.context.BattleView
import dugsolutions.leaf.v35.player.decision.context.CreatureCardView
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.context.DieView
import dugsolutions.leaf.v35.player.decision.random.StrategyRandomizer
import dugsolutions.leaf.v35.player.decision.support.HandDieChoice
import dugsolutions.leaf.v35.round.domain.RoundCard
import dugsolutions.leaf.v35.round.domain.RoundCardEffect
import dugsolutions.leaf.v35.round.domain.RoundCardType
import dugsolutions.leaf.v35.tokens.Critter
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class HumanBaselineBattleStrategyTest {

    /**
     * Executable statement of the approved ordinary-human Battle behavior.
     *
     * These tests intentionally assert externally visible strategy choices rather than
     * helper arithmetic. Together they pin representative First Main tactical choice,
     * actual-value/fresh-context die placement, Support-versus-Final-Main continuation,
     * cumulative reachability, and premium-resource preservation. The actual-placement
     * tie tests also keep StrategyRandomizer confined to genuine decision ties.
     *
     * Target/branch compatibility and target-before-RNG timing are exercised by the
     * focused HumanBaselineEffectStrategy*Battle*AlignmentTest classes; authoritative
     * Done/Live-Threat semantics remain focused in BattleRowAssessorTest,
     * BattleSupportCapacityAssessorTest, and BattleContinuationAssessorTest. Those are
     * part of the same approved contract without duplicating their edge matrices here.
     */
    @Nested
    inner class `Human Baseline Behavior Contract` {
    @Test
    fun `strong Battle Plant can beat Draw`() {
        val thorn = CreatureCard(
            id = CreatureCardId(2),
            card = PlantCard(6, "Vine_09_02", "Parting Thorn", PlantType.VINE, 9, null, "", "", "", "", "", "", "", GameEffect.FLIP_OWN_PLANT_OR_WOUND_CHOSEN_OPPONENT_CHOOSE_CARD_IN_BATTLE, PlantScoringRule.Fixed(1)),
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
    fun `expected Draw that flips a loss can beat a weak Plant`() {
        val actorId = PlayerId(0)
        val opponentId = PlayerId(1)
        val weakPlant = CreatureCard(
            id = CreatureCardId(3),
            card = PlantCard(6, "Vine_07_01", "Berry Important", PlantType.VINE, 7, null, "", "", "", "", "", "", "", GameEffect.RAISE_ANY_DIE_PLUS_1, PlantScoringRule.Fixed(1)),
            side = CreatureSide.LEFT,
            position = CreaturePosition(-1, 0),
            facing = CreatureCard.Facing.FACE_UP
        )
        val context = battleContext(
            actorId = actorId,
            opponentId = opponentId,
            actorTotal = 5,
            opponentTotal = 7,
            supplySides = 6
        )

        val chosen = HumanBaselineBattleStrategy().chooseFirstMainAction(
            ChooseBattleFirstMainActionRequest(
                roundCard = round(),
                legalChoices = listOf(BattleMainAction.Draw, BattleMainAction.ActivatePlant(weakPlant)),
                context = context
            )
        )

        assertEquals(BattleMainAction.Draw, chosen)
    }

    @Test
    fun `strong Plant still beats Draw when expected placement only pads a secure row`() {
        val actorId = PlayerId(0)
        val opponentId = PlayerId(1)
        val thorn = CreatureCard(
            id = CreatureCardId(4),
            card = PlantCard(6, "Vine_09_02", "Parting Thorn", PlantType.VINE, 9, null, "", "", "", "", "", "", "", GameEffect.FLIP_OWN_PLANT_OR_WOUND_CHOSEN_OPPONENT_CHOOSE_CARD_IN_BATTLE, PlantScoringRule.Fixed(1)),
            side = CreatureSide.LEFT,
            position = CreaturePosition(-1, 0),
            facing = CreatureCard.Facing.FACE_UP
        )
        val context = battleContext(
            actorId = actorId,
            opponentId = opponentId,
            actorTotal = 15,
            opponentTotal = 5,
            supplySides = 4
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
    fun `non-grid Round Effect keeps intrinsic value against tactically weak Draw`() {
        val actorId = PlayerId(0)
        val opponentId = PlayerId(1)
        val context = battleContext(
            actorId = actorId,
            opponentId = opponentId,
            actorTotal = 15,
            opponentTotal = 5,
            supplySides = 4
        )

        val chosen = HumanBaselineBattleStrategy().chooseFirstMainAction(
            ChooseBattleFirstMainActionRequest(
                roundCard = round(),
                legalChoices = listOf(BattleMainAction.Draw, BattleMainAction.RoundEffect1),
                context = context
            )
        )

        assertEquals(BattleMainAction.RoundEffect1, chosen)
    }

    @Test
    fun `actual placement uses rolled value rather than die expectation`() {
        val actorId = PlayerId(0)
        val opponentId = PlayerId(1)
        val randomizer = RecordingRandomizer(result = 1)
        val context = placementContext(
            actorId,
            opponentId,
            placementRow(StrikeRow.TOP, actorId, opponentId, actorTotal = 0, opponentTotal = 5),
            placementRow(StrikeRow.MIDDLE, actorId, opponentId, actorTotal = 0, opponentTotal = 2)
        )

        val chosen = HumanBaselineBattleStrategy(
            scoreEngine = BaselineScoreEngine(randomizer)
        ).chooseDiePlacement(
            ChooseBattleDiePlacementRequest(
                die = HandDieChoice(index = 0, sides = 20, value = 1),
                reason = BattleDiePlacementReason.MAIN_DRAW,
                legalRows = listOf(StrikeRow.TOP, StrikeRow.MIDDLE),
                context = context
            )
        )

        assertEquals(StrikeRow.TOP, chosen)
        assertTrue(randomizer.bounds.isEmpty())
    }

    @Test
    fun `actual placement reevaluates each fresh Battle context`() {
        val actorId = PlayerId(0)
        val opponentId = PlayerId(1)
        val strategy = HumanBaselineBattleStrategy()
        val request: (DecisionContext) -> ChooseBattleDiePlacementRequest = { context ->
            ChooseBattleDiePlacementRequest(
                die = HandDieChoice(index = 0, sides = 6, value = 3),
                reason = BattleDiePlacementReason.MULCH,
                legalRows = listOf(StrikeRow.TOP, StrikeRow.MIDDLE),
                context = context
            )
        }
        val topNeedsDie = placementContext(
            actorId,
            opponentId,
            placementRow(StrikeRow.TOP, actorId, opponentId, actorTotal = 5, opponentTotal = 7),
            placementRow(StrikeRow.MIDDLE, actorId, opponentId, actorTotal = 10, opponentTotal = 1)
        )
        val middleNeedsDie = placementContext(
            actorId,
            opponentId,
            placementRow(StrikeRow.TOP, actorId, opponentId, actorTotal = 10, opponentTotal = 1),
            placementRow(StrikeRow.MIDDLE, actorId, opponentId, actorTotal = 5, opponentTotal = 7)
        )

        assertEquals(StrikeRow.TOP, strategy.chooseDiePlacement(request(topNeedsDie)))
        assertEquals(StrikeRow.MIDDLE, strategy.chooseDiePlacement(request(middleNeedsDie)))
    }

    @Test
    fun `exact actual placement tie uses StrategyRandomizer`() {
        val actorId = PlayerId(0)
        val opponentId = PlayerId(1)
        val randomizer = RecordingRandomizer(result = 1)
        val context = placementContext(
            actorId,
            opponentId,
            placementRow(StrikeRow.TOP, actorId, opponentId, actorTotal = 1, opponentTotal = 3),
            placementRow(StrikeRow.MIDDLE, actorId, opponentId, actorTotal = 1, opponentTotal = 3)
        )

        val chosen = HumanBaselineBattleStrategy(
            scoreEngine = BaselineScoreEngine(randomizer)
        ).chooseDiePlacement(
            ChooseBattleDiePlacementRequest(
                die = HandDieChoice(index = 0, sides = 6, value = 3),
                reason = BattleDiePlacementReason.EFFECT,
                legalRows = listOf(StrikeRow.TOP, StrikeRow.MIDDLE),
                context = context
            )
        )

        assertEquals(StrikeRow.MIDDLE, chosen)
        assertEquals(listOf(2), randomizer.bounds)
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

        @Test
        fun `meaningful Support can delay Final Main`() {
            val actorId = PlayerId(0)
            val opponentId = PlayerId(1)
            val context = battleContext(actorId, opponentId, actorTotal = 4, opponentTotal = 7, supplySides = 4)
                .let { it.copy(self = it.self.copy(board = it.self.board.copy(bees = 1))) }

            val chosen = HumanBaselineBattleStrategy().chooseTurnAction(
                ChooseBattleTurnActionRequest(
                    roundCard = round(),
                    passNumber = 1,
                    legalChoices = listOf(
                        BattleTurnAction.Support(BattleSupportAction.PlaceCritter(Critter.BEE, StrikeRow.TOP)),
                        BattleTurnAction.FinalMain(BattleMainAction.RoundEffect1)
                    ),
                    context = context
                )
            )

            assertIs<BattleTurnAction.Support>(chosen)
        }

        @Test
        fun `policy-rejected Support does not delay Final Main`() {
            val actorId = PlayerId(0)
            val opponentId = PlayerId(1)
            val context = battleContext(actorId, opponentId, actorTotal = 1, opponentTotal = 20, supplySides = 4)
                .let { it.copy(self = it.self.copy(board = it.self.board.copy(worms = 1))) }

            val chosen = HumanBaselineBattleStrategy().chooseTurnAction(
                ChooseBattleTurnActionRequest(
                    roundCard = round(),
                    passNumber = 1,
                    legalChoices = listOf(
                        BattleTurnAction.Support(BattleSupportAction.PlaceCritter(Critter.WORM, StrikeRow.TOP)),
                        BattleTurnAction.FinalMain(BattleMainAction.RoundEffect1)
                    ),
                    context = context
                )
            )

            assertIs<BattleTurnAction.FinalMain>(chosen)
        }

        @Test
        fun `cumulative Bee reachability can justify continuing one Support at a time`() {
            val actorId = PlayerId(0)
            val opponentId = PlayerId(1)
            val context = battleContext(actorId, opponentId, actorTotal = 4, opponentTotal = 9, supplySides = 4)
                .let { it.copy(self = it.self.copy(board = it.self.board.copy(bees = 3, beeValue = 2))) }

            val chosen = HumanBaselineBattleStrategy().chooseTurnAction(
                ChooseBattleTurnActionRequest(
                    roundCard = round(),
                    passNumber = 1,
                    legalChoices = listOf(
                        BattleTurnAction.Support(BattleSupportAction.PlaceCritter(Critter.BEE, StrikeRow.TOP)),
                        BattleTurnAction.FinalMain(BattleMainAction.RoundEffect1)
                    ),
                    context = context
                )
            )

            assertIs<BattleTurnAction.Support>(chosen)
        }

    }

    private fun battleContext(
        actorId: PlayerId,
        opponentId: PlayerId,
        actorTotal: Int,
        opponentTotal: Int,
        supplySides: Int
    ): DecisionContext {
        val row = BattleRowView(
            row = StrikeRow.TOP,
            closed = false,
            players = listOf(
                BattlePlayerRowView(
                    actorId,
                    StrikeRow.TOP,
                    listOf(BattleDieView(0, 6, 1)),
                    emptyList(),
                    actorTotal,
                    0,
                    actorTotal,
                    false
                ),
                BattlePlayerRowView(
                    opponentId,
                    StrikeRow.TOP,
                    emptyList(),
                    emptyList(),
                    opponentTotal,
                    0,
                    opponentTotal,
                    false
                )
            )
        )
        return DecisionContext.EMPTY.copy(
            phase = RoundCardType.BATTLE,
            self = DecisionContext.EMPTY.self.copy(
                board = DecisionContext.EMPTY.self.board.copy(
                    id = actorId,
                    supply = listOf(DieView(0, supplySides, 1))
                )
            ),
            battle = BattleView(
                playerOrder = listOf(actorId, opponentId),
                rows = listOf(row)
            )
        )
    }

    private fun placementContext(
        actorId: PlayerId,
        opponentId: PlayerId,
        vararg rows: BattleRowView
    ): DecisionContext =
        DecisionContext.EMPTY.copy(
            phase = RoundCardType.BATTLE,
            self = DecisionContext.EMPTY.self.copy(
                board = DecisionContext.EMPTY.self.board.copy(id = actorId)
            ),
            battle = BattleView(
                playerOrder = listOf(actorId, opponentId),
                rows = rows.toList()
            )
        )

    private fun placementRow(
        row: StrikeRow,
        actorId: PlayerId,
        opponentId: PlayerId,
        actorTotal: Int,
        opponentTotal: Int
    ): BattleRowView =
        BattleRowView(
            row = row,
            closed = false,
            players = listOf(
                BattlePlayerRowView(
                    actorId,
                    row,
                    emptyList(),
                    emptyList(),
                    actorTotal,
                    0,
                    actorTotal,
                    false
                ),
                BattlePlayerRowView(
                    opponentId,
                    row,
                    emptyList(),
                    emptyList(),
                    opponentTotal,
                    0,
                    opponentTotal,
                    false
                )
            )
        )

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

    private class RecordingRandomizer(
        private val result: Int
    ) : StrategyRandomizer {
        val bounds = mutableListOf<Int>()

        override fun nextInt(until: Int): Int {
            bounds += until
            return result
        }
    }
}
