package dugsolutions.leaf.v35.effect.special

import dugsolutions.leaf.v35.chronicle.domain.Moment
import dugsolutions.leaf.v35.chronicle.domain.stateSnapshot
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
 * Gain one Butterfly from the Grove or steal one controlled by an opponent,
 * then turn every Butterfly controlled by the actor face up.
 *
 * The strategy chooses the exact source + Butterfly pair from all currently
 * legal Grove and opponent sources. If no Butterfly can be gained or stolen,
 * the acquisition is skipped but the Refresh portion still resolves.
 */
class AlluringNectarEffect : EffectHandler {

    override fun canExecute(
        request: GameEffectRequest
    ): Boolean =
        request.effect == GameEffect.GAIN_OR_STEAL_BUTTERFLY_AND_REFRESH_ALL_BUTTERFLIES ||
            request.effect == GameEffect.STEAL_BUTTERFLY_AND_REFRESH_ALL_BUTTERFLIES

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

            takeButterfly(
                request = request,
                choice = chosen
            )
        }

        request.actor.butterflies.all.forEach { butterfly ->
            stateCheck(request.actor.butterflies.faceUp(butterfly)) {
                "Alluring Nectar could not Refresh owned Butterfly: $butterfly"
            }
        }

        request.game.chronicle.record(
            Moment.ButterflyState(
                playerId = request.actor.id,
                butterflies = request.actor.butterflies.stateSnapshot()
            )
        )
    }

    private fun legalTargets(
        request: GameEffectRequest
    ): List<EffectButterflyTargetChoice> =
        buildList {
            if (request.effect == GameEffect.GAIN_OR_STEAL_BUTTERFLY_AND_REFRESH_ALL_BUTTERFLIES) {
                request.game.grove.butterflies.all.forEach { butterfly ->
                    add(
                        EffectButterflyTargetChoice(
                            ownerId = null,
                            butterfly = butterfly
                        )
                    )
                }
            }

            request.game.players
                .filter { it !== request.actor }
                .forEach { opponent ->
                    opponent.butterflies.all.forEach { butterfly ->
                        add(
                            EffectButterflyTargetChoice(
                                ownerId = opponent.id,
                                butterfly = butterfly
                            )
                        )
                    }
                }
        }

    private fun takeButterfly(
        request: GameEffectRequest,
        choice: EffectButterflyTargetChoice
    ) {
        if (choice.ownerId == null) {
            stateCheck(
                request.game.grove.butterflies.remove(choice.butterfly)
            ) {
                "Chosen Grove no longer contains Butterfly: $choice"
            }
        } else {
            val opponent =
                request.game.players.firstOrNull {
                    it !== request.actor &&
                        it.id == choice.ownerId
                }

            stateCheck(opponent != null) {
                "Chosen Butterfly owner is not a legal opponent: ${choice.ownerId}"
            }

            stateCheck(
                opponent.butterflies.remove(choice.butterfly)
            ) {
                "Chosen opponent no longer controls Butterfly: $choice"
            }
        }

        request.actor.butterflies.add(choice.butterfly)
    }

}
