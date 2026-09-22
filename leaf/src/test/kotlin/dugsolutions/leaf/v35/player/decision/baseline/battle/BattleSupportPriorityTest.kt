package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.baseline.card.HumanBaselineCardScorerRegistry
import dugsolutions.leaf.v35.player.decision.battle.BattleSupportAction
import dugsolutions.leaf.v35.player.decision.context.BattleDieView
import dugsolutions.leaf.v35.player.decision.context.BattlePlayerRowView
import dugsolutions.leaf.v35.player.decision.context.BattleRowView
import dugsolutions.leaf.v35.player.decision.context.BattleView
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.support.HandDieChoice
import dugsolutions.leaf.v35.player.decision.support.SupportAction
import dugsolutions.leaf.v35.tokens.Critter
import dugsolutions.leaf.v35.wisp.domain.WispCard
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

    private fun context(
        actorTotal: Int,
        opponentTotal: Int,
        actorDice: List<BattleDieView> = emptyList()
    ): DecisionContext =
        DecisionContext.EMPTY.copy(
            self = DecisionContext.EMPTY.self.copy(
                board = DecisionContext.EMPTY.self.board.copy(id = actor)
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
}
