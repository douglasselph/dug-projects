package dugsolutions.leaf.v14.main.domain

import dugsolutions.leaf.v14.random.die.Die

data class DiceInfo(
    val values: List<DieInfo> = emptyList()
) {

    fun copyForItemSelect(): DiceInfo {
        return copy(values = values.map { it.copy(highlight = HighlightInfo.SELECTABLE) })
    }
}

data class DieInfo(
    val index: Int = -1,
    val value: String,
    val highlight: HighlightInfo = HighlightInfo.NONE,
    val backingDie: Die? = null
)
