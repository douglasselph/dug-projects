package dugsolutions.leaf.v35.effect.handler

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.GameEffectExecutor
import dugsolutions.leaf.v35.effect.GameEffectRequest
import dugsolutions.leaf.v35.random.die.DieSides
import dugsolutions.leaf.v35.tokens.Token

/**
 * Effects that gain, spend, or attach shared non-die resources.
 *
 * This currently owns Water, Mulch storage, and direct VP gain. Future simple
 * Critter/Wisp/Butterfly gains can naturally join this family. Effects with a
 * substantial algorithm should still receive their own special implementation.
 */
class ResourceEffectHandler : EffectHandler {

    override fun canExecute(
        request: GameEffectRequest
    ): Boolean =
        when (request.effect) {
            GameEffect.GAIN_WATER_TOKEN ->
                request.game.grove.tokens.hasWater

            GameEffect.MULCH_DIE_FROM_HAND ->
                request.actor.dice.hand.isNotEmpty() &&
                    request.game.grove.tokens.mulchTokens.any { it.sides == null }

            GameEffect.GAIN_ONE_VP ->
                true

            else -> false
        }

    override fun execute(
        request: GameEffectRequest,
        executor: GameEffectExecutor
    ) {
        check(canExecute(request)) {
            "Resource handler cannot execute effect: ${request.effect}"
        }

        when (request.effect) {
            GameEffect.GAIN_WATER_TOKEN ->
                gainWater(request)

            GameEffect.MULCH_DIE_FROM_HAND ->
                mulchFromHand(request)

            GameEffect.GAIN_ONE_VP ->
                request.actor.addVp(1)

            else -> error(
                "Unsupported effect reached ResourceEffectHandler: ${request.effect}"
            )
        }
    }

    private fun gainWater(
        request: GameEffectRequest
    ) {
        val token = checkNotNull(
            request.game.grove.tokens.pull(Token.WATER)
        ) {
            "Validated Water effect could not take Water from Grove"
        }
        request.actor.tokens.add(token)
    }

    private fun mulchFromHand(
        request: GameEffectRequest
    ) {
        val emptyMulch = request.game.grove.tokens.mulchTokens
            .firstOrNull { it.sides == null }
        check(emptyMulch != null) {
            "Validated Mulch effect has no empty Mulch token in Grove"
        }

        val die = chooseRequiredHandDie(
            request = request,
            legalChoices = handChoices(request.actor)
        )
        val sides = DieSides.from(die.sides)

        check(request.actor.dice.removeFromHand(die) != null) {
            "Validated Mulch die could not be removed from Hand: $die"
        }
        check(request.game.grove.tokens.pull(emptyMulch) != null) {
            "Validated empty Mulch token could not be removed from Grove"
        }

        /*
         * A die stored on Mulch cannot be used until a later round. Keep it
         * pending through the rest of this Build; Cultivation cleanup
         * normalizes it into an ordinary usable Mulch token.
         */
        request.actor.tokens.add(
            Token.PENDING_MULCH(sides)
        )
    }
}
