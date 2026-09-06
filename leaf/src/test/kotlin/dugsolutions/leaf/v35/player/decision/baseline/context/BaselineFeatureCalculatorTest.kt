package dugsolutions.leaf.v35.player.decision.baseline.context

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.player.decision.baseline.common.ReserveResource
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.context.GameProgressView
import dugsolutions.leaf.v35.round.domain.RoundCardType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class BaselineFeatureCalculatorTest {

    @Test
    fun calculate_collectsSharedNeutralFeaturesWithoutMutatingContext() {
        val context = DecisionContext.EMPTY.copy(
            phase = RoundCardType.CULTIVATION,
            progress = GameProgressView.EMPTY.copy(
                currentCultivationRoundNumber = 1,
                totalCultivationRounds = 8
            )
        )

        val features = BaselineFeatureCalculator().calculate(context)

        assertEquals(1, features.plantDeficit)
        assertEquals(33, features.dicePowerDeficit)
        assertEquals(2, features.openGrowthSlots)
        assertEquals(1, features.reserveStatus.getValue(ReserveResource.BEE).deficit)
        assertFalse(features.rowNeed.getValue(StrikeRow.TOP).available)
        assertEquals(DecisionContext.EMPTY.self, context.self)
    }
}
