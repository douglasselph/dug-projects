package dugsolutions.leaf.v35.player.decision.battle

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.support.SupportAction

/** Stable research identity for a Battle Step-5 support/final-main candidate. */
sealed interface BattleSupportCandidateId {
    val stableId: String

    data class Shared(val kind: String, val detail: String? = null) : BattleSupportCandidateId {
        override val stableId: String = buildString {
            append("SUPPORT:")
            append(kind)
            detail?.let { append(':').append(it) }
        }
    }

    data class Sunlight(val fundedMain: String) : BattleSupportCandidateId {
        override val stableId: String = "SUPPORT:SUNLIGHT->$fundedMain"
    }

    data class Critter(val critter: String, val row: String) : BattleSupportCandidateId {
        override val stableId: String = "SUPPORT:CRITTER:$critter:$row"
    }

    data class FinalMain(val main: String) : BattleSupportCandidateId {
        override val stableId: String = "FINAL_MAIN:$main"
    }
}

/** Stable identity for an ordinary Battle Main Action, including a specific Plant. */
fun BattleMainAction.stableBattleMainId(): String =
    when (this) {
        BattleMainAction.Draw -> "DRAW"
        is BattleMainAction.ActivatePlant -> "PLANT:${card.card.name}"
        BattleMainAction.RoundEffect1 -> "ROUND_EFFECT:1"
        BattleMainAction.RoundEffect2 -> "ROUND_EFFECT:2"
    }

fun BattleTurnAction.stableBattleSupportCandidateId(): BattleSupportCandidateId =
    when (this) {
        is BattleTurnAction.FinalMain ->
            BattleSupportCandidateId.FinalMain(action.stableBattleMainId())

        is BattleTurnAction.Support -> when (val support = action) {
            is BattleSupportAction.UseSunlight ->
                BattleSupportCandidateId.Sunlight(support.mainAction.stableBattleMainId())

            is BattleSupportAction.PlaceCritter ->
                BattleSupportCandidateId.Critter(support.critter.name, support.row.name)

            is BattleSupportAction.Shared -> when (val shared = support.action) {
                is SupportAction.PlayWisp ->
                    BattleSupportCandidateId.Shared("WISP", shared.card.name)
                is SupportAction.UseWaterReroll ->
                    BattleSupportCandidateId.Shared(
                        "WATER_REROLL",
                        "D${shared.die.sides}@${shared.die.index}"
                    )
                SupportAction.UseWaterRefresh ->
                    BattleSupportCandidateId.Shared("WATER_REFRESH")
                is SupportAction.UseMulch ->
                    BattleSupportCandidateId.Shared("MULCH")
                is SupportAction.UseWormFlip ->
                    BattleSupportCandidateId.Shared("WORM_FLIP", shared.cardId.value.toString())
                is SupportAction.UseButterfly ->
                    BattleSupportCandidateId.Shared(
                        "BUTTERFLY",
                        "D${shared.die.sides}@${shared.die.index}"
                    )
            }
        }
    }

/**
 * Immutable visible-state seam for a future learned Battle Support policy.
 *
 * The context is exactly the player-facing [DecisionContext] already supplied
 * to Human Baseline. No Game, RNG, hidden deck state, or engine-private state
 * is exposed through this policy boundary.
 */
data class BattleSupportObservation(
    val passNumber: Int,
    val roundCardName: String,
    val firstRoundEffect: GameEffect,
    val secondRoundEffect: GameEffect,
    val context: DecisionContext
)

/** One policy decision among already-legal Battle Step-5 candidates. */
data class ChooseBattleSupportActionRequest(
    val legalActions: List<BattleTurnAction>,
    val referenceAction: BattleTurnAction,
    val observation: BattleSupportObservation
) {
    init {
        require(legalActions.isNotEmpty()) {
            "Battle Support policy requires at least one legal Step-5 action"
        }
    }

    val legalActionIds: List<BattleSupportCandidateId> =
        legalActions.map { it.stableBattleSupportCandidateId() }
}

/**
 * Chooses the high-level Battle Step-5 action: a Support candidate or the
 * final normal Main Action that ends Support timing.
 *
 * Legal Support candidates are already fully formed by the engine. In
 * particular, each Sunlight candidate names the ordinary Main Action it would
 * fund. Lower-level effect targets remain in the existing Human/effect policy
 * layers and are deliberately outside this seam.
 */
fun interface BattleSupportPolicy {
    fun chooseSupport(request: ChooseBattleSupportActionRequest): BattleTurnAction
}

/** Canonical Human seam: preserve the exact choice made by Human Battle logic. */
class HumanBattleSupportPolicy : BattleSupportPolicy {
    override fun chooseSupport(request: ChooseBattleSupportActionRequest): BattleTurnAction =
        request.referenceAction
}

/** Mechanical Control likewise preserves its existing deterministic choice. */
class MechanicalBattleSupportPolicy : BattleSupportPolicy {
    override fun chooseSupport(request: ChooseBattleSupportActionRequest): BattleTurnAction =
        request.referenceAction
}
