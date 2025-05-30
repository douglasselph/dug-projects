package dugsolutions.leaf.player.decisions.core

interface DecisionDrawCount {
    suspend operator fun invoke(): Int
}
