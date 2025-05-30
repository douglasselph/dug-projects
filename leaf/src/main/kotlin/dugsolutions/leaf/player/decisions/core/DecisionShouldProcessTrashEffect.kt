package dugsolutions.leaf.player.decisions.core

import dugsolutions.leaf.components.GameCard

/**
 * Returns if a card should be trashed in order to activate its trash effect.
 * There are two modes in which this can be called: before the battle phase, during regular card play.
 * Then during battle phase, in which case, trashing for additional resilience might be considered given the amount
 * of incoming damage the player is receiving.
 */
interface DecisionShouldProcessTrashEffect {

    enum class Result {
        DO_NOT_TRASH,
        TRASH,
        TRASH_IF_NEEDED
    }

    operator fun invoke(card: GameCard): Result
    fun reset()

}
