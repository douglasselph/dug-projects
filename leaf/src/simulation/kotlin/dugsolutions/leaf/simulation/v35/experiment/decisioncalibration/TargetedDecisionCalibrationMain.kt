package dugsolutions.leaf.simulation.v35.experiment.decisioncalibration

import dugsolutions.leaf.v35.chronicle.domain.GameEntry
import dugsolutions.leaf.v35.common.CardDataFiles
import dugsolutions.leaf.v35.common.FirstGameDefault
import dugsolutions.leaf.v35.di.appModules
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.game.GameConfig
import dugsolutions.leaf.v35.game.GameRoundSetup
import dugsolutions.leaf.v35.game.GameRunner
import dugsolutions.leaf.v35.game.di.GameFactory
import dugsolutions.leaf.v35.plant.PlantCardManager
import dugsolutions.leaf.v35.plant.PlantCardRegistry
import dugsolutions.leaf.v35.game.PlayerDecisionFactory
import dugsolutions.leaf.v35.round.RoundCardManager
import dugsolutions.leaf.v35.round.RoundCardRegistry
import dugsolutions.leaf.v35.wisp.WispCardManager
import dugsolutions.leaf.v35.wisp.WispCardRegistry
import org.koin.dsl.koinApplication

data class CalibrationOpportunity(
    val game: Int,
    val player: Int,
    val target: String,
    val chosen: Boolean,
    val selectedAction: String,
    val targetBaseUtility: Int,
    val targetFinalUtility: Int,
    val utilityComponents: List<String>,
    val drawUtility: Int?,
    val refreshAdjustment: Int,
    val preservationAdjustment: Int,
    val recycleAdjustment: Int,
    val targetOrBranch: String?,
    val bucket: String,
    val rejectedHighValueAlternatives: List<String>,
    val nearFutureDecision: String?,
    val battleContribution: String?,
    val finalVp: Int?
)

private data class Options(val target: String, val games: Int, val seed: Long, val strategySeed: Long) {
    companion object {
        fun parse(args: Array<String>): Options {
            val values = args.associate { arg ->
                require(arg.startsWith("--") && '=' in arg) { "Expected --name=value, got $arg" }
                arg.removePrefix("--").split('=', limit = 2).let { it[0] to it[1] }
            }
            return Options(
                target = values["target"] ?: "mulch",
                games = values["games"]?.toInt() ?: 200,
                seed = values["seed"]?.toLong() ?: 312_000L,
                strategySeed = values["strategy-seed"]?.toLong() ?: 322_000L
            )
        }
    }
}

fun main(args: Array<String>) {
    val options = Options.parse(args)
    val app = koinApplication { modules(appModules) }
    try {
        val koin = app.koin
        loadCatalogs(koin.get(), koin.get(), koin.get(), koin.get(), koin.get(), koin.get())
        val plantManager = koin.get<PlantCardManager>()
        val plants = FirstGameDefault.PLANT_NAMES.map { name -> requireNotNull(plantManager.getCard(name)) }.toMutableList()
        val requestedPlant = plantManager.getCard(options.target)
            ?: if (options.target.equals("forget-me-not", true)) plantManager.getCard("Flower_17_02") else null
        if (requestedPlant != null && plants.none { it.name == requestedPlant.name }) {
            val slot = plants.indexOfFirst { it.type == requestedPlant.type && it.cost == requestedPlant.cost }
            require(slot >= 0) { "Target Plant ${requestedPlant.name} has no matching default Grove slot" }
            plants[slot] = requestedPlant
        }
        val records = mutableListOf<CalibrationOpportunity>()
        repeat(options.games) { sample ->
            val game = koin.get<GameFactory>()(
                GameConfig(
                    selectedPlantCards = plants,
                    playerDecisionFactories = List(4) { PlayerDecisionFactory.humanBaseline() },
                    roundSetup = GameRoundSetup.standard(),
                    seed = options.seed + sample,
                    strategySeed = options.strategySeed + sample,
                    recordDecisionReasoning = true
                )
            )
            koin.get<GameRunner>().run(game)
            records += extract(sample + 1, options.target, game.chronicle.entries, game)
        }
        println(render(options, records))
    } finally { app.close() }
}

