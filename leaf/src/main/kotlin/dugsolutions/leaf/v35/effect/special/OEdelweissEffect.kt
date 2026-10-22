package dugsolutions.leaf.v35.effect.special

import dugsolutions.leaf.v35.effect.handler.effectDecisionStrategy
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.GameEffectExecutor
import dugsolutions.leaf.v35.effect.GameEffectRequest
import dugsolutions.leaf.v35.effect.GameEffectSource
import dugsolutions.leaf.v35.effect.handler.EffectHandler
import dugsolutions.leaf.v35.effect.handler.decisionContext
import dugsolutions.leaf.v35.error.decisionCheck
import dugsolutions.leaf.v35.error.effectCheck
import dugsolutions.leaf.v35.error.stateCheck
import dugsolutions.leaf.v35.player.creature.CreatureCard
import dugsolutions.leaf.v35.player.creature.CreatureCardId
import dugsolutions.leaf.v35.player.decision.effect.ChooseOEdelweissRequest
import dugsolutions.leaf.v35.player.decision.effect.EffectPlantChoice
import dugsolutions.leaf.v35.player.decision.effect.OEdelweissChoice

/**
 * O Edelweiss:
 *
 * Twice, you may play another grafted Plant card or flip another card.
 *
 * The two decisions are made sequentially and legal choices are rebuilt after
 * the first resolves. The spent-only variant can first flip a face-up Plant
 * down and then replay it; the any-facing variant permits replay directly.
 *
 * Play resolves another Plant effect without consuming its facing. For an
 * already-spent Plant, the spent state is preserved. Flip toggles facing.
 */
class OEdelweissEffect : EffectHandler {

    private fun accepts(effect: GameEffect) = effect in setOf(
        GameEffect.PLAY_OR_FLIP_ANOTHER_CARD_TWICE,
        GameEffect.PLAY_SPENT_OR_FLIP_ANOTHER_CARD_TWICE,
        GameEffect.PLAY_ANY_OR_FLIP_ANOTHER_CARD_TWICE
    )

    private fun allowsFaceUp(effect: GameEffect) =
        effect == GameEffect.PLAY_ANY_OR_FLIP_ANOTHER_CARD_TWICE

    override fun canExecute(
        request: GameEffectRequest
    ): Boolean =
        accepts(request.effect) &&
            flippableCards(request).isNotEmpty()

    override fun execute(
        request: GameEffectRequest,
        executor: GameEffectExecutor
    ) {
        effectCheck(
            accepts(request.effect)
        ) {
            "O Edelweiss received wrong effect: ${request.effect}"
        }

        repeat(2) { zeroBased ->
            val choiceNumber =
                zeroBased + 1
            val legalChoices =
                legalChoices(
                    request = request,
                    executor = executor
                )

            // "Twice, you may..." always permits ending early.
            val offered =
                legalChoices +
                    OEdelweissChoice.Done

            val chosen =
                request.effectDecisionStrategy()
                    .chooseOEdelweiss(
                        ChooseOEdelweissRequest(
                            effect = request.effect,
                            choiceNumber =
                                choiceNumber,
                            legalChoices =
                                offered,
                            context = request.decisionContext()
                        )
                    )

            decisionCheck(
                chosen in offered
            ) {
                "EffectStrategy returned illegal O Edelweiss choice: " +
                    "$chosen; legal=$offered"
            }

            when (chosen) {
                OEdelweissChoice.Done ->
                    return

                is OEdelweissChoice.Flip ->
                    executeFlip(
                        request = request,
                        choice = chosen
                    )

                is OEdelweissChoice.Play ->
                    executePlay(
                        request = request,
                        executor = executor,
                        choice = chosen
                    )
            }
        }
    }

