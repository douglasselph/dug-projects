package dugsolutions.leaf.main.domain

data class CardInfo(
    val index: Int,
    val name: String,
    val type: String,
    val resilience: Int,
    val cost: List<String>,
    val primary: String?,
    val match: String?,
    val trash: String?,
    val thorn: Int,
    val highlight: HighlightInfo
)
