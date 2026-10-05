package dugsolutions.leaf.v35.player.decision.battle

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.DecisionDirector
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.support.HandDieChoice
import dugsolutions.leaf.v35.player.decision.support.SupportAction
import dugsolutions.leaf.v35.tokens.Critter
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame

class BattleSupportPolicyTest {
    @Test
    fun `stable candidate identities distinguish final mains and fully formed supports`() {
        val die = HandDieChoice(index = 2, sides = 8, value = 5)
        val choices = listOf(
            BattleTurnAction.Support(BattleSupportAction.Shared(SupportAction.UseWaterReroll(die))),
            BattleTurnAction.Support(BattleSupportAction.PlaceCritter(Critter.BEE, StrikeRow.MIDDLE)),
            BattleTurnAction.Support(BattleSupportAction.UseSunlight(BattleMainAction.Draw)),
            BattleTurnAction.Support(BattleSupportAction.UseSunlight(BattleMainAction.RoundEffect1)),
            BattleTurnAction.FinalMain(BattleMainAction.Draw)
        )

        assertEquals(
            listOf(
                "SUPPORT:WATER_REROLL:D8@2",
                "SUPPORT:CRITTER:BEE:MIDDLE",
                "SUPPORT:SUNLIGHT->DRAW",
                "SUPPORT:SUNLIGHT->ROUND_EFFECT:1",
                "FINAL_MAIN:DRAW"
            ),
            choices.map { it.stableBattleSupportCandidateId().stableId }
        )
    }

    @Test
    fun `Human Battle Support policy preserves existing Human Battle choice exactly`() {
        val reference = BattleTurnAction.Support(
            BattleSupportAction.UseSunlight(BattleMainAction.RoundEffect2)
        )
        val request = ChooseBattleSupportActionRequest(
            legalActions = listOf(reference, BattleTurnAction.FinalMain(BattleMainAction.Draw)),
            referenceAction = reference,
            observation = BattleSupportObservation(
                passNumber = 2,
                roundCardName = "Round",
                firstRoundEffect = GameEffect.GAIN_SUNLIGHT_TOKEN,
                secondRoundEffect = GameEffect.GAIN_WATER_TOKEN,
                context = DecisionContext.EMPTY
            )
        )

        assertSame(reference, HumanBattleSupportPolicy().chooseSupport(request))
        assertEquals(
            listOf("SUPPORT:SUNLIGHT->ROUND_EFFECT:2", "FINAL_MAIN:DRAW"),
            request.legalActionIds.map { it.stableId }
        )
    }

    @Test
    fun `Battle Support policy is independently replaceable`() {
        val baseline = DecisionDirector.humanBaseline()
        val replacement = BattleSupportPolicy { request -> request.legalActions.last() }
        val changed = baseline.copy(battleSupport = replacement)

        assertSame(replacement, changed.battleSupport)
        assertSame(baseline.reward, changed.reward)
        assertSame(baseline.wound, changed.wound)
        assertSame(baseline.placement, changed.placement)
        assertSame(baseline.cultivation, changed.cultivation)
        assertSame(baseline.cultivationMain, changed.cultivationMain)
        assertSame(baseline.cultivationSupport, changed.cultivationSupport)
        assertSame(baseline.battle, changed.battle)
        assertSame(baseline.buy, changed.buy)
        assertSame(baseline.support, changed.support)
        assertSame(baseline.effect, changed.effect)
    }

    @Test
    fun `named directors expose pass-through Battle Support policies`() {
        assertIs<HumanBattleSupportPolicy>(DecisionDirector.humanBaseline().battleSupport)
        assertIs<MechanicalBattleSupportPolicy>(DecisionDirector.mechanicalControl().battleSupport)
    }
}
