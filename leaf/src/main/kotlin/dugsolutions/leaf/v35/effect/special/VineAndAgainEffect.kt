package dugsolutions.leaf.v35.effect.special

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.GameEffectExecutor
import dugsolutions.leaf.v35.effect.GameEffectRequest
import dugsolutions.leaf.v35.effect.GameEffectSource
import dugsolutions.leaf.v35.effect.handler.EffectHandler
import dugsolutions.leaf.v35.error.decisionCheck
import dugsolutions.leaf.v35.error.effectCheck
import dugsolutions.leaf.v35.error.stateCheck
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.creature.CreatureCard
import dugsolutions.leaf.v35.player.decision.effect.ChooseEffectPlantRequest
import dugsolutions.leaf.v35.player.decision.effect.EffectPlantChoice

/**
 * Vine and Again:
 *
 * Use the effect of another spent Root or Vine.
 *
 * "Spent" means face down. The chosen card remains face down; only its effect
 * is executed. A small recursion path is carried through nested Plant effects
 * so legal chains are allowed while cycles such as VineAgain-A -> VineAgain-B
 * -> VineAgain-A are rejected before execution.
 */
class VineAndAgainEffect : EffectHandler {

    override fun canExecute(
        request: GameEffectRequest
    ): Boolean =
        request.effect ==
            GameEffect.REUSE_SPENT_ROOT_OR_VINE_EFFECT &&
            structuralCandidates(request).isNotEmpty()

    override fun execute(
        request: GameEffectRequest,
        executor: GameEffectExecutor
    ) {
        effectCheck(
            request.effect ==
                GameEffect.REUSE_SPENT_ROOT_OR_VINE_EFFECT
        ) {
            "Vine and Again received wrong effect: ${request.effect}"
        }

        val activePath = activePath(request)

        val candidates =
            structuralCandidates(request)
                .mapNotNull { card ->
                    val nested =
                        nestedRequest(
                            request = request,
                            card = card,
                            activePath = activePath
                        )

                    card.takeIf {
                        executor.canExecute(
                            nested
                        )
                    }
                }

        effectCheck(
            candidates.isNotEmpty()
        ) {
            "Vine and Again has no spent Root or Vine with an executable effect"
        }

        val legalChoices =
            candidates.map {
                it.toChoice()
            }

        val chosen =
            request.actor.decisions.effect
                .choosePlantEffect(
                    ChooseEffectPlantRequest(
                        effect = request.effect,
                        legalChoices =
                            legalChoices
                    )
                )

        decisionCheck(
            chosen in legalChoices
        ) {
            "EffectStrategy returned illegal Vine and Again target: " +
                "$chosen; legal=$legalChoices"
        }

        val current =
            request.actor.creature.get(
                chosen.cardId
            )

        decisionCheck(
            current != null &&
                current.card.name ==
                    chosen.cardName &&
                current.isFaceDown &&
                current.card.type in
                    setOf(
                        PlantType.ROOT,
                        PlantType.VINE
                    ) &&
                current.id !in activePath
        ) {
            "Chosen Vine and Again target is stale or no longer a spent Root/Vine: $chosen"
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
            "Chosen Vine and Again Plant effect is no longer executable: $chosen"
        }

        executor.execute(
            nested
        )

        // The reused card was already spent and remains spent. This is an
        // effect rule, not a strategy choice, so restore that state even if
        // the nested effect could otherwise manipulate its own source.
        stateCheck(
            request.actor.creature.faceDown(
                current.id
            )
        ) {
            "Vine and Again could not preserve reused Plant as spent: $chosen"
        }
    }

    private fun structuralCandidates(
        request: GameEffectRequest
    ): List<CreatureCard> {
        val activePath =
            activePath(
                request
            )

        return request.actor.creature.cards
            .filter {
                it.isFaceDown &&
                    it.card.type in
                    setOf(
                        PlantType.ROOT,
                        PlantType.VINE
                    ) &&
                    it.id !in
                    activePath
            }
    }

    private fun activePath(
        request: GameEffectRequest
    ) =
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
        activePath: List<
            dugsolutions.leaf.v35.player.creature.CreatureCardId
        >
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
