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
    val seed: Long = 2026103000L,
    val strategySeed: Long = 2026104000L,
    val wispSeed: Long = 2026105000L,
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
                "--samples", "--players", "--battles", "--policy", "--seed", "--strategy-seed", "--wisp-seed", "--out"
            ) }) { "Unknown option(s): ${values.keys}" }
            fun csv(key: String, default: List<Int>): List<Int> = values[key]
                ?.split(",")?.map { it.trim().toInt() } ?: default
            val options = BatchOptions(
                samples = values["--samples"]?.toInt() ?: 10,
                players = csv("--players", listOf(2, 3, 4)),
                battles = csv("--battles", listOf(1, 2, 3)),
                policy = values["--policy"] ?: "mechanical",
                seed = values["--seed"]?.toLong() ?: 2026103000L,
                strategySeed = values["--strategy-seed"]?.toLong() ?: 2026104000L,
                wispSeed = values["--wisp-seed"]?.toLong() ?: 2026105000L,
                output = Paths.get(values["--out"] ?: "output/experiments/battle-pacing/checkpoint3")
            )
            require(options.samples > 0)
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
            appendLine("experiment=battle-pacing-AB-checkpoint3")
            appendLine("status=exploratory; runtime failures ABORT; no significance claim for mismatched tactical policies")
            appendLine("rounds=3/2/2; pre-target Battles canonical A in BOTH arms")
            appendLine("resolveWisp=excluded identically in A and B")
            appendLine("samples_per_cohort=${options.samples}")
            appendLine("players=${options.players.joinToString(",")}")
            appendLine("battle_ordinals=${options.battles.joinToString(",")}")
            appendLine("policy=${options.policy}")
            appendLine("base_seed=${options.seed}")
            appendLine("base_strategy_seed=${options.strategySeed}")
            appendLine("base_wisp_seed=${options.wispSeed}")
            appendLine("eventBasedDieRolls=true; sharedPercentileDieRolls=true")
            appendLine("candidateHumanGuided=existing Human Main and B11/B12 Support scoring, NOT calibrated to new timing")
            appendLine("CI=normal paired-mean/within-battle sample; B last actor mean has cluster-by-Battle CI; descriptive, not causal")
            listOf(
                "data/Cultivation_Cards - Root.csv", "data/Cultivation_Cards - VF.csv",
                "data/Wisp_Cards.csv", "data/Turn_Cards.csv",
                "src/simulation/kotlin/dugsolutions/leaf/simulation/v35/experiment/battlepacing/EngagementOrderSelector.kt",
                "src/simulation/kotlin/dugsolutions/leaf/simulation/v35/experiment/battlepacing/PairedBattleOpeningReplay.kt",
                "src/simulation/kotlin/dugsolutions/leaf/simulation/v35/experiment/battlepacing/PairedBattleComparison.kt",
                "src/simulation/kotlin/dugsolutions/leaf/simulation/v35/experiment/battlepacing/ThreeEngagementBattle.kt",
                "src/simulation/kotlin/dugsolutions/leaf/simulation/v35/experiment/battlepacing/HumanGuidedEngagementPolicy.kt",
                "src/simulation/kotlin/dugsolutions/leaf/simulation/v35/experiment/battlepacing/BattlePacingStatistics.kt",
                "src/simulation/kotlin/dugsolutions/leaf/simulation/v35/experiment/battlepacing/BattlePacingABMain.kt",
                "doc/run_battle_pacing_ab.sh"
            ).forEach { relative ->
                val path = Paths.get(relative)
                if (Files.isRegularFile(path)) appendLine("sha256:$relative=${sha(path)}")
            }
        }
        Files.writeString(options.output.resolve("manifest.txt"), manifest)
        val startNanos = System.nanoTime()
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
                            val cohortStart = System.nanoTime()
                            repeat(options.samples) { sample ->
                                val mechanicalSeed = options.seed + sample * 1009L + players * 100003L + ordinal * 1000003L
                                val strategySeed = options.strategySeed + sample * 1013L + players * 100019L + ordinal * 1000033L
                                val deckSeed = options.wispSeed + sample * 1021L + players * 100043L + ordinal * 1000037L
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
                                    val result = compare.run(paired,
                                        if (options.policy == "mechanical") MechanicalEngagementPolicy
                                        else HumanGuidedEngagementPolicy(paired.candidateGame)
                                    )
                                    check(result.openingSha256 == paired.fingerprint.sha256)
                                    // Pure scoring projections; for Battle 3 they are actual
                                    // end-game totals, for Battles 1/2 merely snapshots.
                                    val aFinal = FinalScorer().score(paired.controlGame)
                                    val bFinal = FinalScorer().score(paired.candidateGame)
                                    summary.add(result, initialVP)
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
                                    result.candidate.engagements.forEachIndexed { index, engagement ->
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
                                } catch (failure: Throwable) {
                                    val message = "FAIL players=$players battle=$ordinal policy=${options.policy} " +
                                        "sample=$sample seed=$mechanicalSeed strategySeed=$strategySeed wispSeed=$deckSeed " +
                                        "reason=${failure.stackTraceToString()}"
                                    Files.writeString(options.output.resolve("FAILURE.txt"), message)
                                    System.err.println(message)
                                    throw failure
                                }
                                if ((sample + 1) % 100 == 0) println("COMPLETE $players-player Battle=$ordinal ${sample + 1}/${options.samples}")
                            }
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
        println("COMPLETE ${options.output} timeSeconds=${fmt((System.nanoTime() - startNanos) / 1e9)}")
    } finally {
        app.close()
    }
}
