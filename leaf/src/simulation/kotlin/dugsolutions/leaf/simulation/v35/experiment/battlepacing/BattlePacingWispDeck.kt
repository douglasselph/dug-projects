package dugsolutions.leaf.simulation.v35.experiment.battlepacing

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.wisp.WispCardManager
import dugsolutions.leaf.v35.wisp.domain.WispCard
import java.util.Random

/** Historical A/B cohort helper only. Normal v38 uses the unmodified Wisp deck. */
object BattlePacingWispDeck {
    fun withoutResolve(manager: WispCardManager, seed: Long): List<WispCard> {
        val cards = manager.getAllCards().cards.filterNot {
            it.effect == GameEffect.RESOLVE_STRIKE_IMMEDIATELY_AND_CLEAR_ROW
        }.flatMap { card -> List(card.quantity) { card } }.toMutableList()
        check(cards.isNotEmpty()) { "No experimental Wisps" }
        java.util.Collections.shuffle(cards, Random(seed))
        return cards
    }
}
