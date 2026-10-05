package dugsolutions.leaf.v35.player.decision.battle

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.context.DecisionContext

/** Normal Battle Main stage. Sunlight-funded Mains remain owned by Battle Support. */
enum class BattleMainPolicyStage {
    FIRST,
    FINAL
}

/** Immutable visible-state seam for learned normal Battle Main selection. */
data class BattleMainObservation(
    val stage: BattleMainPolicyStage,
    val roundCardName: String,
    val firstRoundEffect: GameEffect,
    val secondRoundEffect: GameEffect,
    val context: DecisionContext
)

/** One policy decision among already-legal ordinary Battle Main actions. */
data class ChooseBattleMainActionRequest(
    val legalActions: List<BattleMainAction>,
    val referenceAction: BattleMainAction,
    val observation: BattleMainObservation
) {
    init {
        require(legalActions.isNotEmpty()) { "Battle Main policy requires at least one legal Main Action" }
        require(referenceAction in legalActions) {
            "Reference Battle Main Action was not legal: $referenceAction; legal=$legalActions"
        }
    }

    val legalActionIds: List<String> = legalActions.map(BattleMainAction::stableBattleMainId)
}

/**
 * Chooses WHICH ordinary Battle Main Action is taken.
 *
 * Battle Support remains responsible for Support timing and for deciding when
 * Step 5 commits to the final Main. Sunlight-funded Main Actions are not routed
 * through this policy and do not consume either normal Main.
 */
fun interface BattleMainPolicy {
    fun chooseMainAction(request: ChooseBattleMainActionRequest): BattleMainAction
}

/** Canonical Human seam: preserve the existing Human BattleStrategy choice exactly. */
class HumanBattleMainPolicy : BattleMainPolicy {
    override fun chooseMainAction(request: ChooseBattleMainActionRequest): BattleMainAction =
        request.referenceAction
}

/** Mechanical Control likewise preserves its existing deterministic Battle Main choice. */
class MechanicalBattleMainPolicy : BattleMainPolicy {
    override fun chooseMainAction(request: ChooseBattleMainActionRequest): BattleMainAction =
        request.referenceAction
}
