package dugsolutions.leaf.integration.v35.sanity.cultivation

import dugsolutions.leaf.integration.v35.support.DieSpec
import dugsolutions.leaf.integration.v35.support.graftPlant
import dugsolutions.leaf.integration.v35.support.player
import dugsolutions.leaf.integration.v35.support.setPlayerDice
import dugsolutions.leaf.integration.v35.support.decision.ScriptedDecisionDirector
import dugsolutions.leaf.v35.player.creature.CreatureSide
import dugsolutions.leaf.v35.player.decision.DecisionDirector
import dugsolutions.leaf.v35.player.decision.buy.BuyPayment
import dugsolutions.leaf.v35.random.die.DieSides
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * R2-D real-engine seam coverage for Human Baseline Graft Placement.
 *
 * The purchase itself is scripted so this test owns only the placement seam.
 * BuyCoordinator prepares the graft through the production GraftResolver,
 * which supplies live legal placements and DecisionContext to the real Human
 * Baseline placement strategy, then commits the selected placement.
 */
class HumanBaselineGraftPlacementIntegrationTest {

    @Test
    fun `Plant purchase uses live Human Baseline weak balance preference`() {
        val first = ScriptedDecisionDirector(fallback = DecisionDirector.humanBaseline()).apply {
            buy.thenPurchasePlant("Root_05_02")
            buy.thenPayment { request ->
                BuyPayment(dice = listOf(request.availableDice.single { it.value == 5 }))
            }
        }
        val second = ScriptedDecisionDirector()

        cultivationHarness(first = first, second = second).use { harness ->
            // Seed one Root on whichever side the deterministic setup helper
            // reaches first. The purchased Root should then use the other side
            // because the live topology is otherwise equivalent and balance is
            // the approved weak secondary preference.
            val existing = harness.graftPlant(1, "Root_05_01")
            harness.setPlayerDice(1, hand = listOf(DieSpec(DieSides.D6, 5)))
            harness.setPlayerDice(2)
            harness.revealNextRound()

            harness.runCultivationBuy()

            val roots = harness.player(1).creature.cards.filter { it.card.type.name == "ROOT" }
            val purchased = roots.single { it.card.name == "Root_05_02" }
            assertEquals(2, roots.size)
            assertNotEquals(existing.side, purchased.side)
            assertEquals(
                if (existing.side == CreatureSide.LEFT) CreatureSide.RIGHT else CreatureSide.LEFT,
                purchased.side
            )
            first.assertExhausted()
            second.assertExhausted()
        }
    }
}
