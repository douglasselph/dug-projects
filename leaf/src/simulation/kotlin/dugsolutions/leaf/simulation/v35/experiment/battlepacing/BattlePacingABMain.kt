package dugsolutions.leaf.simulation.v35.experiment.battlepacing

import dugsolutions.leaf.simulation.v35.learning.cultivation.loadCultivationResearchCards
import dugsolutions.leaf.v35.di.appModules
import dugsolutions.leaf.v35.game.GameConfig
import dugsolutions.leaf.v35.game.GameRoundSetup
import dugsolutions.leaf.v35.game.PlayerDecisionFactory
import dugsolutions.leaf.v35.game.di.GameFactory
import dugsolutions.leaf.v35.game.round.RoundCoordinator
import dugsolutions.leaf.v35.game.round.battle.BattleRound
import dugsolutions.leaf.v35.plant.PlantCardManager
import dugsolutions.leaf.v35.plant.PlantCardRegistry
import dugsolutions.leaf.v35.round.RoundCardManager
import dugsolutions.leaf.v35.round.RoundCardRegistry
import dugsolutions.leaf.v35.wisp.WispCardManager
import dugsolutions.leaf.v35.wisp.WispCardRegistry
import dugsolutions.leaf.v35.tokens.Critter
import dugsolutions.leaf.v35.player.Player
import dugsolutions.leaf.v35.game.scoring.FinalScorer
import org.koin.dsl.koinApplication
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.util.Locale

private data class BatchOptions(
    val samples: Int = 10,
    val players: List<Int> = listOf(2, 3, 4),
    val battles: List<Int> = listOf(1, 2, 3),
    val policy: String = "mechanical",
    val scenario: BattlePacingScenario = BattlePacingScenario.NATURAL,
    val maxAttempts: Int? = null,
    val seed: Long = 2026103000L,
    val strategySeed: Long = 2026104000L,
    val wispSeed: Long = 2026105000L,
    val timingModel: Path? = null,
    val task7Counterfactual: Boolean = false,
    val output: Path = Paths.get("output/experiments/battle-pacing/checkpoint3")
) {
    companion object {
        fun parse(args: Array<String>): BatchOptions {
            require(args.size % 2 == 0) { "Options require '--name value' pairs" }
            val values = args.toList().chunked(2).associate { (key, value) ->
                require(key.startsWith("--")) { "Unknown argument: $key" }
                key to value
            }
            require(values.keys.all { it in setOf(
                "--samples", "--players", "--battles", "--policy", "--scenario", "--max-attempts", "--seed", "--strategy-seed", "--wisp-seed", "--timing-model", "--task7-counterfactual", "--out"
            ) }) { "Unknown option(s): ${values.keys}" }
            fun csv(key: String, default: List<Int>): List<Int> = values[key]
                ?.split(",")?.map { it.trim().toInt() } ?: default
            val options = BatchOptions(
                samples = values["--samples"]?.toInt() ?: 10,
                players = csv("--players", listOf(2, 3, 4)),
                battles = csv("--battles", listOf(1, 2, 3)),
                policy = values["--policy"] ?: "mechanical",
                scenario = BattlePacingScenario.parse(values["--scenario"] ?: "natural"),
                maxAttempts = values["--max-attempts"]?.toInt(),
                seed = values["--seed"]?.toLong() ?: 2026103000L,
                strategySeed = values["--strategy-seed"]?.toLong() ?: 2026104000L,
                wispSeed = values["--wisp-seed"]?.toLong() ?: 2026105000L,
                timingModel = values["--timing-model"]?.let(Paths::get),
                task7Counterfactual = values["--task7-counterfactual"]?.toBooleanStrict() ?: false,
                output = Paths.get(values["--out"] ?: "output/experiments/battle-pacing/checkpoint3")
            )
            require(options.samples > 0)
            require(options.maxAttempts == null || options.maxAttempts >= options.samples) { "--max-attempts must be >= --samples" }
            require(options.samples <= Int.MAX_VALUE / 40) { "--samples too large for default max attempts" }
            require(options.players.isNotEmpty() && options.players.distinct().size == options.players.size &&
                options.players.all { it in 2..4 })
            require(options.battles.isNotEmpty() && options.battles.distinct().size == options.battles.size &&
                options.battles.all { it in 1..3 })
            require(options.policy in setOf("mechanical", "human-guided"))
            return options
        }
    }
}

private class CsvFile(path: Path, header: String) : AutoCloseable {
    private val writer = Files.newBufferedWriter(path, Charsets.UTF_8,
        StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)
    init { writer.appendLine(header) }
    fun row(vararg parts: Any?) {
        writer.appendLine(parts.joinToString(",") { value ->
            val raw = value?.toString() ?: ""
            if (raw.contains(',') || raw.contains('"') || raw.contains('\n'))
                "\"${raw.replace("\"", "\"\"")}\"" else raw
        })
    }
    override fun close() = writer.close()
}

private fun fmt(value: Double): String =
    if (value.isNaN()) "NA" else String.format(Locale.ROOT, "%.6f", value)

