package dugsolutions.leaf.v35.effect.handler

import dugsolutions.leaf.v35.effect.GameEffectExecutor
import dugsolutions.leaf.v35.effect.GameEffectRequest

/**
 * Cohesive implementation unit for one family of related [GameEffectRequest]s.
 *
 * The top-level GameEffectExecutor remains responsible for routing, common
 * validation, and Chronicle recording. Handlers interpret and execute the
 * mechanics of their assigned effects.
 *
 * [executor] is supplied during execution because a small number of effects
 * legitimately trigger other effects recursively (for example, an immediate
 * Wisp gained while Wispquake is resolving). Most handlers simply ignore it.
 */
interface EffectHandler {
    fun canExecute(request: GameEffectRequest): Boolean

    fun execute(
        request: GameEffectRequest,
        executor: GameEffectExecutor
    )
}
