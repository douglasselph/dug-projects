package dugsolutions.leaf.v35.player.decision.battle

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.support.HandDieChoice
import dugsolutions.leaf.v35.player.decision.support.SupportAction
import dugsolutions.leaf.v35.round.domain.RoundCard
import dugsolutions.leaf.v35.round.domain.RoundCardEffect
import dugsolutions.leaf.v35.round.domain.RoundCardType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class BaselineBattleStrategyTest {

    private val strategy = BaselineBattleStrategy()

    @Test
    fun firstMain_prefersDrawWhenAvailable() {
        val request = ChooseBattleFirstMainActionRequest(
            roundCard = roundCard(),
            legalChoices = listOf(
                BattleMainAction.RoundEffect1,
                BattleMainAction.Draw,
                BattleMainAction.RoundEffect2
            )
        )

        assertEquals(
            BattleMainAction.Draw,
            strategy.chooseFirstMainAction(request)
        )
    }

    @Test
    fun firstMain_usesFirstLegalWhenDrawUnavailable() {
        val request = ChooseBattleFirstMainActionRequest(
            roundCard = roundCard(),
            legalChoices = listOf(
                BattleMainAction.RoundEffect2,
                BattleMainAction.RoundEffect1
            )
        )

        assertEquals(
            BattleMainAction.RoundEffect2,
            strategy.chooseFirstMainAction(request)
        )
    }

    @Test
    fun step5_prefersFinalMainRatherThanSpendingSupport() {
        val support =
            BattleTurnAction.Support(
                BattleSupportAction.Shared(
                    SupportAction.UseWaterRefresh
                )
            )
        val request = ChooseBattleTurnActionRequest(
            roundCard = roundCard(),
            passNumber = 1,
            legalChoices = listOf(
                support,
                BattleTurnAction.FinalMain(BattleMainAction.RoundEffect1),
                BattleTurnAction.FinalMain(BattleMainAction.Draw)
            )
        )

        assertEquals(
            BattleTurnAction.FinalMain(BattleMainAction.Draw),
            strategy.chooseTurnAction(request)
        )
    }

    @Test
    fun placement_choosesFirstLegalRow() {
        val request = ChooseBattleDiePlacementRequest(
            die = HandDieChoice(0, 8, 6),
            reason = BattleDiePlacementReason.MAIN_DRAW,
            legalRows = listOf(StrikeRow.MIDDLE, StrikeRow.BOTTOM)
        )

        assertEquals(
            StrikeRow.MIDDLE,
            strategy.chooseDiePlacement(request)
        )
    }

    @Test
    fun requests_defensivelyCopyLegalChoices() {
        val mainMutable =
            mutableListOf<BattleMainAction>(BattleMainAction.Draw)
        val turnMutable =
            mutableListOf<BattleTurnAction>(
                BattleTurnAction.FinalMain(BattleMainAction.Draw)
            )
        val rowMutable = mutableListOf(StrikeRow.TOP)

        val main = ChooseBattleFirstMainActionRequest(roundCard(), mainMutable)
        val turn = ChooseBattleTurnActionRequest(roundCard(), 1, turnMutable)
        val placement = ChooseBattleDiePlacementRequest(
            HandDieChoice(0, 6, 4),
            BattleDiePlacementReason.MULCH,
            rowMutable
        )

        mainMutable.clear()
        turnMutable.clear()
        rowMutable.clear()

        assertEquals(1, main.legalChoices.size)
        assertEquals(1, turn.legalChoices.size)
        assertEquals(1, placement.legalRows.size)
    }

    private fun roundCard() =
        RoundCard(
            quantity = 1,
            name = "Battle",
            type = RoundCardType.BATTLE,
            firstEffect = effect("A"),
            secondEffect = effect("B"),
            backImage = ""
        )

    private fun effect(title: String) =
        RoundCardEffect(
            title = title,
            backgroundColor = "",
            textColor = "",
            image = "",
            icon = null,
            effect = GameEffect.GAIN_ONE_VP
        )
}
