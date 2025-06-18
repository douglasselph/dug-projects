package dugsolutions.leaf.main.domain

/**
 * This is better separated from the main GameState because I was getting a bug
 * where this action request was getting removed during state updates. Since this
 * causes a pause in the processing, that was fatal to the game. So this is now
 * kept completely independent to prevent that from happening.
 */
data class MainActionDomain(
    val actionInstruction: String? = null,
    val actionButton: ActionButton = ActionButton.NONE,
    val booleanInstruction: String? = null,
    val drawCountForPlayerName: String? = null
)
