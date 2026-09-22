package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.plant.domain.PlantScoringRule
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.creature.CreatureCard
import dugsolutions.leaf.v35.player.creature.CreatureCardId
import dugsolutions.leaf.v35.player.creature.CreaturePosition
import dugsolutions.leaf.v35.player.creature.CreatureSide
import dugsolutions.leaf.v35.player.decision.battle.BattleMainAction
import dugsolutions.leaf.v35.player.decision.battle.BattleSupportAction
import dugsolutions.leaf.v35.player.decision.context.BattleDieView
import dugsolutions.leaf.v35.player.decision.context.BattlePlayerRowView
import dugsolutions.leaf.v35.player.decision.context.BattleRowView
import dugsolutions.leaf.v35.player.decision.context.BattleView
import dugsolutions.leaf.v35.player.decision.context.ButterflyView
import dugsolutions.leaf.v35.player.decision.context.CreatureCardView
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.context.DieView
import dugsolutions.leaf.v35.player.decision.support.SupportAction
import dugsolutions.leaf.v35.round.domain.RoundCard
import dugsolutions.leaf.v35.round.domain.RoundCardEffect
import dugsolutions.leaf.v35.round.domain.RoundCardType
import dugsolutions.leaf.v35.tokens.Butterfly
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BattleEnablingSupportAnalyzerTest {
    private val analyzer = BattleEnablingSupportAnalyzer()

    @Test
    fun `Worm Flip passes only for a face-down Plant with a better Final Main`() {
        val card = maxDiePlant(faceDown = true)
        val action = worm(card.id)
        val passed = requireNotNull(
            analyzer(
                context = context(listOf(card), actorTotal = 4, opponentTotal = 10),
                roundCard = round(),
                action = action,
                currentFinalMains = listOf(BattleMainAction.RoundEffect1),
                legalSupports = listOf(action)
            )
        )
        val faceUp = requireNotNull(
            analyzer(
                context = context(listOf(card.copy(facing = CreatureCard.Facing.FACE_UP))),
                roundCard = round(),
                action = action,
                currentFinalMains = listOf(BattleMainAction.RoundEffect1),
                legalSupports = listOf(action)
            )
        )

        assertEquals(BattleEnablingSupportGate.PASSED, passed.gate)
        assertTrue(passed.incrementalValue > 0)
        assertTrue(passed.individuallyWorthwhile)
        assertEquals(BattleEnablingSupportGate.TARGET_IS_NOT_FACE_DOWN, faceUp.gate)
        assertFalse(faceUp.individuallyWorthwhile)
    }

    @Test
    fun `Worm Flip rejects preservation that does not improve the visible Final Main`() {
        val card = weakPlant()
        val action = worm(card.id)
        val result = requireNotNull(
            analyzer(
                context = context(listOf(card), actorTotal = 12, opponentTotal = 1),
                roundCard = round(
                    first = GameEffect.STEAL_RANDOM_WISP_FROM_ALL_OPPONENTS
                ),
                action = action,
                currentFinalMains = listOf(BattleMainAction.RoundEffect1),
                legalSupports = listOf(action)
            )
        )

        assertEquals(BattleEnablingSupportGate.NO_BETTER_FINAL_MAIN, result.gate)
        assertTrue(result.incrementalValue <= 0)
        assertFalse(result.individuallyWorthwhile)
    }

    @Test
    fun `Worm Flip compares against a current Plant Main with the same tactical analysis`() {
        val target = maxDiePlant(faceDown = true)
        val current = maxDiePlant(faceDown = false).copy(id = CreatureCardId(9))
        val action = worm(target.id)
        val result = requireNotNull(
            analyzer(
                context = context(
                    listOf(target, current),
                    actorTotal = 4,
                    opponentTotal = 10
                ),
                roundCard = round(),
                action = action,
                currentFinalMains = listOf(
                    BattleMainAction.ActivatePlant(creatureCard(current))
                ),
                legalSupports = listOf(action)
            )
        )

        assertEquals(BattleEnablingSupportGate.NO_BETTER_FINAL_MAIN, result.gate)
        assertEquals(0, result.incrementalValue)
    }

    @Test
    fun `Water Refresh requires the policy minimum improvement steps`() {
        val strong = maxDiePlant(faceDown = true)
        val weak = raiseFourPlant()
        val strongResult = analyzeWater(
            context(listOf(strong), actorTotal = 4, opponentTotal = 10)
        )
        val oneStepResult = analyzeWater(
            context(listOf(weak), actorTotal = 4, opponentTotal = 10)
        )

        assertEquals(BattleEnablingSupportGate.PASSED, strongResult.gate)
        assertTrue(requireNotNull(strongResult.enabledOpportunity).improvementStepCount >= 2)
        assertEquals(
            BattleEnablingSupportGate.WATER_REQUIRES_IMPROVEMENT_STEPS,
            oneStepResult.gate
        )
        assertEquals(1, requireNotNull(oneStepResult.enabledOpportunity).improvementStepCount)
    }

    @Test
    fun `refreshed Butterflies are secondary and cannot bypass the Plant gate`() {
        val butterflies = listOf(
            ButterflyView(Butterfly.GREEN, isFaceUp = false),
            ButterflyView(Butterfly.YELLOW, isFaceUp = false)
        )
        val withoutPlant = analyzeWater(context(emptyList(), butterflies = butterflies))
        val withPlant = analyzeWater(
            context(
                listOf(maxDiePlant(faceDown = true)),
                actorTotal = 4,
                opponentTotal = 10,
                butterflies = butterflies
            )
        )

        assertEquals(BattleEnablingSupportGate.NO_FACE_DOWN_PLANT, withoutPlant.gate)
        assertFalse(withoutPlant.individuallyWorthwhile)
        assertEquals(2, withoutPlant.refreshedButterflyCount)
        assertEquals(BattleEnablingSupportGate.PASSED, withPlant.gate)
        assertEquals(2, withPlant.refreshedButterflyCount)
        assertTrue(withPlant.priority.adjustments.any { "secondary" in it.reason })
    }

    @Test
    fun `Worm is preferred over Water for equivalent single-Plant recovery`() {
        val card = maxDiePlant(faceDown = true)
        val worm = worm(card.id)
        val water = water()
        val context = context(listOf(card), actorTotal = 4, opponentTotal = 10)
        val legal = listOf(worm, water)

        val wormResult = requireNotNull(
            analyzer(
                context,
                round(),
                worm,
                listOf(BattleMainAction.RoundEffect1),
                legal
            )
        )
        val waterResult = requireNotNull(
            analyzer(
                context,
                round(),
                water,
                listOf(BattleMainAction.RoundEffect1),
                legal
            )
        )

        assertEquals(1, wormResult.equivalentRecoveryPreference)
        assertEquals(-1, waterResult.equivalentRecoveryPreference)
        assertTrue(wormResult.priority.total > waterResult.priority.total)
    }

    private fun analyzeWater(context: DecisionContext): BattleEnablingSupportAnalysis =
        requireNotNull(
            analyzer(
                context = context,
                roundCard = round(),
                action = water(),
                currentFinalMains = listOf(BattleMainAction.RoundEffect1),
                legalSupports = listOf(water())
            )
        )

    private fun context(
        plants: List<CreatureCardView>,
        actorTotal: Int = 4,
        opponentTotal: Int = 10,
        butterflies: List<ButterflyView> = emptyList()
    ): DecisionContext {
        val die = BattleDieView(handIndex = 0, sides = 12, value = actorTotal)
        return DecisionContext.EMPTY.copy(
            self = DecisionContext.EMPTY.self.copy(
                board = DecisionContext.EMPTY.self.board.copy(
                    id = ACTOR,
                    hand = listOf(DieView(0, 12, actorTotal)),
                    butterflies = butterflies,
                    creature = plants
                )
            ),
            battle = BattleView(
                playerOrder = listOf(ACTOR, OPPONENT),
                rows = listOf(
                    BattleRowView(
                        StrikeRow.TOP,
                        false,
                        listOf(
                            BattlePlayerRowView(
                                ACTOR,
                                StrikeRow.TOP,
                                listOf(die),
                                emptyList(),
                                actorTotal,
                                0,
                                actorTotal,
                                false
                            ),
                            BattlePlayerRowView(
                                OPPONENT,
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
                )
            )
        )
    }

    private fun maxDiePlant(faceDown: Boolean) = plant(
        id = 1,
        name = "Vine_11_04",
        effect = GameEffect.SET_DIE_UP_TO_D12_TO_MAX,
        type = PlantType.VINE,
        faceDown = faceDown
    )

    private fun raiseFourPlant() = plant(
        id = 2,
        name = "Root_05_02",
        effect = GameEffect.RAISE_DIE_PLUS_4,
        type = PlantType.ROOT,
        faceDown = true
    )

    private fun weakPlant() = plant(
        id = 3,
        name = "Root_05_03",
        effect = GameEffect.REROLL_DIE_UNTIL_3_PLUS_IGNORE_ROLL_REWARDS,
        type = PlantType.ROOT,
        faceDown = true
    )

    private fun plant(
        id: Int,
        name: String,
        effect: GameEffect,
        type: PlantType,
        faceDown: Boolean
    ) = CreatureCardView(
        id = CreatureCardId(id),
        name = name,
        title = name,
        type = type,
        cost = 5,
        effect = effect,
        scoringRule = PlantScoringRule.Fixed(1),
        side = CreatureSide.LEFT,
        position = CreaturePosition(-id, 0),
        facing = if (faceDown) CreatureCard.Facing.FACE_DOWN else CreatureCard.Facing.FACE_UP,
        isSnippable = true
    )

    private fun creatureCard(view: CreatureCardView) = CreatureCard(
        id = view.id,
        card = PlantCard(
            quantity = 1,
            name = view.name,
            title = view.title,
            type = view.type,
            cost = view.cost,
            lineIcon = null,
            vpIcon = "",
            typeIcon = "",
            fgColor = "",
            textColor = "",
            fullImage = "",
            backgroundImage = "",
            cardBackgroundImage = "",
            effect = view.effect,
            scoringRule = view.scoringRule
        ),
        side = view.side,
        position = view.position,
        facing = view.facing
    )

    private fun worm(cardId: CreatureCardId) = BattleSupportAction.Shared(
        SupportAction.UseWormFlip(cardId)
    )

    private fun water() = BattleSupportAction.Shared(SupportAction.UseWaterRefresh)

    private fun round(
        first: GameEffect = GameEffect.GAIN_ONE_VP,
        second: GameEffect = GameEffect.GAIN_ONE_WISP
    ) = RoundCard(
        quantity = 1,
        name = "battle",
        type = RoundCardType.BATTLE,
        firstEffect = RoundCardEffect("first", "", "", "", null, first),
        secondEffect = RoundCardEffect("second", "", "", "", null, second),
        backImage = ""
    )

    private companion object {
        val ACTOR = PlayerId(0)
        val OPPONENT = PlayerId(1)
    }
}
