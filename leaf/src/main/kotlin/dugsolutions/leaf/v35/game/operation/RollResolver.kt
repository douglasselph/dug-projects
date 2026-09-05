package dugsolutions.leaf.v35.game.operation

import dugsolutions.leaf.v35.error.effectNotNull
import dugsolutions.leaf.v35.error.decisionCheck
import dugsolutions.leaf.v35.error.stateCheck
import dugsolutions.leaf.v35.chronicle.Chronicle
import dugsolutions.leaf.v35.chronicle.domain.ChronicleRollRewardPolicy
import dugsolutions.leaf.v35.chronicle.domain.Moment
import dugsolutions.leaf.v35.chronicle.domain.RollReason
import dugsolutions.leaf.v35.chronicle.domain.RollRewardKind
import dugsolutions.leaf.v35.grove.Grove
import dugsolutions.leaf.v35.player.Player
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.reward.ChooseCritterRequest
import dugsolutions.leaf.v35.random.die.Die
import dugsolutions.leaf.v35.tokens.Critter
import dugsolutions.leaf.v35.wisp.domain.WispCard

/**
 * Controls whether a rule-significant roll resolves its normal Roll Reward.
 */
enum class RollRewardPolicy {
    NORMAL,
    IGNORE
}

/**
 * What actually happened as a result of the roll.
 *
 * This is useful to coordinators/tests without requiring them to rediscover
 * the outcome by inspecting mutable Player/Grove state after the fact.
 */
sealed interface RollRewardResult {

    data object None : RollRewardResult

    data object Ignored : RollRewardResult

    data object CritterUnavailable : RollRewardResult

    data object WispUnavailable : RollRewardResult

    data class CritterGained(
        val critter: Critter
    ) : RollRewardResult

    data class WispGained(
        val card: WispCard
    ) : RollRewardResult

    data class WispPlayedImmediately(
        val card: WispCard
    ) : RollRewardResult
}

data class RollResolution(
    val die: Die,
    val reward: RollRewardResult
)

/**
 * Central v35 rule boundary for rolling dice and immediately resolving
 * Roll Rewards.
 *
 * Normal Draw:
 *   PlayerDice.draw()
 *       -> selects lowest-sided available die
 *       -> refills Supply only when needed
 *       -> rolls it
 *       -> moves it into Hand
 *   RollResolver then immediately resolves the resulting Roll Reward.
 *
 * Other rule-significant rolls/rerolls should use [roll] so they cannot
 * accidentally forget Roll Rewards. Effects that explicitly suppress rewards
 * pass [RollRewardPolicy.IGNORE].
 *
 * RollResolver chooses/transfers rewards, but it does not execute card effects.
 */
