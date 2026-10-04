package dugsolutions.leaf.v35.effect.special

import dugsolutions.leaf.v35.battle.BattlePlacementResolver
import dugsolutions.leaf.v35.error.effectCheck
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.GameEffectExecutor
import dugsolutions.leaf.v35.effect.GameEffectPhase
import dugsolutions.leaf.v35.effect.GameEffectRequest
import dugsolutions.leaf.v35.effect.GameEffectSource
import dugsolutions.leaf.v35.effect.handler.EffectHandler
import dugsolutions.leaf.v35.effect.handler.battleHandChoices
import dugsolutions.leaf.v35.effect.handler.battleStateForEffect
import dugsolutions.leaf.v35.effect.handler.chooseRequiredHandDie
import dugsolutions.leaf.v35.effect.handler.chooseRequiredStrikeRow
import dugsolutions.leaf.v35.effect.handler.decisionContext
import dugsolutions.leaf.v35.effect.handler.decisionContextFor
import dugsolutions.leaf.v35.effect.handler.handChoices
import dugsolutions.leaf.v35.effect.handler.openStrikeRows
import dugsolutions.leaf.v35.game.operation.RollResolver
import dugsolutions.leaf.v35.player.decision.battle.BattleDiePlacementReason

/** Research-only Vine and Punishment variants built around rolling a 3. */
class VineAndPunishmentResearchEffect(
    private val battlePlacementResolver: BattlePlacementResolver = BattlePlacementResolver()
) : EffectHandler {

    override fun canExecute(request: GameEffectRequest): Boolean =
        when (request.effect) {
            GameEffect.REROLL_DIE_ON_3_DRAW_ONE_CULTIVATION_OR_REDUCE_OPPOSING_STRIKE_ROW_BY_3 ->
                when (request.phase) {
                    GameEffectPhase.CULTIVATION -> request.actor.dice.hand.isNotEmpty()
                    GameEffectPhase.BATTLE -> openStrikeRows(request).isNotEmpty()
                }

            GameEffect.REROLL_DIE_ON_3_DRAW_ONE_AND_REDUCE_OPPOSING_STRIKE_ROW_BY_3 ->
                when (request.phase) {
                    GameEffectPhase.CULTIVATION -> request.actor.dice.hand.isNotEmpty()
                    GameEffectPhase.BATTLE ->
                        battleHandChoices(request).isNotEmpty() && openStrikeRows(request).isNotEmpty()
                }

            else -> false
        }

    override fun execute(request: GameEffectRequest, executor: GameEffectExecutor) {
        effectCheck(canExecute(request)) {
            "Vine and Punishment research effect cannot execute: ${request.effect}"
        }
        val resolver = rollResolver(request, executor)

        when (request.effect) {
            GameEffect.REROLL_DIE_ON_3_DRAW_ONE_CULTIVATION_OR_REDUCE_OPPOSING_STRIKE_ROW_BY_3 ->
                when (request.phase) {
                    GameEffectPhase.CULTIVATION -> rerollOnThreeDraw(request, resolver)
                    GameEffectPhase.BATTLE -> reduceBattleRow(request)
                }

            GameEffect.REROLL_DIE_ON_3_DRAW_ONE_AND_REDUCE_OPPOSING_STRIKE_ROW_BY_3 -> {
                rerollOnThreeDraw(request, resolver)
                if (request.phase == GameEffectPhase.BATTLE) reduceBattleRow(request)
            }

            else -> error("Unexpected Vine and Punishment research effect: ${request.effect}")
        }
    }

    private fun rerollOnThreeDraw(request: GameEffectRequest, resolver: RollResolver) {
        val choices = if (request.phase == GameEffectPhase.BATTLE) {
            battleHandChoices(request)
        } else {
            handChoices(request.actor)
        }
        val die = chooseRequiredHandDie(request, choices)
        resolver.roll(request.actor, die)
        if (die.value != 3) return

        val drawn = resolver.draw(request.actor) ?: return
        if (request.phase == GameEffectPhase.BATTLE) {
            val battleState = battleStateForEffect(request, "VineAndPunishmentResearch")
            if (battlePlacementResolver.legalRows(battleState, request.actor).isEmpty()) {
                if (request.actor.dice.removeExactFromHand(drawn.die) != null) {
                    request.actor.dice.addToDiscard(drawn.die)
                }
            } else {
                battlePlacementResolver.placeNewHandDie(
                    battleState = battleState,
                    player = request.actor,
                    die = drawn.die,
                    reason = BattleDiePlacementReason.EFFECT,
                    context = request.decisionContext()
                )
            }
        }
    }

    private fun reduceBattleRow(request: GameEffectRequest) {
        val battleState = battleStateForEffect(request, "VineAndPunishmentResearch")
        val row = chooseRequiredStrikeRow(request, openStrikeRows(request))
        battleState.playersInBattleOrder
            .filter { it.id != request.actor.id }
            .forEach { opponent ->
                battleState.grid.square(opponent.id, row).dice.forEach { it.adjustBy(-3) }
            }
    }

    private fun rollResolver(request: GameEffectRequest, executor: GameEffectExecutor): RollResolver =
        RollResolver(
            grove = request.game.grove,
            chronicle = request.game.chronicle,
            mechanicalIntervention = request.game.mechanicalIntervention,
            immediateWispHandler = { player, card ->
                executor.execute(
                    GameEffectRequest(
                        game = request.game,
                        actor = player,
                        effect = card.effect,
                        source = GameEffectSource.Wisp(card),
                        phase = request.phase,
                        battleState = request.battleState,
                        plantEffectPath = request.plantEffectPath
                    )
                )
            },
            decisionContext = { player -> request.decisionContextFor(player) }
        )
}
