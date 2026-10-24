package dugsolutions.leaf.simulation.v35.experiment.baseline

import dugsolutions.leaf.v35.chronicle.domain.EffectSourceKind
import dugsolutions.leaf.v35.chronicle.domain.GameEntry
import dugsolutions.leaf.v35.chronicle.domain.PurchaseKind
import dugsolutions.leaf.v35.game.Game
import dugsolutions.leaf.v35.game.GameRunResult
import dugsolutions.leaf.v35.plant.domain.PlantCard
import java.io.PrintWriter

/** Writes the original per-card TSV rows without retaining completed games. */
class BaselineResyncCardBalanceReporter {
    fun writeGame(writer: PrintWriter, sample: Int, players: Int, grove: List<PlantCard>,
                  game: Game, result: GameRunResult, allPlants: List<PlantCard>) {
        val entries = game.chronicle.entries
        val scores = result.finalScoring.scores.associate { it.playerId to it.totalVp }
        val winners = result.finalScoring.winnerIds.toSet()
        val totalVp = scores.values.sum()
        for (card in allPlants) {
            val name = card.name
            val exposed = if (grove.any { it.name == name }) 1 else 0
            val optionsForCard = entries.filterIsInstance<GameEntry.BuyDecision>()
                .flatMap { decision -> decision.plants.filter { it.cardName == name }
                    .map { plant -> Triple(plant.legal, plant.affordable,
                decision.selectedKind == PurchaseKind.PLANT && decision.selectedItemName == name) } }
            val purchases = entries.filterIsInstance<GameEntry.Purchase>()
                .count { it.kind == PurchaseKind.PLANT && it.itemName == name }
            val activations = entries.filterIsInstance<GameEntry.EffectResolved>()
                .count { it.sourceKind == EffectSourceKind.PLANT && it.sourceName == name }
            val owners = game.players.filter { player -> player.creature.cards.any { it.card.name == name } }
            val winShare = owners.sumOf { player ->
                if (player.id in winners && winners.isNotEmpty()) 1.0 / winners.size else 0.0
            }
            val ownerVp = owners.sumOf { scores[it.id] ?: 0 }
            writer.println(listOf(sample, players, name, exposed,
                optionsForCard.count { it.first },
                optionsForCard.count { it.second },
                optionsForCard.count { it.third },
                purchases, activations, owners.sumOf { p -> p.creature.cards.count { it.card.name == name } },
                winShare, ownerVp, totalVp).joinToString("\t"))
        }
    }
}
