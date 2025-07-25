package dugsolutions.leaf.player.decisions.baseline

import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.decisions.core.DecisionFlowerSelect

class DecisionFlowerSelectBaseline(
    private val player: Player
) : DecisionFlowerSelect {

    override suspend operator fun invoke(): DecisionFlowerSelect.Result {
        return DecisionFlowerSelect.Result(player.floralCards)
    }

} 
