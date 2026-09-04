package dugsolutions.leaf.v35.effect.handler

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.GameEffectExecutor
import dugsolutions.leaf.v35.effect.GameEffectRequest
import dugsolutions.leaf.v35.game.operation.UpgradeResolver
import dugsolutions.leaf.v35.player.decision.effect.EffectDieChoice

/**
 * Upgrade-family effects.
 *
 * The handler interprets the card effect while [UpgradeResolver] remains the
 * reusable owner of the actual Upgrade rules: size ladder, Graft Bed
 * availability, D4 return, replacement creation, and destination.
 */
class UpgradeEffectHandler(
    private val upgradeResolver: UpgradeResolver = UpgradeResolver()
) : EffectHandler {

    override fun canExecute(
        request: GameEffectRequest
    ): Boolean =
        when (request.effect) {
            GameEffect.UPGRADE_DIE_FROM_HAND ->
                upgradeChoices(request).isNotEmpty()

            else -> false
        }

    override fun execute(
        request: GameEffectRequest,
        executor: GameEffectExecutor
    ) {
        check(canExecute(request)) {
            "Upgrade handler cannot execute effect: ${request.effect}"
        }

        when (request.effect) {
            GameEffect.UPGRADE_DIE_FROM_HAND -> {
                val die = chooseRequiredHandDie(
                    request = request,
                    legalChoices = upgradeChoices(request)
                )

                upgradeResolver.upgradeFromHandToDiscard(
                    game = request.game,
                    player = request.actor,
                    die = die
                )
            }

            else -> error(
                "Unsupported effect reached UpgradeEffectHandler: ${request.effect}"
            )
        }
    }

    private fun upgradeChoices(
        request: GameEffectRequest
    ): List<EffectDieChoice> =
        handChoices(request.actor) { die ->
            upgradeResolver.canUpgradeNormalStep(
                game = request.game,
                die = die
            )
        }
}
