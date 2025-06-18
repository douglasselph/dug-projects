package dugsolutions.leaf.player.decisions.local.monitor

import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.random.die.Die

sealed class DecisionID {

    data object NONE : DecisionID()
    data class ACQUIRE_SELECT(val possibleCards: List<GameCard>, val possibleDice: List<Die>) : DecisionID()
    data class DAMAGE_ABSORPTION(val amount: Int) : DecisionID()
    data class DRAW_COUNT(val player: Player) : DecisionID()
    data object FLOWER_SELECT : DecisionID()
    data class SHOULD_PROCESS_TRASH_EFFECT(val card: GameCard): DecisionID()
    data object START_GAME : DecisionID()
    data object NEXT_STEP : DecisionID()

}
