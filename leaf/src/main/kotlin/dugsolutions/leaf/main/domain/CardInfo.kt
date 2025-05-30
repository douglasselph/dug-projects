package dugsolutions.leaf.main.domain

data class CardInfo(
    val name: String,
    val type: String,
    val resilience: Int,
    val primary: String?,
    val match: String?,
    val trash: String?,
    val thorn: Int
)
