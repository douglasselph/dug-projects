package dugsolutions.leaf.v35.player

/** The three zero-cost Battle Support permissions granted by active Plant effects. */
enum class FreeBattleSupport { WORM, BEE, BUTTERFLY }

/**
 * Permissions are player-local and last until the end of the Battle, across
 * all three Engagements. Granting a permission twice does not stack discounts.
 * The corresponding Critter/Butterfly must still be available and is spent
 * normally. This state never affects Cultivation Support actions.
 */
class BattleFreeSupportState {
    private val granted = mutableSetOf<FreeBattleSupport>()

    fun grant(kind: FreeBattleSupport) { granted += kind }
    fun contains(kind: FreeBattleSupport): Boolean = kind in granted
    fun clearBattle() { granted.clear() }
}
