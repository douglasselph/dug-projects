package dugsolutions.leaf.v35.player.decision.baseline.buy

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.plant.domain.PlantScoringRule
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.decision.buy.BuyItem
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class PurchasePriorityTest {
    @Test
    fun `card acquire value breaks a same-cost Plant tie`() {
        val awakening = plant("Root_09_01", GameEffect.UPGRADE_DIE_AND_USE_NOW)
        val cause = plant("Root_09_02", GameEffect.FLIP_OWN_DIE_TO_OPPOSITE_FACE)

        val strong = PurchasePriority.score(DecisionContext.EMPTY, BuyItem.Plant(awakening)).total
        val ordinary = PurchasePriority.score(DecisionContext.EMPTY, BuyItem.Plant(cause)).total

        assertTrue(strong > ordinary)
    }

    private fun plant(name: String, effect: GameEffect) = PlantCard(
        quantity = 6,
        name = name,
        title = name,
        type = PlantType.ROOT,
        cost = 9,
        lineIcon = null,
        vpIcon = "",
        typeIcon = "",
        fgColor = "",
        textColor = "",
        fullImage = "",
        backgroundImage = "",
        cardBackgroundImage = "",
        effect = effect,
        scoringRule = PlantScoringRule.Fixed(1)
    )
}
