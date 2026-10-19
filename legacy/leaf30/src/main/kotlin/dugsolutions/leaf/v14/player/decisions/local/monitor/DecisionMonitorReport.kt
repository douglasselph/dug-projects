package dugsolutions.leaf.v14.player.decisions.local.monitor

import dugsolutions.leaf.v14.chronicle.GameChronicle
import dugsolutions.leaf.v14.chronicle.domain.Moment
import dugsolutions.leaf.v14.common.Commons
import dugsolutions.leaf.v14.player.decisions.core.DecisionAcquireSelect
import dugsolutions.leaf.v14.player.decisions.core.DecisionDamageAbsorption
import dugsolutions.leaf.v14.player.decisions.core.DecisionDrawCount
import dugsolutions.leaf.v14.player.decisions.core.DecisionFlowerSelect

class DecisionMonitorReport(
    private val chronicle: GameChronicle
) {

    companion object {
        private const val KEYWORD = Commons.DECISION_KEYWORD
    }

    operator fun <T> invoke(id: DecisionID?, value: T?) {
        value ?: return
        when (id) {
            is DecisionID.ACQUIRE_SELECT -> {
                if (value is DecisionAcquireSelect.BuyItem) {
                    chronicle(Moment.REPORT("$KEYWORD Acquire $value"))
                }
            }

            is DecisionID.DAMAGE_ABSORPTION -> {
                if (value is DecisionDamageAbsorption.Result) {
                    chronicle(Moment.REPORT("$KEYWORD Absorb Damage $value"))
                }
            }

            is DecisionID.DRAW_COUNT -> {
                if (value is DecisionDrawCount.Result) {
                    chronicle(Moment.REPORT("$KEYWORD Draw Cards ${value.count}"))
                }
            }

            DecisionID.FLOWER_SELECT -> {
                if (value is DecisionFlowerSelect.Result) {
                    chronicle(Moment.REPORT("$KEYWORD Select Flowers $value"))
                }
            }

            is DecisionID.SHOULD_PROCESS_TRASH_EFFECT -> {
                chronicle(Moment.REPORT("$KEYWORD Trash for Effect ${id.card.name}"))
            }

            else -> return
        }

    }
}
