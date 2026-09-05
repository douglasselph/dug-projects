package dugsolutions.leaf.simulation.v35.experiment

/** A named collection of matchups for comparative strategy experiments. */
data class Tournament(
    val name: String,
    val matchups: List<Matchup>
) {
    init {
        require(name.isNotBlank()) { "Tournament name cannot be blank" }
        require(matchups.isNotEmpty()) { "Tournament requires at least one matchup" }
    }
}
