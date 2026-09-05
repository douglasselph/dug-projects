package dugsolutions.leaf.v35.effect.special

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.GameEffectExecutor
import dugsolutions.leaf.v35.effect.GameEffectRequest
import dugsolutions.leaf.v35.effect.GameEffectSource
import dugsolutions.leaf.v35.effect.handler.EffectHandler
import dugsolutions.leaf.v35.effect.handler.decisionContextFor
import dugsolutions.leaf.v35.error.decisionCheck
import dugsolutions.leaf.v35.error.effectCheck
import dugsolutions.leaf.v35.error.stateCheck
import dugsolutions.leaf.v35.player.Player
import dugsolutions.leaf.v35.player.decision.effect.ChooseWispsToKeepRequest
import dugsolutions.leaf.v35.player.decision.effect.EffectWispChoice
import dugsolutions.leaf.v35.wisp.domain.WispCard

/**
 * Wisp Reckoning, using the current CSV rule:
 *
 * - Each opponent keeps up to 3 Wisps.
 * - The actor keeps up to 4 Wisps.
 * - Trash the rest.
 *
 * Every affected player chooses their own Wisps to keep through that player's
 * EffectStrategy. All required decisions are collected and validated before
 * any Wisp is Trashed, so a buggy decision from one player cannot leave the
 * effect half-resolved.
 *
 * The Wisp Reckoning card currently being resolved is excluded from the
 * actor's keep/trash calculation; SupportActionExecutor removes that played
 * Wisp after this effect returns.
 *
 * This creates no ongoing hand limit.
 */
class WispReckoningEffect : EffectHandler {

    private companion object {
        const val OPPONENT_KEEP_LIMIT = 3
        const val ACTOR_KEEP_LIMIT = 4
    }

    override fun canExecute(
        request: GameEffectRequest
    ): Boolean =
        request.effect ==
            GameEffect.LIMIT_WISPS_AND_TRASH_EXCESS

    override fun execute(
        request: GameEffectRequest,
        executor: GameEffectExecutor
    ) {
        effectCheck(
            canExecute(
                request
            )
        ) {
            "Wisp Reckoning received wrong effect: ${request.effect}"
        }

        val source =
            request.source
                as? GameEffectSource.Wisp

        effectCheck(
            source != null
        ) {
            "Wisp Reckoning must be executed from a Wisp source"
        }

        val plans =
            buildList {
                request.game.players
                    .filter {
                        it !== request.actor
                    }
                    .forEach { opponent ->
                        createTrimPlan(
                            request = request,
                            player = opponent,
                            limit =
                                OPPONENT_KEEP_LIMIT,
                            sourceToExclude =
                                null,
                            effect =
                                request.effect
                        )?.let(::add)
                    }

                createTrimPlan(
                    request = request,
                    player = request.actor,
                    limit = ACTOR_KEEP_LIMIT,
                    sourceToExclude =
                        source.card,
                    effect = request.effect
                )?.let(::add)
            }

        // No state mutation happens until every player's decision is valid.
        plans.forEach {
            applyTrimPlan(
                it
            )
        }
    }

    private fun createTrimPlan(
        request: GameEffectRequest,
        player: Player,
        limit: Int,
        sourceToExclude: WispCard?,
        effect: GameEffect
    ): TrimPlan? {
        val current =
            currentEligibleCards(
                player = player,
                sourceToExclude =
                    sourceToExclude
            )

        if (current.size <= limit) {
            return null
        }

        val legalChoices =
            current.mapIndexed {
                index,
                card ->
                card.toChoice(
                    index
                )
            }

        val chosen =
            player.decisions.effect
                .chooseWispsToKeep(
                    ChooseWispsToKeepRequest(
                        effect = effect,
                        playerId = player.id,
                        keepLimit = limit,
                        legalChoices =
                            legalChoices,
                        context = request.decisionContextFor(player)
                    )
                )

        decisionCheck(
            chosen.selected.size ==
                limit
        ) {
            "Wisp Reckoning player ${player.id.value} must keep exactly " +
                "$limit Wisps; chose ${chosen.selected.size}"
        }

        decisionCheck(
            chosen.selected.all {
                it in legalChoices
            }
        ) {
            "Wisp Reckoning strategy chose Wisp not in current hand: " +
                "${chosen.selected}; legal=$legalChoices"
        }

        decisionCheck(
            chosen.selected
                .map {
                    it.index
                }
                .distinct()
                .size ==
                chosen.selected.size
        ) {
            "Wisp Reckoning strategy chose the same Wisp more than once: " +
                chosen.selected
        }

        val latest =
            currentEligibleCards(
                player = player,
                sourceToExclude =
                    sourceToExclude
            )

        decisionCheck(
            latest.mapIndexed {
                index,
                card ->
                card.toChoice(
                    index
                )
            } == legalChoices
        ) {
            "Wisp hand changed while Wisp Reckoning decision was being made " +
                "for player ${player.id.value}"
        }

        return TrimPlan(
            player = player,
            eligibleCards = latest,
            keepIndexes =
                chosen.selected
                    .map {
                        it.index
                    }
                    .toSet()
        )
    }

    private fun applyTrimPlan(
        plan: TrimPlan
    ) {
        plan.eligibleCards.forEachIndexed {
            index,
            card ->
            if (index !in plan.keepIndexes) {
                stateCheck(
                    plan.player.wisps.remove(
                        card
                    )
                ) {
                    "Wisp Reckoning could not Trash excess Wisp " +
                        "${card.name} for player ${plan.player.id.value}"
                }
            }
        }
    }

    /**
     * Excludes exactly one occurrence of the currently resolving Wisp.
     *
     * Physical copies of a Wisp definition are intentionally interchangeable
     * and WispDeck expands copies using the same immutable card definition, so
     * equality/identity cannot distinguish copies. Removing one occurrence is
     * the correct simulation model.
     */
    private fun currentEligibleCards(
        player: Player,
        sourceToExclude: WispCard?
    ): List<WispCard> {
        val cards =
            player.wisps.cards.cards

        if (sourceToExclude == null) {
            return cards
        }

        var excluded = false
        return cards.filter { card ->
            if (!excluded &&
                card ==
                sourceToExclude
            ) {
                excluded = true
                false
            } else {
                true
            }
        }
    }

    private fun WispCard.toChoice(
        index: Int
    ) =
        EffectWispChoice(
            index = index,
            name = name,
            title = title,
            effect = effect
        )

    private data class TrimPlan(
        val player: Player,
        val eligibleCards: List<WispCard>,
        val keepIndexes: Set<Int>
    )
}
