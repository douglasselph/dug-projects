package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.player.decision.context.DecisionContext

/**
 * Expected non-Strike option value from a NORMAL draw's roll rewards.
 * RollResolver currently grants a choice of available Critters on 1 and draws
 * a Wisp on 2 in Battle too. Each face has probability 1/sides.
 *
 * A Critter can support a Strike later (or heal); its provisional 2-point
 * option value is NOT an immediate +2 on any row. Do not add tactical Strike
 * transitions before the actual Support choice and placement are known.
 */
internal object BattleDrawRollRewardUtility {
    private const val AVAILABLE_CRITTER_OPTION = 2.0

    fun expected(context: DecisionContext, sides: Int): Double {
        require(sides > 0)
        val critter = if (context.grove.bees > 0 || context.grove.worms > 0) {
            AVAILABLE_CRITTER_OPTION
        } else 0.0
        val wisp = if (BattleResourceUtility.availableWispGain(context) > 0) {
            BattleUtilityPolicy.WISP_OPTION.toDouble()
        } else 0.0
        return (critter + wisp) / sides
    }
}
