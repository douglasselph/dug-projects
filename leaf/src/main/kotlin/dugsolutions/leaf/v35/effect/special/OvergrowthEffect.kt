package dugsolutions.leaf.v35.effect.special

import dugsolutions.leaf.v35.error.effectNotNull
import dugsolutions.leaf.v35.error.effectCheck
import dugsolutions.leaf.v35.error.stateCheck
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.GameEffectExecutor
import dugsolutions.leaf.v35.effect.GameEffectPhase
import dugsolutions.leaf.v35.effect.GameEffectRequest
import dugsolutions.leaf.v35.effect.GameEffectSource
import dugsolutions.leaf.v35.effect.handler.EffectHandler
import dugsolutions.leaf.v35.effect.handler.battleHandChoices
import dugsolutions.leaf.v35.effect.handler.battleStateForEffect
import dugsolutions.leaf.v35.effect.handler.chooseRequiredHandDie
import dugsolutions.leaf.v35.effect.handler.handChoices
import dugsolutions.leaf.v35.game.operation.RollResolver
import dugsolutions.leaf.v35.game.operation.UpgradeResolver
import dugsolutions.leaf.v35.random.die.DieSides

/**
 * Overgrowth: Upgrade +2 currently available larger steps, skipping missing
 * sizes, then use the gained die immediately.
 *
 * Battle support waits for Battle placement state because replacing a Grid die
 * must preserve its Strike Square. Cultivation is fully implemented here.
 */
class OvergrowthEffect(
    private val upgradeResolver: UpgradeResolver = UpgradeResolver()
) : EffectHandler {

    override fun canExecute(
        request: GameEffectRequest
    ): Boolean =
        request.effect == GameEffect.UPGRADE_DIE_TWO_STEPS_SKIP_MISSING_AND_USE_NOW &&
            overgrowthChoices(request).isNotEmpty()

    override fun execute(
        request: GameEffectRequest,
        executor: GameEffectExecutor
    ) {
        effectCheck(canExecute(request)) {
            "Overgrowth cannot execute in the current state"
        }

        val legalChoices = overgrowthChoices(request)
        val die = chooseRequiredHandDie(request, legalChoices)
        val from = DieSides.from(die.sides)
        val to = effectNotNull(
            upgradeResolver.availableStep(
                game = request.game,
                sides = from,
                step = 2
            )
        ) {
            "Validated Overgrowth target lost its second available step: $from"
        }

        val battleState =
            if (request.phase == GameEffectPhase.BATTLE) {
                battleStateForEffect(
                    request = request,
                    context = "Overgrowth"
                )
            } else {
                null
            }

        if (battleState != null) {
            stateCheck(
                battleState.grid.locationOf(die)?.playerId ==
                    request.actor.id
            ) {
                "Overgrowth Battle target lost its Grid location before Upgrade: $die"
            }
        }

        val upgraded = upgradeResolver.upgradeFromHandToHand(
            game = request.game,
            player = request.actor,
            die = die,
            to = to
        )

        if (battleState != null) {
            battleState.grid.replaceDie(
                oldDie = die,
                newDie = upgraded.replacement
            )
        }

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
        ).roll(request.actor, upgraded.replacement)
    }

    private fun overgrowthChoices(
        request: GameEffectRequest
    ) =
        when (request.phase) {
            GameEffectPhase.CULTIVATION ->
                handChoices(request.actor) { die ->
                    upgradeResolver.canUpgradeAvailableSteps(
                        game = request.game,
                        die = die,
                        steps = 2
                    )
                }

            GameEffectPhase.BATTLE ->
                battleHandChoices(request).filter { choice ->
                    val die =
                        request.actor.dice.hand.getOrNull(
                            choice.index
                        ) ?: return@filter false

                    upgradeResolver.canUpgradeAvailableSteps(
                        game = request.game,
                        die = die,
                        steps = 2
                    )
                }
        }
}
