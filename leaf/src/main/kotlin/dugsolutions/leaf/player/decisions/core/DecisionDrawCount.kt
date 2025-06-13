package dugsolutions.leaf.player.decisions.core

interface DecisionDrawCount {

    data class Result(
        val count: Int
    )

    suspend operator fun invoke(): Result
}
