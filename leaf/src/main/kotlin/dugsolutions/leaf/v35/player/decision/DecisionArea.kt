package dugsolutions.leaf.v35.player.decision

/**
 * Stable strategic dimensions composed by [DecisionDirector].
 *
 * Keeping these areas explicit lets simulation code compare one decision
 * policy at a time while holding the rest of the player behavior constant.
 */
enum class DecisionArea {
    CULTIVATION,
    BATTLE,
    BUY,
    GRAFT_PLACEMENT,
    WOUND,
    CRITTER_REWARD,
    BUTTERFLY,
    EFFECT
}
