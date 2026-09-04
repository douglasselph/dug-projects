package dugsolutions.leaf.v35.effect.special

import dugsolutions.leaf.v35.battle.BattlePlacementResolver
import dugsolutions.leaf.v35.error.effectCheck
import dugsolutions.leaf.v35.error.decisionCheck
import dugsolutions.leaf.v35.error.stateCheck
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.GameEffectExecutor
import dugsolutions.leaf.v35.effect.GameEffectPhase
import dugsolutions.leaf.v35.effect.GameEffectRequest
import dugsolutions.leaf.v35.effect.handler.EffectHandler
import dugsolutions.leaf.v35.effect.handler.battleHandChoices
import dugsolutions.leaf.v35.effect.handler.battleStateForEffect
import dugsolutions.leaf.v35.effect.handler.handChoices
import dugsolutions.leaf.v35.effect.handler.resolveHandDie
import dugsolutions.leaf.v35.game.operation.TrashResolver
import dugsolutions.leaf.v35.player.decision.effect.ChoosePetalToDie4Request
import dugsolutions.leaf.v35.player.decision.battle.BattleDiePlacementReason
import dugsolutions.leaf.v35.player.decision.effect.PetalToDie4Choice
import dugsolutions.leaf.v35.random.die.DieSides

/**
 * Petal To Die 4:
 *
 * Choose one:
 * - Gain one available D4 into Hand and Set it to 4.
 * - Trash one of your Hand D4s, then Raise all remaining dice +4.
 *
 * During Battle, the Gain branch places the new D4 in a legal Strike Square,
 * while the Trash branch removes the exact D4 from its Grid location before
 * applying the universal D4 Trash rule.
 */
class PetalToDie4Effect(
    private val trashResolver: TrashResolver =
        TrashResolver(),
    private val battlePlacementResolver: BattlePlacementResolver =
        BattlePlacementResolver()
) : EffectHandler {

    override fun canExecute(
        request: GameEffectRequest
    ): Boolean =
        request.effect ==
            GameEffect.GAIN_D4_SET_TO_4_OR_TRASH_D4_RAISE_ALL_DICE_PLUS_4 &&
            legalChoices(request).isNotEmpty()

    override fun execute(
        request: GameEffectRequest,
        executor: GameEffectExecutor
    ) {
        effectCheck(canExecute(request)) {
            "Petal To Die 4 is not currently executable"
        }

        val legalChoices = legalChoices(request)
        val chosen =
            request.actor.decisions.effect.choosePetalToDie4(
                ChoosePetalToDie4Request(
                    effect = request.effect,
                    legalChoices = legalChoices
                )
            )

        decisionCheck(chosen in legalChoices) {
            "EffectStrategy returned illegal Petal To Die 4 choice: " +
                "$chosen; legal=$legalChoices"
        }

        when (chosen) {
            PetalToDie4Choice.GainD4 ->
                gainD4SetToFour(request)

            is PetalToDie4Choice.TrashD4AndRaiseAll ->
                trashD4RaiseAll(
                    request = request,
                    choice = chosen
                )
        }
    }

    private fun legalChoices(
        request: GameEffectRequest
    ): List<PetalToDie4Choice> =
        buildList {
            val canGainD4 =
                request.game.grove.graftBed.has(DieSides.D4) &&
                    when (request.phase) {
                        GameEffectPhase.CULTIVATION -> true
                        GameEffectPhase.BATTLE -> {
                            val battleState = request.battleState
                            battleState != null &&
                                battlePlacementResolver.availableSlots(
                                    battleState = battleState,
                                    player = request.actor
                                ) > 0
                        }
                    }

            if (canGainD4) {
                add(PetalToDie4Choice.GainD4)
            }

            val trashChoices =
                when (request.phase) {
                    GameEffectPhase.CULTIVATION ->
                        handChoices(request.actor) {
                            it.sides == DieSides.D4.value
                        }

                    GameEffectPhase.BATTLE ->
                        battleHandChoices(request).filter {
                            it.sides == DieSides.D4.value
                        }
                }

            trashChoices.forEach { die ->
                add(
                    PetalToDie4Choice.TrashD4AndRaiseAll(
                        die = die
                    )
                )
            }
        }

    private fun gainD4SetToFour(
        request: GameEffectRequest
    ) {
        stateCheck(
            request.game.grove.graftBed.take(
                DieSides.D4
            )
        ) {
            "Validated Petal To Die 4 D4 was no longer available"
        }

        val die =
            request.game.dieFactory(
                DieSides.D4
            ).adjustTo(4)

        request.actor.dice.addToHand(die)

        if (request.phase == GameEffectPhase.BATTLE) {
            val battleState = battleStateForEffect(
                request = request,
                context = "PetalToDie4"
            )
            battlePlacementResolver.placeNewHandDie(
                battleState = battleState,
                player = request.actor,
                die = die,
                reason = BattleDiePlacementReason.EFFECT
            )
        }
    }

    private fun trashD4RaiseAll(
        request: GameEffectRequest,
        choice: PetalToDie4Choice.TrashD4AndRaiseAll
    ) {
        val die =
            resolveHandDie(
                player = request.actor,
                choice = choice.die
            )

        decisionCheck(die.sides == DieSides.D4.value) {
            "Petal To Die 4 trash target is no longer a D4: $die"
        }

        if (request.phase == GameEffectPhase.BATTLE) {
            val battleState = battleStateForEffect(
                request = request,
                context = "PetalToDie4"
            )
            stateCheck(
                battleState.grid.removeDie(die) != null
            ) {
                "Petal To Die 4 Battle D4 lost its Grid location before Trash: $die"
            }
        }

        trashResolver.trashDieFromHand(
            game = request.game,
            player = request.actor,
            die = die
        )

        request.actor.dice.hand.forEach {
            it.adjustBy(4)
        }
    }
}
