package dugsolutions.leaf.v35.effect

import dugsolutions.leaf.v35.game.Game
import dugsolutions.leaf.v35.player.Player
import dugsolutions.leaf.v35.player.creature.CreatureCard
import dugsolutions.leaf.v35.player.creature.CreatureCardId
import dugsolutions.leaf.v35.round.domain.RoundCard
import dugsolutions.leaf.v35.wisp.domain.WispCard

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

    data class Wisp(
        val card: WispCard
    ) : GameEffectSource
}

data class GameEffectRequest(
    val game: Game,
    val actor: Player,
    val effect: GameEffect,
    val source: GameEffectSource,
    val phase: GameEffectPhase,
    /**
     * Plant cards whose effects are already active higher in the current
     * recursive effect chain.
     *
     * This is engine-only recursion context. Strategies never receive it.
     * Vine and Again and O Edelweiss use it to permit legitimate nested Plant
     * effects while rejecting cycles such as A -> B -> A.
     */
    val plantEffectPath: List<CreatureCardId> = emptyList()
)

/**
 * Progressive execution seam for gameplay effects.
 *
 * [canExecute] lets round/action coordinators avoid offering effects whose
 * required target or limited Grove resource is currently unavailable. The
 * default remains true so focused recording executors used by tests and future
 * specialized handlers stay lightweight.
 */
fun interface GameEffectExecutor {
    fun execute(request: GameEffectRequest)

    fun canExecute(request: GameEffectRequest): Boolean = true
}
