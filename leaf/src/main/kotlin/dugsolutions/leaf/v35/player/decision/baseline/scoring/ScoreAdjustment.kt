package dugsolutions.leaf.v35.player.decision.baseline.scoring

/** One contextual reason that moves a candidate above or below its base score. */
data class ScoreAdjustment(
    val amount: Int,
    val reason: String
) {
    init {
        require(reason.isNotBlank()) {
            "Score adjustment reason cannot be blank"
        }
    }
}