class RollResolver(
    private val grove: Grove,
    private val chronicle: Chronicle,
    private val immediateWispHandler: ((Player, WispCard) -> Unit)? = null,
    private val decisionContext: (Player) -> DecisionContext = { DecisionContext.EMPTY }
) {

    /**
     * Performs a normal player Draw and immediately resolves its Roll Reward.
     *
     * Returns null only when PlayerDice cannot draw because both Supply and
     * Discard are empty.
     */
    fun draw(
        player: Player,
        rewardPolicy: RollRewardPolicy = RollRewardPolicy.NORMAL
    ): RollResolution? {
        val die =
            player.dice.draw()
                ?: return null

        return resolveCompletedRoll(
            player = player,
            die = die,
            rewardPolicy = rewardPolicy,
            reason = RollReason.DRAW
        )
    }

    /**
     * Rolls the supplied live die in its current location and immediately
     * resolves its Roll Reward.
     *
     * This method deliberately does not move the die. The caller owns any
     * location rule (Hand, Battle placement, Mulch use, etc.).
     */
    fun roll(
        player: Player,
        die: Die,
        rewardPolicy: RollRewardPolicy =
            RollRewardPolicy.NORMAL
    ): RollResolution {
        die.roll()

        return resolveCompletedRoll(
            player = player,
            die = die,
            rewardPolicy = rewardPolicy,
            reason = RollReason.ROLL
        )
    }

    private fun resolveCompletedRoll(
        player: Player,
        die: Die,
        rewardPolicy: RollRewardPolicy,
        reason: RollReason
    ): RollResolution {
        chronicle.record(
            Moment.DieRolled(
                playerId = player.id,
                sides = die.sides,
                value = die.value,
                rewardPolicy = when (rewardPolicy) {
                    RollRewardPolicy.NORMAL -> ChronicleRollRewardPolicy.NORMAL
                    RollRewardPolicy.IGNORE -> ChronicleRollRewardPolicy.IGNORE
                },
                reason = reason
            )
        )

        val reward =
            if (
                rewardPolicy ==
                RollRewardPolicy.IGNORE
            ) {
                RollRewardResult.Ignored
            } else {
                when (die.value) {
                    1 -> resolveCritterReward(player)
                    2 -> resolveWispReward(player)
                    else -> RollRewardResult.None
                }
            }

        recordReward(
            player = player,
            reward = reward
        )

        return RollResolution(
            die = die,
            reward = reward
        )
    }

    private fun resolveCritterReward(
        player: Player
    ): RollRewardResult {
        val legalChoices =
            listOf(
                Critter.BEE,
                Critter.WORM
            ).filter {
                grove.critters.count(it) > 0
            }

        if (legalChoices.isEmpty()) {
            return RollRewardResult.CritterUnavailable
        }

        val chosen =
            player.decisions.reward.chooseCritter(
                ChooseCritterRequest(
                    legalChoices = legalChoices,
                    ownedCritters =
                        player.critters.all,
                    context = decisionContext(player)
                )
            )

        decisionCheck(chosen in legalChoices) {
            "RewardStrategy returned illegal Critter choice: " +
                "$chosen; legal=$legalChoices"
        }

        stateCheck(
            grove.critters.remove(chosen)
        ) {
            "Chosen Critter was no longer available in Grove: $chosen"
        }

        player.critters.add(chosen)

        return RollRewardResult.CritterGained(
            chosen
        )
    }

    private fun resolveWispReward(
        player: Player
    ): RollRewardResult {
        val card =
            grove.wispDeck.draw()
                ?: return RollRewardResult.WispUnavailable

        if (card.playImmediately) {
            val handler = effectNotNull(immediateWispHandler) {
                "Immediate-play Wisp requires an execution handler: ${card.name}"
            }
            handler(player, card)
            return RollRewardResult.WispPlayedImmediately(card)
        }

        player.wisps.add(card)

        return RollRewardResult.WispGained(card)
    }

    private fun recordReward(
        player: Player,
        reward: RollRewardResult
    ) {
        if (reward == RollRewardResult.None) return

        val moment = when (reward) {
            RollRewardResult.None -> error("Handled above")
            RollRewardResult.Ignored -> Moment.RollReward(
                player.id, RollRewardKind.IGNORED
            )
            RollRewardResult.CritterUnavailable -> Moment.RollReward(
                player.id, RollRewardKind.CRITTER_UNAVAILABLE
            )
            RollRewardResult.WispUnavailable -> Moment.RollReward(
                player.id, RollRewardKind.WISP_UNAVAILABLE
            )
            is RollRewardResult.CritterGained -> Moment.RollReward(
                playerId = player.id,
                kind = RollRewardKind.CRITTER_GAINED,
                critter = reward.critter
            )
            is RollRewardResult.WispGained -> Moment.RollReward(
                playerId = player.id,
                kind = RollRewardKind.WISP_GAINED,
                wispName = reward.card.name
            )
            is RollRewardResult.WispPlayedImmediately -> Moment.RollReward(
                playerId = player.id,
                kind = RollRewardKind.WISP_PLAYED_IMMEDIATELY,
                wispName = reward.card.name
            )
        }
        chronicle.record(moment)
    }
}
