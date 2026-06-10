package dugsolutions.leaf.v30.round.domain

data class RoundCard(
    val id: RoundCardID,
    val quantity: Int,
    val name: String,
    val title: String,
    val effect1Title: String,
    val effect1Text: String,
    val effect1Bg: String,
    val effect1TextFg: String,
    val effect1Image: String?,
    val effect1Icon: String?,
    val effect2Title: String,
    val effect2Text: String,
    val effect2Bg: String,
    val effect2TextFg: String,
    val effect2Image: String?,
    val effect2Icon: String?,
    val backImage: String?
)