    private fun legalChoices(
        request: GameEffectRequest,
        executor: GameEffectExecutor
    ): List<OEdelweissChoice> {
        val activePath =
            activePath(
                request
            )

        return buildList {
            request.actor.creature.cards
                .filter {
                    it.id !in
                        activePath
                }
                .forEach { card ->
                    // The experimental any-facing variant also offers face-up cards.
                    if (card.isFaceDown || allowsFaceUp(request.effect)) {
                        val nested =
                            nestedRequest(
                                request = request,
                                card = card,
                                activePath =
                                    activePath
                            )

                        if (executor.canExecute(
                                nested
                            )
                        ) {
                            add(
                                OEdelweissChoice.Play(
                                    card.toChoice()
                                )
                            )
                        }
                    }

                    // Any other card may be flipped, regardless of facing.
                    add(
                        OEdelweissChoice.Flip(
                            card.toChoice()
                        )
                    )
                }
        }
    }

    private fun executeFlip(
        request: GameEffectRequest,
        choice: OEdelweissChoice.Flip
    ) {
        val activePath =
            activePath(
                request
            )

        val current =
            request.actor.creature.get(
                choice.card.cardId
            )

        decisionCheck(
            current != null &&
                current.card.name ==
                    choice.card.cardName &&
                current.isFaceUp ==
                    choice.card.isFaceUp &&
                current.id !in activePath
        ) {
            "Chosen O Edelweiss Flip target is stale or illegal: ${choice.card}"
        }

        stateCheck(
            request.actor.creature.flip(
                current.id
            )
        ) {
            "O Edelweiss could not flip Plant target: ${choice.card}"
        }
    }

    private fun executePlay(
        request: GameEffectRequest,
        executor: GameEffectExecutor,
        choice: OEdelweissChoice.Play
    ) {
        val activePath =
            activePath(
                request
            )

        val current =
            request.actor.creature.get(
                choice.card.cardId
            )

        decisionCheck(
            current != null &&
                current.card.name ==
                    choice.card.cardName &&
                current.isFaceUp == choice.card.isFaceUp &&
                (current.isFaceDown || allowsFaceUp(request.effect)) &&
                current.id !in activePath
        ) {
            "O Edelweiss Play target is stale or not eligible: ${choice.card}"
        }

        val nested =
            nestedRequest(
                request = request,
                card = current,
                activePath = activePath
            )

        decisionCheck(
            executor.canExecute(
                nested
            )
        ) {
            "Chosen O Edelweiss Plant effect is no longer executable: ${choice.card}"
        }

        val originallySpent = current.isFaceDown
        executor.execute(
            nested
        )

        // Replaying a spent card uses its effect; it does not refresh that
        // card. Enforce the spent state even if the nested effect itself could
        // otherwise manipulate its source card.
        if (originallySpent) {
            stateCheck(request.actor.creature.faceDown(current.id)) {
                "O Edelweiss could not preserve replayed Plant as spent: ${choice.card}"
            }
        }
    }

    private fun flippableCards(
        request: GameEffectRequest
    ): List<CreatureCard> {
        val activePath =
            activePath(
                request
            )

        return request.actor.creature.cards
            .filter {
                it.id !in activePath
            }
    }

    private fun activePath(
        request: GameEffectRequest
    ): List<CreatureCardId> =
        buildList {
            addAll(
                request.plantEffectPath
            )
            val source =
                request.source
                    as? GameEffectSource.Plant
            if (source != null &&
                source.card.id !in this
            ) {
                add(
                    source.card.id
                )
            }
        }

    private fun nestedRequest(
        request: GameEffectRequest,
        card: CreatureCard,
        activePath: List<CreatureCardId>
    ): GameEffectRequest =
        GameEffectRequest(
            game = request.game,
            actor = request.actor,
            effect = request.game.config.plantValues.effectFor(card.card),
            source =
                GameEffectSource.Plant(
                    card
                ),
            phase = request.phase,
            battleState = request.battleState,
            plantEffectPath =
                activePath
        )

    private fun CreatureCard.toChoice() =
        EffectPlantChoice(
            cardId = id,
            cardName = card.name,
            isFaceUp = isFaceUp
        )
}
