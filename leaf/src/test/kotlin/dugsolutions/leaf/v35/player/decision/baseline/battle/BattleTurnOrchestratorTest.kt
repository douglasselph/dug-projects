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
import dugsolutions.leaf.v35.player.decision.context.GameProgressView
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


    @Test
    fun `Sunlight continues when extra Main is clearly worthwhile`() {
        val result = orchestrator(
            context = context(actorTotal = 4, opponentTotal = 7, bees = 0),
            roundCard = round(),
            legalChoices = listOf(
                sunlight(BattleMainAction.RoundEffect1),
                finalMain()
            )
        )

        assertTrue(result.chooseSupport)
        assertTrue(result.worthwhileSupports.any { it is BattleSupportAction.UseSunlight })
    }

    @Test
    fun `Sunlight is preserved when available extra Main is weak`() {
        val weakRound = RoundCard(
            quantity = 1,
            name = "Battle_Weak",
            type = RoundCardType.BATTLE,
            firstEffect = RoundCardEffect("Weak", "", "", "", null, GameEffect.RAISE_DIE_PLUS_3),
            secondEffect = RoundCardEffect("Weak2", "", "", "", null, GameEffect.RAISE_DIE_PLUS_3),
            backImage = ""
        )
        val result = orchestrator(
            context = context(actorTotal = 20, opponentTotal = 1, bees = 0),
            roundCard = weakRound,
            legalChoices = listOf(
                sunlight(BattleMainAction.RoundEffect1),
                BattleTurnAction.FinalMain(BattleMainAction.RoundEffect1)
            )
        )

        assertFalse(result.chooseSupport)
    }


    @Test
    fun `early Battle keeps last Sunlight for marginal extra Main`() {
        val result = orchestrator(
            context = context(actorTotal = 4, opponentTotal = 7, bees = 0, sunlight = 1, battlesRemaining = 3),
            roundCard = round(),
            legalChoices = listOf(
                sunlight(BattleMainAction.RoundEffect1),
                finalMain()
            )
        )

        assertFalse(result.chooseSupport)
    }

    @Test
    fun `surplus Sunlight allows marginal extra Main without sacrificing last reserve`() {
        val result = orchestrator(
            context = context(actorTotal = 4, opponentTotal = 7, bees = 0, sunlight = 2, battlesRemaining = 3),
            roundCard = round(),
            legalChoices = listOf(
                sunlight(BattleMainAction.RoundEffect1),
                finalMain()
            )
        )

        assertTrue(result.chooseSupport)
    }

    @Test
    fun `final Battle spends Sunlight on otherwise modest legal extra Main`() {
        val weakRound = RoundCard(
            quantity = 1,
            name = "Battle_Final_Sunlight",
            type = RoundCardType.BATTLE,
            firstEffect = RoundCardEffect("Weak", "", "", "", null, GameEffect.RAISE_DIE_PLUS_3),
            secondEffect = RoundCardEffect("Weak2", "", "", "", null, GameEffect.RAISE_DIE_PLUS_3),
            backImage = ""
        )
        val result = orchestrator(
            context = context(actorTotal = 4, opponentTotal = 7, bees = 0, sunlight = 1, battlesRemaining = 1, finalBattle = true),
            roundCard = weakRound,
            legalChoices = listOf(
                sunlight(BattleMainAction.RoundEffect1),
                BattleTurnAction.FinalMain(BattleMainAction.RoundEffect1)
            )
        )

        assertTrue(result.chooseSupport)
    }

    private fun context(
        actorTotal: Int,
        opponentTotal: Int,
        bees: Int,
        worms: Int = 0,
        beeValue: Int = Critter.BEE.baseValue,
        sunlight: Int = 1,
        battlesRemaining: Int? = null,
        finalBattle: Boolean = false
    ): DecisionContext =
        DecisionContext.EMPTY.copy(
            phase = RoundCardType.BATTLE,
            progress = DecisionContext.EMPTY.progress.copy(
                battleRoundsRemaining = battlesRemaining,
                isFinalBattleRound = finalBattle
            ),
            self = DecisionContext.EMPTY.self.copy(
                board = DecisionContext.EMPTY.self.board.copy(
                    id = actor,
                    bees = bees,
                    worms = worms,
                    beeValue = beeValue,
                    sunlight = sunlight
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


    private fun sunlight(main: BattleMainAction): BattleTurnAction =
        BattleTurnAction.Support(BattleSupportAction.UseSunlight(main))

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
