package dugsolutions.leaf.simulation.v35.experiment.decisioncalibration

import dugsolutions.leaf.v35.chronicle.domain.GameEntry
import dugsolutions.leaf.v35.common.CardDataFiles
import dugsolutions.leaf.v35.common.FirstGameDefault
import dugsolutions.leaf.v35.di.appModules
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.baseline.card.CardScoringHelpers
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

private data class TransplantTulipBattleSwapOpportunity(
    val game: Int,
    val player: Int,
    val decision: Int,
    val selected: Boolean,
    val choice: String,
    val sourceDie: String,
    val targetDie: String,
    val sourceRow: String,
    val targetRow: String,
    val raisedDie: String,
    val raiseGain: Int,
    val sourceBefore: String,
    val sourceAfter: String,
    val targetBefore: String,
    val targetAfter: String,
    val winningDelta: Int,
    val woundRiskDelta: Int,
    val battleVpBefore: Int,
    val battleVpAfter: Int,
    val battleVpGain: Int,
    val tacticalUtility: Int,
    val finalUtility: Int
)

private data class OEdelweissDownstreamOpportunity(
    val game: Int,
    val player: Int,
    val decision: Int,
    val choiceNumber: Int,
    val phase: String,
    val selected: Boolean,
    val choice: String,
    val branch: String,
    val targetCard: String?,
    val targetFaceUp: Boolean?,
    val faceUpPlants: Int,
    val faceDownPlants: Int,
    val baseUtility: Int,
    val finalUtility: Int,
    val utilityComponents: List<String>
)

private data class BuySynergyOpportunity(
    val game: Int,
    val player: Int,
    val card: String,
    val plantType: String,
    val cost: Int,
    val selected: Boolean,
    val qualifies: Boolean,
    val baseUtility: Int,
    val synergyAdjustment: Int,
    val finalUtility: Int,
    val sameCostCompetitors: String,
    val nearCostCandidates: String
)


private data class Options(
    val target: String,
    val games: Int,
    val seed: Long,
    val strategySeed: Long,
    val counterfactual: Boolean
) {
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
                strategySeed = values["strategy-seed"]?.toLong() ?: 322_000L,
                counterfactual = values["counterfactual"]?.toBooleanStrictOrNull() ?: false
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
        val resolvedTarget = resolveTarget(options.target)
        val requestedPlant = plantManager.getCard(resolvedTarget)
        if (requestedPlant != null && plants.none { it.name == requestedPlant.name }) {
            val slot = plants.indexOfFirst { it.type == requestedPlant.type && it.cost == requestedPlant.cost }
            require(slot >= 0) { "Target Plant ${requestedPlant.name} has no matching default Grove slot" }
            plants[slot] = requestedPlant
        }
        if (options.counterfactual) {
            println(runCounterfactual(options, plants, koin.get(), koin.get()))
            return
        }
        val records = mutableListOf<CalibrationOpportunity>()
        val buySynergyRecords = mutableListOf<BuySynergyOpportunity>()
        val transplantTulipBattleSwaps = mutableListOf<TransplantTulipBattleSwapOpportunity>()
        val oEdelweissDownstream = mutableListOf<OEdelweissDownstreamOpportunity>()
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
            records += extract(sample + 1, resolvedTarget, game.chronicle.entries, game)
            buySynergyRecords += extractBuySynergy(sample + 1, resolvedTarget, game.chronicle.entries)
            transplantTulipBattleSwaps += extractTransplantTulipBattleSwaps(sample + 1, resolvedTarget, game.chronicle.entries)
            oEdelweissDownstream += extractOEdelweissDownstream(sample + 1, resolvedTarget, game.chronicle.entries)
        }
        println(render(options, records, buySynergyRecords, transplantTulipBattleSwaps, oEdelweissDownstream))
    } finally { app.close() }
}

