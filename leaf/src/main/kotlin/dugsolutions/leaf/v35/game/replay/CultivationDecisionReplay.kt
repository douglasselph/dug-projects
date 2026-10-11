package dugsolutions.leaf.v35.game.replay

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.RoundEffectSlot
import dugsolutions.leaf.v35.round.RoundValueResolver
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.cultivation.CultivationMainAction
import dugsolutions.leaf.v35.round.domain.RoundCard

/** Stable, value-only decision identity, independent of mutable card references. */
enum class ReplayMainAction { DRAW, ACTIVATE_PLANT, ROUND_EFFECT_1, ROUND_EFFECT_2 }

fun CultivationMainAction.replayKind(): ReplayMainAction = when (this) {
    CultivationMainAction.Draw -> ReplayMainAction.DRAW
    is CultivationMainAction.ActivatePlant -> ReplayMainAction.ACTIVATE_PLANT
    CultivationMainAction.RoundEffect1 -> ReplayMainAction.ROUND_EFFECT_1
    CultivationMainAction.RoundEffect2 -> ReplayMainAction.ROUND_EFFECT_2
}

/**
 * Choose a recorded decision ordinal for the targeted player in a particular
 * round. Counts *Main policy consultations*, not just completed Main actions.
 * Ordinals are zero-based. Target must be legal at the actual fork.
 */
data class CultivationReplayFork(
    val roundNumber: Int,
    val playerId: PlayerId,
    val consultationIndex: Int,
    val replacement: ReplayMainAction
) {
    init {
        require(roundNumber > 0)
        require(consultationIndex >= 0)
        require(replacement != ReplayMainAction.ACTIVATE_PLANT) {
            "Plant activation requires an exact card identity; use a different alternative"
        }
    }
}

/** Retains only scalar decision evidence; never holds Game or Player objects. */
data class CultivationReplayDecision(
    val roundNumber: Int,
    val playerId: Int,
    val consultationIndex: Int,
    val mainActionsRemaining: Int,
    val roundCardName: String,
    val firstEffect: GameEffect,
    val secondEffect: GameEffect,
    val legal: List<ReplayMainAction>,
    val policyChoice: ReplayMainAction,
    val actualChoice: ReplayMainAction,
    val plantName: String?,
    val intervened: Boolean
)

/**
 * Isolated per GameConfig, never shared between concurrently running games.
 * A paired run constructs a fresh instance for each branch.
 *
 * WARNING: replaying from the original seeds and replacing one choice is a
 * seeded counterfactual, NOT a midgame snapshot. Mechanical RNG consumption may
 * diverge after the fork; paired repetitions are required for causal inference.
 */
class CultivationDecisionReplay(
    val fork: CultivationReplayFork? = null,
    /** Seat-specific intervention: replace first N legal non-Compost Main choices with Compost. */
    val forceCompostPlayerId: PlayerId? = null,
    val forceCompostLimit: Int = 0
) {
    init { require(forceCompostLimit >= 0) }
    var forcedCompostCount: Int = 0
        private set
    private val consultations = mutableMapOf<Pair<Int, Int>, Int>()
    private val mutableDecisions = mutableListOf<CultivationReplayDecision>()
    val decisions: List<CultivationReplayDecision> get() = mutableDecisions.toList()
    var forkApplied: Boolean = false
        private set

    fun observe(
        roundNumber: Int,
        playerId: PlayerId,
        mainActionsRemaining: Int,
        roundCard: RoundCard,
        roundValues: RoundValueResolver,
        legalActions: List<CultivationMainAction>,
        chosen: CultivationMainAction
    ): CultivationMainAction {
        val key = roundNumber to playerId.value
        val index = consultations.getOrDefault(key, 0)
        consultations[key] = index + 1
        val shouldFork = fork != null && !forkApplied &&
            fork.roundNumber == roundNumber && fork.playerId == playerId &&
            fork.consultationIndex == index
        val compostSlot = when {
            roundValues.effectFor(roundCard, RoundEffectSlot.FIRST) == GameEffect.UPGRADE_DIE_FROM_HAND &&
                ReplayMainAction.ROUND_EFFECT_1 in legalActions.map { it.replayKind() } -> ReplayMainAction.ROUND_EFFECT_1
            roundValues.effectFor(roundCard, RoundEffectSlot.SECOND) == GameEffect.UPGRADE_DIE_FROM_HAND &&
                ReplayMainAction.ROUND_EFFECT_2 in legalActions.map { it.replayKind() } -> ReplayMainAction.ROUND_EFFECT_2
            else -> null
        }
        val shouldForce = !shouldFork && forceCompostPlayerId == playerId &&
            forcedCompostCount < forceCompostLimit && compostSlot != null && chosen.replayKind() != compostSlot
        val effective = if (shouldFork) {
            require(fork!!.replacement in legalActions.map { it.replayKind() }) {
                "Fork ${fork.replacement} is not legal at round $roundNumber, player ${playerId.value}, consultation $index"
            }
            forkApplied = true
            legalActions.first { it.replayKind() == fork.replacement }
        } else if (shouldForce) {
            forcedCompostCount++
            legalActions.first { it.replayKind() == compostSlot }
        } else chosen
        mutableDecisions += CultivationReplayDecision(
            roundNumber = roundNumber,
            playerId = playerId.value,
            consultationIndex = index,
            mainActionsRemaining = mainActionsRemaining,
            roundCardName = roundCard.name,
            firstEffect = roundValues.effectFor(roundCard, RoundEffectSlot.FIRST),
            secondEffect = roundValues.effectFor(roundCard, RoundEffectSlot.SECOND),
            legal = legalActions.map { it.replayKind() },
            policyChoice = chosen.replayKind(),
            actualChoice = effective.replayKind(),
            plantName = (effective as? CultivationMainAction.ActivatePlant)?.card?.card?.name,
            intervened = shouldFork || shouldForce
        )
        return effective
    }
}
