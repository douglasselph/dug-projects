package dugsolutions.leaf.v35.effect.handler

import dugsolutions.leaf.v35.battle.BattlePlacementResolver
import dugsolutions.leaf.v35.chronicle.domain.Moment
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.GameEffectExecutor
import dugsolutions.leaf.v35.effect.GameEffectPhase
import dugsolutions.leaf.v35.effect.GameEffectRequest
import dugsolutions.leaf.v35.effect.GameEffectSource
import dugsolutions.leaf.v35.error.decisionCheck
import dugsolutions.leaf.v35.error.effectCheck
import dugsolutions.leaf.v35.error.stateCheck
import dugsolutions.leaf.v35.game.operation.RollResolver
import dugsolutions.leaf.v35.player.decision.battle.BattleDiePlacementReason
import dugsolutions.leaf.v35.player.decision.effect.ChooseEffectDieSizeRequest
import dugsolutions.leaf.v35.random.die.DieSides

/**
 * NC-21/22: choose an actually available Graft Bed die at or below a printed
 * maximum. Surge puts it into Discard; Swell rolls it into the Battle Hand and
 * requires a legal open Strike Square. No fallback to an unavailable fixed die.
 */
class BoundedDieGainEffectHandler(
    private val placement: BattlePlacementResolver = BattlePlacementResolver()
) : EffectHandler {

    override fun canExecute(request: GameEffectRequest): Boolean {
        if (request.phase != GameEffectPhase.BATTLE || legalDieSizes(request).isEmpty()) return false
        return request.effect != GameEffect.GAIN_UP_TO_D8_AND_USE_NOW ||
            request.battleState?.let { placement.availableSlots(it, request.actor) > 0 } == true
    }

    override fun execute(request: GameEffectRequest, executor: GameEffectExecutor) {
        effectCheck(canExecute(request)) {
            "No available die or legal Battle placement for ${request.effect}"
        }
        val legal = legalDieSizes(request)
        val chosen = request.effectDecisionStrategy().chooseDieSize(
            ChooseEffectDieSizeRequest(
                effect = request.effect,
                legalChoices = legal,
                context = request.decisionContext()
            )
        )
        decisionCheck(chosen in legal) {
            "Chosen die $chosen exceeds effect limit or is unavailable; legal=$legal"
        }
        stateCheck(request.game.grove.graftBed.take(chosen)) {
            "Selected Graft Bed die disappeared before gain: $chosen"
        }
        val die = request.game.dieFactory(chosen)
        if (request.effect == GameEffect.GAIN_UP_TO_D12_TO_DISCARD) {
            request.actor.dice.addToDiscard(die)
        } else {
            request.actor.dice.addToHand(die)
        }
        request.game.chronicle.record(Moment.DieGained(request.actor.id, chosen))

        if (request.effect == GameEffect.GAIN_UP_TO_D8_AND_USE_NOW) {
            val resolver = RollResolver(
                grove = request.game.grove,
                chronicle = request.game.chronicle,
                mechanicalIntervention = request.game.mechanicalIntervention,
                eventRolls = request.game.eventRollRandomness,
                currentRoundNumber = { request.game.roundNumber },
                immediateWispHandler = { player, wisp ->
                    executor.execute(
                        GameEffectRequest(
                            game = request.game,
                            actor = player,
                            effect = wisp.effect,
                            source = GameEffectSource.Wisp(wisp),
                            phase = request.phase,
                            battleState = request.battleState,
                            plantEffectPath = request.plantEffectPath
                        )
                    )
                },
                decisionContext = { player -> request.decisionContextFor(player) }
            )
            val roll = resolver.roll(request.actor, die)
            val battle = requireNotNull(request.battleState)
            // Unlike ordinary DrawEffectHandler's fallback, "Use it now" must
            // not silently put an unplaceable die into Discard.
            val placed = placement.placeNewHandDie(
                battleState = battle,
                player = request.actor,
                die = die,
                reason = BattleDiePlacementReason.EFFECT,
                context = request.decisionContext()
            )
            request.game.assetProvenance.markBattlePlacement(request.actor.id, die, placed.row)
            request.game.chronicle.record(
                Moment.BattleDieRow(
                    rollSequence = roll.chronicleSequence,
                    playerId = request.actor.id,
                    sides = die.sides,
                    value = die.value,
                    row = placed.row
                )
            )
        }
    }

    private fun legalDieSizes(request: GameEffectRequest): List<DieSides> {
        val maximum = when (request.effect) {
            GameEffect.GAIN_UP_TO_D12_TO_DISCARD -> DieSides.D12.value
            GameEffect.GAIN_UP_TO_D8_AND_USE_NOW -> DieSides.D8.value
            else -> return emptyList()
        }
        return DieSides.entries.filter {
            it.value <= maximum && request.game.grove.graftBed.has(it)
        }
    }
}
