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
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.decision.effect.ChooseEffectOpponentPlantWoundRequest
import dugsolutions.leaf.v35.player.decision.effect.ChooseEffectPlantRequest
import dugsolutions.leaf.v35.player.decision.effect.EffectOpponentPlantWoundChoice
import dugsolutions.leaf.v35.player.decision.effect.EffectPlantChoice

/** Current Shift Happens rule. */
class ShiftHappensEffect : EffectHandler {

    override fun canExecute(request: GameEffectRequest): Boolean =
        request.effect == GameEffect.FLIP_OWN_PLANT_OR_FLIP_OPPONENT_ROOT_OR_VINE_IN_BATTLE &&
            when (request.phase) {
                GameEffectPhase.CULTIVATION -> request.actor.creature.cards.isNotEmpty()
                GameEffectPhase.BATTLE -> legalOpponentChoices(request).isNotEmpty()
            }

    override fun execute(
        request: GameEffectRequest,
        executor: GameEffectExecutor
    ) {
        effectCheck(canExecute(request)) {
            "Shift Happens cannot execute in the current state"
        }

        when (request.phase) {
            GameEffectPhase.CULTIVATION -> flipOwnPlant(request)
            GameEffectPhase.BATTLE -> flipOpponentRootOrVine(request)
        }
    }

    private fun flipOwnPlant(request: GameEffectRequest) {
        val legalChoices = request.actor.creature.cards.map {
            EffectPlantChoice(
                cardId = it.id,
                cardName = it.card.name,
                isFaceUp = it.isFaceUp
            )
        }
        val chosen = request.actor.decisions.effect.choosePlantEffect(
            ChooseEffectPlantRequest(
                effect = request.effect,
                legalChoices = legalChoices,
                context = request.decisionContext()
            )
        )
        decisionCheck(chosen in legalChoices) {
            "EffectStrategy returned illegal Shift Happens own-Plant target: " +
                "$chosen; legal=$legalChoices"
        }
        val current = request.actor.creature.get(chosen.cardId)
        decisionCheck(
            current != null &&
                current.card.name == chosen.cardName &&
                current.isFaceUp == chosen.isFaceUp
        ) {
            "Chosen Shift Happens Plant target is stale or no longer owned: $chosen"
        }
        stateCheck(request.actor.creature.flip(chosen.cardId)) {
            "Shift Happens could not flip own Plant: $chosen"
        }
    }

    private fun flipOpponentRootOrVine(request: GameEffectRequest) {
        val legalChoices = legalOpponentChoices(request)
        val chosen = request.actor.decisions.effect.chooseOpponentPlantWound(
            ChooseEffectOpponentPlantWoundRequest(
                effect = request.effect,
                legalChoices = legalChoices,
                context = request.decisionContext()
            )
        )
        decisionCheck(chosen in legalChoices) {
            "EffectStrategy returned illegal Shift Happens opponent target: " +
                "$chosen; legal=$legalChoices"
        }
        val opponent = request.game.players.firstOrNull {
            it !== request.actor && it.id == chosen.ownerId
        }
        decisionCheck(opponent != null) {
            "Chosen Shift Happens opponent is not part of this game: ${chosen.ownerId}"
        }
        val current = opponent.creature.get(chosen.cardId)
        decisionCheck(
            current != null &&
                current.card.name == chosen.cardName &&
                current.card.type in setOf(PlantType.ROOT, PlantType.VINE)
        ) {
            "Chosen Shift Happens target is stale or no longer a Root/Vine: $chosen"
        }
        stateCheck(opponent.creature.flip(chosen.cardId)) {
            "Shift Happens could not flip opponent Plant: $chosen"
        }
    }

    private fun legalOpponentChoices(
        request: GameEffectRequest
    ): List<EffectOpponentPlantWoundChoice> =
        request.game.players
            .filter { it !== request.actor }
            .flatMap { opponent ->
                opponent.creature.cards
                    .filter { it.card.type == PlantType.ROOT || it.card.type == PlantType.VINE }
                    .map { card ->
                        EffectOpponentPlantWoundChoice.Flip(
                            ownerId = opponent.id,
                            cardId = card.id,
                            cardName = card.card.name
                        )
                    }
            }
}
