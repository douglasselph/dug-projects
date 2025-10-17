package dugsolutions.leaf.main.domain

data class CardInfo(
    val index: Int,
    val name: String,
    val type: String,
    val resilience: Int,
    val nutrient: Int,
    val cost: String,
    val primary: String?,
    val match: String?,
    val trash: String?,
    val bloom: String?,
    val thorn: Int,
    val highlight: HighlightInfo,
    val image: String? = null
)
