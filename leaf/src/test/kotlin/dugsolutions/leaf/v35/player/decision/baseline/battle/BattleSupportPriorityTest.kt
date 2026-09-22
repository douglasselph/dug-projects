package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.plant.domain.PlantScoringRule
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.baseline.card.HumanBaselineCardScorerRegistry
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
import dugsolutions.leaf.v35.player.decision.context.CreatureCardView
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.context.DieView
import dugsolutions.leaf.v35.player.decision.support.HandDieChoice
import dugsolutions.leaf.v35.player.decision.support.SupportAction
import dugsolutions.leaf.v35.tokens.Critter
import dugsolutions.leaf.v35.wisp.domain.WispCard
import dugsolutions.leaf.v35.round.domain.RoundCard
import dugsolutions.leaf.v35.round.domain.RoundCardEffect
import dugsolutions.leaf.v35.round.domain.RoundCardType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BattleSupportPriorityTest {
    private val actor = PlayerId(0)
    private val opponent = PlayerId(1)

    @Test
    fun `named Battle transition dominates ordinary direct Support scoring`() {
        val priority = BattleSupportPriority()
        val context = context(actorTotal = 4, opponentTotal = 5)

        val score = priority.score(
            context,
            BattleSupportAction.PlaceCritter(Critter.BEE, StrikeRow.TOP)
        )

        assertTrue(score.total >= 500)
    }

    @Test
    fun `failed Water premium gate is categorically disqualified`() {
        val priority = BattleSupportPriority()
        val die = BattleDieView(handIndex = 0, sides = 6, value = 1)
        val context = context(
            actorTotal = 11,
            opponentTotal = 5,
            actorDice = listOf(die)
        )

        val score = priority.score(
            context,
            BattleSupportAction.Shared(
                SupportAction.UseWaterReroll(
                    HandDieChoice(index = 0, sides = 6, value = 1)
                )
            )
        )

        assertTrue(score.total < 0)
        assertTrue(score.adjustments.any { "positive named" in it.reason })
    }

    @Test
    fun `direct Worm without meaningful VP gain is categorically disqualified`() {
        val score = BattleSupportPriority().score(
            context(actorTotal = 4, opponentTotal = 8),
            BattleSupportAction.PlaceCritter(Critter.WORM, StrikeRow.TOP)
        )

        assertTrue(score.total < 0)
        assertTrue(score.adjustments.any { "Strike-VP gain" in it.reason })
    }

    @Test
    fun `ordinary Wisp retains its card-specific intrinsic score`() {
        val card = WispCard(
            quantity = 2,
            name = "Wisp_Award_VP",
            title = "Wisp of Honor",
            count = 2,
            effect = GameEffect.GAIN_ONE_VP,
            lineIcons = null,
            lineIconsHeight = 80,
            vpIcon = null,
            mainBackdrop = "",
            endGameVp = 2
        )
        val context = context(actorTotal = 1, opponentTotal = 3)
        val registry = HumanBaselineCardScorerRegistry()
        val priority = BattleSupportPriority(cardScorers = registry)

        val expected = registry.forWisp(card).wispPlayScore(context, card)
        val actual = priority.score(
            context,
            BattleSupportAction.Shared(SupportAction.PlayWisp(card))
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `full turn scoring routes Worm Flip through enabled Final Main analysis`() {
        val card = CreatureCardView(
            id = CreatureCardId(7),
            name = "Vine_11_04",
            title = "Vine's the Limit",
            type = PlantType.VINE,
            cost = 11,
            effect = GameEffect.SET_DIE_UP_TO_D12_TO_MAX,
            scoringRule = PlantScoringRule.Fixed(1),
            side = CreatureSide.LEFT,
            position = CreaturePosition(-1, 0),
            facing = CreatureCard.Facing.FACE_DOWN,
            isSnippable = true
        )
        val action = BattleSupportAction.Shared(SupportAction.UseWormFlip(card.id))
        val score = BattleSupportPriority().score(
            context = context(
                actorTotal = 4,
                opponentTotal = 10,
                actorDice = listOf(BattleDieView(0, 12, 4)),
                creature = listOf(card)
            ),
            roundCard = round(),
            action = action,
            currentFinalMains = listOf(BattleMainAction.RoundEffect1),
            legalSupports = listOf(action)
        )

        assertTrue(score.total > 500)
    }

    private fun context(
        actorTotal: Int,
        opponentTotal: Int,
        actorDice: List<BattleDieView> = emptyList(),
        creature: List<CreatureCardView> = emptyList()
    ): DecisionContext =
        DecisionContext.EMPTY.copy(
            self = DecisionContext.EMPTY.self.copy(
                board = DecisionContext.EMPTY.self.board.copy(
                    id = actor,
                    hand = actorDice.map { DieView(it.handIndex, it.sides, it.value) },
                    creature = creature
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
                                actor,
                                StrikeRow.TOP,
                                actorDice,
                                emptyList(),
                                actorTotal,
                                0,
                                actorTotal,
                                false
                            ),
                            BattlePlayerRowView(
                                opponent,
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

    private fun round() = RoundCard(
        quantity = 1,
        name = "battle",
        type = RoundCardType.BATTLE,
        firstEffect = RoundCardEffect("first", "", "", "", null, GameEffect.GAIN_ONE_VP),
        secondEffect = RoundCardEffect("second", "", "", "", null, GameEffect.GAIN_ONE_WISP),
        backImage = ""
    )
}
