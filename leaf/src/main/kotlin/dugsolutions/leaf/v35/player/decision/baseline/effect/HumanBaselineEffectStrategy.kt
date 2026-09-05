package dugsolutions.leaf.v35.player.decision.baseline.effect

import dugsolutions.leaf.v35.player.decision.baseline.scoring.BaselineScoreEngine
import dugsolutions.leaf.v35.player.decision.effect.EffectStrategy
import dugsolutions.leaf.v35.player.decision.mechanical.effect.MechanicalEffectStrategy

/**
 * Human Baseline effect policy shell.
 *
 * The Human Baseline scoring/context implementation will replace this
 * delegate incrementally. Keeping a distinct type now cleanly separates the
 * simulation baseline from Mechanical Control without changing behavior yet.
 */
class HumanBaselineEffectStrategy(
    private val delegate: EffectStrategy = MechanicalEffectStrategy(),
    internal val scoreEngine: BaselineScoreEngine = BaselineScoreEngine()
) : EffectStrategy by delegate
