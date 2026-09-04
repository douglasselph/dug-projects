package dugsolutions.leaf.v35.wisp.domain

import dugsolutions.leaf.v35.effect.GameEffect

data class WispCard(
    val quantity: Int,
    val name: String,
    val title: String,
    val count: Int,
    val effect: GameEffect,
    val lineIcons: String?,
    val lineIconsHeight: Int,
    val vpIcon: String?,
    val mainBackdrop: String,
    val playImmediately: Boolean = false,
    val battleOnly: Boolean = false,
    val endGameVp: Int = 0
) {
    init {
        require(endGameVp >= 0) {
            "Wisp end-game VP must be non-negative: $endGameVp"
        }
    }
}
