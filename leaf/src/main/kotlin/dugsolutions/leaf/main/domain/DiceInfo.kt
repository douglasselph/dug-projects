package dugsolutions.leaf.main.domain

data class DiceInfo(
    val values: List<DieInfo>
) {

    fun copyForItemSelect(): DiceInfo {
        return copy(values = values.map { it.copy(highlight = HighlightInfo.SELECTABLE) })
    }
}

data class DieInfo(
    val index: Int = -1,
    val value: String,
    val highlight: HighlightInfo = HighlightInfo.NONE
)
