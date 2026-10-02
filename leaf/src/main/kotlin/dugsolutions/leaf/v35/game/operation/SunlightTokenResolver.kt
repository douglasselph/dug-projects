package dugsolutions.leaf.v35.game.operation

import dugsolutions.leaf.v35.chronicle.domain.Moment
import dugsolutions.leaf.v35.chronicle.domain.SunlightTokenChange
import dugsolutions.leaf.v35.game.Game
import dugsolutions.leaf.v35.player.Player
import dugsolutions.leaf.v35.tokens.Token

/** Transfers the shared Sunlight resource between the Grove and one player. */
object SunlightTokenResolver {

    fun gain(game: Game, player: Player): Boolean {
        val token = game.grove.tokens.pull(Token.SUNLIGHT) ?: return false
        player.tokens.add(token)
        game.chronicle.record(
            Moment.SunlightTokenChanged(player.id, SunlightTokenChange.GAINED)
        )
        return true
    }

    fun spend(game: Game, player: Player): Boolean {
        val token = player.tokens.pull(Token.SUNLIGHT) ?: return false
        game.grove.tokens.add(token)
        game.chronicle.record(
            Moment.SunlightTokenChanged(player.id, SunlightTokenChange.SPENT)
        )
        return true
    }
}
