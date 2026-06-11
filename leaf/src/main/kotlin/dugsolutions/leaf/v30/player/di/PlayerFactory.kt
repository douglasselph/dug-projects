package dugsolutions.leaf.v30.player.di

import dugsolutions.leaf.v30.player.Player
import dugsolutions.leaf.v30.player.decision.domain.DecisionDirector

class PlayerFactory(
    private val decisionDirector: DecisionDirector
) {

    operator fun invoke(): Player {
        return Player(decisionDirector)
    }
}
