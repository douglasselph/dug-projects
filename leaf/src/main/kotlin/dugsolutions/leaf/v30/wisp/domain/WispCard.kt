package dugsolutions.leaf.v30.wisp.domain

data class WispCard(
    val id: WispCardID,
    val quantity: Int,
    val name: String,
    val title: String,
    val count: Int,
    val effect: String,
    val lineIcons: String?,
    val lineIconsHeight: Int,
    val mainBackdrop: String?
)
