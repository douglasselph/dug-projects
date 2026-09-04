package dugsolutions.leaf.v35.effect.special

import dugsolutions.leaf.v35.error.effectCheck
import dugsolutions.leaf.v35.error.stateCheck
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.GameEffectExecutor
import dugsolutions.leaf.v35.effect.GameEffectRequest
import dugsolutions.leaf.v35.effect.GameEffectSource
import dugsolutions.leaf.v35.effect.handler.EffectHandler
import dugsolutions.leaf.v35.effect.handler.chooseOptionalHandDie
import dugsolutions.leaf.v35.effect.handler.handChoices
import dugsolutions.leaf.v35.game.operation.RollResolver

/**
 * Wispquake is complex enough to deserve a named implementation rather than
 * being buried inside a broad family handler.
 *
 * It may preserve one of the actor's dice, rerolls every other player-owned
 * Hand die, resolves Roll Rewards normally, and can recursively trigger another
 * immediate Wisp through the top-level [GameEffectExecutor].
 */
class WispquakeEffect : EffectHandler {

    override fun canExecute(
        request: GameEffectRequest
    ): Boolean =
        request.effect ==
            GameEffect.REROLL_ALL_PLAYERS_DICE_KEEP_ONE_OWN

    override fun execute(
        request: GameEffectRequest,
        executor: GameEffectExecutor
    ) {
        effectCheck(canExecute(request)) {
            "Wispquake handler cannot execute effect: ${request.effect}"
        }

        val keptDie = chooseOptionalHandDie(
            request = request,
            legalChoices = handChoices(request.actor)
        )

        val rollResolver = RollResolver(
            grove = request.game.grove,
            chronicle = request.game.chronicle,
            immediateWispHandler = { player, card ->
                executor.execute(
                    GameEffectRequest(
                        game = request.game,
                        actor = player,
                        effect = card.effect,
                        source = GameEffectSource.Wisp(card),
                        phase = request.phase
                    )
                )
            }
        )

        /*
         * Snapshot membership before the first reroll. Roll Rewards can alter
         * Critters/Wisps and can recursively execute another immediate Wisp,
         * but a reroll itself does not move these dice out of Hand.
         */
        val diceToReroll = request.game.players.flatMap { player ->
            player.dice.hand.map { die -> player to die }
        }

        diceToReroll.forEach { (player, die) ->
            if (!(player === request.actor && die === keptDie)) {
                rollResolver.roll(player, die)
            }
        }
    }
}
