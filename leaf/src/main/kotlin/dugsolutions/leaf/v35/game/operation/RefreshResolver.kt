package dugsolutions.leaf.v35.game.operation

import dugsolutions.leaf.v35.chronicle.Chronicle
import dugsolutions.leaf.v35.chronicle.domain.Moment
import dugsolutions.leaf.v35.player.Player

/** Coordinates the Plant-and-Butterfly refresh rule for one player. */
class RefreshResolver(
    private val chronicle: Chronicle
) {

    /** Performs normal cleanup refresh only when the Creature qualifies. */
    fun refreshIfReady(player: Player): Boolean {
        if (!player.creature.allFaceDown) return false
        return refresh(player)
    }

    /** Performs an unconditional refresh requested by an action or effect. */
    fun refresh(player: Player): Boolean {
        val hasFaceDownPlant = player.creature.cards.any { it.isFaceDown }
        val hasFaceDownButterfly = player.butterflies.all.any {
            player.butterflies.isFaceDown(it)
        }

        if (!hasFaceDownPlant && !hasFaceDownButterfly) return false

        player.creature.faceUpAll()
        player.butterflies.all.forEach {
            player.butterflies.faceUp(it)
        }

        chronicle.record(
            Moment.Marker(
                "REFRESH player=${player.id.value}"
            )
        )
        return true
    }
}
