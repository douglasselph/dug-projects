package dugsolutions.leaf.v30.game.round

import dugsolutions.leaf.v30.round.domain.RoundCard
import dugsolutions.leaf.v30.table.Table

abstract class RoundBase(
    protected val table: Table,
    val card: RoundCard
) {
    open fun run() {
    }
}
