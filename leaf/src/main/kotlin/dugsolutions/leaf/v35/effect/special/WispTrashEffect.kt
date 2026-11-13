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
import dugsolutions.leaf.v35.player.decision.effect.ChooseEffectPlayerRequest
import dugsolutions.leaf.v35.player.decision.effect.ChooseWispsToKeepRequest
import dugsolutions.leaf.v35.player.decision.effect.EffectWispChoice
import dugsolutions.leaf.v35.wisp.domain.WispCard

/**
 * November 2026 Wisp of Honor and Berry Patient rules.
 *
 * Wisp of Honor: choose a player (including self) who has another Wisp;
 * the selected player chooses one of their eligible Wisps to Trash.
 * Berry Patient: each player with an eligible Wisp chooses one to Trash.
 *
 * The currently playing Wisp is removed by the Support/Cultivation coordinator
 * AFTER this handler returns. Exclude exactly one matching source copy from
 * its actor's eligible choices. All victims choose against one pre-trash
 * snapshot, and all decisions are validated before removing any cards.
 *
 * Self-targeting, excluding the played card and simultaneous selections are
 * the WISP-1..3 proposed interpretations pending explicit designer approval.
 */
class WispTrashEffect : EffectHandler {

    override fun canExecute(request: GameEffectRequest): Boolean {
        if (request.source !is GameEffectSource.Wisp) return false
        return when (request.effect) {
            GameEffect.CHOOSE_PLAYER_WITH_WISPS_TRASH_ONE -> eligiblePlayers(request).isNotEmpty()
            GameEffect.EACH_PLAYER_WITH_WISPS_TRASH_ONE -> true
            else -> false
        }
    }

    override fun execute(request: GameEffectRequest, executor: GameEffectExecutor) {
        effectCheck(canExecute(request)) { "Revised Wisp Trash effect cannot execute: ${request.effect}" }
        val victims = when (request.effect) {
            GameEffect.CHOOSE_PLAYER_WITH_WISPS_TRASH_ONE -> {
                val eligible = eligiblePlayers(request)
                val legalIds = eligible.map { it.player.id }
                val target = request.actor.decisions.effect.choosePlayer(
                    ChooseEffectPlayerRequest(request.effect, legalIds, request.decisionContextFor(request.actor))
                )
                decisionCheck(target in legalIds) {
                    "Wisp of Honor selected player without an eligible Wisp: $target; legal=$legalIds"
                }
                listOf(eligible.first { it.player.id == target })
            }
            GameEffect.EACH_PLAYER_WITH_WISPS_TRASH_ONE -> eligiblePlayers(request)
            else -> error("Unexpected Wisp trash effect ${request.effect}")
        }

        // Resolve every target's choice before changing any hand, so the
        // per-player choices describe one simultaneous pre-effect snapshot.
        val planned = victims.map { victim -> victim.player to chosenVictim(request, victim) }
        planned.forEach { (player, card) ->
            stateCheck(player.wisps.remove(card)) {
                "Could not Trash selected Wisp ${card.name} from player ${player.id.value}"
            }
        }
    }

    private fun chosenVictim(request: GameEffectRequest, victim: EligiblePlayer): WispCard {
        val choices = victim.cards.mapIndexed { index, card ->
            EffectWispChoice(index, card.name, card.title, card.effect)
        }
        val selected = victim.player.decisions.effect.chooseWispsToKeep(
            ChooseWispsToKeepRequest(
                effect = request.effect,
                playerId = victim.player.id,
                keepLimit = choices.size - 1,
                legalChoices = choices,
                context = request.decisionContextFor(victim.player)
            )
        ).selected
        val keptIndices = selected.map { it.index }
        decisionCheck(
            selected.size == choices.size - 1 &&
                selected.all { it in choices } &&
                keptIndices.distinct().size == keptIndices.size
        ) {
            "Wisp trash choice must keep exactly ${choices.size - 1} distinct eligible Wisps: $selected"
        }
        val currentCards = eligibleCards(request, victim.player)
        decisionCheck(currentCards == victim.cards) {
            "Wisp hand changed during target selection for player ${victim.player.id.value}"
        }
        return victim.cards[choices.indices.single { it !in keptIndices }]
    }

    private fun eligiblePlayers(request: GameEffectRequest): List<EligiblePlayer> =
        request.game.players.mapNotNull { player ->
            val cards = eligibleCards(request, player)
            if (cards.isEmpty()) null else EligiblePlayer(player, cards)
        }

    private fun eligibleCards(request: GameEffectRequest, player: Player): List<WispCard> {
        val cards = player.wisps.cards.cards
        if (player !== request.actor) return cards
        val source = (request.source as GameEffectSource.Wisp).card
        var excluded = false
        return cards.filter { card ->
            if (!excluded && card == source) {
                excluded = true
                false
            } else true
        }
    }

    private data class EligiblePlayer(val player: Player, val cards: List<WispCard>)
}
