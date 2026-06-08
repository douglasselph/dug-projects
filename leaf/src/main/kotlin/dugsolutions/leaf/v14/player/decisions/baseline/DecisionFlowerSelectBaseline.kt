package dugsolutions.leaf.v14.player.decisions.baseline

import dugsolutions.leaf.v14.player.Player
import dugsolutions.leaf.v14.player.decisions.core.DecisionFlowerSelect

class DecisionFlowerSelectBaseline(
    private val player: Player
) : DecisionFlowerSelect {

    override suspend operator fun invoke(): DecisionFlowerSelect.Result {
        return DecisionFlowerSelect.Result(player.floralCards)
    }

} 
