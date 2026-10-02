package dugsolutions.leaf.v35.tokens

/**
 * Physical shared-resource categories whose finite Grove supply can matter to research.
 *
 * This deliberately describes component identity rather than gameplay value.  Mulch is
 * one physical token category regardless of whether a player's token currently carries
 * a stored die; Butterflies are counted as one finite category across their four colors.
 */
enum class SharedTokenResource {
    WATER,
    SUNLIGHT,
    MULCH,
    BEE,
    WORM,
    BUTTERFLY
}

/**
 * Optional observation seam for a shared Grove inventory.
 *
 * Player-owned inventories use [NONE], so gameplay behavior is unchanged.  The Grove
 * attaches a game-local observer used only by compact research accounting.
 */
interface SharedTokenSupplyObserver {
    /** One attempt to move a physical component out of the Grove supply. */
    fun onGainAttempt(resource: SharedTokenResource, success: Boolean)

    /** One physical component returned to the Grove supply. */
    fun onReturn(resource: SharedTokenResource)

    companion object {
        val NONE: SharedTokenSupplyObserver = object : SharedTokenSupplyObserver {
            override fun onGainAttempt(resource: SharedTokenResource, success: Boolean) = Unit
            override fun onReturn(resource: SharedTokenResource) = Unit
        }
    }
}