/** Available *resource inventory*, not guaranteed legal actions or an observed time. */
private fun supportInventory(player: Player): Int =
    player.critters.count(Critter.BEE) + player.critters.count(Critter.WORM) +
    player.tokens.waterCount + player.tokens.sunlightCount +
    player.tokens.mulchTokens.size + player.wisps.cards.cards.size +
    player.butterflies.all.count { player.butterflies.isFaceUp(it) }

private fun listIds(ids: Collection<dugsolutions.leaf.v35.player.PlayerId>): String =
    ids.joinToString("|") { it.value.toString() }

private fun sha(path: Path): String {
    val md = MessageDigest.getInstance("SHA-256")
    Files.newInputStream(path).use { input ->
        val bytes = ByteArray(65536)
        while (true) {
            val n = input.read(bytes)
            if (n < 0) break
            md.update(bytes, 0, n)
        }
    }
    return md.digest().joinToString("") { "%02x".format(it) }
}

/**
 * Entry point for *actual* paired legal Battle openings. A/B trajectories before
 * selected Battle are canonical A in BOTH arms; candidate is local intervention.
 * Any invalid action/effect aborts the whole cohort rather than biasing estimates.
 */
fun main(args: Array<String>) {
    val options = BatchOptions.parse(args)
    require(!Files.exists(options.output)) {
        "Output exists, refusing accidental aggregation/overwrite: ${options.output}"
    }
    Files.createDirectories(options.output)
    val app = koinApplication { modules(appModules) }
    try {
        val k = app.koin
        loadCultivationResearchCards(k.get<PlantCardRegistry>(), k.get<PlantCardManager>(),
            k.get<WispCardRegistry>(), k.get<WispCardManager>(),
            k.get<RoundCardRegistry>(), k.get<RoundCardManager>())
        val round = k.get<BattleRound>()
        val replay = PairedBattleOpeningReplay(k.get<GameFactory>(), k.get<RoundCoordinator>(), round)
        val compare = PairedBattleComparison(round)
        val manifest = buildString {
            appendLine("experiment=battle-pacing-task7-fairness; task6 outputs retained")
            appendLine("task7Counterfactual=${options.task7Counterfactual}; rotation=one seat clockwise from rule B selected starter per Engagement")
            appendLine("status=exploratory; runtime failures ABORT; no significance claim for mismatched tactical policies")
            appendLine("rounds=3/2/2; pre-target Battles canonical A in BOTH arms")
            appendLine("resolveWisp=excluded identically in A and B")
            appendLine("samples_per_cohort=${options.samples}")
            appendLine("players=${options.players.joinToString(",")}")
            appendLine("battle_ordinals=${options.battles.joinToString(",")}")
            appendLine("policy=${options.policy}")
            appendLine("scenario=${options.scenario.cliName}")
            appendLine("scenarioEligibility=pre-Battle Support inventory only; all games legal; rejected openings excluded before execution")
            appendLine("max_attempts_per_cohort=${options.maxAttempts ?: if (options.scenario == BattlePacingScenario.NATURAL) options.samples else options.samples * 40}")
            appendLine("selectionSeedIndex=attemptIndex; accepted sample index retained in existing CSV contracts")
            appendLine("base_seed=${options.seed}")
            appendLine("base_strategy_seed=${options.strategySeed}")
            appendLine("base_wisp_seed=${options.wispSeed}")
            appendLine("eventBasedDieRolls=true; sharedPercentileDieRolls=true")
            appendLine("candidateHumanGuided=v37 Main choice; v37 B10-B14 Support continuation and chooser constrained to two Supports per Engagement; exploratory, NOT calibrated to new timing")
            appendLine("CI=normal paired-mean/within-battle sample; B last actor mean has cluster-by-Battle CI; descriptive, not causal")
            appendLine("task6Timing=model-based estimates only; simulator CPU time is not human play time")
            appendLine("task6TimingModel=${options.timingModel ?: "built-in-fast-typical-analysis-heavy"}")
            appendLine("task6DecisionComplexity=log2(1+legalChoices)+0.5*(unresolvedStrikes-1), capped at 10")
            appendLine("task6Reassessment=changed rows + leader/margin changes + capped dice/Plant changes, capped at 10")
            listOf(
                "data/Cultivation_Cards - Root.csv", "data/Cultivation_Cards - VF.csv",
                "data/Wisp_Cards.csv", "data/Turn_Cards.csv",
                "src/simulation/kotlin/dugsolutions/leaf/simulation/v35/experiment/battlepacing/BattlePacingScenario.kt",
                "src/main/kotlin/dugsolutions/leaf/v35/game/round/battle/BattleActionCoordinator.kt",
                "src/main/kotlin/dugsolutions/leaf/v35/game/round/battle/BattleRound.kt",
                "src/main/kotlin/dugsolutions/leaf/v35/effect/special/PetalToDie4Effect.kt",
                "src/simulation/kotlin/dugsolutions/leaf/simulation/v35/experiment/battlepacing/EngagementOrderSelector.kt",
                "src/simulation/kotlin/dugsolutions/leaf/simulation/v35/experiment/battlepacing/PairedBattleOpeningReplay.kt",
                "src/simulation/kotlin/dugsolutions/leaf/simulation/v35/experiment/battlepacing/PairedBattleComparison.kt",
                "src/simulation/kotlin/dugsolutions/leaf/simulation/v35/experiment/battlepacing/ThreeEngagementBattle.kt",
                "src/simulation/kotlin/dugsolutions/leaf/simulation/v35/experiment/battlepacing/HumanGuidedEngagementPolicy.kt",
                "src/simulation/kotlin/dugsolutions/leaf/simulation/v35/experiment/battlepacing/EngagementSupportContinuation.kt",
                "src/simulation/kotlin/dugsolutions/leaf/simulation/v35/experiment/battlepacing/EngagementMainTimingPlanner.kt",
                "src/simulation/kotlin/dugsolutions/leaf/simulation/v35/experiment/battlepacing/BaselineMainActionForecast.kt",
                "src/simulation/kotlin/dugsolutions/leaf/simulation/v35/experiment/battlepacing/BattlePacingStatistics.kt",
                "src/simulation/kotlin/dugsolutions/leaf/simulation/v35/experiment/battlepacing/BattlePacingTimingModel.kt",
                "src/simulation/kotlin/dugsolutions/leaf/simulation/v35/experiment/battlepacing/BattlePacingTask6Summary.kt",
                "src/simulation/kotlin/dugsolutions/leaf/simulation/v35/experiment/battlepacing/BattlePacingABMain.kt",
                "doc/tools/battle/battle_pacing_task6_report.py",
                "doc/battle/run_battle_pacing_ab.sh"
            ).forEach { relative ->
                val path = Paths.get(relative)
                if (Files.isRegularFile(path)) appendLine("sha256:$relative=${sha(path)}")
            }
        }
        Files.writeString(options.output.resolve("manifest.txt"), manifest)
        val startNanos = System.nanoTime()
        val timingProfiles = BattleTimingProfiles.load(options.timingModel)
        CsvFile(options.output.resolve("timing_model.csv"), BattleTimingProfiles.csvHeader.joinToString(",")).use { modelCsv ->
            timingProfiles.forEach { modelCsv.row(*BattleTimingProfiles.row(it).toTypedArray()) }
        }
        val pacingWindows = CsvFile(options.output.resolve("pacing_windows.csv"),
            "players,battle,policy,scenario,sample,openingSha,ruleSet,window,actor,stage,row,unresolvedStrikes,legalChoices,meaningfulWindow,actionCount,passes,emptyOpportunity,normalMains,sunlightMains,supports,diceRolled,diceRerolled,dicePlaced,diceMoved,diceValueChanges,cardsActivated,plantFacingTransitions,refreshes,resourcesSpent,resourcesGained,changedRows,leaderChanges,marginChanges,reassessmentUnits,complexityUnits,laterStrikeUnits,actionKinds")
        val counterRows = CsvFile(options.output.resolve("task7_counterfactual.csv"),
            "players,battle,policy,scenario,sample,openingSha,rotation,player,referenceBattleVP,rotatedBattleVP,deltaBattleVP,referenceUnusedSupports,rotatedUnusedSupports,referenceWounds,rotatedWounds")
        val initialRows = CsvFile(options.output.resolve("task7_initial_rows.csv"),
            "players,battle,policy,scenario,sample,openingSha,row,player,openingTotal,openingMargin,initialSupports,initialReadyPlants,initialVP,gridColumn")
        val actorRows = CsvFile(options.output.resolve("task7_row_actors.csv"),
            "players,battle,policy,scenario,sample,openingSha,ruleSet,row,lastMeaningfulActor,lastWindow,rowChanges,swungIntoWinningStatus,beforeMargin,afterMargin,finalWinners,finalWounds,finalVPPerWinner")
        val pacingBattles = CsvFile(options.output.resolve("pacing_battles.csv"),
            "players,battle,policy,scenario,sample,openingSha,ruleSet,profile,estimatedSeconds,decisionSeconds,executionSeconds,handoffSeconds,reassessmentSeconds,decisionWindows,meaningfulDecisionWindows,handoffs,normalMains,sunlightMains,supports,passes,emptyOpportunities,diceRolled,diceRerolled,dicePlaced,diceMoved,diceValueChanges,cardsActivated,plantFacingTransitions,refreshes,reassessmentWindows,complexityUnits,reassessmentUnits,resourcesSpent,resourcesGained,strikes,wounds,doomDice,cleanupDice,returnedCritters,unusedSupportsPerPlayer,candidateLaterStrikeUnits")
        val pacingPlayers = CsvFile(options.output.resolve("pacing_players.csv"),
            "players,battle,policy,scenario,sample,openingSha,ruleSet,profile,player,totalWaitingSeconds,longestWaitingSeconds,terminalWaitingSeconds,consequentialWatchingSeconds,noMeaningfulLegalActionSeconds,unusedSupports")
        val pacingSensitivity = CsvFile(options.output.resolve("pacing_sensitivity.csv"),
            "players,battle,policy,scenario,sample,openingSha,profile,baselineSeconds,candidateSeconds,savingsSeconds,savingsPercent,candidateLaterStrikeUnits,additionalCandidatePlanningSecondsPerLaterStrikeUnitToBreakEven")
        val pacingSummary = CsvFile(options.output.resolve("pacing_summary.csv"),
            "players,battle,policy,scenario,profile,completedPairs,AmeanSeconds,AmedianSeconds,AP90Seconds,AP95Seconds,BmeanSeconds,BmedianSeconds,BP90Seconds,BP95Seconds,meanSavingsSeconds,meanSavingsPercent,deltaSecondsCI95Low,deltaSecondsCI95High,AmeanPlayerWait,BmeanPlayerWait,AmedianPlayerWait,BmedianPlayerWait,AP95PlayerWait,BP95PlayerWait,AmeanLongestWait,BmeanLongestWait,AmedianLongestWait,BmedianLongestWait,AP95LongestWait,BP95LongestWait,AmeanTerminalWait,BmeanTerminalWait,AmedianTerminalWait,BmedianTerminalWait,AP95TerminalWait,BP95TerminalWait,AmeanNoLegalWait,BmeanNoLegalWait,AmeanConsequentialWatching,BmeanConsequentialWatching,AmeanDecisionWindows,BmeanDecisionWindows,AmeanMeaningfulWindows,BmeanMeaningfulWindows,AmeanHandoffs,BmeanHandoffs,AmeanSupports,BmeanSupports,AmeanUnusedSupportsPerPlayer,BmeanUnusedSupportsPerPlayer,meanBreakEvenExtraCandidatePlanningSecondsPerLaterStrikeUnit")
        try {
        // Additive selection audit (including rejected LEGAL openings). The existing
        // pairs/strikes/opportunities/summary CSV headers remain unchanged.
        CsvFile(options.output.resolve("opening_selection.csv"),
            "players,battle,policy,scenario,attemptIndex,acceptedSample,accepted,seed,strategySeed,wispSeed,openingSha,initialSupports,minSupports,maxSupports,supportGap,initialVP,vpGap").use { selection ->
        CsvFile(options.output.resolve("selection_summary.csv"),
            "players,battle,policy,scenario,acceptedPairs,attemptedOpenings,rejectedOpenings,acceptanceRate,maxAttempts").use { selectionSummaries ->
        // Additive research output: do not change or break the existing CSV contracts.
        CsvFile(options.output.resolve("main_plans.csv"),
            "players,battle,policy,sample,openingSha,planTrace").use { mainPlans ->
        CsvFile(options.output.resolve("pairs.csv"),
            "players,battle,policy,sample,seed,strategySeed,wispSeed,openingSha,player,initialVP,baselineFinalVP,candidateFinalVP,baselineBattleVP,candidateBattleVP,deltaBattleVP,initialSupports,baselineRemainingSupports,candidateRemainingSupports,baselineTotalIfGameEndedNow,candidateTotalIfGameEndedNow,baselinePlantVP,candidatePlantVP,baselineWispVP,candidateWispVP,baselineWouldWin,candidateWouldWin").use { pairs ->
            CsvFile(options.output.resolve("strikes.csv"),
                "players,battle,policy,sample,openingSha,row,baselineWinners,candidateWinners,baselineVPPerWinner,candidateVPPerWinner,baselineWounds,candidateWounds,candidateStarter,candidateLastActor,lastUniqueWin,lastSharedWin,lastSwing,lastHadAction,initialTotals,initialProjectedWinners,lastBeforeTotals,lastAfterTotals").use { strikes ->
                CsvFile(options.output.resolve("opportunities.csv"),
                    "players,battle,policy,sample,openingSha,row,position,actor,actions,mainCount,supportCount,maxLegalMains,maxLegalSupports,beforeTotals,afterTotals").use { opportunities ->
                    CsvFile(options.output.resolve("summary.csv"),
                        "players,battle,policy,completedPairs,pairedDeltaBattleVPPerPlayer,deltaVP_CI95Low,deltaVP_CI95High,baselineBattleVPPerPlayer,candidateBattleVPPerPlayer,deltaStrikeVPPerPlayer,deltaWoundsPerPlayer,baselineMainActions,baselineSupportActions,candidateMainActions,candidateSupportActions,candidateNoActionOpportunities,candidateTotalOpportunities,Bstrikes,lastUniqueWins,lastSharedWins,lastActorSwings,lastActorActs,lastUniqueRate,lastUniqueClusterCILow,lastUniqueClusterCIHigh,seconds").use { summaries ->
                        options.players.forEach { players -> options.battles.forEach { ordinal ->
                            val summary = BattlePacingCohortSummary()
                            val task6ByProfile = timingProfiles.associate { it.name to Task6CohortSummary() }
                            val cohortStart = System.nanoTime()
                            var accepted = 0
                            var attempted = 0
                            val maxAttempts = options.maxAttempts ?: if (options.scenario == BattlePacingScenario.NATURAL)
                                options.samples else options.samples * 40
                            while (accepted < options.samples) {
                                if (attempted >= maxAttempts) {
                                    val failure = "INCOMPLETE scenario=${options.scenario.cliName} " +
                                        "players=$players battle=$ordinal accepted=$accepted/${options.samples} " +
                                        "attempts=$attempted maxAttempts=$maxAttempts. " +
                                        "Increase --max-attempts or relax scenario. Never use partial results as a complete cohort."
                                    Files.writeString(options.output.resolve("FAILURE.txt"), failure)
                                    error(failure)
                                }
                                val attemptIndex = attempted++
                                val sample = accepted // existing output's per-cohort accepted index
                                val mechanicalSeed = options.seed + attemptIndex * 1009L + players * 100003L + ordinal * 1000003L
                                val strategySeed = options.strategySeed + attemptIndex * 1013L + players * 100019L + ordinal * 1000033L
                                val deckSeed = options.wispSeed + attemptIndex * 1021L + players * 100043L + ordinal * 1000037L
                                try {
                                    val config = GameConfig(
                                        playerDecisionFactories = List(players) {
                                            if (options.policy == "mechanical") PlayerDecisionFactory.mechanicalControl()
                                            else PlayerDecisionFactory.humanBaseline()
                                        },
                                        roundSetup = GameRoundSetup.standard(),
                                        seed = mechanicalSeed, strategySeed = strategySeed,
                                        eventBasedDieRolls = true, sharedPercentileDieRolls = true
                                    )
                                    val deck = BattlePacingWispDeck.withoutResolve(k.get<WispCardManager>(), deckSeed)
                                    val paired = replay.create(config, ordinal, exactWispCards = deck)
                                    val initialVP = paired.controlGame.players.map { it.vp }
                                    val initialSupports = paired.controlGame.players.map(::supportInventory)
                                    val opening = BattlePacingSelection(initialSupports, initialVP)
                                    val eligible = options.scenario.accepts(initialSupports)
                                    selection.row(players, ordinal, options.policy, options.scenario.cliName,
                                        attemptIndex, if (eligible) sample else "", eligible,
                                        mechanicalSeed, strategySeed, deckSeed, paired.fingerprint.sha256,
                                        initialSupports.joinToString("|"), opening.minimumSupports,
                                        opening.maximumSupports, opening.supportGap,
                                        initialVP.joinToString("|"), opening.vpGap)
                                    if (!eligible) continue
                                    dugsolutions.leaf.v35.battle.domain.StrikeRow.entries.forEach { strike ->
                                        val rowTotals = paired.controlGame.players.associate { player ->
                                            player.id to paired.controlBattle.grid.square(player.id, strike).let { square ->
                                                square.dice.sumOf { it.value } + square.critters.sumOf { player.critterValues.valueOf(it) }
                                            }
                                        }
                                        paired.controlGame.players.forEach { player ->
                                            initialRows.row(players, ordinal, options.policy, options.scenario.cliName, sample,
                                                paired.fingerprint.sha256, strike.name, player.id.value, rowTotals.getValue(player.id),
                                                rowTotals.getValue(player.id) - rowTotals.filterKeys { it != player.id }.values.max(),
                                                supportInventory(player), player.creature.cards.count { it.isFaceUp }, player.vp,
                                                paired.controlBattle.grid.playerIdsInGridOrder.indexOf(player.id))
                                        }
                                    }
                                    val result = compare.run(paired,
                                        if (options.policy == "mechanical") MechanicalEngagementPolicy
                                        else HumanGuidedEngagementPolicy(paired.candidateGame)
                                    )
                                    check(result.openingSha256 == paired.fingerprint.sha256)
                                    // Pure scoring projections; for Battle 3 they are actual
                                    // end-game totals, for Battles 1/2 merely snapshots.
                                    if (options.task7Counterfactual) {
                                        // Fresh independently replayed legal opening. The only intervention is
                                        // a one-seat shift of each B Engagement starter; rules and policies are identical.
                                        val cf = replay.create(config, ordinal, exactWispCards = deck)
                                        check(cf.fingerprint == paired.fingerprint) { "Counterfactual opening mismatch" }
                                        val rotated = ThreeEngagementBattle(round, starterRotation = 1).execute(
                                            cf.candidateGame, cf.candidateBattle,
                                            if (options.policy == "mechanical") MechanicalEngagementPolicy
                                            else HumanGuidedEngagementPolicy(cf.candidateGame))
                                        check(rotated.engagements.size == 3)
                                        cf.candidateGame.players.forEachIndexed { index, player ->
                                            val source = paired.candidateGame.players[index]
                                            val referenceWounds = result.candidate.engagements.sumOf { e ->
                                                e.resolution.woundedPlayerIds.count { it == source.id }
                                            }
                                            val rotatedWounds = rotated.engagements.sumOf { e ->
                                                e.resolution.woundedPlayerIds.count { it == player.id }
                                            }
                                            counterRows.row(players, ordinal, options.policy, options.scenario.cliName, sample,
                                                paired.fingerprint.sha256, 1, player.id.value,
                                                source.vp - initialVP[index], player.vp - initialVP[index],
                                                player.vp - source.vp, supportInventory(source), supportInventory(player),
                                                referenceWounds, rotatedWounds)
                                        }
                                    }
                                    val aFinal = FinalScorer().score(paired.controlGame)
                                    val bFinal = FinalScorer().score(paired.candidateGame)
                                    summary.add(result, initialVP)
                                    val aRemainingSupports = paired.controlGame.players.associate { it.id to supportInventory(it) }
                                    val bRemainingSupports = paired.candidateGame.players.associate { it.id to supportInventory(it) }
                                    val aPacing = baselinePacingObserved(paired.controlGame, result.baselineActions, result.baselineStrikes,
                                        result.baselineDoom, result.baselineCleanup, aRemainingSupports)
                                    val bPacing = candidatePacingObserved(paired.candidateGame, result.candidate, bRemainingSupports)
                                    fun writeWindows(observed: BattlePacingObserved) {
                                        observed.windows.forEach { w ->
                                            val complexity = w.steps.sumOf { it.complexityUnits }
                                            val laterUnits = w.steps.sumOf { kotlin.math.max(0, it.unresolvedStrikes - 1) }
                                            pacingWindows.row(players, ordinal, options.policy, options.scenario.cliName, sample,
                                                result.openingSha256, observed.ruleSet.name, w.index, w.playerId.value, w.stage,
                                                w.row?.name ?: "", w.unresolvedStrikes, w.legalChoiceCount, w.meaningfulDecisionWindows > 0,
                                                w.actionCount, w.passCount, w.emptyOpportunity, w.normalMainActions, w.sunlightMainActions,
                                                w.supportActions, w.diceRolled, w.diceRerolled, w.dicePlaced, w.diceMoved, w.diceValueChanges,
                                                w.cardsActivated, w.plantFacingTransitions, w.refreshes, w.resourcesSpent, w.resourcesGained,
                                                w.reassessment.changedRows, w.reassessment.leaderChanges, w.reassessment.marginChanges,
                                                w.reassessment.reassessmentUnits, fmt(complexity), laterUnits,
                                                w.steps.mapNotNull { it.actionKind ?: if (it.pass) "PASS" else null }.joinToString("|"))
                                        }
                                    }
                                    writeWindows(aPacing); writeWindows(bPacing)
                                    listOf(aPacing to result.baselineStrikes.strikes,
                                        bPacing to result.candidate.engagements.map { it.resolution }).forEach { (pacing, resolutions) ->
                                        resolutions.forEach { strike ->
                                            val e = rowActorEvidence(pacing, strike.row)
                                            actorRows.row(players, ordinal, options.policy, options.scenario.cliName, sample,
                                                result.openingSha256, pacing.ruleSet.name, strike.row.name,
                                                e.lastActor?.value ?: "", e.lastWindow ?: "", e.rowChanges,
                                                e.actorChangedWinningStatus, e.beforeLeadMargin ?: "", e.afterLeadMargin ?: "",
                                                listIds(strike.winnerIds), listIds(strike.woundedPlayerIds), strike.vpPerWinner)
                                        }
                                    }
                                    timingProfiles.forEach { timing ->
                                        val ae = estimateBattleTime(aPacing, timing); val be = estimateBattleTime(bPacing, timing)
                                        task6ByProfile.getValue(timing.name).add(aPacing, bPacing, ae, be)
                                        fun writeBattle(observed: BattlePacingObserved, estimate: BattleTimeEstimate) {
                                            pacingBattles.row(players, ordinal, options.policy, options.scenario.cliName, sample,
                                                result.openingSha256, observed.ruleSet.name, timing.name, fmt(estimate.totalSeconds),
                                                fmt(estimate.totalDecisionSeconds), fmt(estimate.totalExecutionSeconds),
                                                fmt(estimate.totalHandoffSeconds), fmt(estimate.totalReassessmentSeconds),
                                                observed.decisionWindows, observed.meaningfulDecisionWindows, observed.handoffs,
                                                observed.normalMainActions, observed.sunlightMainActions, observed.supportActions,
                                                observed.passes, observed.emptyOpportunities, observed.diceRolled, observed.diceRerolled,
                                                observed.dicePlaced, observed.diceMoved, observed.diceValueChanges, observed.cardsActivated,
                                                observed.plantFacingTransitions, observed.refreshesDuringActions, observed.reassessmentWindows,
                                                fmt(observed.complexityUnits), observed.reassessmentUnits,
                                                observed.resourcesSpent, observed.resourcesGained, observed.strikeCount, observed.woundCount,
                                                observed.doomDiceCount, observed.cleanupDiceCount, observed.returnedCritters,
                                                fmt(observed.remainingSupportInventory.values.average()), estimate.candidateLaterStrikeUnits)
                                            estimate.playerDowntime.forEach { d ->
                                                pacingPlayers.row(players, ordinal, options.policy, options.scenario.cliName, sample,
                                                    result.openingSha256, observed.ruleSet.name, timing.name, d.playerId.value,
                                                    fmt(d.totalWaitingSeconds), fmt(d.longestWaitingSeconds), fmt(d.terminalWaitingSeconds),
                                                    fmt(d.consequentialWatchingSeconds), fmt(d.noMeaningfulLegalActionSeconds),
                                                    observed.remainingSupportInventory.getValue(d.playerId))
                                            }
                                        }
                                        writeBattle(aPacing, ae); writeBattle(bPacing, be)
                                        val saving = ae.totalSeconds - be.totalSeconds
                                        val pct = if (ae.totalSeconds == 0.0) 0.0 else saving * 100.0 / ae.totalSeconds
                                        val breakEven = if (be.candidateLaterStrikeUnits == 0) Double.NaN else saving / be.candidateLaterStrikeUnits
                                        pacingSensitivity.row(players, ordinal, options.policy, options.scenario.cliName, sample,
                                            result.openingSha256, timing.name, fmt(ae.totalSeconds), fmt(be.totalSeconds), fmt(saving),
                                            fmt(pct), be.candidateLaterStrikeUnits, fmt(breakEven))
                                    }
                                    result.baselineFinalVP.indices.forEach { seat ->
                                        val aGain = result.baselineFinalVP[seat] - initialVP[seat]
                                        val bGain = result.candidateFinalVP[seat] - initialVP[seat]
                                        val aScore = aFinal.scores[seat]
                                        val bScore = bFinal.scores[seat]
                                        pairs.row(players, ordinal, options.policy, sample, mechanicalSeed,
                                            strategySeed, deckSeed, result.openingSha256, seat + 1,
                                            initialVP[seat], result.baselineFinalVP[seat],
                                            result.candidateFinalVP[seat], aGain, bGain, bGain - aGain,
                                            initialSupports[seat], supportInventory(paired.controlGame.players[seat]),
                                            supportInventory(paired.candidateGame.players[seat]),
                                            aScore.totalVp, bScore.totalVp, aScore.plantVp, bScore.plantVp,
                                            aScore.unplayedWispVp, bScore.unplayedWispVp,
                                            aScore.playerId in aFinal.winnerIds, bScore.playerId in bFinal.winnerIds)
                                    }
                                    result.candidate.trace.asSequence()
                                        .filter { it.startsWith("ENGAGEMENT_MAIN_PLAN,") }
                                        .forEach { planLine ->
                                            mainPlans.row(players, ordinal, options.policy, sample,
                                                result.openingSha256, planLine)
                                        }
                                    result.candidate.engagements.forEach engagementLoop@ { engagement ->
                                        // A Wisp may have finished a future row before any turns there.
                                        // No "last actor" observation exists for that row.
                                        if (engagement.preempted) return@engagementLoop
                                        val baseline = result.baselineStrikes.strikes.first { it.row == engagement.order.row }
                                        val last = engagement.opportunities.last()
                                        val lastObs = lastActorObservation(engagement)
                                        fun totals(map: Map<dugsolutions.leaf.v35.player.PlayerId, Int>): String =
                                            map.entries.joinToString("|") { "${it.key.value}:${it.value}" }
                                        strikes.row(players, ordinal, options.policy, sample, result.openingSha256,
                                            engagement.order.row.name, listIds(baseline.winnerIds),
                                            listIds(engagement.resolution.winnerIds), baseline.vpPerWinner,
                                            engagement.resolution.vpPerWinner, listIds(baseline.woundedPlayerIds),
                                            listIds(engagement.resolution.woundedPlayerIds), engagement.order.starter.value,
                                            last.playerId.value, lastObs.uniqueWinner, lastObs.sharedWinner,
                                            lastObs.swungIntoWin, lastObs.hadAction, totals(engagement.initialTotals),
                                            listIds(projectedWinners(engagement.initialTotals)), totals(last.beforeTotals), totals(last.afterTotals))
                                        engagement.opportunities.forEach { turn ->
                                            opportunities.row(players, ordinal, options.policy, sample, result.openingSha256,
                                                engagement.order.row.name, turn.actingPosition, turn.playerId.value,
                                                turn.actions.joinToString("|") { it.toString() },
                                                turn.actions.count { it is EngagementChoice.Main },
                                                turn.actions.count { it is EngagementChoice.Support },
                                                turn.maxLegalMains, turn.maxLegalSupports,
                                                totals(turn.beforeTotals), totals(turn.afterTotals))
                                        }
                                    }
                                    accepted++
                                } catch (failure: Throwable) {
                                    val message = "FAIL players=$players battle=$ordinal policy=${options.policy} " +
                                        "sample=$sample attemptIndex=$attemptIndex seed=$mechanicalSeed strategySeed=$strategySeed wispSeed=$deckSeed " +
                                        "reason=${failure.stackTraceToString()}"
                                    Files.writeString(options.output.resolve("FAILURE.txt"), message)
                                    System.err.println(message)
                                    throw failure
                                }
                                if (accepted % 100 == 0 && accepted > 0) println("COMPLETE $players-player Battle=$ordinal $accepted/${options.samples}; openings=$attempted")
                            }
                            selectionSummaries.row(players, ordinal, options.policy, options.scenario.cliName,
                                accepted, attempted, attempted - accepted, fmt(accepted.toDouble() / attempted), maxAttempts)
                            val ci = summary.pairedVpPerPlayer.interval95()
                            val uniqueCI = summary.lastUniqueSharePerBattle.interval95()
                            val seconds = (System.nanoTime() - cohortStart) / 1e9
                            summaries.row(players, ordinal, options.policy, summary.pairedVpPerPlayer.count,
                                fmt(summary.pairedVpPerPlayer.mean), fmt(ci.first), fmt(ci.second),
                                fmt(summary.baselineBattleVpPerPlayer.mean), fmt(summary.candidateBattleVpPerPlayer.mean),
                                fmt(summary.pairedStrikeVpPerPlayer.mean), fmt(summary.pairedWoundsPerPlayer.mean),
                                summary.baselineMainActions, summary.baselineSupportActions,
                                summary.candidateMainActions, summary.candidateSupportActions,
                                summary.candidateEmptyOpportunities, summary.candidateOpportunities,
                                summary.strikes, summary.lastUniqueWins, summary.lastSharedWins,
                                summary.lastActorSwings, summary.lastActorActs,
                                fmt(summary.lastUniqueWins.toDouble() / summary.strikes),
                                fmt(uniqueCI.first), fmt(uniqueCI.second), fmt(seconds))
                            timingProfiles.forEach { timing ->
                                val t6 = task6ByProfile.getValue(timing.name)
                                val durationCI = t6.pairedDurationDelta.interval95()
                                pacingSummary.row(players, ordinal, options.policy, options.scenario.cliName, timing.name,
                                    t6.aDuration.size, fmt(mean(t6.aDuration)), fmt(median(t6.aDuration)),
                                    fmt(percentile(t6.aDuration, .90)), fmt(percentile(t6.aDuration, .95)),
                                    fmt(mean(t6.bDuration)), fmt(median(t6.bDuration)),
                                    fmt(percentile(t6.bDuration, .90)), fmt(percentile(t6.bDuration, .95)),
                                    fmt(mean(t6.savings)), fmt(mean(t6.savingsPct)), fmt(durationCI.first), fmt(durationCI.second),
                                    fmt(mean(t6.aWait)), fmt(mean(t6.bWait)), fmt(median(t6.aWait)), fmt(median(t6.bWait)), fmt(percentile(t6.aWait,.95)), fmt(percentile(t6.bWait,.95)),
                                    fmt(mean(t6.aLongest)), fmt(mean(t6.bLongest)), fmt(median(t6.aLongest)), fmt(median(t6.bLongest)), fmt(percentile(t6.aLongest,.95)), fmt(percentile(t6.bLongest,.95)),
                                    fmt(mean(t6.aTerminal)), fmt(mean(t6.bTerminal)), fmt(median(t6.aTerminal)), fmt(median(t6.bTerminal)), fmt(percentile(t6.aTerminal,.95)), fmt(percentile(t6.bTerminal,.95)),
                                    fmt(mean(t6.aNoLegal)), fmt(mean(t6.bNoLegal)), fmt(mean(t6.aWatching)), fmt(mean(t6.bWatching)),
                                    fmt(mean(t6.aDecisionWindows)), fmt(mean(t6.bDecisionWindows)),
                                    fmt(mean(t6.aMeaningfulWindows)), fmt(mean(t6.bMeaningfulWindows)),
                                    fmt(mean(t6.aHandoffs)), fmt(mean(t6.bHandoffs)), fmt(mean(t6.aSupports)), fmt(mean(t6.bSupports)),
                                    fmt(mean(t6.aUnused)), fmt(mean(t6.bUnused)), fmt(mean(t6.breakEvenPlanning)))
                            }
                            println("COHORT players=$players battle=$ordinal n=${summary.pairedVpPerPlayer.count} " +
                                "B-A mean VP/player=${fmt(summary.pairedVpPerPlayer.mean)} " +
                                "CI95=[${fmt(ci.first)},${fmt(ci.second)}] " +
                                "last unique wins=${summary.lastUniqueWins}/${summary.strikes} " +
                                "seconds=${fmt(seconds)}")
                        } }
                    }
                }
            }
        }
        counterRows.close(); initialRows.close()
        } // main_plans.csv
        } // selection_summary.csv
        } // opening_selection.csv
        println("COMPLETE ${options.output} timeSeconds=${fmt((System.nanoTime() - startNanos) / 1e9)}")
        } finally {
            actorRows.close(); pacingWindows.close(); pacingBattles.close(); pacingPlayers.close(); pacingSensitivity.close(); pacingSummary.close()
        }
    } finally {
        app.close()
    }
}
