package dugsolutions.leaf.v35.player.decision.baseline.buy

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.decision.baseline.card.HumanBaselineCardScorerRegistry
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlantAcquisitionUsabilityTest {
    private val card = PlantCard(
        quantity = 1, name = "Root_09_01", title = "Root Awakening",
        type = PlantType.ROOT, cost = 9, lineIcon = null,
        vpIcon = "", typeIcon = "", fgColor = "", textColor = "",
        fullImage = "", backgroundImage = "", cardBackgroundImage = "",
        effect = GameEffect.UPGRADE_DIE_AND_USE_NOW
    )
    private val scorer = HumanBaselineCardScorerRegistry().forPlant(card)

    @Test
    fun `unknown schedule is neutral rather than pretending no rounds remain`() {
        val result = PlantAcquisitionUsability.estimate(DecisionContext.EMPTY, card, scorer)
        assertEquals(0, result.adjustment)
        assertTrue(result.legalPlacements > 0)
    }

    @Test
    fun `final Buy does not credit future activation`() {
        val context = DecisionContext.EMPTY.copy(progress = DecisionContext.EMPTY.progress.copy(
            cultivationRoundsRemaining = 0, battleRoundsRemaining = 0
        ))
        assertEquals(-12, PlantAcquisitionUsability.estimate(context, card, scorer).adjustment)
    }

    @Test
    fun `more known rounds increase bounded acquisition usability`() {
        fun context(cultivation: Int, battle: Int) = DecisionContext.EMPTY.copy(
            progress = DecisionContext.EMPTY.progress.copy(
                cultivationRoundsRemaining = cultivation, battleRoundsRemaining = battle
            )
        )
        val short = PlantAcquisitionUsability.estimate(context(0, 1), card, scorer)
        val long = PlantAcquisitionUsability.estimate(context(3, 3), card, scorer)
        assertTrue(long.adjustment >= short.adjustment)
        assertTrue(long.adjustment <= 12)
    }
}
