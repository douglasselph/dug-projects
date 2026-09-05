package dugsolutions.leaf.v35.effect.special

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
 * the first resolves. This is important: a face-up Plant cannot be "played
 * again", but it may first be Flipped face down and then be a legal Play choice
 * for the second decision.
 *
 * Play resolves the effect of a spent (face-down) Plant and leaves that Plant
 * face down. Flip simply toggles the chosen Plant's facing.
 */
class OEdelweissEffect : EffectHandler {

    override fun canExecute(
        request: GameEffectRequest
    ): Boolean =
        request.effect ==
            GameEffect.PLAY_OR_FLIP_ANOTHER_CARD_TWICE &&
            flippableCards(request).isNotEmpty()

    override fun execute(
        request: GameEffectRequest,
        executor: GameEffectExecutor
    ) {
        effectCheck(
            request.effect ==
                GameEffect.PLAY_OR_FLIP_ANOTHER_CARD_TWICE
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
                request.actor.decisions.effect
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
                    // Only an already-spent card can be played again.
                    if (card.isFaceDown) {
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
                current.isFaceDown &&
                !choice.card.isFaceUp &&
                current.id !in activePath
        ) {
            "O Edelweiss can only replay another currently spent Plant: ${choice.card}"
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

        executor.execute(
            nested
        )

        // Replaying a spent card uses its effect; it does not refresh that
        // card. Enforce the spent state even if the nested effect itself could
        // otherwise manipulate its source card.
        stateCheck(
            request.actor.creature.faceDown(
                current.id
            )
        ) {
            "O Edelweiss could not preserve replayed Plant as spent: ${choice.card}"
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
            effect = card.card.effect,
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
