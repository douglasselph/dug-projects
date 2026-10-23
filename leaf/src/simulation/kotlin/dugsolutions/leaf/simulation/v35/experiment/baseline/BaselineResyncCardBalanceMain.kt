package dugsolutions.leaf.simulation.v35.experiment.baseline

import dugsolutions.leaf.simulation.v35.experiment.plant.PlantExperimentResearchConfig
import dugsolutions.leaf.simulation.v35.experiment.plant.resolveResearchGroveForSample
import dugsolutions.leaf.simulation.v35.experiment.round.RoundExperimentResearchConfig
import dugsolutions.leaf.v35.chronicle.domain.EffectSourceKind
import dugsolutions.leaf.v35.chronicle.domain.GameEntry
import dugsolutions.leaf.v35.chronicle.domain.PurchaseKind
import dugsolutions.leaf.v35.common.FirstGameDefault
import dugsolutions.leaf.v35.di.appModules
import dugsolutions.leaf.v35.game.GameConfig
import dugsolutions.leaf.v35.game.GameRoundSetup
import dugsolutions.leaf.v35.game.GameRunner
import dugsolutions.leaf.v35.game.di.GameFactory
import dugsolutions.leaf.v35.plant.PlantCardManager
import dugsolutions.leaf.v35.plant.PlantCardRegistry
import dugsolutions.leaf.v35.round.RoundCardManager
import dugsolutions.leaf.v35.round.RoundCardRegistry
import dugsolutions.leaf.v35.wisp.WispCardManager
import dugsolutions.leaf.v35.wisp.WispCardRegistry
import dugsolutions.leaf.v35.game.PlayerDecisionFactory
import org.koin.dsl.koinApplication
import java.io.File
import java.nio.file.Paths

/** Baseline-only, random effective resync Grove, one stable TSV row per observed game/card.
 * No learned policy or mechanical RNG is used by the research layer other than the
 * dedicated Grove sampling seed. Game and decision strategy streams remain separate.
 */
fun main(args: Array<String>) {
    val options = args.associate { arg ->
        require(arg.startsWith("--") && '=' in arg) { "Expected --key=value: $arg" }
        val parts = arg.removePrefix("--").split('=', limit = 2)
        parts[0] to parts[1]
    }
    fun string(key: String, default: String): String = options[key] ?: default
    val start = string("start", "0").toInt()
    val games = string("games", "100").toInt()
    val players = string("players", "4").toInt()
    require(start >= 0 && games > 0 && players in 2..4)
    val baseSeed = string("seed", "120000").toLong()
    val strategySeed = string("strategy-seed", "220000").toLong()
    val groveSeed = string("grove-seed", "320000").toLong()
    val plantFile = Paths.get(string("plant-overrides", "data/research/resync/resync-current.csv"))
    val roundFile = Paths.get(string("round-overrides", "data/research/resync/round-resync-current.csv"))
    val output = File(string("output", "build/reports/baseline-resync-cards.tsv"))
    output.parentFile?.mkdirs()
    val app = koinApplication { modules(appModules) }
    try {
        val k = app.koin
        loadCatalogs(k.get<PlantCardRegistry>(), k.get<PlantCardManager>(),
            k.get<WispCardRegistry>(), k.get<WispCardManager>(),
            k.get<RoundCardRegistry>(), k.get<RoundCardManager>())
        val plantManager = k.get<PlantCardManager>()
        val roundManager = k.get<RoundCardManager>()
        val allPlants = plantManager.getAllCards().cards
        val researchPlant = PlantExperimentResearchConfig.resolve(plantFile, allPlants)
        val researchRound = RoundExperimentResearchConfig.resolve(roundFile, roundManager.getAllCards().cards)
        val defaults = FirstGameDefault.PLANT_NAMES.map { requireNotNull(plantManager.getCard(it)) }
        val factory = k.get<GameFactory>()
        val runner = k.get<GameRunner>()
        val header = "sample\tplayers\tcard\tgrove_exposed\tlegal_buy_options\taffordable_options\tselected_buy_options\tpurchases\tactivations\tfinal_owned\twinner_share_of_owners\towner_vp_total\tall_player_vp_total"
        output.printWriter().use { writer ->
            writer.println(header)
            repeat(games) { index ->
                val sample = start + index
                val grove = resolveResearchGroveForSample(
                    grovePattern = "000000000", groveSeed = groveSeed, sample = sample,
                    plantManager = plantManager, defaultGrove = defaults,
                    allPlants = allPlants, plantValues = researchPlant.values
                )
                val game = factory(GameConfig(
                    selectedPlantCards = grove,
                    playerDecisionFactories = List(players) { PlayerDecisionFactory.humanBaseline() },
                    roundSetup = GameRoundSetup.standard(),
                    seed = baseSeed + sample,
                    strategySeed = strategySeed + sample,
                    recordDecisionReasoning = false,
                    plantValues = researchPlant.values,
                    roundValues = researchRound.values
                ))
                val result = runner.run(game)
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
                if ((index + 1) % 25 == 0) { writer.flush(); println("Baseline resync completed ${index + 1}/$games games; sample=$sample") }
            }
        }
        println("Wrote $games games / ${allPlants.size} Plant identities to ${output.absolutePath}")
    } finally { app.close() }
}
