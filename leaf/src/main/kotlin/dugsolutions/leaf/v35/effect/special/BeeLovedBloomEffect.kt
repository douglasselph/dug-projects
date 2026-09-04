package dugsolutions.leaf.v35.effect.special

import dugsolutions.leaf.v35.error.effectCheck
import dugsolutions.leaf.v35.error.decisionCheck
import dugsolutions.leaf.v35.error.stateCheck
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.GameEffectExecutor
import dugsolutions.leaf.v35.effect.GameEffectRequest
import dugsolutions.leaf.v35.effect.handler.EffectHandler
import dugsolutions.leaf.v35.player.decision.effect.ChooseBeeSourceRequest
import dugsolutions.leaf.v35.player.decision.effect.EffectBeeSourceChoice
import dugsolutions.leaf.v35.tokens.Critter

/**
 * Bee-loved Bloom:
 *
 * Gain or Steal one Bee, then each Bee controlled by the actor is worth
 * exactly 4 for the remainder of the round.
 *
 * Unlike Root Appreciation's additive +2 Worm boost, Bee-loved Bloom is an
 * exact round value. Repeated resolutions remain 4 rather than stacking.
 *
 * The source decision is explicit: Grove, or the exact opponent from whom the
 * Bee will be stolen.
 */
class BeeLovedBloomEffect : EffectHandler {

    override fun canExecute(
        request: GameEffectRequest
    ): Boolean =
        request.effect ==
            GameEffect.GAIN_OR_STEAL_BEE_AND_BOOST_BEES_THIS_ROUND

    override fun execute(
        request: GameEffectRequest,
        executor: GameEffectExecutor
    ) {
        effectCheck(canExecute(request)) {
            "Bee-loved Bloom received wrong effect: ${request.effect}"
        }

        val legalSources =
            legalSources(request)

        if (legalSources.isNotEmpty()) {
            val chosen =
                request.actor.decisions.effect.chooseBeeSource(
                    ChooseBeeSourceRequest(
                        effect = request.effect,
                        legalChoices = legalSources
                    )
                )

            decisionCheck(chosen in legalSources) {
                "EffectStrategy returned illegal Bee source: " +
                    "$chosen; legal=$legalSources"
            }

            gainBeeFrom(
                request = request,
                source = chosen
            )
        }

        /*
         * "Each of your Bees is worth 4 this round" is exact, not additive.
         * This also applies to Bees gained later in the same round.
         */
        request.actor.critterValues.setForRound(
            critter = Critter.BEE,
            value = 4
        )
    }

    private fun legalSources(
        request: GameEffectRequest
    ): List<EffectBeeSourceChoice> =
        buildList {
            if (
                request.game.grove.critters.count(
                    Critter.BEE
                ) > 0
            ) {
                add(EffectBeeSourceChoice.Grove)
            }

            request.game.players
                .filter {
                    it !== request.actor &&
                        it.critters.count(Critter.BEE) > 0
                }
                .forEach {
                    add(
                        EffectBeeSourceChoice.Opponent(
                            playerId = it.id
                        )
                    )
                }
        }

    private fun gainBeeFrom(
        request: GameEffectRequest,
        source: EffectBeeSourceChoice
    ) {
        when (source) {
            EffectBeeSourceChoice.Grove -> {
                stateCheck(
                    request.game.grove.critters.remove(
                        Critter.BEE
                    )
                ) {
                    "Chosen Grove Bee source is no longer available"
                }
            }

            is EffectBeeSourceChoice.Opponent -> {
                val opponent =
                    request.game.players.firstOrNull {
                        it.id == source.playerId &&
                            it !== request.actor
                    }

                stateCheck(opponent != null) {
                    "Chosen Bee opponent is not part of this game: " +
                        source.playerId
                }

                stateCheck(
                    opponent.critters.remove(
                        Critter.BEE
                    )
                ) {
                    "Chosen opponent no longer has a Bee: " +
                        source.playerId
                }
            }
        }

        request.actor.critters.add(
            Critter.BEE
        )
    }
}
