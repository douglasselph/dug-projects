package dugsolutions.leaf.v35.game.round

/**
 * Deprecated extension seam retained for binary/source compatibility.
 * v38 Three Engagements now runs directly from src/main and no longer relies
 * on a ServiceLoader provider from the simulation source set.
 */
@Deprecated("Three Engagements is part of the main engine; configure RoundCoordinator directly")
interface BattleStructureProvider {
    fun create(legacyBattle: RoundExecutor): RoundExecutor
}
