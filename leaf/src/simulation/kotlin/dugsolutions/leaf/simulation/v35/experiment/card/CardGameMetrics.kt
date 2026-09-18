package dugsolutions.leaf.simulation.v35.experiment.card

import dugsolutions.leaf.simulation.v35.strategy.planned.TargetCard
import dugsolutions.leaf.v35.chronicle.domain.EffectSourceKind
import dugsolutions.leaf.v35.chronicle.domain.GameEntry
import dugsolutions.leaf.v35.chronicle.domain.PurchaseKind
import dugsolutions.leaf.v35.game.Game
import dugsolutions.leaf.v35.game.GameRunResult
import dugsolutions.leaf.v35.plant.domain.PlantScoringRule
import dugsolutions.leaf.v35.player.PlayerId

/** Extracts focused-card metrics from one completed production Game. */
internal object CardGameMetrics {
    fun observe(
        game: Game,
        runResult: GameRunResult,
        focusSeat: Int,
        targetCard: TargetCard
    ): CardFocusGameObservation {
        require(game.isComplete) { "Card experiment can observe only a completed game" }
        require(focusSeat in game.players.indices) { "Focus seat out of range: $focusSeat" }

        val focusPlayer = game.players[focusSeat]
        val focusId = focusPlayer.id
        val scores = runResult.finalScoring.scores
        val focusScore = scores.first { it.playerId == focusId }
        val baselineScores = scores.filterNot { it.playerId == focusId }
        require(baselineScores.isNotEmpty()) { "Focused-card experiment requires baseline opponents" }

        val winnerIds = runResult.finalScoring.winnerIds
        val focusWon = focusId in winnerIds
        val focusWinShare = if (focusWon) 1.0 / winnerIds.size.toDouble() else 0.0
        val baselineTotalWinShare = 1.0 - focusWinShare
        val baselineAverageWinShare = baselineTotalWinShare / baselineScores.size.toDouble()

        val entries = game.chronicle.entries
        val purchases = entries.count { entry ->
            entry is GameEntry.Purchase &&
                entry.playerId == focusId &&
                entry.kind == PurchaseKind.PLANT &&
                entry.itemName == targetCard.cardName
        }
        val activations = entries.count { entry ->
            entry is GameEntry.EffectResolved &&
                entry.playerId == focusId &&
                entry.sourceKind == EffectSourceKind.PLANT &&
                entry.sourceName == targetCard.cardName
        }
        val strikeVp = entries
            .filterIsInstance<GameEntry.StrikeResolved>()
            .sumOf { strike -> if (focusId in strike.winnerIds) strike.vpPerWinner else 0 }

        val targetCardsAtEnd = focusPlayer.creature.cards.filter {
            it.card.name == targetCard.cardName
        }
        val targetPlantVp = targetCardsAtEnd.sumOf { creatureCard ->
            when (val rule = creatureCard.card.scoringRule) {
                is PlantScoringRule.Fixed -> rule.points
                PlantScoringRule.PerGraftedVine -> focusPlayer.creature.vines.size
                PlantScoringRule.PerButterfly -> focusPlayer.butterflies.size
            }
        }

        return CardFocusGameObservation(
            focusSeat = focusSeat,
            focusWon = focusWon,
            focusWinShare = focusWinShare,
            baselineAverageWinShare = baselineAverageWinShare,
            focusVp = focusScore.totalVp,
            baselineAverageVp = baselineScores.map { it.totalVp }.average(),
            targetPurchasedCopies = purchases,
            targetGraftedCopiesAtEnd = targetCardsAtEnd.size,
            targetActivations = activations,
            targetPlantVp = targetPlantVp,
            battleStrikeVp = strikeVp
        )
    }
}