private fun extract(gameNumber: Int, target: String, entries: List<GameEntry>, game: dugsolutions.leaf.v35.game.Game): List<CalibrationOpportunity> {
    val reasoning = entries.filterIsInstance<GameEntry.DecisionReasoning>()
    val finalVp = entries.filterIsInstance<GameEntry.FinalScore>().associate { it.playerId.value to it.totalVp }
    return reasoning.mapIndexedNotNull { index, event ->
        val targetAlt = event.alternatives.firstOrNull { matches(target, it.observations) } ?: return@mapIndexedNotNull null
        val selected = event.alternatives.singleOrNull { it.selected }
        val next = reasoning.drop(index + 1).firstOrNull { it.playerId == event.playerId }
        val observations = targetAlt.observations
        val targetOrBranch = listOfNotNull(observations["targetDieSides"]?.let { "D$it" }, observations["targetDieValue"]?.let { "showing $it" }).joinToString(" ").ifBlank { null }
        val provenance = if (targetAlt.selected && (target.equals("Flower_17_02", true) || target.equals("forget-me-not", true))) {
            val sides = observations["targetDieSides"]?.toIntOrNull()
            game.assetProvenance.forgetMeNot.filter { it.playerId == event.playerId && (sides == null || it.dieSides == sides) }.lastOrNull()?.let {
                "reachedBattle=${it.reachedNextBattle}, placed=${it.placedInBattle}, winning=${it.contributedToWinningStrike}, decisive=${it.individuallyWinnerDecisive}, battleVp=${it.associatedBattleVp}"
            }
        } else null
        CalibrationOpportunity(
            game = gameNumber,
            player = event.playerId.value,
            target = target,
            chosen = targetAlt.selected,
            selectedAction = selected?.choiceLabel ?: event.choiceLabel,
            targetBaseUtility = targetAlt.baseScore,
            targetFinalUtility = targetAlt.total,
            utilityComponents = targetAlt.adjustments.map { "${it.amount}:${it.reason}" },
            drawUtility = event.alternatives.firstOrNull { it.choiceLabel.contains("Draw") }?.total ?: observations["drawUtility"]?.toIntOrNull(),
            refreshAdjustment = targetAlt.adjustments.filter { it.reason.contains("Refresh") }.sumOf { it.amount },
            preservationAdjustment = targetAlt.adjustments.filter { it.reason.contains("Preservation") }.sumOf { it.amount },
            recycleAdjustment = targetAlt.adjustments.filter { it.reason.contains("recycle", true) || it.reason.contains("available", true) }.sumOf { it.amount },
            targetOrBranch = targetOrBranch,
            bucket = bucket(target, observations),
            rejectedHighValueAlternatives = event.alternatives.filter { !it.selected && it.total >= targetAlt.total - 10 }.sortedByDescending { it.total }.take(3).map { "${it.choiceLabel}=${it.total}" },
            nearFutureDecision = next?.choiceLabel,
            battleContribution = provenance,
            finalVp = finalVp[event.playerId.value]
        )
    }
}

private fun matches(target: String, observations: Map<String, String>): Boolean = when (target.lowercase()) {
    "mulch" -> observations["effect"] == GameEffect.MULCH_DIE_FROM_HAND.name
    "forget-me-not", "flower_17_02" -> observations["card"] == "Flower_17_02"
    else -> observations["card"].equals(target, true) || observations["effect"].equals(target, true)
}

private fun bucket(target: String, o: Map<String, String>): String = when (target.lowercase()) {
    "mulch" -> "roll=${o["targetDieValue"] ?: "?"}|sides=${o["targetDieSides"] ?: "?"}|battle=${o["battleNext"] ?: "?"}|upcoming=${o["upcomingDiceQuality"] ?: "?"}"
    "forget-me-not", "flower_17_02" -> "discard=D${o["targetDieSides"] ?: "?"}|recycle=${o["recycleDistance"] ?: "?"}|battle=${o["battleNext"] ?: "?"}|upcoming=${o["upcomingDiceQuality"] ?: "?"}"
    else -> "next=${o["nextPhase"] ?: "?"}|up=${o["faceUpPlants"] ?: "?"}|down=${o["faceDownPlants"] ?: "?"}"
}

private fun render(options: Options, records: List<CalibrationOpportunity>): String = buildString {
    appendLine("TARGETED DECISION CALIBRATION")
    appendLine("target=${options.target} games=${options.games} opportunities=${records.size}")
    appendLine("chosen=${records.count { it.chosen }} (${if (records.isEmpty()) "0.0" else "%.1f".format(100.0 * records.count { it.chosen } / records.size)}%)")
    appendLine()
    appendLine("STATE BUCKETS")
    records.groupBy { it.bucket }.toSortedMap().forEach { (bucket, rows) ->
        appendLine("$bucket  n=${rows.size} chosen=${rows.count { it.chosen }} avgUtility=${"%.1f".format(rows.map { it.targetFinalUtility }.average())} avgDraw=${"%.1f".format(rows.mapNotNull { it.drawUtility }.averageOrZero())}")
    }
    appendLine()
    appendLine("QUALIFYING OPPORTUNITIES (first 30)")
    records.take(30).forEach { r ->
        appendLine("g${r.game}/p${r.player} ${r.bucket} chosen=${r.chosen} target=${r.targetBaseUtility}->${r.targetFinalUtility} draw=${r.drawUtility} refresh=${r.refreshAdjustment} preserve=${r.preservationAdjustment} recycle=${r.recycleAdjustment} target=${r.targetOrBranch ?: "-"} selected=${r.selectedAction}")
        if (r.utilityComponents.isNotEmpty()) appendLine("  components: ${r.utilityComponents.joinToString()}")
        if (r.rejectedHighValueAlternatives.isNotEmpty()) appendLine("  close rejected: ${r.rejectedHighValueAlternatives.joinToString()}")
        appendLine("  next=${r.nearFutureDecision ?: "-"} battle=${r.battleContribution ?: "-"} finalVP=${r.finalVp ?: -1}")
    }
}

private fun List<Int>.averageOrZero(): Double = if (isEmpty()) 0.0 else average()

private fun loadCatalogs(
    plantRegistry: PlantCardRegistry, plantManager: PlantCardManager,
    wispRegistry: WispCardRegistry, wispManager: WispCardManager,
    roundRegistry: RoundCardRegistry, roundManager: RoundCardManager
) {
    val root = CardDataFiles.dataDirectory()
    plantRegistry.clear(); plantRegistry.loadFromCsv(CardDataFiles.dataPath(CardDataFiles.ROOT_CARD_LIST, root), CardDataFiles.dataPath(CardDataFiles.VF_CARD_LIST, root)); plantManager.loadCards(plantRegistry)
    wispRegistry.clear(); wispRegistry.loadFromCsv(CardDataFiles.dataPath(CardDataFiles.WISP_LIST, root)); wispManager.loadCards(wispRegistry)
    roundRegistry.clear(); roundRegistry.loadFromCsv(CardDataFiles.dataPath(CardDataFiles.ROUND_CARD_LIST, root)); roundManager.loadCards(roundRegistry)
}
