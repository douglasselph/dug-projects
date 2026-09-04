package dugsolutions.leaf.v35.effect.handler

import dugsolutions.leaf.v35.error.effectNotNull
import dugsolutions.leaf.v35.error.unsupportedGameEffect
import dugsolutions.leaf.v35.error.effectCheck
import dugsolutions.leaf.v35.error.stateCheck
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.GameEffectExecutor
import dugsolutions.leaf.v35.effect.GameEffectPhase
import dugsolutions.leaf.v35.effect.GameEffectRequest
import dugsolutions.leaf.v35.effect.GameEffectSource
import dugsolutions.leaf.v35.game.operation.RollResolver
import dugsolutions.leaf.v35.game.operation.UpgradeResolver
import dugsolutions.leaf.v35.player.decision.effect.EffectDieChoice
import dugsolutions.leaf.v35.random.die.DieSides

/**
 * Upgrade-family effects.
 *
 * The handler interprets the card effect while [UpgradeResolver] remains the
 * reusable owner of the actual Upgrade rules: size ladder, Graft Bed
 * availability, D4 return, replacement creation, and destination.
 */
class UpgradeEffectHandler(
    private val upgradeResolver: UpgradeResolver = UpgradeResolver()
) : EffectHandler {

    override fun canExecute(
        request: GameEffectRequest
    ): Boolean =
        when (request.effect) {
            GameEffect.UPGRADE_DIE_FROM_HAND ->
                upgradeChoices(request).isNotEmpty()

            GameEffect.UPGRADE_DIE_AND_USE_NOW ->
                rootAwakeningChoices(request).isNotEmpty()

            else -> false
        }

    override fun execute(
        request: GameEffectRequest,
        executor: GameEffectExecutor
    ) {
        effectCheck(canExecute(request)) {
            "Upgrade handler cannot execute effect: ${request.effect}"
        }

        when (request.effect) {
            GameEffect.UPGRADE_DIE_FROM_HAND -> {
                val die = chooseRequiredHandDie(
                    request = request,
                    legalChoices = upgradeChoices(request)
                )

                upgradeResolver.upgradeFromHandToDiscard(
                    game = request.game,
                    player = request.actor,
                    die = die
                )
            }

            GameEffect.UPGRADE_DIE_AND_USE_NOW -> {
                val die = chooseRequiredHandDie(
                    request = request,
                    legalChoices = rootAwakeningChoices(request)
                )
                val from = DieSides.from(die.sides)
                val to = effectNotNull(upgradeResolver.nextNormalStep(from)) {
                    "Validated Root Awakening source had no next step: $from"
                }

                val battleState =
                    if (request.phase == GameEffectPhase.BATTLE) {
                        battleStateForEffect(
                            request = request,
                            context = "RootAwakening"
                        )
                    } else {
                        null
                    }

                if (battleState != null) {
                    stateCheck(
                        battleState.grid.locationOf(die)?.playerId ==
                            request.actor.id
                    ) {
                        "Root Awakening Battle target lost its Grid location before Upgrade: $die"
                    }
                }

                val upgraded = upgradeResolver.upgradeFromHandToHand(
                    game = request.game,
                    player = request.actor,
                    die = die,
                    to = to
                )

                /*
                 * During Battle, replacing a die changes the live object but not
                 * its Strike Square. Put the replacement on the Grid before the
                 * roll so any immediate Roll Reward sees coherent Battle state.
                 */
                if (battleState != null) {
                    battleState.grid.replaceDie(
                        oldDie = die,
                        newDie = upgraded.replacement
                    )
                }

                /* "Use the new die now": roll it in Hand and resolve reward. */
                rollResolver(request, executor).roll(
                    request.actor,
                    upgraded.replacement
                )
            }

            else -> unsupportedGameEffect(
                "Unsupported effect reached UpgradeEffectHandler: ${request.effect}"
            )
        }
    }

    private fun rootAwakeningChoices(
        request: GameEffectRequest
    ): List<EffectDieChoice> =
        when (request.phase) {
            GameEffectPhase.CULTIVATION ->
                upgradeChoices(request)

            GameEffectPhase.BATTLE ->
                battleHandChoices(request).filter { choice ->
                    val die =
                        request.actor.dice.hand.getOrNull(
                            choice.index
                        ) ?: return@filter false

                    upgradeResolver.canUpgradeNormalStep(
                        game = request.game,
                        die = die
                    )
                }
        }

    private fun upgradeChoices(
        request: GameEffectRequest
    ): List<EffectDieChoice> =
        handChoices(request.actor) { die ->
            upgradeResolver.canUpgradeNormalStep(
                game = request.game,
                die = die
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
            }
        )
}
