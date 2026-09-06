package dugsolutions.leaf.v35.player.decision.baseline.context

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.player.decision.baseline.common.DevelopmentTarget
import dugsolutions.leaf.v35.player.decision.baseline.common.ReserveResource
import dugsolutions.leaf.v35.player.decision.baseline.common.ResourceReserveStatus
import dugsolutions.leaf.v35.player.decision.baseline.common.RowNeed

/** Shared derived observations used by multiple Human Baseline decision areas. */
data class BaselineFeatures(
    val development: DevelopmentTarget,
    val openGrowthSlots: Int,
    val reserveStatus: Map<ReserveResource, ResourceReserveStatus>,
    val rowNeed: Map<StrikeRow, RowNeed>
) {
    val plantCount: Int get() = development.currentPlantCount
    val dicePower: Int get() = development.currentDicePower
    val plantDeficit: Int get() = development.plantDeficit
    val dicePowerDeficit: Int get() = development.dicePowerDeficit
}
