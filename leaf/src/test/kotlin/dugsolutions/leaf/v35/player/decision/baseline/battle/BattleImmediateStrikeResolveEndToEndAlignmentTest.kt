package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.baseline.effect.HumanBaselineEffectStrategy
import dugsolutions.leaf.v35.player.decision.battle.BattleMainAction
import dugsolutions.leaf.v35.player.decision.battle.BattleSupportAction
import dugsolutions.leaf.v35.player.decision.battle.BattleTurnAction
import dugsolutions.leaf.v35.player.decision.context.*
import dugsolutions.leaf.v35.player.decision.effect.ChooseEffectStrikeRowRequest
import dugsolutions.leaf.v35.player.decision.support.SupportAction
import dugsolutions.leaf.v35.round.domain.RoundCard
import dugsolutions.leaf.v35.round.domain.RoundCardEffect
import dugsolutions.leaf.v35.round.domain.RoundCardType
import dugsolutions.leaf.v35.wisp.domain.WispCard
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BattleImmediateStrikeResolveEndToEndAlignmentTest {
    private val actor = PlayerId(0)
    private val opponent = PlayerId(1)

    @Test
    fun `top-level rejects Resolve when no row meets B13 lock-in policy`() {
        val context = context(ownTotal = 9, opponentTotal = 5, opponentWisps = 3)
        val support = support()
        val evaluation = BattleImmediateStrikeResolveEvaluator()(context, StrikeRow.TOP)
        val orchestration = orchestrate(context, support)

        assertEquals(false, evaluation?.passesPolicy)
        assertTrue(BattleSupportPriority().score(context, support).total < 0)
        assertFalse(orchestration.chooseSupport)
    }

    @Test
    fun `top-level recognizes Resolve when B13 has a useful legal lock-in row`() {
        val context = context(ownTotal = 10, opponentTotal = 5, opponentWisps = 3)
        val support = support()
        val evaluation = BattleImmediateStrikeResolveEvaluator()(context, StrikeRow.TOP)
        val downstream = HumanBaselineEffectStrategy().chooseStrikeRow(
            ChooseEffectStrikeRowRequest(
                effect = GameEffect.RESOLVE_STRIKE_IMMEDIATELY_AND_CLEAR_ROW,
                legalChoices = listOf(StrikeRow.TOP),
                context = context
            )
        )
        val orchestration = orchestrate(context, support)

        assertEquals(true, evaluation?.passesPolicy)
        assertEquals(StrikeRow.TOP, downstream)
        assertTrue(BattleSupportPriority().score(context, support).total > 0)
        assertTrue(orchestration.chooseSupport)
    }

    private fun orchestrate(
        context: DecisionContext,
        support: BattleSupportAction
    ) = BattleTurnOrchestrator()(
        context = context,
        roundCard = round(),
        legalChoices = listOf(
            BattleTurnAction.Support(support),
            BattleTurnAction.FinalMain(BattleMainAction.RoundEffect1)
        )
    )

    private fun support(): BattleSupportAction =
        BattleSupportAction.Shared(SupportAction.PlayWisp(resolveWisp()))

    private fun resolveWisp() = WispCard(
        quantity = 4,
        name = "Wisps_Resolve",
        title = "Wisp's Resolve",
        count = 4,
        effect = GameEffect.RESOLVE_STRIKE_IMMEDIATELY_AND_CLEAR_ROW,
        lineIcons = null,
        lineIconsHeight = 40,
        vpIcon = null,
        mainBackdrop = "",
        endGameVp = 0
    )

    private fun context(
        ownTotal: Int,
        opponentTotal: Int,
        opponentWisps: Int
    ): DecisionContext = DecisionContext.EMPTY.copy(
        phase = RoundCardType.BATTLE,
        self = DecisionContext.EMPTY.self.copy(
            board = DecisionContext.EMPTY.self.board.copy(id = actor),
            wisps = listOf(
                WispView(
                    index = 0,
                    name = "Wisps_Resolve",
                    title = "Wisp's Resolve",
                    effect = GameEffect.RESOLVE_STRIKE_IMMEDIATELY_AND_CLEAR_ROW,
                    playImmediately = false,
                    battleOnly = false,
                    endGameVp = 0
                )
            )
        ),
        opponents = listOf(
            OpponentView(
                board = DecisionContext.EMPTY.self.board.copy(id = opponent),
                wispCount = opponentWisps
            )
        ),
        battle = BattleView(
            playerOrder = listOf(actor, opponent),
            rows = listOf(
                BattleRowView(
                    row = StrikeRow.TOP,
                    closed = false,
                    players = listOf(
                        player(actor, ownTotal),
                        player(opponent, opponentTotal)
                    )
                )
            )
        )
    )

    private fun player(id: PlayerId, total: Int) = BattlePlayerRowView(
        playerId = id,
        row = StrikeRow.TOP,
        dice = emptyList(),
        critters = emptyList(),
        dieTotal = total,
        critterTotal = 0,
        total = total,
        withdrawn = false
    )

    private fun round() = RoundCard(
        quantity = 1,
        name = "Battle_Test",
        type = RoundCardType.BATTLE,
        firstEffect = RoundCardEffect("Bloom", "", "", "", null, GameEffect.GAIN_ONE_VP),
        secondEffect = RoundCardEffect("Burrow", "", "", "", null, GameEffect.GAIN_TWO_WORMS),
        backImage = ""
    )
}
