package dugsolutions.leaf.v30.player.decision.domain

import dugsolutions.leaf.v30.cards.domain.GameCard

sealed interface MainAction {
    data object PullDie : MainAction
    data class DoRoundAction(val roundAction: RoundAction) : MainAction
    data class ExecuteCard(val card: GameCard) : MainAction
}
