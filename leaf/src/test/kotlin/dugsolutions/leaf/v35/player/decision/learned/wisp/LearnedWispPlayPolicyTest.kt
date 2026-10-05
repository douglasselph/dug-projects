package dugsolutions.leaf.v35.player.decision.learned.wisp

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.wisp.*
import dugsolutions.leaf.v35.round.domain.RoundCardType
import dugsolutions.leaf.v35.wisp.domain.WispCard
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.nio.file.Files

class LearnedWispPlayPolicyTest {
    private val first = wisp("Wisp_First", 1)
    private val second = wisp("Wisp_Second", 3)
    private fun request(reference: WispCard? = first, allowHold: Boolean = true) = ChooseWispPlayRequest(
        legalCards = listOf(first, second),
        referenceCard = reference,
        allowHold = allowHold,
        observation = WispPlayObservation(RoundCardType.CULTIVATION, mainActionsRemaining = 1, context = DecisionContext.EMPTY)
    )

    @Test fun `human policy preserves reference wisp`() {
        assertEquals(WispPlayDecision.Play(second), HumanWispPlayPolicy().chooseWisp(request(second)))
    }

    @Test fun `learned policy may hold when hold is legal`() {
        val raw = LearnedWispPlayWeights.zeros().toDoubleArray()
        raw[WispPlayFeature.ACTION_HOLD.ordinal] = 10.0
        val policy = LearnedWispPlayPolicy(LearnedWispPlayWeights.fromDoubleArray(raw))
        assertEquals(WispPlayDecision.Hold, policy.chooseWisp(request()))
    }

    @Test fun `learned policy chooses only offered card when hold is forbidden`() {
        val policy = LearnedWispPlayPolicy(LearnedWispPlayWeights.zeros())
        val chosen = policy.chooseWisp(request(allowHold = false)) as WispPlayDecision.Play
        require(chosen.card in listOf(first, second))
    }

    @Test fun `weights persist named wisp features`() {
        val path = Files.createTempFile("wisp-policy", ".weights")
        try {
            val key = LearnedWispPlayWeights.wispFeature(first.name)
            LearnedWispPlayWeights.zeros(listOf(key)).withNamed(key, 2.5).save(path)
            assertEquals(2.5, LearnedWispPlayWeights.load(path).named(key))
        } finally {
            Files.deleteIfExists(path)
        }
    }

    private fun wisp(name: String, vp: Int) = WispCard(
        quantity = 1, name = name, title = name, count = 1,
        effect = GameEffect.GAIN_ONE_WISP, lineIcons = null, lineIconsHeight = 0,
        vpIcon = null, mainBackdrop = "", endGameVp = vp
    )
}
