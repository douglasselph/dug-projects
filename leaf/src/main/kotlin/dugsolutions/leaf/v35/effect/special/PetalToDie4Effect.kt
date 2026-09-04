package dugsolutions.leaf.v35.effect.special

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.GameEffectExecutor
import dugsolutions.leaf.v35.effect.GameEffectPhase
import dugsolutions.leaf.v35.effect.GameEffectRequest
import dugsolutions.leaf.v35.effect.handler.EffectHandler
import dugsolutions.leaf.v35.effect.handler.handChoices
import dugsolutions.leaf.v35.effect.handler.resolveHandDie
import dugsolutions.leaf.v35.game.operation.TrashResolver
import dugsolutions.leaf.v35.player.decision.effect.ChoosePetalToDie4Request
import dugsolutions.leaf.v35.player.decision.effect.PetalToDie4Choice
import dugsolutions.leaf.v35.random.die.DieSides

/**
 * Petal To Die 4:
 *
 * Choose one:
 * - Gain one available D4 into Hand and Set it to 4.
 * - Trash one of your Hand D4s, then Raise all remaining dice +4.
 *
 * The Gain branch is Cultivation-only for now because a die newly added to a
 * Battle Hand must also be assigned a Battle Grid location. Until BattleState
 * exists, exposing only half the choice in Battle would distort the decision,
 * so this complete effect remains unavailable there.
 */
class PetalToDie4Effect(
    private val trashResolver: TrashResolver =
        TrashResolver()
) : EffectHandler {

    override fun canExecute(
        request: GameEffectRequest
    ): Boolean =
        request.effect ==
            GameEffect.GAIN_D4_SET_TO_4_OR_TRASH_D4_RAISE_ALL_DICE_PLUS_4 &&
            request.phase == GameEffectPhase.CULTIVATION &&
            legalChoices(request).isNotEmpty()

    override fun execute(
        request: GameEffectRequest,
        executor: GameEffectExecutor
    ) {
        check(canExecute(request)) {
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

        check(chosen in legalChoices) {
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
            if (
                request.phase == GameEffectPhase.CULTIVATION &&
                request.game.grove.graftBed.has(DieSides.D4)
            ) {
                add(PetalToDie4Choice.GainD4)
            }

            handChoices(request.actor) {
                it.sides == DieSides.D4.value
            }.forEach { die ->
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
        check(
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

        check(die.sides == DieSides.D4.value) {
            "Petal To Die 4 trash target is no longer a D4: $die"
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
