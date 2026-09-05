package dugsolutions.leaf.integration.v35.support

import dugsolutions.leaf.v35.chronicle.domain.GameEntry
import dugsolutions.leaf.v35.player.PlayerId

/** Query helpers over the structured Chronicle. */
object ChronicleQueries {

    inline fun <reified T : GameEntry> ofType(entries: List<GameEntry>): List<T> =
        entries.filterIsInstance<T>()

    fun roundReveal(
        entries: List<GameEntry>,
        roundNumber: Int
    ): GameEntry.RoundRevealed? =
        entries.filterIsInstance<GameEntry.RoundRevealed>()
            .singleOrNull { it.roundNumber == roundNumber }

    fun roundCompletion(
        entries: List<GameEntry>,
        roundNumber: Int
    ): GameEntry.RoundCompleted? =
        entries.filterIsInstance<GameEntry.RoundCompleted>()
            .singleOrNull { it.roundNumber == roundNumber }

    /**
     * Returns Chronicle entries from the requested reveal through its matching
     * completion. If the Round has only been revealed, returns entries from the
     * reveal through the current end of the Chronicle.
     */
    fun entriesForRound(
        entries: List<GameEntry>,
        roundNumber: Int
    ): List<GameEntry> {
        val reveal = requireNotNull(roundReveal(entries, roundNumber)) {
            "No RoundRevealed entry for Round $roundNumber"
        }
        val completion = roundCompletion(entries, roundNumber)
        val endSequence = completion?.sequence ?: Long.MAX_VALUE
        return entries.filter { it.sequence in reveal.sequence..endSequence }
    }

    fun dieRollsFor(
        entries: List<GameEntry>,
        playerId: PlayerId
    ): List<GameEntry.DieRolled> =
        entries.filterIsInstance<GameEntry.DieRolled>()
            .filter { it.playerId == playerId }

    fun mainActionsFor(
        entries: List<GameEntry>,
        playerId: PlayerId
    ): List<GameEntry.MainAction> =
        entries.filterIsInstance<GameEntry.MainAction>()
            .filter { it.playerId == playerId }

    fun supportActionsFor(
        entries: List<GameEntry>,
        playerId: PlayerId
    ): List<GameEntry.SupportAction> =
        entries.filterIsInstance<GameEntry.SupportAction>()
            .filter { it.playerId == playerId }

    fun purchasesFor(
        entries: List<GameEntry>,
        playerId: PlayerId
    ): List<GameEntry.Purchase> =
        entries.filterIsInstance<GameEntry.Purchase>()
            .filter { it.playerId == playerId }


    fun rollRewardsFor(
        entries: List<GameEntry>,
        playerId: PlayerId
    ): List<GameEntry.RollReward> =
        entries.filterIsInstance<GameEntry.RollReward>()
            .filter { it.playerId == playerId }

    fun effectsFor(
        entries: List<GameEntry>,
        playerId: PlayerId
    ): List<GameEntry.EffectResolved> =
        entries.filterIsInstance<GameEntry.EffectResolved>()
            .filter { it.playerId == playerId }

    fun cleanupsFor(
        entries: List<GameEntry>,
        playerId: PlayerId
    ): List<GameEntry.Cleanup> =
        entries.filterIsInstance<GameEntry.Cleanup>()
            .filter { it.playerId == playerId }

    fun refreshesFor(
        entries: List<GameEntry>,
        playerId: PlayerId
    ): List<GameEntry.Refresh> =
        entries.filterIsInstance<GameEntry.Refresh>()
            .filter { it.playerId == playerId }

    fun buyOrders(entries: List<GameEntry>): List<GameEntry.BuyOrder> =
        entries.filterIsInstance<GameEntry.BuyOrder>()

    fun battleOrders(entries: List<GameEntry>): List<GameEntry.BattleOrder> =
        entries.filterIsInstance<GameEntry.BattleOrder>()

    fun strikes(entries: List<GameEntry>): List<GameEntry.StrikeResolved> =
        entries.filterIsInstance<GameEntry.StrikeResolved>()

    fun woundsFor(
        entries: List<GameEntry>,
        playerId: PlayerId
    ): List<GameEntry.Wound> =
        entries.filterIsInstance<GameEntry.Wound>()
            .filter { it.playerId == playerId }

    fun doom(entries: List<GameEntry>): List<GameEntry.Doom> =
        entries.filterIsInstance<GameEntry.Doom>()

    fun finalScoreFor(
        entries: List<GameEntry>,
        playerId: PlayerId
    ): GameEntry.FinalScore? =
        entries.filterIsInstance<GameEntry.FinalScore>()
            .singleOrNull { it.playerId == playerId }
}
