package dugsolutions.leaf.v35.player.decision.learned.cultivation.support

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.cultivation.*
import dugsolutions.leaf.v35.player.decision.support.SupportAction
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class LearnedCultivationSupportPolicyTest {
    private val request = ChooseCultivationSupportActionRequest(
        legalActions = listOf(SupportAction.UseWaterRefresh),
        referenceAction = null,
        observation = CultivationSupportObservation(1,"test",GameEffect.UNKNOWN,GameEffect.UNKNOWN,DecisionContext.EMPTY)
    )

    @Test fun learnedPolicyCanChoosePass() {
        val a=DoubleArray(CultivationSupportFeature.entries.size)
        a[CultivationSupportFeature.ACTION_PASS.ordinal]=10.0
        val choice=LearnedCultivationSupportPolicy(LearnedCultivationSupportWeights.fromDoubleArray(a)).chooseSupport(request)
        assertEquals(CultivationSupportDecision.Pass,choice)
    }

    @Test fun learnedPolicyCanChooseOnlyOfferedSupport() {
        val a=DoubleArray(CultivationSupportFeature.entries.size)
        a[CultivationSupportFeature.ACTION_WATER_REFRESH.ordinal]=10.0
        val choice=LearnedCultivationSupportPolicy(LearnedCultivationSupportWeights.fromDoubleArray(a)).chooseSupport(request)
        assertIs<CultivationSupportDecision.Use>(choice)
        assertEquals(SupportAction.UseWaterRefresh,choice.action)
    }

    @Test fun weightRoundTripPreservesStandardWeights() {
        val a=DoubleArray(CultivationSupportFeature.entries.size){it.toDouble()/10.0}
        val weights=LearnedCultivationSupportWeights.fromDoubleArray(a)
        val path=Files.createTempFile("cultivation-support",".weights")
        try {
            weights.save(path)
            assertEquals(a.toList(), LearnedCultivationSupportWeights.load(path).toDoubleArray().toList())
        } finally { Files.deleteIfExists(path) }
    }
}
