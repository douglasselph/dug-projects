package dugsolutions.leaf.game.actions

import dugsolutions.leaf.common.domain.Action
import dugsolutions.leaf.game.actions.choices.AcquireBug
import dugsolutions.leaf.game.actions.choices.AcquireCard
import dugsolutions.leaf.game.actions.choices.AcquireDie
import dugsolutions.leaf.game.actions.choices.ExecuteCard
import dugsolutions.leaf.game.actions.choices.ExecuteOnDie
import dugsolutions.leaf.game.actions.choices.ExecuteOnPlayerDie
import dugsolutions.leaf.game.actions.choices.PullDie
import dugsolutions.leaf.player.Player

class ExecuteAction(
    private val acquireBug: AcquireBug,
    private val acquireCard: AcquireCard,
    private val acquireDie: AcquireDie,
    private val executeCard: ExecuteCard,
    private val executeOnDie: ExecuteOnDie,
    private val executeOnPlayerDie: ExecuteOnPlayerDie,
    private val pullDie: PullDie
) {

    operator fun invoke(player: Player, action: Action): Boolean {
        when(action) {
            Action.None -> return false
            is Action.AcquireBug -> acquireBug(player, action)
            is Action.AcquireCard -> acquireCard(player, action)
            is Action.AcquireDie -> acquireDie(player, action)
            is Action.ExecuteCard -> executeCard(player, action)
            is Action.ExecuteOnDie -> executeOnDie(player, action)
            is Action.ExecuteOnPlayerDie -> executeOnPlayerDie(player, action)
            is Action.PullDie -> pullDie(player, action)
        }
        return true
    }

}
