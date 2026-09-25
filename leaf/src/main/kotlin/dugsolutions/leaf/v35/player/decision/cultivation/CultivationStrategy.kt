package dugsolutions.leaf.v35.player.decision.cultivation

import dugsolutions.leaf.v35.player.creature.CreatureCard
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.support.SupportAction
import dugsolutions.leaf.v35.round.domain.RoundCard

sealed interface CultivationMainAction {
    data object Draw : CultivationMainAction

    data class ActivatePlant(
        val card: CreatureCard
    ) : CultivationMainAction

    data object RoundEffect1 : CultivationMainAction

    data object RoundEffect2 : CultivationMainAction
}

/** One decision opportunity during Cultivation Build. */
sealed interface CultivationAction {
    /**
     * One Main Action choice. [decisionProbabilityPercent] is optional strategy
     * trace metadata for a probabilistic Human Baseline choice (currently
     * Compost / UPGRADE_DIE_FROM_HAND). It is deliberately ignored by equality
     * so attaching diagnostic metadata never makes the choice fail the
     * coordinator's "chosen in legalChoices" validation.
     */
    class Main(
        val action: CultivationMainAction,
        val decisionProbabilityPercent: Int? = null
    ) : CultivationAction {
        init {
            require(decisionProbabilityPercent == null || decisionProbabilityPercent in 0..100) {
                "Main Action decision probability must be 0..100: $decisionProbabilityPercent"
            }
        }

        fun withDecisionProbability(percent: Int?): Main =
            Main(action, percent)

        override fun equals(other: Any?): Boolean =
            other is Main && action == other.action

        override fun hashCode(): Int = action.hashCode()

        override fun toString(): String =
            buildString {
                append("Main(action=$action")
                decisionProbabilityPercent?.let { append(", decisionProbabilityPercent=$it") }
                append(')')
            }
    }

    data class Support(
        val action: SupportAction
    ) : CultivationAction

    /** Legal only after both Main Actions have been completed. */
    data object Done : CultivationAction
}

class ChooseCultivationActionRequest(
    val roundCard: RoundCard,
    val mainActionsRemaining: Int,
    legalChoices: List<CultivationAction>,
    val context: DecisionContext = DecisionContext.EMPTY
) {
    val legalChoices: List<CultivationAction> = legalChoices.toList()

    init {
        require(mainActionsRemaining in 0..2) {
            "Cultivation Main Actions remaining must be 0 to 2: $mainActionsRemaining"
        }
        require(this.legalChoices.isNotEmpty()) {
            "Cultivation decision requires at least one legal choice"
        }
    }
}

interface CultivationStrategy {
    fun chooseAction(
        request: ChooseCultivationActionRequest
    ): CultivationAction
}
