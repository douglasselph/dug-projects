package dugsolutions.leaf.game.battle.domain

import dugsolutions.leaf.common.domain.Token
import dugsolutions.leaf.random.die.DieValue

data class DieInsects(
    val dieValue: DieValue,
    val insects: List<Token>
)
