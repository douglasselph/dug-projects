package dugsolutions.leaf.v35.game.round

/** Simulation source-set extension point for selectable Battle systems. */
interface BattleStructureProvider {
    fun create(canonical: RoundExecutor): RoundExecutor
}
