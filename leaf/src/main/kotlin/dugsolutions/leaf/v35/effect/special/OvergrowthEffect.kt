package dugsolutions.leaf.v35.effect.special

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.GameEffectExecutor
import dugsolutions.leaf.v35.effect.GameEffectPhase
import dugsolutions.leaf.v35.effect.GameEffectRequest
import dugsolutions.leaf.v35.effect.GameEffectSource
import dugsolutions.leaf.v35.effect.handler.EffectHandler
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
            request.phase == GameEffectPhase.CULTIVATION &&
            handChoices(request.actor) { die ->
                upgradeResolver.canUpgradeAvailableSteps(
                    game = request.game,
                    die = die,
                    steps = 2
                )
            }.isNotEmpty()

    override fun execute(
        request: GameEffectRequest,
        executor: GameEffectExecutor
    ) {
        check(canExecute(request)) {
            "Overgrowth cannot execute in the current state"
        }

        val legalChoices = handChoices(request.actor) { die ->
            upgradeResolver.canUpgradeAvailableSteps(
                game = request.game,
                die = die,
                steps = 2
            )
        }
        val die = chooseRequiredHandDie(request, legalChoices)
        val from = DieSides.from(die.sides)
        val to = checkNotNull(
            upgradeResolver.availableStep(
                game = request.game,
                sides = from,
                step = 2
            )
        ) {
            "Validated Overgrowth target lost its second available step: $from"
        }

        val upgraded = upgradeResolver.upgradeFromHandToHand(
            game = request.game,
            player = request.actor,
            die = die,
            to = to
        )

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
                        phase = request.phase
                    )
                )
            }
        ).roll(request.actor, upgraded.replacement)
    }
}
