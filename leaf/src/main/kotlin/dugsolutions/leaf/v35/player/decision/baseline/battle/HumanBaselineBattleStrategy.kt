package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.player.decision.battle.BattleStrategy
import dugsolutions.leaf.v35.player.decision.mechanical.battle.MechanicalBattleStrategy

/**
 * Human Baseline battle policy shell.
 *
 * The Human Baseline scoring/context implementation will replace this
 * delegate incrementally. Keeping a distinct type now cleanly separates the
 * simulation baseline from Mechanical Control without changing behavior yet.
 */
class HumanBaselineBattleStrategy(
    private val delegate: BattleStrategy = MechanicalBattleStrategy()
) : BattleStrategy by delegate
