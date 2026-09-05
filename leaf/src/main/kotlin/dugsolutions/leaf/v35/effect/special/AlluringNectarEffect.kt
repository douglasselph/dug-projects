package dugsolutions.leaf.v35.effect.special

import dugsolutions.leaf.v35.error.effectCheck
import dugsolutions.leaf.v35.error.decisionCheck
import dugsolutions.leaf.v35.error.stateCheck
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.GameEffectExecutor
import dugsolutions.leaf.v35.effect.GameEffectRequest
import dugsolutions.leaf.v35.effect.handler.EffectHandler
import dugsolutions.leaf.v35.effect.handler.decisionContext
import dugsolutions.leaf.v35.player.decision.effect.ChooseEffectButterflyTargetRequest
import dugsolutions.leaf.v35.player.decision.effect.EffectButterflyTargetChoice

/**
 * Alluring Nectar:
 *
 * Steal one Butterfly controlled by an opponent, when any are available, then
 * turn every Butterfly controlled by the actor face up.
 *
 * The strategy chooses the exact opponent + Butterfly pair. If no opponent
 * controls a Butterfly, the Steal is skipped but the Refresh portion still
 * resolves.
 */
class AlluringNectarEffect : EffectHandler {

    override fun canExecute(
        request: GameEffectRequest
    ): Boolean =
        request.effect ==
            GameEffect.STEAL_BUTTERFLY_AND_REFRESH_ALL_BUTTERFLIES

    override fun execute(
        request: GameEffectRequest,
        executor: GameEffectExecutor
    ) {
        effectCheck(canExecute(request)) {
            "Alluring Nectar received wrong effect: ${request.effect}"
        }

        val legalChoices = legalTargets(request)

        if (legalChoices.isNotEmpty()) {
            val chosen =
                request.actor.decisions.effect.chooseButterflyTarget(
                    ChooseEffectButterflyTargetRequest(
                        effect = request.effect,
                        legalChoices = legalChoices,
                        context = request.decisionContext()
                    )
                )

            decisionCheck(chosen in legalChoices) {
                "EffectStrategy returned illegal Butterfly target: " +
                    "$chosen; legal=$legalChoices"
            }

            stealButterfly(
                request = request,
                choice = chosen
            )
        }

        request.actor.butterflies.all.forEach { butterfly ->
            stateCheck(request.actor.butterflies.faceUp(butterfly)) {
                "Alluring Nectar could not Refresh owned Butterfly: $butterfly"
            }
        }
    }

    private fun legalTargets(
        request: GameEffectRequest
    ): List<EffectButterflyTargetChoice> =
        request.game.players
            .filter { it !== request.actor }
            .flatMap { opponent ->
                opponent.butterflies.all.map { butterfly ->
                    EffectButterflyTargetChoice(
                        ownerId = opponent.id,
                        butterfly = butterfly
                    )
                }
            }

    private fun stealButterfly(
        request: GameEffectRequest,
        choice: EffectButterflyTargetChoice
    ) {
        val opponent =
            request.game.players.firstOrNull {
                it !== request.actor &&
                    it.id == choice.ownerId
            }

        stateCheck(opponent != null) {
            "Chosen Butterfly owner is not a legal opponent: ${choice.ownerId}"
        }

        stateCheck(
            opponent.butterflies.remove(
                choice.butterfly
            )
        ) {
            "Chosen opponent no longer controls Butterfly: $choice"
        }

        request.actor.butterflies.add(
            choice.butterfly
        )
    }
}
