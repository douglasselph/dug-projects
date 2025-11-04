package dugsolutions.leaf.common.domain

import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.random.die.Die
import dugsolutions.leaf.random.die.DieSides
import dugsolutions.leaf.random.die.DieValue

sealed class Action {

    data class AcquireBug(val bug: Token, val using: DieValue): Action()
    data class AcquireCard(val card: GameCard, val using: List<DieValue>): Action()
    data class AcquireDie(val sides: DieSides, val using: List<DieValue>): Action()
    data class ExecuteOnDie(val targetDie: Die, val using: GameCard): Action()
    data class ExecuteOnPlayerDie(val targetPlayer: Player, val targetDie: Die, val using: GameCard): Action()
    data class Execute(val using: GameCard): Action()

}
