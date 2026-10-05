package dugsolutions.leaf.v35.player.decision.cultivation

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.DecisionDirector
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.support.SupportAction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class CultivationSupportPolicyTest {
    private fun request(reference: SupportAction?) = ChooseCultivationSupportActionRequest(
        legalActions = listOf(SupportAction.UseWaterRefresh),
        referenceAction = reference,
        observation = CultivationSupportObservation(
            mainActionsRemaining = 1,
            roundCardName = "test",
            firstRoundEffect = GameEffect.UNKNOWN,
            secondRoundEffect = GameEffect.UNKNOWN,
            context = DecisionContext.EMPTY
        )
    )

    @Test fun humanPolicyPreservesReferenceSupport() {
        assertEquals(
            CultivationSupportDecision.Use(SupportAction.UseWaterRefresh),
            HumanCultivationSupportPolicy().chooseSupport(request(SupportAction.UseWaterRefresh))
        )
    }

    @Test fun humanPolicyPassesWhenReferencePreferredMainOrDone() {
        assertEquals(CultivationSupportDecision.Pass, HumanCultivationSupportPolicy().chooseSupport(request(null)))
    }

    @Test fun standardDirectorsExposeExpectedSupportPolicy() {
        assertIs<HumanCultivationSupportPolicy>(DecisionDirector.humanBaseline().cultivationSupport)
        assertIs<MechanicalCultivationSupportPolicy>(DecisionDirector.mechanicalControl().cultivationSupport)
    }
}
