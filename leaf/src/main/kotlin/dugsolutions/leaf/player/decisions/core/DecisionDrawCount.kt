package dugsolutions.leaf.player.decisions.core

import dugsolutions.leaf.player.Player

interface DecisionDrawCount {

    data class Result(
        val count: Int
    )

    suspend operator fun invoke(player: Player): Result
}
