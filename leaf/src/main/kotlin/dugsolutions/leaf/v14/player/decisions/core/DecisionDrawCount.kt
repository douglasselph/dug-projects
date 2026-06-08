package dugsolutions.leaf.v14.player.decisions.core

import dugsolutions.leaf.v14.player.Player

interface DecisionDrawCount {

    data class Result(
        val count: Int
    )

    suspend operator fun invoke(player: Player): Result
}
