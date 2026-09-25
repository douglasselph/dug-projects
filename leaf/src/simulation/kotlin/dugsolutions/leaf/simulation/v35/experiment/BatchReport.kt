package dugsolutions.leaf.simulation.v35.experiment

import dugsolutions.leaf.simulation.v35.analysis.GameSummary

/** Small generic batch report; concrete experiments may add their own metrics. */
object BatchReport {
    fun render(result: BatchRunResult): String = buildString {
        appendLine("Matchup: ${result.matchupName}")
        appendLine("Games: ${result.gamesCompleted}")
        result.summaries.first().players.indices.forEach { seat ->
            val players = result.summaries.map { it.players[seat] }
            appendLine(
                "Seat ${seat + 1}: win share ${pct(players.map { it.winShare }.average())}, " +
                    "winner rate ${pct(players.count { it.won }.toDouble() / players.size)}, " +
                    "avg VP ${fmt(players.map { it.totalVp.toDouble() }.average())}"
            )
        }
    }.trimEnd()

    /** CSV is intentionally summary-level: one row per player per completed game. */
    fun renderCsv(result: BatchRunResult): String = buildString {
        appendLine("game,mechanical_seed,strategy_seed,seat,player_id,won,win_share,total_vp,battle_strike_vp,wounds_taken,wisps_gained,wisps_played,final_wisps,final_plants,final_plant_cost,final_dice,final_dice_power")
        result.summaries.forEachIndexed { gameIndex, summary ->
            summary.players.forEach { player ->
                appendLine(
                    listOf(
                        gameIndex + 1,
                        summary.mechanicalSeed ?: "",
                        summary.strategySeed ?: "",
                        player.seat + 1,
                        player.playerId,
                        player.won,
                        player.winShare,
                        player.totalVp,
                        player.battleStrikeVp,
                        player.woundsTaken,
                        player.rollRewardWispsGained,
                        player.wispsPlayed,
                        player.finalWispCount,
                        player.finalPlantCount,
                        player.finalPlantPrintedCost,
                        player.finalDiceCount,
                        player.finalDicePower
                    ).joinToString(",")
                )
            }
        }
    }.trimEnd()

    private fun pct(value: Double): String = "%.2f%%".format(value * 100.0)
    private fun fmt(value: Double): String = "%.3f".format(value)
}
