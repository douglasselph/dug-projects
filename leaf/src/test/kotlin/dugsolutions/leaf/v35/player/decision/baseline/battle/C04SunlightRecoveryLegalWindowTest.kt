package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.battle.BattleMainAction
import dugsolutions.leaf.v35.player.decision.battle.BattleSupportAction
import dugsolutions.leaf.v35.player.decision.context.*
import dugsolutions.leaf.v35.round.domain.RoundCard
import dugsolutions.leaf.v35.round.domain.RoundCardEffect
import dugsolutions.leaf.v35.round.domain.RoundCardType
import dugsolutions.leaf.v35.tokens.Critter
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class C04SunlightRecoveryLegalWindowTest {
    private val self = PlayerId(0)
    private val rival = PlayerId(1)
    private val wormSupport = BattleSupportAction.PlaceCritter(Critter.WORM, StrikeRow.entries.first())

    private fun context(): DecisionContext = DecisionContext.EMPTY.copy(
        phase = RoundCardType.BATTLE,
        grove = DecisionContext.EMPTY.grove.copy(worms = 2),
        self = DecisionContext.EMPTY.self.copy(board = DecisionContext.EMPTY.self.board.copy(id = self)),
        battle = BattleView(
            playerOrder = listOf(self, rival),
            rows = StrikeRow.entries.map { row -> BattleRowView(
                row = row, closed = false, players = listOf(
                    BattlePlayerRowView(self, row, emptyList(), emptyList(), 0, 0, 0, false),
                    BattlePlayerRowView(rival, row, emptyList(), emptyList(), 0, 0, 0, false)
                )
            ) }
        )
    )

    private fun round() = RoundCard(
        quantity = 1, name = "C04_sunlight_recovery_window", type = RoundCardType.BATTLE,
        firstEffect = RoundCardEffect("Worms", "", "", "", null, GameEffect.GAIN_TWO_WORMS),
        secondEffect = RoundCardEffect("VP", "", "", "", null, GameEffect.GAIN_ONE_VP),
        backImage = ""
    )

    @Test fun `sunlight recovery cannot claim confirmed Support when no legal follow-up is supplied`() {
        val context = context()
        val value = BattleRecoveryResourceUtility.sunlightFundedMain(
            context, GameEffect.GAIN_TWO_WORMS, emptyList(), listOf(BattleMainAction.Draw)
        )!!
        assertEquals(0, value.total)
        assertTrue(value.adjustments.any { it.reason.contains("unconfirmed") })
    }

    @Test fun `currently legal worm Support confirms a limited recovery opportunity`() {
        val context = context()
        val value = BattleRecoveryResourceUtility.sunlightFundedMain(
            context, GameEffect.GAIN_TWO_WORMS, listOf(wormSupport), listOf(BattleMainAction.Draw)
        )!!
        assertTrue(value.total > 0)
    }

    @Test fun `sunlight action observation consumes supplied legality rather than only visible rows`() {
        val context = context()
        val card = round()
        val conservative = SunlightBattleSpendingValue.observe(
            context, card, BattleMainAction.RoundEffect1,
            legalSupports = emptyList(), legalFinalMains = listOf(BattleMainAction.Draw)
        )
        val confirmed = SunlightBattleSpendingValue.observe(
            context, card, BattleMainAction.RoundEffect1,
            legalSupports = listOf(wormSupport), legalFinalMains = listOf(BattleMainAction.Draw)
        )
        assertEquals(0, conservative.fundedMainValue)
        assertTrue(confirmed.fundedMainValue > conservative.fundedMainValue)
        assertTrue(confirmed.netSupportValue > conservative.netSupportValue)
    }

    @Test fun `unrelated non recovery action retains its original score`() {
        val context = context()
        val card = round()
        val original = SunlightBattleSpendingValue.observe(context, card, BattleMainAction.RoundEffect2)
        val legal = SunlightBattleSpendingValue.observe(
            context, card, BattleMainAction.RoundEffect2,
            legalSupports = emptyList(), legalFinalMains = emptyList()
        )
        assertEquals(original.fundedMainValue, legal.fundedMainValue)
    }
}
