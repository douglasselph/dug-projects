package dugsolutions.leaf.v35.player.decision.cultivation

import dugsolutions.leaf.v35.player.creature.CreatureCard
import dugsolutions.leaf.v35.round.domain.RoundCard

sealed interface CultivationMainAction {
    data object Draw : CultivationMainAction

    data class ActivatePlant(
        val card: CreatureCard
    ) : CultivationMainAction

    data object RoundEffect1 : CultivationMainAction

    data object RoundEffect2 : CultivationMainAction
}

class ChooseCultivationMainActionRequest(
    val roundCard: RoundCard,
    val actionNumber: Int,
    legalChoices: List<CultivationMainAction>
) {
    val legalChoices: List<CultivationMainAction> = legalChoices.toList()

    init {
        require(actionNumber in 1..2) {
            "Cultivation action number must be 1 or 2: $actionNumber"
        }
        require(this.legalChoices.isNotEmpty()) {
            "Cultivation decision requires at least one legal choice"
        }
    }
}

interface CultivationStrategy {
    fun chooseMainAction(
        request: ChooseCultivationMainActionRequest
    ): CultivationMainAction
}
