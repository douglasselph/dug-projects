package dugsolutions.leaf.v30.player.di

import dugsolutions.leaf.v30.chronicle.Chronicle
import dugsolutions.leaf.v30.player.Player
import dugsolutions.leaf.v30.player.decision.domain.DecisionDirector

class PlayerFactory(
    private val decisionDirector: DecisionDirector,
    private val chronicle: Chronicle
) {

    operator fun invoke(): Player {
        return Player(
            decisionDirector = decisionDirector,
            chronicle = chronicle
        )
    }
}
