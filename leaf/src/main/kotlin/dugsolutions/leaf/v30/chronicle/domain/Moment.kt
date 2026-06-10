package dugsolutions.leaf.v30.chronicle.domain

import dugsolutions.leaf.v30.cards.domain.GameCard
import dugsolutions.leaf.v30.player.Player

sealed class Moment {
    data class Warning(
        val player: Player,
        val type: WarningType,
        val card: GameCard
    ) : Moment()
}
