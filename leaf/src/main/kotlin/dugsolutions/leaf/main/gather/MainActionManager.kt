package dugsolutions.leaf.main.gather

import dugsolutions.leaf.main.domain.ActionButton
import dugsolutions.leaf.main.domain.MainActionDomain
import dugsolutions.leaf.player.decisions.local.monitor.DecisionID
import dugsolutions.leaf.player.decisions.local.monitor.DecisionMonitor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MainActionManager(
    private val decisionMonitor: DecisionMonitor
) {

    private val _state = MutableStateFlow(MainActionDomain())

    // region public

    val state: StateFlow<MainActionDomain> = _state.asStateFlow()

    fun initialize() {
        decisionMonitor.subscribe { id -> updateId(id) }
        updateId(null)
    }

    // endregion public

    private fun updateId(id: DecisionID?) {
        _state.update { currentState ->
            currentState.copy(
                actionInstruction = actionInstruction(id),
                actionButton = actionButton(id),
                booleanInstruction = booleanInstruction(id),
                drawCountForPlayerName = drawCountForPlayerName(id)
            )
        }
    }

    private fun actionInstruction(id: DecisionID?): String? {
        return when (id) {
            is DecisionID.ACQUIRE_SELECT -> "Select cards and/or dice from the Grove"
            is DecisionID.DAMAGE_ABSORPTION -> "Select cards and/or dice to absorb ${id.amount} damage."
            is DecisionID.DRAW_COUNT -> "Select the number of cards to draw for ${id.player.name}"
            DecisionID.FLOWER_SELECT -> "Select flower cards to contribute toward played Bloom card."
            else -> null
        }
    }

    private fun actionButton(id: DecisionID?): ActionButton {
        return when (id) {
            is DecisionID.DAMAGE_ABSORPTION -> ActionButton.DONE
            DecisionID.FLOWER_SELECT -> ActionButton.DONE
            DecisionID.START_GAME -> ActionButton.RUN
            DecisionID.NONE -> ActionButton.NEXT
            else -> ActionButton.NONE
        }
    }

    private fun booleanInstruction(id: DecisionID?): String? {
        return when (id) {
            is DecisionID.SHOULD_PROCESS_TRASH_EFFECT -> "Trash card for effect?"
            else -> null
        }
    }

    private fun drawCountForPlayerName(id: DecisionID?): String? {
        return when (id) {
            is DecisionID.DRAW_COUNT -> id.player.name
            else -> null
        }
    }

}
