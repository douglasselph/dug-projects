package dugsolutions.leaf.v35.effect.handler

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.GameEffectExecutor
import dugsolutions.leaf.v35.effect.GameEffectRequest

/**
 * Related effects that directly change visible die values without moving the
 * die between gameplay zones.
 *
 * Additional Raise/Set/Flip effects should generally join this family rather
 * than each becoming a separate implementation class.
 */
class DieValueEffectHandler : EffectHandler {

    override fun canExecute(
        request: GameEffectRequest
    ): Boolean =
        when (request.effect) {
            GameEffect.RAISE_DIE_PLUS_3 ->
                request.actor.dice.hand.isNotEmpty()

            else -> false
        }

    override fun execute(
        request: GameEffectRequest,
        executor: GameEffectExecutor
    ) {
        check(canExecute(request)) {
            "Die-value handler cannot execute effect: ${request.effect}"
        }

        when (request.effect) {
            GameEffect.RAISE_DIE_PLUS_3 ->
                chooseRequiredHandDie(
                    request = request,
                    legalChoices = handChoices(request.actor)
                ).adjustBy(3)

            else -> error(
                "Unsupported effect reached DieValueEffectHandler: ${request.effect}"
            )
        }
    }
}
