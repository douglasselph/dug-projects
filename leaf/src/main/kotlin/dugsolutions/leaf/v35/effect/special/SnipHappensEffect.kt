package dugsolutions.leaf.v35.effect.special

import dugsolutions.leaf.v35.error.effectCheck
import dugsolutions.leaf.v35.error.decisionCheck
import dugsolutions.leaf.v35.error.stateCheck
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.GameEffectExecutor
import dugsolutions.leaf.v35.effect.GameEffectRequest
import dugsolutions.leaf.v35.effect.handler.EffectHandler
import dugsolutions.leaf.v35.effect.handler.decisionContext
import dugsolutions.leaf.v35.effect.handler.decisionContextFor
import dugsolutions.leaf.v35.game.operation.WoundResolver
import dugsolutions.leaf.v35.player.Player
import dugsolutions.leaf.v35.player.decision.effect.ChooseEffectOpponentPlantWoundRequest
import dugsolutions.leaf.v35.player.decision.effect.EffectOpponentPlantWoundChoice
import dugsolutions.leaf.v35.player.decision.wound.WoundChoice

/**
 * Snip Happens:
 *
 * "Wound 1 card of your choice of an opponent's.
 *  You must choose a face up card first if there is one."
 *
 * Unlike an ordinary Wound, the effect actor chooses both the opponent and the
 * exact Plant card. The normal wound priority still applies separately to each
 * targeted opponent:
 *
 * - If that opponent has any face-up Plants, only those face-up cards may be
 *   chosen and the selected card is flipped face down.
 * - If that opponent has no face-up Plants, only current snippable outer cards
 *   may be chosen and the selected card is Snipped back to the Grove.
 *
 * WoundResolver performs the final current-state validation and mutation so
 * this effect cannot drift from the game's normal Flip-It-or-Snip-It rule.
 */
class SnipHappensEffect : EffectHandler {

    override fun canExecute(
        request: GameEffectRequest
    ): Boolean =
        request.effect ==
            GameEffect.WOUND_OPPONENT_PLANT_OF_YOUR_CHOICE

    override fun execute(
        request: GameEffectRequest,
        executor: GameEffectExecutor
    ) {
        effectCheck(
            canExecute(
                request
            )
        ) {
            "Snip Happens received wrong effect: ${request.effect}"
        }

        val woundResolver =
            WoundResolver(
                grove = request.game.grove,
                chronicle = request.game.chronicle,
                decisionContext = { player -> request.decisionContextFor(player) }
            )

        val legalChoices =
            request.game.players
                .filter {
                    it !== request.actor
                }
                .flatMap { opponent ->
                    woundResolver
                        .legalChoices(
                            opponent
                        )
                        .map { wound ->
                            wound.toEffectChoice(
                                opponent
                            )
                        }
                }

        // If no opponent has a legal Plant wound target, the effect simply
        // resolves without mutation.
        if (legalChoices.isEmpty()) {
            return
        }

        val chosen =
            request.actor.decisions.effect
                .chooseOpponentPlantWound(
                    ChooseEffectOpponentPlantWoundRequest(
                        effect = request.effect,
                        legalChoices =
                            legalChoices,
                        context = request.decisionContext()
                    )
                )

        /*
         * This is the first guard against a buggy strategy. In particular, a
         * face-down card is never offered for an opponent who still has a
         * face-up card.
         */
        decisionCheck(
            chosen in legalChoices
        ) {
            "EffectStrategy returned illegal Snip Happens target: " +
                "$chosen; legal=$legalChoices"
        }

        val opponent =
            request.game.players
                .firstOrNull {
                    it !== request.actor &&
                        it.id ==
                        chosen.ownerId
                }

        decisionCheck(
            opponent != null
        ) {
            "Chosen Snip Happens opponent is not part of this game: " +
                chosen.ownerId
        }

        val currentCard =
            opponent.creature.get(
                chosen.cardId
            )

        decisionCheck(
            currentCard != null &&
                currentCard.card.name ==
                chosen.cardName
        ) {
            "Chosen Snip Happens Plant target is stale or no longer owned: $chosen"
        }

        val woundChoice =
            when (chosen) {
                is EffectOpponentPlantWoundChoice.Flip ->
                    WoundChoice.Flip(
                        currentCard
                    )

                is EffectOpponentPlantWoundChoice.Snip ->
                    WoundChoice.Snip(
                        currentCard
                    )
            }

        /*
         * WoundResolver recomputes legality again immediately before mutation.
         * If the decision somehow names a face-down card while this opponent
         * has any face-up card, resolve(...) throws and nothing is changed.
         */
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
