package dugsolutions.leaf.v35.player.decision.baseline.scoring

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.cultivation.CultivationAction
import dugsolutions.leaf.v35.player.decision.support.SupportAction
import dugsolutions.leaf.v35.player.decision.buy.BuyCritterResource
import dugsolutions.leaf.v35.player.decision.buy.BuyDieResource
import dugsolutions.leaf.v35.player.decision.buy.BuyPayment
import dugsolutions.leaf.v35.tokens.Critter
import dugsolutions.leaf.v35.wisp.domain.WispCard
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DecisionLabelFormatterTest {
    @Test
    fun `Wisp decision label keeps useful identity and omits template fields`() {
        val wisp = WispCard(
            quantity = 4,
            name = "Wisp_Upgrade_Die",
            title = "Overgrowth",
            count = 4,
            effect = GameEffect.UPGRADE_DIE_TWO_STEPS_SKIP_MISSING_AND_USE_NOW,
            lineIcons = "{{ noisy.line.icons }}",
            lineIconsHeight = 40,
            vpIcon = "{{ images.victory.url }}",
            mainBackdrop = "{{ images.cloud_overgrowth.url }}",
            endGameVp = 1
        )
        val choice = CultivationAction.Support(
            SupportAction.PlayWisp(wisp, decisionProbabilityPercent = 5)
        )

        val label = DecisionCandidate(choice, PriorityScore(99)).label

        assertTrue("Wisp_Upgrade_Die/Overgrowth" in label)
        assertTrue("chance=5%" in label)
        assertFalse("vpIcon" in label)
        assertFalse("mainBackdrop" in label)
        assertFalse("lineIcons" in label)
        assertFalse("{{" in label)
    }

    @Test
    fun `Buy payment decision label renders resources instead of object identity`() {
        val payment = BuyPayment(
            dice = listOf(BuyDieResource(8, 7), BuyDieResource(4, 3)),
            critters = listOf(BuyCritterResource(Critter.BEE, 2))
        )

        assertEquals(
            "Payment(D8=7 D4=3 BEE+2 -> 12)",
            DecisionCandidate(payment, PriorityScore(1)).label
        )
    }
}