private fun resolveTarget(target: String): String = when (target.lowercase()) {
    "root-four-more" -> "Root_05_02"
    "root-cause" -> "Root_09_02"
    "forget-me-not" -> "Flower_17_02"
    "queen\'s-blossom", "queens-blossom", "queen-blossom" -> "Flower_17_04"
    "saplink-trellis" -> "Vine_11_02"
    "bloom-backbone" -> "Flower_11_02"
    "bee-loved-bloom" -> "Flower_14_01"
    "petal-to-die", "petal-to-die-4" -> "Flower_14_04"
    "transplant-tulip" -> "Flower_11_04"
    "o-edelweiss" -> "Flower_17_03"
    else -> target
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
        val provenance = if (targetAlt.selected && target.equals("Flower_17_02", true)) {
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

private fun extractBuySynergy(
    gameNumber: Int,
    target: String,
    entries: List<GameEntry>
): List<BuySynergyOpportunity> {
    val isSaplink = target.equals("Vine_11_02", true)
    val isBackbone = target.equals("Flower_11_02", true)
    if (!isSaplink && !isBackbone) return emptyList()

    return entries.filterIsInstance<GameEntry.DecisionReasoning>().flatMap { event ->
        val alternatives = event.alternatives
        if (alternatives.isEmpty() || alternatives.first().observations["decisionFamily"] != "plant-buy-synergy") {
            return@flatMap emptyList()
        }
        val ownedKey = if (isSaplink) "saplinkOwned" else "bloomBackboneOwned"
        if ((alternatives.first().observations[ownedKey]?.toIntOrNull() ?: 0) <= 0) {
            return@flatMap emptyList()
        }
        val adjustmentKey = if (isSaplink) "saplinkAdjustment" else "bloomBackboneAdjustment"
        val qualifiesKey = if (isSaplink) "qualifiesSaplink" else "qualifiesBloomBackbone"
        alternatives.map { alt ->
            val o = alt.observations
            BuySynergyOpportunity(
                game = gameNumber,
                player = event.playerId.value,
                card = o["card"] ?: alt.choiceLabel,
                plantType = o["plantType"] ?: "?",
                cost = o["cost"]?.toIntOrNull() ?: -1,
                selected = alt.selected,
                qualifies = o[qualifiesKey]?.toBooleanStrictOrNull() ?: false,
                baseUtility = o["baseBuyUtility"]?.toIntOrNull() ?: alt.baseScore,
                synergyAdjustment = o[adjustmentKey]?.toIntOrNull() ?: 0,
                finalUtility = o["finalBuyUtility"]?.toIntOrNull() ?: alt.total,
                sameCostCompetitors = alternatives.filter { it !== alt }
                    .joinToString(",") { other -> "${other.observations["card"] ?: other.choiceLabel}:${other.total}" }
                    .ifBlank { "-" },
                nearCostCandidates = o["nearCostCandidates"] ?: "-"
            )
        }
    }
}

private fun extractTransplantTulipBattleSwaps(
    gameNumber: Int,
    target: String,
    entries: List<GameEntry>
): List<TransplantTulipBattleSwapOpportunity> {
    if (!target.equals("Flower_11_04", true)) return emptyList()
    return entries.filterIsInstance<GameEntry.DecisionReasoning>().flatMapIndexed { decisionIndex, event ->
        event.alternatives.mapNotNull { alt ->
            val o = alt.observations
            if (o["decisionFamily"] != "transplant-tulip-battle-swap") return@mapNotNull null
            TransplantTulipBattleSwapOpportunity(
                game = gameNumber,
                player = event.playerId.value,
                decision = decisionIndex,
                selected = alt.selected,
                choice = alt.choiceLabel,
                sourceDie = o["sourceDie"] ?: "?",
                targetDie = o["targetDie"] ?: "?",
                sourceRow = o["sourceRow"] ?: "?",
                targetRow = o["targetRow"] ?: "?",
                raisedDie = o["raisedDie"] ?: "?",
                raiseGain = o["raiseGain"]?.toIntOrNull() ?: 0,
                sourceBefore = o["sourceBefore"] ?: "?",
                sourceAfter = o["sourceAfter"] ?: "?",
                targetBefore = o["targetBefore"] ?: "?",
                targetAfter = o["targetAfter"] ?: "?",
                winningDelta = o["winningDelta"]?.toIntOrNull() ?: 0,
                woundRiskDelta = o["woundRiskDelta"]?.toIntOrNull() ?: 0,
                battleVpBefore = o["battleVpBefore"]?.toIntOrNull() ?: 0,
                battleVpAfter = o["battleVpAfter"]?.toIntOrNull() ?: 0,
                battleVpGain = o["battleVpGain"]?.toIntOrNull() ?: 0,
                tacticalUtility = o["tacticalUtility"]?.toIntOrNull() ?: 0,
                finalUtility = alt.total
            )
        }
    }
}

private fun matches(target: String, observations: Map<String, String>): Boolean = when (target.lowercase()) {
    "mulch" -> observations["effect"] == GameEffect.MULCH_DIE_FROM_HAND.name
    else -> observations["card"].equals(target, true) || observations["effect"].equals(target, true)
}

private fun bucket(target: String, o: Map<String, String>): String = when (target.lowercase()) {
    "mulch" -> "roll=${o["targetDieValue"] ?: "?"}|sides=${o["targetDieSides"] ?: "?"}|battle=${o["battleNext"] ?: "?"}|upcoming=${o["upcomingDiceQuality"] ?: "?"}"
    "flower_17_02" -> "discard=D${o["targetDieSides"] ?: "?"}|recycle=${o["recycleDistance"] ?: "?"}|battle=${o["battleNext"] ?: "?"}|upcoming=${o["upcomingDiceQuality"] ?: "?"}"
    "vine_11_02", "flower_11_02" -> "next=${o["nextPhase"] ?: "?"}|synergy=${o["synergyPlants"] ?: "?"}|realizable=${o["realizableRaises"] ?: "?"}|headroom=${o["dieHeadroom"] ?: "?"}"
    "flower_14_01" -> "next=${o["nextPhase"] ?: "?"}|bees=${o["bees"] ?: "?"}|beeValue=${o["beeValue"] ?: "?"}"
    "flower_14_04" -> "next=${o["nextPhase"] ?: "?"}|d4=${o["ownedD4"] ?: "?"}|gain=${o["gainD4Branch"] ?: "NA"}|trash=${o["bestTrashD4Branch"] ?: "NA"}"
    "flower_11_04" -> "next=${o["nextPhase"] ?: "?"}|hand=${o["handDice"] ?: "?"}|power=${o["handPower"] ?: "?"}"
    "flower_17_03" -> "next=${o["nextPhase"] ?: "?"}|spent=${o["spentPlants"] ?: "?"}"
    else -> "next=${o["nextPhase"] ?: "?"}|up=${o["faceUpPlants"] ?: "?"}|down=${o["faceDownPlants"] ?: "?"}"
}

private fun extractOEdelweissDownstream(
    gameNumber: Int,
    target: String,
    entries: List<GameEntry>
): List<OEdelweissDownstreamOpportunity> {
    if (!target.equals("Flower_17_03", true)) return emptyList()
    return entries.filterIsInstance<GameEntry.DecisionReasoning>().flatMapIndexed { decisionIndex, event ->
        event.alternatives.mapNotNull { alt ->
            val o = alt.observations
            if (o["decisionFamily"] != "o-edelweiss-downstream") return@mapNotNull null
            OEdelweissDownstreamOpportunity(
                game = gameNumber,
                player = event.playerId.value,
                decision = decisionIndex,
                choiceNumber = o["choiceNumber"]?.toIntOrNull() ?: -1,
                phase = o["phase"] ?: "?",
                selected = alt.selected,
                choice = alt.choiceLabel,
                branch = o["branch"] ?: "?",
                targetCard = o["targetCard"],
                targetFaceUp = o["targetFaceUp"]?.toBooleanStrictOrNull(),
                faceUpPlants = o["faceUpPlants"]?.toIntOrNull() ?: -1,
                faceDownPlants = o["faceDownPlants"]?.toIntOrNull() ?: -1,
                baseUtility = alt.baseScore,
                finalUtility = alt.total,
                utilityComponents = alt.adjustments.map { "${it.amount}:${it.reason}" }
            )
        }
    }
}

private fun render(
    options: Options,
    records: List<CalibrationOpportunity>,
    buySynergyRecords: List<BuySynergyOpportunity>,
    transplantTulipBattleSwaps: List<TransplantTulipBattleSwapOpportunity>,
    oEdelweissDownstream: List<OEdelweissDownstreamOpportunity>
): String = buildString {
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
    if (transplantTulipBattleSwaps.isNotEmpty()) {
        appendLine()
        appendLine("TRANSPLANT TULIP BATTLE SWAP")
        val comparisons = transplantTulipBattleSwaps.groupBy { Triple(it.game, it.player, it.decision) }
        appendLine("candidate observations=${transplantTulipBattleSwaps.size} swap comparisons=${comparisons.size}")
        appendLine("selected avgTactical=${"%.1f".format(transplantTulipBattleSwaps.filter { it.selected }.map { it.tacticalUtility }.averageOrZero())} avgVpGain=${"%.2f".format(transplantTulipBattleSwaps.filter { it.selected }.map { it.battleVpGain }.averageOrZero())}")
        appendLine("SWAP COMPARISONS (first 30)")
        comparisons.entries.take(30).forEach { (_, rows) ->
            val selected = rows.singleOrNull { it.selected }
            if (selected != null) {
                appendLine("g${selected.game}/p${selected.player} selected=${selected.choice} tactical=${selected.tacticalUtility} utility=${selected.finalUtility} vp=${selected.battleVpBefore}->${selected.battleVpAfter} (${selected.battleVpGain}) winDelta=${selected.winningDelta} woundRiskDelta=${selected.woundRiskDelta}")
                appendLine("  swap ${selected.sourceDie}[${selected.sourceRow}] <-> ${selected.targetDie}[${selected.targetRow}]; raise=${selected.raisedDie}+${selected.raiseGain}")
                appendLine("  ${selected.sourceRow}: ${selected.sourceBefore} -> ${selected.sourceAfter}")
                appendLine("  ${selected.targetRow}: ${selected.targetBefore} -> ${selected.targetAfter}")
                rows.filter { !it.selected }.sortedByDescending { it.finalUtility }.take(3).forEach { rejected ->
                    appendLine("  rejected ${rejected.choice} tactical=${rejected.tacticalUtility} utility=${rejected.finalUtility} vpGain=${rejected.battleVpGain} winDelta=${rejected.winningDelta} woundRiskDelta=${rejected.woundRiskDelta}")
                }
            }
        }
    }
    if (oEdelweissDownstream.isNotEmpty()) {
        appendLine()
        appendLine("O EDELWEISS DOWNSTREAM CHOICES")
        val decisions = oEdelweissDownstream.groupBy { Triple(it.game, it.player, it.decision) }
        appendLine("candidate observations=${oEdelweissDownstream.size} downstream decisions=${decisions.size} first=${decisions.values.count { it.first().choiceNumber == 1 }} second=${decisions.values.count { it.first().choiceNumber == 2 }}")
        appendLine("selected branches=" + oEdelweissDownstream.filter { it.selected }.groupingBy { it.branch }.eachCount().toSortedMap())
        appendLine("DOWNSTREAM DECISIONS (first 40)")
        decisions.entries.take(40).forEach { (_, rows) ->
            val selected = rows.singleOrNull { it.selected }
            if (selected != null) {
                appendLine("g${selected.game}/p${selected.player} choice#${selected.choiceNumber} phase=${selected.phase} state=up${selected.faceUpPlants}/down${selected.faceDownPlants} selected=${selected.choice} utility=${selected.baseUtility}->${selected.finalUtility}")
                if (selected.utilityComponents.isNotEmpty()) appendLine("  components: ${selected.utilityComponents.joinToString()}")
                rows.filter { !it.selected }.sortedByDescending { it.finalUtility }.take(4).forEach { rejected ->
                    appendLine("  rejected ${rejected.choice} branch=${rejected.branch} target=${rejected.targetCard ?: "-"} faceUp=${rejected.targetFaceUp ?: false} utility=${rejected.baseUtility}->${rejected.finalUtility}" +
                        if (rejected.utilityComponents.isEmpty()) "" else " components=${rejected.utilityComponents.joinToString()}")
                }
            }
        }
        appendLine("SEQUENTIAL PAIRS (first 25 with both choices)")
        val byGamePlayer = decisions.values.mapNotNull { rows -> rows.singleOrNull { it.selected } }
            .groupBy { it.game to it.player }
        byGamePlayer.values.mapNotNull { selectedRows ->
            val ordered = selectedRows.sortedBy { it.decision }
            val first = ordered.firstOrNull { it.choiceNumber == 1 } ?: return@mapNotNull null
            val second = ordered.firstOrNull { it.choiceNumber == 2 && it.decision > first.decision } ?: return@mapNotNull null
            first to second
        }.take(25).forEach { (first, second) ->
            appendLine("g${first.game}/p${first.player} #1 ${first.choice} (${first.finalUtility}) state=up${first.faceUpPlants}/down${first.faceDownPlants} -> #2 ${second.choice} (${second.finalUtility}) state=up${second.faceUpPlants}/down${second.faceDownPlants}")
        }
    }
    if (buySynergyRecords.isNotEmpty()) {
        appendLine()
        appendLine("OWNERSHIP BUY SYNERGY")
        appendLine("candidate observations=${buySynergyRecords.size} buy comparisons=${buySynergyRecords.count { it.selected }}")
        buySynergyRecords.groupBy { it.qualifies }.toSortedMap().forEach { (qualifies, rows) ->
            appendLine("qualifies=$qualifies n=${rows.size} selected=${rows.count { it.selected }} avgBase=${"%.1f".format(rows.map { it.baseUtility }.average())} avgSynergy=${"%.1f".format(rows.map { it.synergyAdjustment }.average())} avgFinal=${"%.1f".format(rows.map { it.finalUtility }.average())}")
        }
        appendLine("BUY OPPORTUNITIES (first 40 candidate rows)")
        buySynergyRecords.take(40).forEach { r ->
            appendLine("g${r.game}/p${r.player} ${r.card} type=${r.plantType} cost=${r.cost} qualifies=${r.qualifies} selected=${r.selected} utility=${r.baseUtility}+${r.synergyAdjustment}->${r.finalUtility}")
            appendLine("  same-cost=${r.sameCostCompetitors} near-cost=${r.nearCostCandidates}")
        }
    }
}

private fun List<Int>.averageOrZero(): Double = if (isEmpty()) 0.0 else average()


private data class CounterfactualResult(
    val sample: Int,
    val opportunity: Boolean,
    val useVp: Int?,
    val drawVp: Int?,
    val useWon: Boolean,
    val drawWon: Boolean,
    val useProvenance: String?,
    val baselineChoice: String?
)

private fun runCounterfactual(
    options: Options,
    plants: List<dugsolutions.leaf.v35.plant.domain.PlantCard>,
    gameFactory: GameFactory,
    gameRunner: GameRunner
): String {
    val cardName = when (options.target.lowercase()) {
        "forget-me-not" -> "Flower_17_02"
        "root-four-more" -> "Root_05_02"
        else -> options.target
    }
    require(cardName in setOf("Flower_17_02", "Root_05_02")) {
        "Counterfactual mode is intentionally limited to Flower_17_02 and Root_05_02; got ${options.target}"
    }
    val rows = (0 until options.games).map { sample ->
        fun branch(which: CounterfactualBranch): Pair<dugsolutions.leaf.v35.game.Game, CounterfactualIntervention> {
            val intervention = CounterfactualIntervention()
            val factories = MutableList(4) { PlayerDecisionFactory.humanBaseline() }
            factories[0] = counterfactualHumanBaselineFactory(cardName, which, intervention)
            val game = gameFactory(
                GameConfig(
                    selectedPlantCards = plants,
                    playerDecisionFactories = factories,
                    roundSetup = GameRoundSetup.standard(),
                    seed = options.seed + sample,
                    strategySeed = options.strategySeed + sample,
                    recordDecisionReasoning = true
                )
            )
            gameRunner.run(game)
            return game to intervention
        }
        val (useGame, useIntervention) = branch(CounterfactualBranch.USE_EFFECT)
        val (drawGame, drawIntervention) = branch(CounterfactualBranch.DRAW)
        check(useIntervention.opportunitySeen == drawIntervention.opportunitySeen) {
            "Paired games disagreed on pre-intervention opportunity at sample ${sample + 1}"
        }
        val playerId = useGame.players.first().id
        fun vp(game: dugsolutions.leaf.v35.game.Game): Int? = game.chronicle.entries
            .filterIsInstance<GameEntry.FinalScore>().singleOrNull { it.playerId == playerId }?.totalVp
        fun won(game: dugsolutions.leaf.v35.game.Game): Boolean = game.chronicle.entries
            .filterIsInstance<GameEntry.FinalWinners>().lastOrNull()?.winnerIds?.contains(playerId) == true
        val provenance = when (cardName) {
            "Flower_17_02" -> useGame.assetProvenance.forgetMeNot.lastOrNull { it.playerId == playerId && it.sourceCard == cardName }?.let {
                "D${it.dieSides} recycle=${it.estimatedNaturalDrawsUntilAvailable} accelerated=${it.estimatedDrawsAccelerated} reachedBattle=${it.reachedNextBattle} winning=${it.contributedToWinningStrike} decisive=${it.individuallyWinnerDecisive} battleVp=${it.associatedBattleVp}"
            }
            "Root_05_02" -> useGame.assetProvenance.immediateDieEffects.lastOrNull { it.playerId == playerId && it.sourceCard == cardName }?.let {
                "delta=${it.before}->${it.after} winning=${it.contributedToWinningStrike} decisive=${it.individuallyWinnerDecisive} battleVp=${it.associatedBattleVp}"
            }
            else -> null
        }
        CounterfactualResult(sample + 1, useIntervention.opportunitySeen, vp(useGame), vp(drawGame), won(useGame), won(drawGame), provenance, useIntervention.baselineChoice)
    }
    val qualified = rows.filter { it.opportunity }
    return buildString {
        appendLine("TARGETED COUNTERFACTUAL CALIBRATION")
        appendLine("target=$cardName games=${options.games} qualifying=${qualified.size}")
        appendLine("PAIRING: USE EFFECT and DRAW start with identical mechanical seed=${options.seed}+sample and strategy seed=${options.strategySeed}+sample.")
        appendLine("The baseline chooser is invoked before override in both branches, preserving strategy-RNG consumption through the intervention decision.")
        appendLine("CONTROL LIMIT: after USE and DRAW create different state, later legal choices and RNG call counts can diverge; subsequent random events are not event-for-event matched.")
        if (qualified.isNotEmpty()) {
            val deltas = qualified.mapNotNull { r -> if (r.useVp != null && r.drawVp != null) r.useVp - r.drawVp else null }
            appendLine("avg final VP: use=${"%.2f".format(qualified.mapNotNull { it.useVp }.averageOrZero())} draw=${"%.2f".format(qualified.mapNotNull { it.drawVp }.averageOrZero())} delta=${"%.2f".format(deltas.averageOrZero())}")
            appendLine("wins: use=${qualified.count { it.useWon }} draw=${qualified.count { it.drawWon }}")
        }
        appendLine("PAIRS (first 30 qualifying)")
        qualified.take(30).forEach { r ->
            appendLine("g${r.sample} useVP=${r.useVp} drawVP=${r.drawVp} delta=${if (r.useVp != null && r.drawVp != null) r.useVp-r.drawVp else "-"} useWon=${r.useWon} drawWon=${r.drawWon}")
            appendLine("  baseline-at-opportunity=${r.baselineChoice ?: "-"}")
            if (r.useProvenance != null) appendLine("  use provenance: ${r.useProvenance}")
        }
    }
}

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
