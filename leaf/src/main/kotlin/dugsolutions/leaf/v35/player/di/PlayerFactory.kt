package dugsolutions.leaf.v35.player.di

import dugsolutions.leaf.v35.player.Player
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.random.die.DieSides
import dugsolutions.leaf.v35.random.die.di.DieFactory

class PlayerFactory(
    private val dieFactory: DieFactory
) {
    operator fun invoke(
        id: PlayerId,
        // decisions: DecisionDirector
    ): Player {
        val player = Player(
            id = id,
           // decisions = decisions
        )

        player.dice.addAllToSupply(
            listOf(
                dieFactory(DieSides.D4),
                dieFactory(DieSides.D4),
                dieFactory(DieSides.D4),
                dieFactory(DieSides.D6),
                dieFactory(DieSides.D6),
                dieFactory(DieSides.D6)
            )
        )

        return player
    }
}
