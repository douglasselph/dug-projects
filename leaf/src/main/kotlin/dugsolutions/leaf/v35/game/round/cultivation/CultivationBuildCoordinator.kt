package dugsolutions.leaf.v35.game.round.cultivation

import dugsolutions.leaf.v35.chronicle.domain.Moment
import dugsolutions.leaf.v35.effect.GameEffectExecutor
import dugsolutions.leaf.v35.effect.GameEffectPhase
import dugsolutions.leaf.v35.effect.GameEffectRequest
import dugsolutions.leaf.v35.effect.GameEffectSource
import dugsolutions.leaf.v35.effect.RoundEffectSlot
import dugsolutions.leaf.v35.game.Game
import dugsolutions.leaf.v35.game.operation.RollResolver
import dugsolutions.leaf.v35.player.Player
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.cultivation.ChooseCultivationMainActionRequest
import dugsolutions.leaf.v35.player.decision.cultivation.CultivationMainAction
import dugsolutions.leaf.v35.round.domain.RoundCard
import dugsolutions.leaf.v35.round.domain.RoundCardType

data class CultivationActionResult(
    val playerId: PlayerId,
    val actionNumber: Int,
    val action: CultivationMainAction
)

data class CultivationBuildResult(
    val openingDrawCounts: Map<PlayerId, Int>,
    val actions: List<CultivationActionResult>
)

/** Executes Cultivation's opening Draw-and-Roll and two-action Build phase. */
class CultivationBuildCoordinator(
    private val rollResolver: RollResolver,
    private val effectExecutor: GameEffectExecutor
) {

    fun execute(
        game: Game,
        roundCard: RoundCard
    ): CultivationBuildResult {
        require(roundCard.type == RoundCardType.CULTIVATION) {
            "Cultivation Build requires a Cultivation Round card: ${roundCard.type}"
        }

        val openingDrawCounts = linkedMapOf<PlayerId, Int>()
        game.players.forEach { player ->
            var count = 0
            repeat(3) {
                if (rollResolver.draw(player) != null) count++
            }
            openingDrawCounts[player.id] = count
            game.chronicle.record(
                Moment.Marker(
                    "CULTIVATION_OPENING_DRAW_COMPLETE player=${player.id.value} count=$count"
                )
            )
        }

        val actions = buildList {
            game.players.forEach { player ->
                for (actionNumber in 1..2) {
                    val legalChoices = legalChoices(player)
                    check(legalChoices.isNotEmpty()) {
                        "Player ${player.id.value} has no legal Cultivation Main Action"
                    }

                    val chosen = player.decisions.cultivation.chooseMainAction(
                        ChooseCultivationMainActionRequest(
                            roundCard = roundCard,
                            actionNumber = actionNumber,
                            legalChoices = legalChoices
                        )
                    )
                    check(chosen in legalChoices) {
                        "CultivationStrategy returned an action that was not offered: $chosen"
                    }

                    executeAction(player, roundCard, chosen)
                    add(CultivationActionResult(player.id, actionNumber, chosen))
                    game.chronicle.record(
                        Moment.Marker(
                            "CULTIVATION_MAIN_ACTION player=${player.id.value} " +
                                "action=$actionNumber type=${actionName(chosen)}"
                        )
                    )
                }
            }
        }

        return CultivationBuildResult(
            openingDrawCounts = openingDrawCounts.toMap(),
            actions = actions
        )
    }

    private fun legalChoices(player: Player): List<CultivationMainAction> =
        buildList {
            if (!player.dice.isSupplyEmpty || !player.dice.isDiscardEmpty) {
                add(CultivationMainAction.Draw)
            }
            player.creature.cards
                .filter { it.isFaceUp }
                .mapTo(this, CultivationMainAction::ActivatePlant)
            add(CultivationMainAction.RoundEffect1)
            add(CultivationMainAction.RoundEffect2)
        }

    private fun executeAction(
        player: Player,
        roundCard: RoundCard,
        action: CultivationMainAction
    ) {
        when (action) {
            CultivationMainAction.Draw ->
                checkNotNull(rollResolver.draw(player)) {
                    "Draw became unavailable for player ${player.id.value}"
                }

            is CultivationMainAction.ActivatePlant -> {
                val current = player.creature.get(action.card.id)
                check(current != null && current.isFaceUp && current == action.card) {
                    "Plant activation target is no longer legal: ${action.card.id}"
                }
                effectExecutor.execute(
                    GameEffectRequest(
                        actor = player,
                        effect = current.card.effect,
                        source = GameEffectSource.Plant(current),
                        phase = GameEffectPhase.CULTIVATION
                    )
                )
                check(player.creature.faceDown(current.id)) {
                    "Activated Plant could not be flipped face down: ${current.id}"
                }
            }

            CultivationMainAction.RoundEffect1 ->
                executeRoundEffect(player, roundCard, RoundEffectSlot.FIRST)

            CultivationMainAction.RoundEffect2 ->
                executeRoundEffect(player, roundCard, RoundEffectSlot.SECOND)
        }
    }

    private fun executeRoundEffect(
        player: Player,
        roundCard: RoundCard,
        slot: RoundEffectSlot
    ) {
        val effect = when (slot) {
            RoundEffectSlot.FIRST -> roundCard.firstEffect.effect
            RoundEffectSlot.SECOND -> roundCard.secondEffect.effect
        }
        effectExecutor.execute(
            GameEffectRequest(
                actor = player,
                effect = effect,
                source = GameEffectSource.Round(roundCard, slot),
                phase = GameEffectPhase.CULTIVATION
            )
        )
    }

    private fun actionName(action: CultivationMainAction): String =
        when (action) {
            CultivationMainAction.Draw -> "DRAW"
            is CultivationMainAction.ActivatePlant -> "ACTIVATE_PLANT"
            CultivationMainAction.RoundEffect1 -> "ROUND_EFFECT_1"
            CultivationMainAction.RoundEffect2 -> "ROUND_EFFECT_2"
        }
}
