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
import dugsolutions.leaf.v35.player.Player
import dugsolutions.leaf.v35.player.decision.effect.ChooseEffectOpponentPlantWoundRequest
import dugsolutions.leaf.v35.player.decision.effect.ChooseOptionalEffectPlantRequest
import dugsolutions.leaf.v35.player.decision.effect.EffectOpponentPlantWoundChoice
import dugsolutions.leaf.v35.player.decision.effect.EffectPlantChoice
import dugsolutions.leaf.v35.player.decision.wound.WoundChoice

/**
 * Parting Thorn:
 *
 * Current rule:
 * Cultivation: the actor may flip one of their grafted Plant cards.
 * Battle: choose one opponent; they suffer one Wound, and the actor chooses
 * the affected card while still obeying Flip-It-or-Snip-It legality.
 *
 * The retired "wound every opponent" enum remains supported temporarily so
 * older focused tests and saved scenarios can be migrated independently.
 */
class PartingThornEffect : EffectHandler {

    override fun canExecute(
        request: GameEffectRequest
    ): Boolean =
        request.effect == GameEffect.FLIP_OWN_PLANT_OR_WOUND_CHOSEN_OPPONENT_CHOOSE_CARD_IN_BATTLE ||
            request.effect == GameEffect.FLIP_OWN_PLANT_OR_WOUND_EACH_OPPONENT_IN_BATTLE

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
                if (request.effect == GameEffect.FLIP_OWN_PLANT_OR_WOUND_CHOSEN_OPPONENT_CHOOSE_CARD_IN_BATTLE) {
                    resolveCurrentBattle(request)
                } else {
                    resolveLegacyBattle(request)
                }
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

    private fun resolveLegacyBattle(
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

    private fun resolveCurrentBattle(
        request: GameEffectRequest
    ) {
        val woundResolver =
            WoundResolver(
                grove = request.game.grove,
                chronicle = request.game.chronicle,
                decisionContext = { player -> request.decisionContextFor(player) }
            )

        val legalChoices = request.game.players
            .filter { it !== request.actor }
            .flatMap { opponent ->
                woundResolver.legalChoices(opponent).map { wound ->
                    wound.toEffectChoice(opponent)
                }
            }

        if (legalChoices.isEmpty()) return

        val chosen = request.actor.decisions.effect.chooseOpponentPlantWound(
            ChooseEffectOpponentPlantWoundRequest(
                effect = request.effect,
                legalChoices = legalChoices,
                context = request.decisionContext()
            )
        )
        decisionCheck(chosen in legalChoices) {
            "EffectStrategy returned illegal Parting Thorn target: " +
                "$chosen; legal=$legalChoices"
        }

        val opponent = request.game.players.firstOrNull {
            it !== request.actor && it.id == chosen.ownerId
        }
        decisionCheck(opponent != null) {
            "Chosen Parting Thorn opponent is not part of this game: ${chosen.ownerId}"
        }

        val currentCard = opponent.creature.get(chosen.cardId)
        decisionCheck(
            currentCard != null && currentCard.card.name == chosen.cardName
        ) {
            "Chosen Parting Thorn Plant target is stale or no longer owned: $chosen"
        }

        val woundChoice = when (chosen) {
            is EffectOpponentPlantWoundChoice.Flip -> WoundChoice.Flip(currentCard)
            is EffectOpponentPlantWoundChoice.Snip -> WoundChoice.Snip(currentCard)
        }

        woundResolver.resolve(
            player = opponent,
            choice = woundChoice
        )
    }

    private fun WoundChoice.toEffectChoice(
        owner: Player
    ): EffectOpponentPlantWoundChoice =
        when (this) {
            is WoundChoice.Flip ->
                EffectOpponentPlantWoundChoice.Flip(
                    ownerId = owner.id,
                    cardId = card.id,
                    cardName = card.card.name
                )

            is WoundChoice.Snip ->
                EffectOpponentPlantWoundChoice.Snip(
                    ownerId = owner.id,
                    cardId = card.id,
                    cardName = card.card.name
                )
        }

}
