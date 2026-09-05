package dugsolutions.leaf.simulation.v35.experiment

import dugsolutions.leaf.simulation.v35.strategy.StrategyProfile

/** One 2-4 player strategy matchup to be repeated by a future batch runner. */
data class Matchup(
    val name: String,
    val players: List<StrategyProfile>
) {
    init {
        require(name.isNotBlank()) { "Matchup name cannot be blank" }
        require(players.size in 2..4) {
            "Leaf & Let Die matchups require 2 to 4 players: ${players.size}"
        }
    }
}
