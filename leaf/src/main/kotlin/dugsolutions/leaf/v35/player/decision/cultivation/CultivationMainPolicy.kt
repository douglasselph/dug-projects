package dugsolutions.leaf.v35.player.decision.cultivation

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.context.DecisionContext

/** Stable research identity for a high-level Cultivation Main Action. */
sealed interface CultivationMainActionId {
    val stableId: String

    data object Draw : CultivationMainActionId {
        override val stableId: String = "DRAW"
    }

    data class Plant(val cardId: String) : CultivationMainActionId {
        override val stableId: String = "PLANT:$cardId"
    }

    data class RoundEffect(val slot: Int) : CultivationMainActionId {
        init {
            require(slot in 1..2) { "Cultivation Round effect slot must be 1 or 2: $slot" }
        }
        override val stableId: String = "ROUND_EFFECT:$slot"
    }

    /** Reserved stable identity for the legal post-Main completion action. */
    data object Done : CultivationMainActionId {
        override val stableId: String = "DONE"
    }
}

fun CultivationMainAction.stableIdentity(): CultivationMainActionId =
    when (this) {
        CultivationMainAction.Draw -> CultivationMainActionId.Draw
        is CultivationMainAction.ActivatePlant -> CultivationMainActionId.Plant(card.card.name)
        CultivationMainAction.RoundEffect1 -> CultivationMainActionId.RoundEffect(1)
        CultivationMainAction.RoundEffect2 -> CultivationMainActionId.RoundEffect(2)
    }

/**
 * Immutable visible-state seam for a future learned Cultivation Main policy.
 *
 * [context] is the same player-facing DecisionContext supplied to Human Baseline;
 * no engine-private or hidden state is added here. The feature vector used by a
 * learned policy will be defined separately rather than leaking Game state into
 * this interface.
 */
data class CultivationMainObservation(
    val mainActionsRemaining: Int,
    val roundCardName: String,
    val firstRoundEffect: GameEffect,
    val secondRoundEffect: GameEffect,
    val context: DecisionContext
)

/** One policy decision among already-legal Cultivation Main Actions. */
data class ChooseCultivationMainActionRequest(
    val legalActions: List<CultivationMainAction>,
    val referenceAction: CultivationMainAction,
    val observation: CultivationMainObservation
) {
    init {
        require(legalActions.isNotEmpty()) { "Cultivation Main policy requires at least one legal Main Action" }
        require(referenceAction in legalActions) {
            "Reference Cultivation Main Action was not legal: $referenceAction"
        }
    }

    val legalActionIds: List<CultivationMainActionId> = legalActions.map { it.stableIdentity() }
}

/**
 * Selects WHICH high-level Cultivation Main Action to take once the surrounding
 * Cultivation strategy has reached a Main-action decision.
 *
 * Optional Support timing and all lower-level effect targets remain outside this
 * policy boundary.
 */
fun interface CultivationMainPolicy {
    fun chooseMainAction(request: ChooseCultivationMainActionRequest): CultivationMainAction
}

/**
 * Canonical Human policy for the new seam.
 *
 * The existing HumanBaselineCultivationStrategy remains the authority that
 * produced [ChooseCultivationMainActionRequest.referenceAction]. Returning that
 * exact choice preserves pre-refactor Human behavior and strategy-RNG use while
 * making the Main selection replaceable by a learned policy later.
 */
class HumanCultivationMainPolicy : CultivationMainPolicy {
    override fun chooseMainAction(request: ChooseCultivationMainActionRequest): CultivationMainAction =
        request.referenceAction
}

/** Mechanical Control likewise preserves the deterministic Main it already chose. */
class MechanicalCultivationMainPolicy : CultivationMainPolicy {
    override fun chooseMainAction(request: ChooseCultivationMainActionRequest): CultivationMainAction =
        request.referenceAction
}
