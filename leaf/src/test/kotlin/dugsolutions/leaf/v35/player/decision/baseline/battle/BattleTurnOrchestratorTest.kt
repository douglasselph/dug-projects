package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.battle.BattleMainAction
import dugsolutions.leaf.v35.player.decision.battle.BattleSupportAction
import dugsolutions.leaf.v35.player.decision.battle.BattleTurnAction
import dugsolutions.leaf.v35.player.decision.context.BattlePlayerRowView
import dugsolutions.leaf.v35.player.decision.context.BattleRowView
import dugsolutions.leaf.v35.player.decision.context.BattleView
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.round.domain.RoundCard
import dugsolutions.leaf.v35.round.domain.RoundCardEffect
import dugsolutions.leaf.v35.round.domain.RoundCardType
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.tokens.Critter
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BattleTurnOrchestratorTest {
    private val actor = PlayerId(0)
    private val opponent = PlayerId(1)
    private val orchestrator = BattleTurnOrchestrator()

    @Test
    fun `secured battle chooses Final Main even when Bee remains`() {
        val result = orchestrator(
            context = context(actorTotal = 20, opponentTotal = 1, bees = 1),
            roundCard = round(),
            legalChoices = listOf(bee(), finalMain())
        )

        assertFalse(result.chooseSupport)
        assertEquals(BattleContinuationReason.ALL_RELEVANT_ROWS_SECURED, result.continuation.reason)
    }

    @Test
    fun `individually worthwhile Bee keeps player active`() {
        val result = orchestrator(
            context = context(actorTotal = 4, opponentTotal = 7, bees = 1),
            roundCard = round(),
            legalChoices = listOf(bee(), finalMain())
        )

        assertTrue(result.chooseSupport)
        assertEquals(listOf((bee() as BattleTurnAction.Support).action), result.worthwhileSupports)
        assertEquals(BattleContinuationReason.INDIVIDUALLY_WORTHWHILE_SUPPORT, result.continuation.reason)
    }

    @Test
    fun `cumulative Bee path keeps player active one support at a time`() {
        val result = orchestrator(
            context = context(actorTotal = 4, opponentTotal = 9, bees = 3, beeValue = 2),
            roundCard = round(),
            legalChoices = listOf(bee(), finalMain())
        )

        assertTrue(result.chooseSupport)
        assertTrue(result.continuation.hasMeaningfulCumulativePath)
        assertEquals(listOf((bee() as BattleTurnAction.Support).action), result.worthwhileSupports)
    }

    @Test
    fun `no useful support path chooses Final Main while Bee remains`() {
        val result = orchestrator(
            context = context(actorTotal = 1, opponentTotal = 20, bees = 0, worms = 1),
            roundCard = round(),
            legalChoices = listOf(worm(), finalMain())
        )

        assertFalse(result.chooseSupport)
        assertEquals(BattleContinuationReason.NO_WORTHWHILE_PATH, result.continuation.reason)
    }

    private fun context(
        actorTotal: Int,
        opponentTotal: Int,
        bees: Int,
        worms: Int = 0,
        beeValue: Int = Critter.BEE.baseValue
    ): DecisionContext =
        DecisionContext.EMPTY.copy(
            phase = RoundCardType.BATTLE,
            self = DecisionContext.EMPTY.self.copy(
                board = DecisionContext.EMPTY.self.board.copy(
                    id = actor,
                    bees = bees,
                    worms = worms,
                    beeValue = beeValue
                )
            ),
            battle = BattleView(
                playerOrder = listOf(actor, opponent),
                rows = listOf(
                    BattleRowView(
                        row = StrikeRow.TOP,
                        closed = false,
                        players = listOf(
                            playerRow(actor, actorTotal),
                            playerRow(opponent, opponentTotal)
                        )
                    )
                )
            )
        )

    private fun playerRow(id: PlayerId, total: Int) =
        BattlePlayerRowView(id, StrikeRow.TOP, emptyList(), emptyList(), total, 0, total, false)

    private fun bee(): BattleTurnAction =
        BattleTurnAction.Support(BattleSupportAction.PlaceCritter(Critter.BEE, StrikeRow.TOP))

    private fun worm(): BattleTurnAction =
        BattleTurnAction.Support(BattleSupportAction.PlaceCritter(Critter.WORM, StrikeRow.TOP))

    private fun finalMain(): BattleTurnAction =
        BattleTurnAction.FinalMain(BattleMainAction.RoundEffect1)

    private fun round() = RoundCard(
        quantity = 1,
        name = "Battle_Test",
        type = RoundCardType.BATTLE,
        firstEffect = RoundCardEffect("Bloom", "", "", "", null, GameEffect.GAIN_ONE_VP),
        secondEffect = RoundCardEffect("Burrow", "", "", "", null, GameEffect.GAIN_TWO_WORMS),
        backImage = ""
    )
}
