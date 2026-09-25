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

    /** Cyclic seat rotations; every strategy occupies every physical seat once. */
    fun seatRotations(): List<Matchup> =
        players.indices.map { offset ->
            Matchup(
                name = "$name [rotation ${offset + 1}/${players.size}]",
                players = List(players.size) { seat ->
                    players[(seat - offset).floorMod(players.size)]
                }
            )
        }

    private fun Int.floorMod(modulus: Int): Int =
        ((this % modulus) + modulus) % modulus
}
