package dugsolutions.leaf.v35.effect.handler

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.GameEffectExecutor
import dugsolutions.leaf.v35.effect.GameEffectRequest

/**
 * Temporary route for newly authored Battle Round effects whose mechanics are
 * intentionally being implemented in the next migration pass.
 *
 * Returning false from canExecute keeps these effects from being offered as a
 * legal Main Action while still making the CSV/dispatcher mapping explicit.
 */
class DeferredRoundEffectHandler : EffectHandler {
    override fun canExecute(request: GameEffectRequest): Boolean = false

    override fun execute(
        request: GameEffectRequest,
        executor: GameEffectExecutor
    ) {
        error("Deferred Battle Round effect is not implemented yet: ${request.effect}")
    }

    companion object {
        val effects: Set<GameEffect> = setOf(
            GameEffect.BARKSKIN_WOUND_ONLY_IF_LOSE_BY_10_PLUS,
            GameEffect.EACH_OPPONENT_TRASH_ONE_WISP,
            GameEffect.GAIN_ANY_ROOT_OR_VINE
        )
    }
}
