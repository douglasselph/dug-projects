package dugsolutions.leaf.v35.effect.special

import dugsolutions.leaf.v35.battle.BattlePlacementResolver
import dugsolutions.leaf.v35.chronicle.domain.Moment
import dugsolutions.leaf.v35.error.effectCheck
import dugsolutions.leaf.v35.error.decisionCheck
import dugsolutions.leaf.v35.error.stateCheck
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.GameEffectExecutor
import dugsolutions.leaf.v35.effect.GameEffectPhase
import dugsolutions.leaf.v35.effect.GameEffectRequest
import dugsolutions.leaf.v35.effect.GameEffectSource
import dugsolutions.leaf.v35.effect.handler.EffectHandler
import dugsolutions.leaf.v35.effect.handler.battleHandChoices
import dugsolutions.leaf.v35.effect.handler.battleStateForEffect
import dugsolutions.leaf.v35.effect.handler.decisionContext
import dugsolutions.leaf.v35.effect.handler.decisionContextFor
import dugsolutions.leaf.v35.effect.handler.handChoices
import dugsolutions.leaf.v35.effect.handler.resolveHandDie
import dugsolutions.leaf.v35.game.operation.RollResolver
import dugsolutions.leaf.v35.game.operation.TrashResolver
import dugsolutions.leaf.v35.player.Player
import dugsolutions.leaf.v35.player.decision.battle.BattleDiePlacementReason
import dugsolutions.leaf.v35.player.decision.effect.ChooseEffectDiceRequest
import dugsolutions.leaf.v35.player.decision.effect.ChoosePetalD4SourceRequest
import dugsolutions.leaf.v35.player.decision.effect.ChoosePetalToDie4Request
import dugsolutions.leaf.v35.player.decision.effect.EffectDieChoice
import dugsolutions.leaf.v35.player.decision.effect.EffectOwnedDieZone
import dugsolutions.leaf.v35.player.decision.effect.PetalD4SourceChoice
import dugsolutions.leaf.v35.player.decision.effect.PetalToDie4Choice
import dugsolutions.leaf.v35.random.die.Die
import dugsolutions.leaf.v35.random.die.DieSides

