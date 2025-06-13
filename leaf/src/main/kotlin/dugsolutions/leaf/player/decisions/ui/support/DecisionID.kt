package dugsolutions.leaf.player.decisions.ui.support

import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.random.die.Die

sealed class DecisionID {

    data object NONE : DecisionID()
    data class ACQUIRE_SELECT(val possibleCards: List<GameCard>, val possibleDice: List<Die>) : DecisionID()
    data object DAMAGE_ABSORPTION : DecisionID()
    data object DRAW_COUNT : DecisionID()
    data object FLOWER_SELECT : DecisionID()
    data class SHOULD_PROCESS_TRASH_EFFECT(val card: GameCard): DecisionID()

}
