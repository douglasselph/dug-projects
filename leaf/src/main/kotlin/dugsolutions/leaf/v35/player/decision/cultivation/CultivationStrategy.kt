package dugsolutions.leaf.v35.player.decision.cultivation

import dugsolutions.leaf.v35.player.creature.CreatureCard
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
    data class Main(
        val action: CultivationMainAction
    ) : CultivationAction

    data class Support(
        val action: SupportAction
    ) : CultivationAction

    /** Legal only after both Main Actions have been completed. */
    data object Done : CultivationAction
}

class ChooseCultivationActionRequest(
    val roundCard: RoundCard,
    val mainActionsRemaining: Int,
    legalChoices: List<CultivationAction>
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
