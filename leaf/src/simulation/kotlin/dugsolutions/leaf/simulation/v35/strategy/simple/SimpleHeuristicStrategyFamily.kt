package dugsolutions.leaf.simulation.v35.strategy.simple

import dugsolutions.leaf.simulation.v35.strategy.StrategyLevel

/**
 * @deprecated Human Baseline is now the explicit Level-1 layer. This marker is
 * retained temporarily so older experiment code still compiles.
 */
@Deprecated("Use the Human Baseline core strategy layer")
object SimpleHeuristicStrategyFamily {
    val level: StrategyLevel = StrategyLevel.HUMAN_BASELINE
}
