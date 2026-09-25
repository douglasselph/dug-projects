package dugsolutions.leaf.simulation.v35.analysis

import dugsolutions.leaf.v35.chronicle.domain.GameEntry
import dugsolutions.leaf.v35.chronicle.domain.RollRewardKind
import dugsolutions.leaf.v35.chronicle.domain.SupportActionKind
import dugsolutions.leaf.v35.game.Game
import dugsolutions.leaf.v35.game.GameRunResult
import dugsolutions.leaf.v35.player.Player
import dugsolutions.leaf.v35.player.PlayerId

/** Collapses one completed production Game into a compact immutable research record. */
object GameSummaryExtractor {

    fun extract(game: Game, runResult: GameRunResult): GameSummary {
        require(game.isComplete) { "Game summary can be extracted only from a completed game" }
        require(runResult.roundsCompleted >= 0) { "Completed round count cannot be negative" }

        val scoresByPlayer = runResult.finalScoring.scores.associateBy { it.playerId }
        val winnerIds = runResult.finalScoring.winnerIds.toList()
        val winnerShare = if (winnerIds.isEmpty()) 0.0 else 1.0 / winnerIds.size.toDouble()
        val entries = game.chronicle.entries

        val playerSummaries = game.players.mapIndexed { seat, player ->
            val score = requireNotNull(scoresByPlayer[player.id]) {
                "Final scoring is missing player ${player.id}"
            }

            PlayerGameSummary(
                seat = seat,
                playerId = player.id,
                won = player.id in winnerIds,
                winShare = if (player.id in winnerIds) winnerShare else 0.0,
                existingVp = score.existingVp,
                plantVp = score.plantVp,
                unplayedWispVp = score.unplayedWispVp,
                totalVp = score.totalVp,
                battleStrikeVp = battleStrikeVp(entries, player.id),
                woundsTaken = woundsTaken(entries, player.id),
                rollRewardWispsGained = rollRewardWispsGained(entries, player.id),
                wispsPlayed = wispsPlayed(entries, player.id),
                finalWispCount = player.wisps.size,
                finalPlantCount = player.creature.size,
                finalPlantPrintedCost = player.creature.cards.sumOf { it.card.cost },
                finalDiceCount = finalDiceCount(player),
                finalDicePower = finalDicePower(player)
            )
        }

        return GameSummary(
            mechanicalSeed = game.config.seed,
            strategySeed = game.config.strategySeed,
            roundsCompleted = runResult.roundsCompleted,
            winnerIds = winnerIds,
            players = playerSummaries
        )
    }

    private fun battleStrikeVp(entries: List<GameEntry>, playerId: PlayerId): Int =
        entries.filterIsInstance<GameEntry.StrikeResolved>()
            .sumOf { strike -> if (playerId in strike.winnerIds) strike.vpPerWinner else 0 }

    private fun woundsTaken(entries: List<GameEntry>, playerId: PlayerId): Int =
        entries.count { it is GameEntry.Wound && it.playerId == playerId }

    private fun rollRewardWispsGained(entries: List<GameEntry>, playerId: PlayerId): Int =
        entries.count { entry ->
            entry is GameEntry.RollReward &&
                entry.playerId == playerId &&
                entry.kind in WISP_GAIN_REWARD_KINDS
        }

    private fun wispsPlayed(entries: List<GameEntry>, playerId: PlayerId): Int =
        entries.count { entry ->
            entry is GameEntry.SupportAction &&
                entry.playerId == playerId &&
                entry.action == SupportActionKind.WISP
        } + entries.count { entry ->
            entry is GameEntry.RollReward &&
                entry.playerId == playerId &&
                entry.kind == RollRewardKind.WISP_PLAYED_IMMEDIATELY
        }

    private fun finalDiceCount(player: Player): Int =
        player.dice.supply.size +
            player.dice.hand.size +
            player.dice.discard.size +
            player.tokens.mulchTokens.size +
            player.tokens.pendingMulchTokens.size

    private fun finalDicePower(player: Player): Int =
        (player.dice.supply + player.dice.hand + player.dice.discard).sumOf { it.sides } +
            player.tokens.mulchTokens.sumOf { it.sides?.value ?: 0 } +
            player.tokens.pendingMulchTokens.sumOf { it.sides?.value ?: 0 }

    private val WISP_GAIN_REWARD_KINDS = setOf(
        RollRewardKind.WISP_GAINED,
        RollRewardKind.WISP_PLAYED_IMMEDIATELY
    )
}