/** Implements both the printed and research-redesigned Petal To Die 4 packages. */
class PetalToDie4Effect(
    private val trashResolver: TrashResolver = TrashResolver(),
    private val battlePlacementResolver: BattlePlacementResolver = BattlePlacementResolver()
) : EffectHandler {

    override fun canExecute(request: GameEffectRequest): Boolean =
        when (request.effect) {
            GameEffect.GAIN_D4_SET_TO_4_OR_TRASH_D4_RAISE_ALL_DICE_PLUS_4 ->
                oldLegalChoices(request).isNotEmpty()

            GameEffect.GAIN_OR_STEAL_D4_THEN_DISCARD_D4S_AND_DRAW ->
                redesignedSources(request).isNotEmpty()

            else -> false
        }

    override fun execute(request: GameEffectRequest, executor: GameEffectExecutor) {
        effectCheck(canExecute(request)) {
            "Petal To Die 4 is not currently executable: ${request.effect}"
        }

        when (request.effect) {
            GameEffect.GAIN_D4_SET_TO_4_OR_TRASH_D4_RAISE_ALL_DICE_PLUS_4 ->
                executePrinted(request)

            GameEffect.GAIN_OR_STEAL_D4_THEN_DISCARD_D4S_AND_DRAW ->
                executeRedesigned(request, executor)

            else -> error("Unexpected Petal To Die 4 effect: ${request.effect}")
        }
    }

    private fun executePrinted(request: GameEffectRequest) {
        val legalChoices = oldLegalChoices(request)
        val chosen = request.actor.decisions.effect.choosePetalToDie4(
            ChoosePetalToDie4Request(
                effect = request.effect,
                legalChoices = legalChoices,
                context = request.decisionContext()
            )
        )
        decisionCheck(chosen in legalChoices) {
            "EffectStrategy returned illegal Petal To Die 4 choice: $chosen; legal=$legalChoices"
        }
        when (chosen) {
            PetalToDie4Choice.GainD4 -> gainD4SetToFour(request)
            is PetalToDie4Choice.TrashD4AndRaiseAll -> trashD4RaiseAll(request, chosen)
        }
    }

    /**
     * Research candidate:
     * Gain or Steal 1 D4. Then discard any number of your Hand D4s; Draw that many dice.
     *
     * A gained/stolen D4 goes to Dice Discard, following normal gained-die semantics.
     * A stolen D4 may come only from an opponent's Hand. During Battle, stealing a
     * placed Hand D4 also removes that exact die from its Strike Square.
     */
    private fun executeRedesigned(request: GameEffectRequest, executor: GameEffectExecutor) {
        val sources = redesignedSources(request)
        val source = request.actor.decisions.effect.choosePetalD4Source(
            ChoosePetalD4SourceRequest(
                effect = request.effect,
                legalChoices = sources,
                context = request.decisionContext()
            )
        )
        decisionCheck(source in sources) {
            "EffectStrategy returned illegal Petal To Die 4 D4 source: $source; legal=$sources"
        }
        acquireD4ToDiscard(request, source)

        val d4Choices = when (request.phase) {
            GameEffectPhase.CULTIVATION -> handChoices(request.actor) { it.sides == 4 }
            GameEffectPhase.BATTLE -> battleHandChoices(request).filter { it.sides == 4 }
        }
        val chosen = request.actor.decisions.effect.chooseDice(
            ChooseEffectDiceRequest(
                effect = request.effect,
                legalChoices = d4Choices,
                minChoices = 0,
                maxChoices = d4Choices.size,
                context = request.decisionContext()
            )
        )
        decisionCheck(chosen.selected.all { it in d4Choices }) {
            "EffectStrategy returned illegal Petal To Die 4 D4 discard set: ${chosen.selected}; legal=$d4Choices"
        }

        val liveDice = chosen.selected
            .sortedByDescending { it.index }
            .map { resolveHandDie(request.actor, it) }

        if (request.phase == GameEffectPhase.BATTLE) {
            val battleState = battleStateForEffect(request, "PetalToDie4Redesigned")
            liveDice.forEach { die ->
                stateCheck(battleState.grid.removeDie(die) != null) {
                    "Petal To Die 4 Battle D4 lost its Grid location before discard: $die"
                }
            }
        }
        liveDice.forEach { die ->
            stateCheck(request.actor.dice.removeExactFromHand(die) != null) {
                "Selected Petal To Die 4 D4 could not be removed from Hand: $die"
            }
            request.actor.dice.addToDiscard(die)
        }

        val rollResolver = rollResolver(request, executor)
        repeat(liveDice.size) {
            val resolution = rollResolver.draw(request.actor) ?: return@repeat
            if (request.phase == GameEffectPhase.BATTLE) {
                val battleState = battleStateForEffect(request, "PetalToDie4Redesigned")
                if (battlePlacementResolver.legalRows(battleState, request.actor).isEmpty()) {
                    stateCheck(request.actor.dice.removeExactFromHand(resolution.die) != null) {
                        "Petal To Die 4 drawn Battle die could not be removed from Hand"
                    }
                    request.actor.dice.addToDiscard(resolution.die)
                } else {
                    battlePlacementResolver.placeNewHandDie(
                        battleState = battleState,
                        player = request.actor,
                        die = resolution.die,
                        reason = BattleDiePlacementReason.EFFECT,
                        context = request.decisionContext()
                    )
                }
            }
        }
    }

    private fun redesignedSources(request: GameEffectRequest): List<PetalD4SourceChoice> =
        buildList {
            if (request.game.grove.graftBed.has(DieSides.D4)) add(PetalD4SourceChoice.Grove)
            request.game.players.filter { it !== request.actor }.forEach { opponent ->
                addZoneSources(opponent, EffectOwnedDieZone.HAND, opponent.dice.hand)
            }
        }

    private fun MutableList<PetalD4SourceChoice>.addZoneSources(
        opponent: Player,
        zone: EffectOwnedDieZone,
        dice: List<Die>
    ) {
        dice.forEachIndexed { index, die ->
            if (die.sides == 4) {
                add(
                    PetalD4SourceChoice.Opponent(
                        playerId = opponent.id,
                        zone = zone,
                        die = EffectDieChoice(index, die.sides, die.value)
                    )
                )
            }
        }
    }

    private fun acquireD4ToDiscard(request: GameEffectRequest, source: PetalD4SourceChoice) {
        val die = when (source) {
            PetalD4SourceChoice.Grove -> {
                stateCheck(request.game.grove.graftBed.take(DieSides.D4)) {
                    "Chosen Grove D4 source is no longer available"
                }
                request.game.dieFactory(DieSides.D4).also {
                    request.game.chronicle.record(Moment.DieGained(request.actor.id, DieSides.D4))
                }
            }

            is PetalD4SourceChoice.Opponent -> {
                val opponent = request.game.players.firstOrNull {
                    it.id == source.playerId && it !== request.actor
                }
                stateCheck(opponent != null) {
                    "Chosen Petal To Die 4 opponent is not part of this game: ${source.playerId}"
                }
                stateCheck(source.zone == EffectOwnedDieZone.HAND) {
                    "Petal To Die 4 may steal a D4 only from an opponent Hand: $source"
                }
                val live = opponent.dice.hand.getOrNull(source.die.index)
                stateCheck(
                    live != null && live.sides == 4 && live.value == source.die.value
                ) {
                    "Chosen opponent D4 source is no longer available: $source"
                }
                if (request.phase == GameEffectPhase.BATTLE) {
                    request.battleState?.grid?.removeDie(live)
                }
                val removed = opponent.dice.removeExactFromHand(live)
                stateCheck(removed != null) {
                    "Chosen opponent D4 could not be removed: $source"
                }
                removed
            }
        }
        request.actor.dice.addToDiscard(die)
    }

    private fun oldLegalChoices(request: GameEffectRequest): List<PetalToDie4Choice> =
        buildList {
            val canGainD4 = request.game.grove.graftBed.has(DieSides.D4) &&
                when (request.phase) {
                    GameEffectPhase.CULTIVATION -> true
                    GameEffectPhase.BATTLE -> request.battleState != null &&
                        battlePlacementResolver.availableSlots(request.battleState, request.actor) > 0
                }
            if (canGainD4) add(PetalToDie4Choice.GainD4)
            val trashChoices = when (request.phase) {
                GameEffectPhase.CULTIVATION -> handChoices(request.actor) { it.sides == 4 }
                GameEffectPhase.BATTLE -> battleHandChoices(request).filter { it.sides == 4 }
            }
            trashChoices.forEach { add(PetalToDie4Choice.TrashD4AndRaiseAll(it)) }
        }

    private fun gainD4SetToFour(request: GameEffectRequest) {
        stateCheck(request.game.grove.graftBed.take(DieSides.D4)) {
            "Validated Petal To Die 4 D4 was no longer available"
        }
        val die = request.game.dieFactory(DieSides.D4).adjustTo(4)
        request.actor.dice.addToHand(die)
        if (request.phase == GameEffectPhase.BATTLE) {
            val battleState = battleStateForEffect(request, "PetalToDie4")
            battlePlacementResolver.placeNewHandDie(
                battleState = battleState,
                player = request.actor,
                die = die,
                reason = BattleDiePlacementReason.EFFECT,
                context = request.decisionContext()
            )
        }
    }

    private fun trashD4RaiseAll(request: GameEffectRequest, choice: PetalToDie4Choice.TrashD4AndRaiseAll) {
        val die = resolveHandDie(request.actor, choice.die)
        decisionCheck(die.sides == 4) { "Petal To Die 4 trash target is no longer a D4: $die" }
        if (request.phase == GameEffectPhase.BATTLE) {
            val battleState = battleStateForEffect(request, "PetalToDie4")
            stateCheck(battleState.grid.removeDie(die) != null) {
                "Petal To Die 4 Battle D4 lost its Grid location before Trash: $die"
            }
        }
        trashResolver.trashDieFromHand(request.game, request.actor, die)
        request.actor.dice.hand.forEach { it.adjustBy(4) }
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
