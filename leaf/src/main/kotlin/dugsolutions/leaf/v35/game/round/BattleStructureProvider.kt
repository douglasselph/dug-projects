package dugsolutions.leaf.v35.game.round

/** Simulation source-set extension point for the adopted v38 Battle scheduler. */
interface BattleStructureProvider {
    fun create(legacyBattle: RoundExecutor): RoundExecutor
}
