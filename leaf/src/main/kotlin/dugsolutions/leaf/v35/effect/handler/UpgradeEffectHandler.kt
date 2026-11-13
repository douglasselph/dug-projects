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
import dugsolutions.leaf.v35.game.operation.RollRewardPolicy
import dugsolutions.leaf.v35.game.operation.RollResolver
import dugsolutions.leaf.v35.game.operation.UpgradeResolver
import dugsolutions.leaf.v35.game.operation.TrashResolver
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
    private val upgradeResolver: UpgradeResolver = UpgradeResolver(),
    private val trashResolver: TrashResolver = TrashResolver()
) : EffectHandler {

    override fun canExecute(
        request: GameEffectRequest
    ): Boolean =
        when (request.effect) {
            // v38 Compost may Trash its chosen die if its next normal size is
            // unavailable (including D20, which has no larger size).
            GameEffect.UPGRADE_DIE_FROM_HAND ->
                request.actor.dice.hand.isNotEmpty()

            GameEffect.UPGRADE_DIE_AND_USE_NOW,
            GameEffect.UPGRADE_DIE_AND_USE_NOW_NO_REWARDS ->
                rootAwakeningChoices(request).isNotEmpty()

            GameEffect.UPGRADE_DIE_CONDITIONAL_TWO_STEPS_AND_USE_NOW ->
                conditionalAwakeningChoices(request).isNotEmpty()

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
                // The player chooses from *all* Hand dice: an available next
                // size upgrades as before, or an unavailable one may be Trashed
                // instead. Choosing that target is the decision to use Trash.
                val die = chooseRequiredHandDie(
                    request = request,
                    legalChoices = handChoices(request.actor)
                )

                if (upgradeResolver.canUpgradeNormalStep(request.game, die)) {
                    upgradeResolver.upgradeFromHandToDiscard(
                        game = request.game,
                        player = request.actor,
                        die = die
                    )
                } else {
                    trashResolver.trashDieFromHand(
                        game = request.game,
                        player = request.actor,
                        die = die,
                        reason = "COMPOST"
                    )
                }
            }

            GameEffect.UPGRADE_DIE_AND_USE_NOW,
            GameEffect.UPGRADE_DIE_AND_USE_NOW_NO_REWARDS,
            GameEffect.UPGRADE_DIE_CONDITIONAL_TWO_STEPS_AND_USE_NOW -> {
                val conditional = request.effect == GameEffect.UPGRADE_DIE_CONDITIONAL_TWO_STEPS_AND_USE_NOW
                val die = chooseRequiredHandDie(
                    request = request,
                    legalChoices = if (conditional) conditionalAwakeningChoices(request)
                        else rootAwakeningChoices(request)
                )
                val from = DieSides.from(die.sides)
                val steps = if (conditional && die.value == 3) 2 else 1
                val to = effectNotNull(upgradeResolver.normalStep(from, steps)) {
                    "Validated Upgrade source had no $steps-step destination: $from"
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
                val rolled = rollResolver(request, executor).roll(
                    request.actor,
                    upgraded.replacement,
                    if (request.effect == GameEffect.UPGRADE_DIE_AND_USE_NOW_NO_REWARDS)
                        RollRewardPolicy.IGNORE else RollRewardPolicy.NORMAL
                )
                if (request.source is GameEffectSource.Round &&
                    request.effect == GameEffect.UPGRADE_DIE_AND_USE_NOW) {
                    request.game.assetProvenance.recordRoundCompostUseNow(
                        player = request.actor,
                        source = request.source,
                        phase = request.phase,
                        sourceDieSides = from.value,
                        sourceDieValue = die.value,
                        replacement = rolled.die
                    )
                    battleState?.grid?.placementOf(rolled.die)?.let { placement ->
                        request.game.assetProvenance.markBattlePlacement(
                            request.actor.id, rolled.die, placement.row
                        )
                    }
                }
            }

            else -> unsupportedGameEffect(
                "Unsupported effect reached UpgradeEffectHandler: ${request.effect}"
            )
        }
    }

    /**
     * NC-05: the die SHOWING 3 follows exactly two positions in the physical
     * ladder, irrespective of intermediate Graft Bed stock. Every other face
     * follows one. If the final size is unavailable the source is illegal.
     * These are the UPGRADE-1/2 provisional interpretations in the register.
     */
    private fun conditionalAwakeningChoices(
        request: GameEffectRequest
    ): List<EffectDieChoice> {
        val eligible = if (request.phase == GameEffectPhase.BATTLE)
            battleHandChoices(request) else handChoices(request.actor)
        return eligible.filter { choice ->
            val die = request.actor.dice.hand.getOrNull(choice.index)
                ?: return@filter false
            upgradeResolver.canUpgradeNormalSteps(
                request.game, die, if (die.value == 3) 2 else 1
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
            mechanicalIntervention = request.game.mechanicalIntervention,
            eventRolls = request.game.eventRollRandomness,
            currentRoundNumber = { request.game.roundNumber },
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
