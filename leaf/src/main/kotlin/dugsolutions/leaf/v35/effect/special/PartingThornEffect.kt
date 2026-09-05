package dugsolutions.leaf.v35.effect.special

import dugsolutions.leaf.v35.error.decisionCheck
import dugsolutions.leaf.v35.error.effectCheck
import dugsolutions.leaf.v35.error.stateCheck
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.GameEffectExecutor
import dugsolutions.leaf.v35.effect.GameEffectPhase
import dugsolutions.leaf.v35.effect.GameEffectRequest
import dugsolutions.leaf.v35.effect.handler.EffectHandler
import dugsolutions.leaf.v35.effect.handler.decisionContext
import dugsolutions.leaf.v35.effect.handler.decisionContextFor
import dugsolutions.leaf.v35.game.operation.WoundResolver
import dugsolutions.leaf.v35.player.decision.effect.ChooseOptionalEffectPlantRequest
import dugsolutions.leaf.v35.player.decision.effect.EffectPlantChoice

/**
 * Parting Thorn:
 *
 * Cultivation: the actor may flip one of their grafted Plant cards.
 * Battle: every opponent suffers one normal Wound.
 *
 * Battle delegates the complete Flip-It-or-Snip-It rule to WoundResolver,
 * including each opponent's own WoundStrategy decision and Grove returns for
 * Snipped cards.
 */
class PartingThornEffect : EffectHandler {

    override fun canExecute(
        request: GameEffectRequest
    ): Boolean =
        request.effect ==
            GameEffect.FLIP_OWN_PLANT_OR_WOUND_EACH_OPPONENT_IN_BATTLE

    override fun execute(
        request: GameEffectRequest,
        executor: GameEffectExecutor
    ) {
        effectCheck(canExecute(request)) {
            "Parting Thorn received wrong effect: ${request.effect}"
        }

        when (request.phase) {
            GameEffectPhase.CULTIVATION ->
                resolveCultivation(request)

            GameEffectPhase.BATTLE ->
                resolveBattle(request)
        }
    }

    private fun resolveCultivation(
        request: GameEffectRequest
    ) {
        val legalChoices =
            request.actor.creature.cards.map {
                EffectPlantChoice(
                    cardId = it.id,
                    cardName = it.card.name,
                    isFaceUp = it.isFaceUp
                )
            }

        if (legalChoices.isEmpty()) return

        val chosen =
            request.actor.decisions.effect.chooseOptionalPlant(
                ChooseOptionalEffectPlantRequest(
                    effect = request.effect,
                    legalChoices = legalChoices,
                    context = request.decisionContext()
                )
            ) ?: return

        decisionCheck(chosen in legalChoices) {
            "EffectStrategy returned illegal Plant target: " +
                "$chosen; legal=$legalChoices"
        }

        val current =
            request.actor.creature.get(
                chosen.cardId
            )

        decisionCheck(
            current != null &&
                current.card.name == chosen.cardName &&
                current.isFaceUp == chosen.isFaceUp
        ) {
            "Chosen Parting Thorn Plant target is stale or no longer owned: $chosen"
        }

        stateCheck(
            request.actor.creature.flip(
                chosen.cardId
            )
        ) {
            "Parting Thorn could not flip Plant target: $chosen"
        }
    }

    private fun resolveBattle(
        request: GameEffectRequest
    ) {
        val woundResolver =
            WoundResolver(
                grove = request.game.grove,
                chronicle = request.game.chronicle,
                decisionContext = { player -> request.decisionContextFor(player) }
            )

        request.game.players
            .filter { it !== request.actor }
            .forEach { opponent ->
                woundResolver.resolve(opponent)
            }
    }
}
