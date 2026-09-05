package dugsolutions.leaf.v35.effect.handler

import dugsolutions.leaf.v35.error.effectCheck
import dugsolutions.leaf.v35.error.stateCheck
import dugsolutions.leaf.v35.error.stateNotNull
import dugsolutions.leaf.v35.error.unsupportedGameEffect
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.GameEffectExecutor
import dugsolutions.leaf.v35.effect.GameEffectPhase
import dugsolutions.leaf.v35.effect.GameEffectRequest
import dugsolutions.leaf.v35.effect.GameEffectSource
import dugsolutions.leaf.v35.game.operation.RollResolver
import dugsolutions.leaf.v35.tokens.Token

/**
 * Effects whose defining behavior can target or exchange another player's
 * Battle die.
 *
 * Root Well owns both of its phase branches here so one GameEffect has one
 * execution owner: Cultivation gains Water; Battle spends 1 Water to reroll
 * either two actor-controlled Battle dice or one opponent-controlled Battle
 * die. Pollen Theft exchanges one
 * actor die with one same-size opponent die without rerolling either.
 */
class CrossPlayerEffectHandler : EffectHandler {

    override fun canExecute(
        request: GameEffectRequest
    ): Boolean =
        when (request.effect) {
            GameEffect.GAIN_WATER_AND_SPEND_1_TO_REROLL_TWO_OWN_OR_ONE_OPPONENT_BATTLE_DIE ->
                when (request.phase) {
                    GameEffectPhase.CULTIVATION ->
                        request.game.grove.tokens.hasWater

                    GameEffectPhase.BATTLE ->
                        request.actor.tokens.waterCount >= 1 &&
                            rootWellBattleChoices(request).isNotEmpty()
                }

            GameEffect.SWAP_OWN_DIE_WITH_OPPONENT_SAME_SIZE ->
                request.phase == GameEffectPhase.BATTLE &&
                    crossPlayerSameSizeSwapChoices(request).isNotEmpty()

            else -> false
        }

    override fun execute(
        request: GameEffectRequest,
        executor: GameEffectExecutor
    ) {
        effectCheck(canExecute(request)) {
            "Cross-player handler cannot execute effect: ${request.effect}"
        }

        when (request.effect) {
            GameEffect.GAIN_WATER_AND_SPEND_1_TO_REROLL_TWO_OWN_OR_ONE_OPPONENT_BATTLE_DIE ->
                when (request.phase) {
                    GameEffectPhase.CULTIVATION -> gainWater(request)
                    GameEffectPhase.BATTLE -> rootWellBattle(request, executor)
                }

            GameEffect.SWAP_OWN_DIE_WITH_OPPONENT_SAME_SIZE ->
                pollenTheft(request)

            else -> unsupportedGameEffect(
                "Unsupported effect reached CrossPlayerEffectHandler: ${request.effect}"
            )
        }
    }

    private fun gainWater(
        request: GameEffectRequest
    ) {
        val token = stateNotNull(
            request.game.grove.tokens.pull(Token.WATER),
            context = "RootWell"
        ) {
            "Validated Root Well could not take Water from Grove"
        }
        request.actor.tokens.add(token)
    }

    private fun rootWellBattle(
        request: GameEffectRequest,
        executor: GameEffectExecutor
    ) {
        val chosen = chooseRootWellBattle(
            request = request,
            legalChoices = rootWellBattleChoices(request)
        )

        stateCheck(
            request.actor.tokens.pull(Token.WATER) != null,
            context = "RootWell"
        ) {
            "Validated Root Well could not spend 1 Water"
        }
        request.game.grove.tokens.add(Token.WATER)

        when (chosen) {
            is dugsolutions.leaf.v35.player.decision.effect.RootWellBattleChoice.OwnDice -> {
                // Resolve both live targets before either reroll occurs. The
                // first reroll can earn an immediate Wisp whose effect changes
                // other Battle dice; resolving both up front keeps the second
                // target stable without exposing mutable dice to the strategy.
                val targets = chosen.dice.map { choice ->
                    resolveBattleDieChoice(request, choice)
                }
                targets.forEach { (owner, _) ->
                    stateCheck(owner.id == request.actor.id, context = "RootWell") {
                        "Root Well own-dice branch targeted another player"
                    }
                }
                targets.forEach { (owner, die) ->
                    rollResolver(request, executor).roll(owner, die)
                }
            }

            is dugsolutions.leaf.v35.player.decision.effect.RootWellBattleChoice.OpponentDie -> {
                val (owner, die) = resolveBattleDieChoice(request, chosen.die)
                stateCheck(owner.id != request.actor.id, context = "RootWell") {
                    "Root Well opponent branch targeted the actor"
                }
                /*
                 * The current controller receives any Roll Reward. RollResolver
                 * does not move the die, so its Strike Square is unchanged.
                 */
                rollResolver(request, executor).roll(owner, die)
            }
        }
    }

    private fun pollenTheft(
        request: GameEffectRequest
    ) {
        val battleState = battleStateForEffect(
            request = request,
            context = "PollenTheft"
        )
        val chosen = chooseRequiredCrossPlayerDieSwap(
            request = request,
            legalChoices = crossPlayerSameSizeSwapChoices(request)
        )
        val (actorOwner, actorDie) = resolveBattleDieChoice(
            request = request,
            choice = chosen.ownDie
        )
        val (opponent, opponentDie) = resolveBattleDieChoice(
            request = request,
            choice = chosen.opponentDie
        )

        stateCheck(actorOwner.id == request.actor.id, context = "PollenTheft") {
            "Pollen Theft actor die is not controlled by the actor"
        }
        stateCheck(opponent.id != request.actor.id, context = "PollenTheft") {
            "Pollen Theft opponent die is controlled by the actor"
        }
        stateCheck(actorDie.sides == opponentDie.sides, context = "PollenTheft") {
            "Pollen Theft dice are no longer the same size"
        }

        /*
         * Swap both Battle location and Hand ownership. This keeps the engine
         * invariant that a die in a player's Strike Square is also the exact
         * live die in that player's Dice Hand. Neither die is rerolled.
         */
        stateCheck(
            actorOwner.dice.swapExactHandDieWith(
                other = opponent.dice,
                ownDie = actorDie,
                otherDie = opponentDie
            ),
            context = "PollenTheft"
        ) {
            "Validated Pollen Theft dice could not exchange Hand ownership"
        }

        battleState.grid.swapDieLocations(
            first = actorDie,
            second = opponentDie
        )
    }

    private fun rollResolver(
        request: GameEffectRequest,
        executor: GameEffectExecutor
    ): RollResolver =
        RollResolver(
            grove = request.game.grove,
            chronicle = request.game.chronicle,
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
