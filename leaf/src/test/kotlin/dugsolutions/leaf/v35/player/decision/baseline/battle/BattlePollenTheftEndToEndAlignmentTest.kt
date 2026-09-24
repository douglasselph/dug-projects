package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.baseline.effect.HumanBaselineEffectStrategy
import dugsolutions.leaf.v35.player.decision.battle.BattleMainAction
import dugsolutions.leaf.v35.player.decision.battle.BattleSupportAction
import dugsolutions.leaf.v35.player.decision.battle.BattleTurnAction
import dugsolutions.leaf.v35.player.decision.context.*
import dugsolutions.leaf.v35.player.decision.effect.ChooseEffectCrossPlayerDieSwapRequest
import dugsolutions.leaf.v35.player.decision.effect.EffectCrossPlayerDieSwapChoice
import dugsolutions.leaf.v35.player.decision.support.SupportAction
import dugsolutions.leaf.v35.round.domain.RoundCard
import dugsolutions.leaf.v35.round.domain.RoundCardEffect
import dugsolutions.leaf.v35.round.domain.RoundCardType
import dugsolutions.leaf.v35.wisp.domain.WispCard
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BattlePollenTheftEndToEndAlignmentTest {
    private val actor = PlayerId(0)
    private val opponent = PlayerId(1)

    @Test
    fun `top-level rejects Pollen Theft when B13-selected swap misses meaningful VP gate`() {
        val context = context(ownValue = 5, opponentValue = 4)
        val support = support()
        val downstream = chooseTarget(context)
        val orchestration = orchestrate(context, support)

        assertEquals(5, downstream.ownDie.die.value)
        assertEquals(4, downstream.opponentDie.die.value)
        assertTrue(BattleSupportPriority().score(context, support).total < 0)
        assertFalse(orchestration.chooseSupport)
    }

    @Test
    fun `top-level recognizes Pollen Theft when B13-selected swap has meaningful immediate realization`() {
        val context = context(ownValue = 4, opponentValue = 5)
        val support = support()
        val downstream = chooseTarget(context)
        val orchestration = orchestrate(context, support)

        assertEquals(4, downstream.ownDie.die.value)
        assertEquals(5, downstream.opponentDie.die.value)
        assertTrue(BattleSupportPriority().score(context, support).total > 0)
        assertTrue(orchestration.chooseSupport)
    }

    private fun chooseTarget(context: DecisionContext): EffectCrossPlayerDieSwapChoice =
        HumanBaselineEffectStrategy().chooseCrossPlayerDieSwap(
            ChooseEffectCrossPlayerDieSwapRequest(
                effect = GameEffect.SWAP_OWN_DIE_WITH_OPPONENT_SAME_SIZE,
                legalChoices = BattlePollenTheftEvaluator().legalChoices(context),
                context = context
            )
        )

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
        BattleSupportAction.Shared(SupportAction.PlayWisp(pollenTheft()))

    private fun pollenTheft() = WispCard(
        quantity = 4,
        name = "Wisp_Swap_Die",
        title = "Pollen Theft",
        count = 4,
        effect = GameEffect.SWAP_OWN_DIE_WITH_OPPONENT_SAME_SIZE,
        lineIcons = null,
        lineIconsHeight = 40,
        vpIcon = null,
        mainBackdrop = "",
        endGameVp = 0
    )

    private fun context(ownValue: Int, opponentValue: Int): DecisionContext {
        val ownDie = BattleDieView(0, 6, ownValue)
        val opponentDie = BattleDieView(0, 6, opponentValue)
        return DecisionContext.EMPTY.copy(
            phase = RoundCardType.BATTLE,
            self = DecisionContext.EMPTY.self.copy(
                board = DecisionContext.EMPTY.self.board.copy(
                    id = actor,
                    hand = listOf(DieView(0, 6, ownValue))
                ),
                wisps = listOf(
                    WispView(
                        index = 0,
                        name = "Wisp_Swap_Die",
                        title = "Pollen Theft",
                        effect = GameEffect.SWAP_OWN_DIE_WITH_OPPONENT_SAME_SIZE,
                        playImmediately = false,
                        battleOnly = false,
                        endGameVp = 0
                    )
                )
            ),
            battle = BattleView(
                playerOrder = listOf(actor, opponent),
                rows = listOf(
                    row(
                        StrikeRow.TOP,
                        player(actor, ownValue, ownDie),
                        player(opponent, opponentValue, opponentDie)
                    )
                )
            )
        )
    }

    private fun row(row: StrikeRow, vararg players: BattlePlayerRowView) =
        BattleRowView(row, false, players.map { it.copy(row = row) })

    private fun player(
        id: PlayerId,
        total: Int,
        vararg dice: BattleDieView
    ) = BattlePlayerRowView(
        playerId = id,
        row = StrikeRow.TOP,
        dice = dice.toList(),
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
