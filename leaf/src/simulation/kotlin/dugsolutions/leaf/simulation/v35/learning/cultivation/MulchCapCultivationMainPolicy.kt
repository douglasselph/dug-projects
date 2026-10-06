package dugsolutions.leaf.simulation.v35.learning.cultivation

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.cultivation.ChooseCultivationMainActionRequest
import dugsolutions.leaf.v35.player.decision.cultivation.CultivationMainAction
import dugsolutions.leaf.v35.player.decision.cultivation.CultivationMainPolicy

/**
 * Research-only wrapper that causally limits Round-card Mulch selections.
 * Once [cap] Mulch selections have been made, Mulch actions are removed from
 * the legal set before the delegate chooses its best remaining alternative.
 * A fresh wrapper is created per Game/player, so the counter is game-local.
 */
class MulchCapCultivationMainPolicy(
    private val delegate: CultivationMainPolicy,
    private val cap: Int
) : CultivationMainPolicy {
    private var mulchSelections = 0

    init { require(cap >= 0) { "Mulch Round-selection cap cannot be negative: $cap" } }

    override fun chooseMainAction(request: ChooseCultivationMainActionRequest): CultivationMainAction {
        val effectiveRequest = if (mulchSelections >= cap) withoutMulch(request) else request
        val chosen = delegate.chooseMainAction(effectiveRequest)
        if (isMulch(chosen, request)) mulchSelections++
        return chosen
    }

    private fun withoutMulch(request: ChooseCultivationMainActionRequest): ChooseCultivationMainActionRequest {
        val alternatives = request.legalActions.filterNot { isMulch(it, request) }
        // If Mulch is literally the only legal Main action, preserve engine legality rather than crash.
        if (alternatives.isEmpty()) return request
        val reference = request.referenceAction.takeIf { it in alternatives } ?: alternatives.first()
        return ChooseCultivationMainActionRequest(
            legalActions = alternatives,
            referenceAction = reference,
            observation = request.observation
        )
    }

    private fun isMulch(action: CultivationMainAction, request: ChooseCultivationMainActionRequest): Boolean =
        when (action) {
            CultivationMainAction.RoundEffect1 -> request.observation.firstRoundEffect == GameEffect.MULCH_DIE_FROM_HAND
            CultivationMainAction.RoundEffect2 -> request.observation.secondRoundEffect == GameEffect.MULCH_DIE_FROM_HAND
            else -> false
        }
}
