package dugsolutions.leaf.v35.effect.special

import dugsolutions.leaf.v35.error.effectCheck
import dugsolutions.leaf.v35.error.decisionCheck
import dugsolutions.leaf.v35.error.stateCheck
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.GameEffectExecutor
import dugsolutions.leaf.v35.effect.GameEffectRequest
import dugsolutions.leaf.v35.effect.handler.EffectHandler
import dugsolutions.leaf.v35.effect.handler.decisionContext
import dugsolutions.leaf.v35.effect.handler.handChoices
import dugsolutions.leaf.v35.effect.handler.resolveHandDie
import dugsolutions.leaf.v35.player.decision.effect.ChooseEffectCritterDieRequest
import dugsolutions.leaf.v35.player.decision.effect.EffectCritterDieChoice
import dugsolutions.leaf.v35.tokens.Critter

/**
 * Vine and Dine: Trash 1 owned Worm/Bee and Raise a chosen die +5.
 *
 * The decision is deliberately atomic: the strategy chooses both the Critter
 * to Trash and the die to Raise in one immutable choice.
 */
class VineAndDineEffect : EffectHandler {

    override fun canExecute(
        request: GameEffectRequest
    ): Boolean =
        request.effect == GameEffect.TRASH_CRITTER_TO_RAISE_DIE_PLUS_5 &&
            legalChoices(request).isNotEmpty()

    override fun execute(
        request: GameEffectRequest,
        executor: GameEffectExecutor
    ) {
        effectCheck(canExecute(request)) {
            "Vine and Dine cannot execute in the current state"
        }

        val legalChoices = legalChoices(request)
        val chosen = request.actor.decisions.effect.chooseCritterAndDie(
            ChooseEffectCritterDieRequest(
                effect = request.effect,
                legalChoices = legalChoices,
                context = request.decisionContext()
            )
        )
        decisionCheck(chosen in legalChoices) {
            "EffectStrategy returned illegal Vine and Dine choice: " +
                "$chosen; legal=$legalChoices"
        }

        /* Resolve/validate every target before mutating either resource. */
        val die = resolveHandDie(request.actor, chosen.die)
        decisionCheck(chosen.critter in request.actor.critters.all) {
            "Chosen Vine and Dine Critter is no longer owned: ${chosen.critter}"
        }

        /* Trash means remove from the game: do NOT return this Critter to Grove. */
        stateCheck(request.actor.critters.remove(chosen.critter)) {
            "Validated Vine and Dine Critter could not be Trashed: ${chosen.critter}"
        }
        die.adjustBy(5)
    }

    private fun legalChoices(
        request: GameEffectRequest
    ): List<EffectCritterDieChoice> {
        val dice = handChoices(request.actor)
        if (dice.isEmpty()) return emptyList()

        val critters = request.actor.critters.all
            .filter { it == Critter.BEE || it == Critter.WORM }
            .distinct()

        return critters.flatMap { critter ->
            dice.map { die ->
                EffectCritterDieChoice(
                    critter = critter,
                    die = die
                )
            }
        }
    }
}
