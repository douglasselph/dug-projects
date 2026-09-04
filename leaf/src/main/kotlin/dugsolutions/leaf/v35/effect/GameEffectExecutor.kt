package dugsolutions.leaf.v35.effect

import dugsolutions.leaf.v35.game.Game
import dugsolutions.leaf.v35.player.Player
import dugsolutions.leaf.v35.player.creature.CreatureCard
import dugsolutions.leaf.v35.round.domain.RoundCard

enum class GameEffectPhase {
    CULTIVATION,
    BATTLE
}

enum class RoundEffectSlot {
    FIRST,
    SECOND
}

sealed interface GameEffectSource {
    data class Plant(
        val card: CreatureCard
    ) : GameEffectSource

    data class Round(
        val card: RoundCard,
        val slot: RoundEffectSlot
    ) : GameEffectSource
}

data class GameEffectRequest(
    val game: Game,
    val actor: Player,
    val effect: GameEffect,
    val source: GameEffectSource,
    val phase: GameEffectPhase
)

/** Progressive execution seam for gameplay effects. */
fun interface GameEffectExecutor {
    fun execute(request: GameEffectRequest)
}
