package dugsolutions.leaf.v35.player.di

import dugsolutions.leaf.v35.player.Player
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.DecisionDirector
import dugsolutions.leaf.v35.random.die.Die
import dugsolutions.leaf.v35.random.die.DieSides

class PlayerFactory(
    private val createDie: (DieSides) -> Die
) {

    operator fun invoke(
        id: PlayerId,
        decisions: DecisionDirector = DecisionDirector.baseline()
    ): Player {
        val player = Player(
            id = id,
            decisions = decisions
        )

        player.dice.addAllToSupply(
            startingDice()
        )

        return player
    }

    private fun startingDice(): List<Die> =
        buildList {
            repeat(3) {
                add(createDie(DieSides.D4))
            }
            repeat(3) {
                add(createDie(DieSides.D6))
            }
        }
}