package dugsolutions.leaf.v35.player.decision.cultivation

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.support.SupportAction

/** Stable research identity for a concrete optional Cultivation Support action. */
fun SupportAction.stableCultivationSupportId(): String = when (this) {
    is SupportAction.PlayWisp -> "CULT_SUPPORT:WISP:${card.name}"
    is SupportAction.UseWaterReroll -> "CULT_SUPPORT:WATER_REROLL:D${die.sides}#${die.index}@${die.value}"
    SupportAction.UseWaterRefresh -> "CULT_SUPPORT:WATER_REFRESH"
    is SupportAction.UseMulch -> "CULT_SUPPORT:MULCH:${token.sides?.value ?: 0}"
    is SupportAction.UseWormFlip -> "CULT_SUPPORT:WORM_FLIP:${cardId.value}"
    is SupportAction.UseButterfly -> "CULT_SUPPORT:BUTTERFLY:${butterfly.name}:D${die.sides}#${die.index}@${die.value}"
}

sealed interface CultivationSupportDecision {
    data object Pass : CultivationSupportDecision
    data class Use(val action: SupportAction) : CultivationSupportDecision
}

/**
 * Immutable visible-state seam for optional Cultivation Support timing.
 *
 * A policy chooses either one already-legal Support action or [CultivationSupportDecision.Pass].
 * Pass means "do not spend an optional Support now"; the coordinator then proceeds to a Main
 * action when one remains, or Done after both normal Main actions are complete.
 */
data class CultivationSupportObservation(
    val mainActionsRemaining: Int,
    val roundCardName: String,
    val firstRoundEffect: GameEffect,
    val secondRoundEffect: GameEffect,
    val context: DecisionContext
)

data class ChooseCultivationSupportActionRequest(
    val legalActions: List<SupportAction>,
    /** Human/Mechanical reference Support; null means the reference strategy preferred Main/Done. */
    val referenceAction: SupportAction?,
    val observation: CultivationSupportObservation
) {
    init {
        require(observation.mainActionsRemaining in 0..2)
        require(referenceAction == null || referenceAction in legalActions) {
            "Reference Cultivation Support action was not legal: $referenceAction"
        }
    }

    val legalActionIds: List<String> = legalActions.map { it.stableCultivationSupportId() }
}

fun interface CultivationSupportPolicy {
    fun chooseSupport(request: ChooseCultivationSupportActionRequest): CultivationSupportDecision
}

/** Behavior-preserving seam for Human Baseline. */
class HumanCultivationSupportPolicy : CultivationSupportPolicy {
    override fun chooseSupport(request: ChooseCultivationSupportActionRequest): CultivationSupportDecision =
        request.referenceAction?.let(CultivationSupportDecision::Use) ?: CultivationSupportDecision.Pass
}

/** Behavior-preserving seam for Mechanical Control. */
class MechanicalCultivationSupportPolicy : CultivationSupportPolicy {
    override fun chooseSupport(request: ChooseCultivationSupportActionRequest): CultivationSupportDecision =
        request.referenceAction?.let(CultivationSupportDecision::Use) ?: CultivationSupportDecision.Pass
}
