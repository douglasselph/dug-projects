package dugsolutions.leaf.simulation.v35.learning.interaction

import dugsolutions.leaf.simulation.v35.analysis.GameSummary
import dugsolutions.leaf.simulation.v35.analysis.GameSummaryExtractor
import dugsolutions.leaf.simulation.v35.experiment.diagnostic.SimulationRunContext
import dugsolutions.leaf.simulation.v35.experiment.diagnostic.withSimulationFailureDiagnostics
import dugsolutions.leaf.simulation.v35.experiment.plant.PlantExperimentResearchConfig
import dugsolutions.leaf.simulation.v35.experiment.plant.resolveResearchGroveForSample
import dugsolutions.leaf.simulation.v35.experiment.round.RoundExperimentResearchConfig
import dugsolutions.leaf.simulation.v35.learning.buy.parseRoundSetup
import dugsolutions.leaf.simulation.v35.learning.cultivation.affectedSeat
import dugsolutions.leaf.simulation.v35.learning.cultivation.loadCultivationResearchCards
import dugsolutions.leaf.v35.chronicle.domain.*
import dugsolutions.leaf.v35.common.FirstGameDefault
import dugsolutions.leaf.v35.di.appModules
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.game.GameConfig
import dugsolutions.leaf.v35.game.GameRunner
import dugsolutions.leaf.v35.game.PlayerDecisionFactory
import dugsolutions.leaf.v35.game.di.GameFactory
import dugsolutions.leaf.v35.plant.GrovePlantCode
import dugsolutions.leaf.v35.plant.PlantCardManager
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.player.decision.baseline.HumanBaselineDecisionDirector
import dugsolutions.leaf.v35.player.decision.learned.battle.LearnedBattleSupportCatalog
import dugsolutions.leaf.v35.player.decision.learned.battle.LearnedBattleSupportPolicy
import dugsolutions.leaf.v35.player.decision.learned.battle.LearnedBattleSupportWeights
import dugsolutions.leaf.v35.player.decision.learned.buy.LearnedBuyCardCatalog
import dugsolutions.leaf.v35.player.decision.learned.buy.LearnedBuyStrategy
import dugsolutions.leaf.v35.player.decision.learned.buy.LearnedBuyWeights
import dugsolutions.leaf.v35.player.decision.learned.cultivation.LearnedCultivationMainCatalog
import dugsolutions.leaf.v35.player.decision.learned.cultivation.LearnedCultivationMainPolicy
import dugsolutions.leaf.v35.player.decision.learned.cultivation.LearnedCultivationMainWeights
import dugsolutions.leaf.v35.player.decision.learned.cultivation.support.LearnedCultivationSupportCatalog
import dugsolutions.leaf.v35.player.decision.learned.cultivation.support.LearnedCultivationSupportPolicy
import dugsolutions.leaf.v35.player.decision.learned.cultivation.support.LearnedCultivationSupportWeights
import dugsolutions.leaf.v35.player.decision.random.StrategyRandomizer
import dugsolutions.leaf.v35.player.decision.trace.DecisionReasoningSink
import dugsolutions.leaf.v35.round.RoundCardManager
import dugsolutions.leaf.v35.tokens.SharedTokenResource
import org.koin.dsl.koinApplication
import java.nio.file.Path
import java.nio.file.Paths

private data class CompletedInteractionGame(val summary: GameSummary, val entries: List<GameEntry>)

data class PolicyInteractionOptions(
    val games: Int,
    val seed: Long,
    val strategySeed: Long,
    val grovePattern: String?,
    val groveSeed: Long,
    val plantOverrides: Path?,
    val roundOverrides: Path?,
    val players: Int,
    val roundLabel: String,
    val buyPolicy: String,
    val buyWeights: Path,
    val cultivationMainPolicy: String,
    val cultivationMainWeights: Path,
    val cultivationSupportPolicy: String,
    val cultivationSupportWeights: Path,
    val battleSupportPolicy: String,
    val battleSupportWeights: Path
) {
    val roundSetup = parseRoundSetup(roundLabel)

    companion object {
        fun parse(args: List<String>): PolicyInteractionOptions {
            var games = 300
            var seed = 301000L
            var strategySeed = 302000L
            var grovePattern: String? = GrovePlantCode.RANDOM_PATTERN
            var groveSeed = 303000L
            var plantOverrides: Path? = null
            var roundOverrides: Path? = null
            var players = 4
            var roundLabel = "3/2/2"
            var buyPolicy = "human"
            var buyWeights = Paths.get("output/ai/buy-policy-v1-trained.weights")
            var cultivationMainPolicy = "human"
            var cultivationMainWeights = Paths.get("output/ai/cultivation-main-policy-v1-trained.weights")
            var cultivationSupportPolicy = "human"
            var cultivationSupportWeights = Paths.get("output/ai/cultivation-support-policy-v1-trained.weights")
            var battleSupportPolicy = "human"
            var battleSupportWeights = Paths.get("output/ai/battle-support-policy-v1-trained.weights")
            var i = 0
            fun value(arg: String): String = if ('=' in arg) arg.substringAfter('=') else args[++i]
            while (i < args.size) {
                val arg = args[i]
                when {
                    arg.startsWith("--games") || arg.startsWith("--samples") -> games = value(arg).toInt()
                    arg.startsWith("--seed") -> seed = value(arg).toLong()
                    arg.startsWith("--strategy-seed") -> strategySeed = value(arg).toLong()
                    arg.startsWith("--grove-seed") -> groveSeed = value(arg).toLong()
                    arg.startsWith("--plant-overrides") -> plantOverrides = Paths.get(value(arg))
                    arg.startsWith("--round-overrides") -> roundOverrides = Paths.get(value(arg))
                    arg.startsWith("--players") -> players = value(arg).toInt()
                    arg.startsWith("--rounds") -> roundLabel = value(arg)
                    arg.startsWith("--buy-policy") -> buyPolicy = value(arg).lowercase()
                    arg.startsWith("--buy-weights") -> buyWeights = Paths.get(value(arg))
                    arg.startsWith("--cultivation-main-policy") -> cultivationMainPolicy = value(arg).lowercase()
                    arg.startsWith("--cultivation-main-weights") -> cultivationMainWeights = Paths.get(value(arg))
                    arg.startsWith("--cultivation-support-policy") -> cultivationSupportPolicy = value(arg).lowercase()
                    arg.startsWith("--cultivation-support-weights") -> cultivationSupportWeights = Paths.get(value(arg))
                    arg.startsWith("--battle-support-policy") -> battleSupportPolicy = value(arg).lowercase()
                    arg.startsWith("--battle-support-weights") -> battleSupportWeights = Paths.get(value(arg))
                    arg.startsWith("--grove") -> grovePattern = GrovePlantCode.validate(value(arg))
                    arg == "--random-grove" -> grovePattern = GrovePlantCode.RANDOM_PATTERN
                    arg == "--first-game-grove" -> grovePattern = null
                    arg == "--help" -> { usage(); kotlin.system.exitProcess(0) }
                    else -> error("Unknown argument: $arg")
                }
                i++
            }
            require(games > 0) { "--samples/--games must be positive" }
            require(players in 2..4) { "--players must be 2, 3, or 4" }
            require(buyPolicy in setOf("human", "learned")) { "--buy-policy must be human or learned" }
            require(cultivationMainPolicy in setOf("human", "learned")) { "--cultivation-main-policy must be human or learned" }
            require(cultivationSupportPolicy in setOf("human", "learned")) { "--cultivation-support-policy must be human or learned" }
            require(battleSupportPolicy in setOf("human", "learned")) { "--battle-support-policy must be human or learned" }
            return PolicyInteractionOptions(games, seed, strategySeed, grovePattern, groveSeed, plantOverrides, roundOverrides, players, roundLabel,
                buyPolicy, buyWeights, cultivationMainPolicy, cultivationMainWeights, cultivationSupportPolicy, cultivationSupportWeights, battleSupportPolicy, battleSupportWeights)
        }

        private fun usage() {
            println("evaluate_policy_interactions [--samples N] [--seed N] [--strategy-seed N] [--grove-seed N] [--random-grove|--first-game-grove|--grove CODE] [--plant-overrides PATH] [--round-overrides PATH] [--players 2|3|4] [--rounds PATTERN] --buy-policy human|learned [--buy-weights PATH] --cultivation-main-policy human|learned [--cultivation-main-weights PATH] --cultivation-support-policy human|learned [--cultivation-support-weights PATH] --battle-support-policy human|learned [--battle-support-weights PATH]")
        }
    }
}

fun main(args: Array<String>) {
    val o = PolicyInteractionOptions.parse(args.toList())
    val app = koinApplication { modules(appModules) }
    try {
        val koin = app.koin
        loadCultivationResearchCards(koin.get(), koin.get(), koin.get(), koin.get(), koin.get(), koin.get())
        val plantManager = koin.get<PlantCardManager>()
        val roundManager = koin.get<RoundCardManager>()
        val plants = plantManager.getAllCards().cards
        val rounds = roundManager.getAllCards().cards
        val defaults = FirstGameDefault.PLANT_NAMES.map { requireNotNull(plantManager.getCard(it)) }
        val plantExp = PlantExperimentResearchConfig.resolve(o.plantOverrides, plants)
        val roundExp = RoundExperimentResearchConfig.resolve(o.roundOverrides, rounds)
        val effectiveCosts = plantExp.effectiveCosts(plants)

        val buyWeights = if (o.buyPolicy == "learned") LearnedBuyCardCatalog.prepare(LearnedBuyWeights.load(o.buyWeights), plants, effectiveCosts) else null
        val cultivationWeights = if (o.cultivationMainPolicy == "learned") LearnedCultivationMainCatalog.prepare(LearnedCultivationMainWeights.load(o.cultivationMainWeights), plants, effectiveCosts) else null
        val cultivationSupportWeights = if (o.cultivationSupportPolicy == "learned") LearnedCultivationSupportCatalog.prepare(LearnedCultivationSupportWeights.load(o.cultivationSupportWeights), plants) else null
        val supportWeights = if (o.battleSupportPolicy == "learned") LearnedBattleSupportCatalog.prepare(LearnedBattleSupportWeights.load(o.battleSupportWeights), plants) else null
        if (buyWeights != null) LearnedBuyCardCatalog.validateCurrentSchema(buyWeights, plants)
        if (cultivationWeights != null) LearnedCultivationMainCatalog.validateCurrentSchema(cultivationWeights, plants, effectiveCosts)
        if (cultivationSupportWeights != null) LearnedCultivationSupportCatalog.validateCurrentSchema(cultivationSupportWeights, plants)
        if (supportWeights != null) LearnedBattleSupportCatalog.validateCurrentSchema(supportWeights, plants)

        val policyFactory = modularFactory(buyWeights, cultivationWeights, cultivationSupportWeights, supportWeights)
        val gameFactory = koin.get<GameFactory>()
        val runner = koin.get<GameRunner>()
        val acc = InteractionAccumulator()

        println("Leaf & Let Die — Modular Policy Interaction Evaluation")
        println("samples=${o.games}; players=${o.players}; rounds=${o.roundLabel}")
        println("Buy=${o.buyPolicy}; CultivationMain=${o.cultivationMainPolicy}; CultivationSupport=${o.cultivationSupportPolicy}; BattleSupport=${o.battleSupportPolicy}; BattleMain=human")
        println("mechanical seeds=${o.seed}..${o.seed + o.games - 1}; strategy seeds=${o.strategySeed}..${o.strategySeed + o.games - 1}; groveSeed=${o.groveSeed}")
        println("Plant baseline=${o.plantOverrides ?: "canonical"}; Round override=${o.roundOverrides ?: "canonical"}")
        if (plantExp.isActive) { println(); println(plantExp.render(plants)) }
        if (roundExp.isActive) { println(); println(roundExp.render(rounds)) }
        println()

        repeat(o.games) { sample ->
            val seat = affectedSeat(sample, o.players)
            val grove = resolveResearchGroveForSample(o.grovePattern, o.groveSeed, sample, plantManager, defaults, plants, plantExp.values)
            val factories = List(o.players) { index -> if (index == seat) policyFactory else PlayerDecisionFactory.humanBaseline() }
            val game = gameFactory(GameConfig(
                selectedPlantCards = grove,
                playerDecisionFactories = factories,
                roundSetup = o.roundSetup,
                seed = o.seed + sample,
                strategySeed = o.strategySeed + sample,
                plantValues = plantExp.values,
                roundValues = roundExp.values
            ))
            val result = withSimulationFailureDiagnostics(
                game,
                SimulationRunContext("evaluate_policy_interactions", sample, "B${o.buyPolicy.first()}-C${o.cultivationMainPolicy.first()}-CS${o.cultivationSupportPolicy.first()}-S${o.battleSupportPolicy.first()}", seat, o.seed+sample, o.strategySeed+sample, GrovePlantCode.describe(grove), o.roundLabel)
            ) { runner.run(game) }
            acc.add(CompletedInteractionGame(GameSummaryExtractor.extract(game, result), game.chronicle.entries.toList()), seat)
        }
        printReport(acc, o.games)
    } finally { app.close() }
}

private fun modularFactory(
    buyWeights: LearnedBuyWeights?,
    cultivationWeights: LearnedCultivationMainWeights?,
    cultivationSupportWeights: LearnedCultivationSupportWeights?,
    supportWeights: LearnedBattleSupportWeights?
): PlayerDecisionFactory = object : PlayerDecisionFactory {
    override fun create() = create(StrategyRandomizer.create(), DecisionReasoningSink.NONE)
    override fun create(strategyRandomizer: StrategyRandomizer) = create(strategyRandomizer, DecisionReasoningSink.NONE)
    override fun create(strategyRandomizer: StrategyRandomizer, reasoningSink: DecisionReasoningSink) =
        HumanBaselineDecisionDirector(strategyRandomizer = strategyRandomizer, reasoningSink = reasoningSink).createDirector().let { baseline ->
            baseline.copy(
                buy = buyWeights?.let { LearnedBuyStrategy(it, baseline.buy) } ?: baseline.buy,
                cultivationMain = cultivationWeights?.let { LearnedCultivationMainPolicy(it) } ?: baseline.cultivationMain,
                cultivationSupport = cultivationSupportWeights?.let { LearnedCultivationSupportPolicy(it) } ?: baseline.cultivationSupport,
                battleSupport = supportWeights?.let { LearnedBattleSupportPolicy(it) } ?: baseline.battleSupport
            )
        }
}

private class InteractionAccumulator {
    var win = 0.0
    var totalVp = 0L; var plantVp = 0L; var battleVp = 0L; var wispVp = 0L; var otherVp = 0L; var wounds = 0L
    var plantPurchases = 0L; var diePurchases = 0L
    val plantPurchaseByName = sortedMapOf<String, Long>(); val diePurchaseByName = sortedMapOf<String, Long>()
    val cultivationActions = linkedMapOf<MainActionKind, Long>()
    val supportActions = linkedMapOf<SupportActionKind, Long>()
    val cultivationSupportActions = linkedMapOf<SupportActionKind, Long>()
    var cultivationSupportPasses = 0L
    var cultivationSupportWindows = 0L
    var battlePlantActivations = 0L; var cultivationPlantActivations = 0L; var strikeWins = 0L
    var sunlightGainOpp = 0L; var sunlightGainUses = 0L
    var sunlightUses = 0L; var sunlightExtraMains = 0L; var sunlightImmediate = 0L; var sunlightDecisive = 0L; var sunlightWoundDecisive = 0L
    var finalDice = 0L; var finalDicePower = 0L
    val roundEffectUses = sortedMapOf<String, Long>()
    val tokenUses = SharedTokenResource.entries.associateWith { 0L }.toMutableMap()
    val tokenGains = SharedTokenResource.entries.associateWith { 0L }.toMutableMap()
    val tokenZeroGames = SharedTokenResource.entries.associateWith { 0L }.toMutableMap()

    fun add(game: CompletedInteractionGame, seat: Int) {
        val p = game.summary.players.single { it.seat == seat }
        win += p.winShare; totalVp += p.totalVp; plantVp += p.plantVp; battleVp += p.battleStrikeVp; wispVp += p.unplayedWispVp
        otherVp += (p.totalVp - p.plantVp - p.battleStrikeVp - p.unplayedWispVp)
        wounds += p.woundsTaken; finalDice += p.finalDiceCount; finalDicePower += p.finalDicePower
        sunlightUses += p.sunlightSupportUses; sunlightExtraMains += p.sunlightExtraMainActions; sunlightImmediate += p.sunlightImmediateStrikeContributions
        sunlightDecisive += p.sunlightWinnerDecisiveContributions; sunlightWoundDecisive += p.sunlightWoundDecisiveContributions

        game.entries.filterIsInstance<GameEntry.Purchase>().filter { it.playerId == p.playerId }.forEach { e ->
            when(e.kind) {
                PurchaseKind.PLANT -> { plantPurchases++; plantPurchaseByName[e.itemName] = (plantPurchaseByName[e.itemName] ?: 0) + 1 }
                PurchaseKind.DIE -> { diePurchases++; diePurchaseByName[e.itemName] = (diePurchaseByName[e.itemName] ?: 0) + 1 }
            }
        }
        game.entries.filterIsInstance<GameEntry.MainAction>().filter { it.playerId == p.playerId && it.phase == ChroniclePhase.CULTIVATION }.forEach { e -> cultivationActions[e.action] = (cultivationActions[e.action] ?: 0) + 1 }
        game.entries.filterIsInstance<GameEntry.SupportAction>().filter { it.playerId == p.playerId && it.phase == ChroniclePhase.BATTLE }.forEach { e -> supportActions[e.action] = (supportActions[e.action] ?: 0) + 1 }
        game.entries.filterIsInstance<GameEntry.SupportAction>().filter { it.playerId == p.playerId && it.phase == ChroniclePhase.CULTIVATION }.forEach { e -> cultivationSupportActions[e.action] = (cultivationSupportActions[e.action] ?: 0) + 1 }
        game.entries.filterIsInstance<GameEntry.CultivationSupportDecision>().filter { it.playerId == p.playerId }.forEach { e -> cultivationSupportWindows++; if (e.selectedActionId == null) cultivationSupportPasses++ }
        game.entries.filterIsInstance<GameEntry.EffectResolved>().filter { it.playerId == p.playerId }.forEach { e ->
            if (e.sourceKind == EffectSourceKind.PLANT) {
                if (e.phase == ChroniclePhase.CULTIVATION) cultivationPlantActivations++ else if (e.phase == ChroniclePhase.BATTLE) battlePlantActivations++
            }
            if (e.sourceKind == EffectSourceKind.ROUND && e.phase == ChroniclePhase.CULTIVATION) roundEffectUses[e.effect.name] = (roundEffectUses[e.effect.name] ?: 0) + 1
        }
        game.entries.filterIsInstance<GameEntry.RoundEffectChoice>().filter { it.playerId == p.playerId && it.phase == ChroniclePhase.CULTIVATION }.forEach { e ->
            val firstSun = e.firstEffect == GameEffect.GAIN_SUNLIGHT_TOKEN && e.firstExecutable
            val secondSun = e.secondEffect == GameEffect.GAIN_SUNLIGHT_TOKEN && e.secondExecutable
            if (firstSun || secondSun) {
                sunlightGainOpp++
                if ((firstSun && e.selectedMainAction == MainActionKind.ROUND_EFFECT_1) || (secondSun && e.selectedMainAction == MainActionKind.ROUND_EFFECT_2)) sunlightGainUses++
            }
        }
        game.entries.filterIsInstance<GameEntry.StrikeResolved>().forEach { e -> if (p.playerId in e.winnerIds) strikeWins++ }
        game.summary.sharedTokenEconomy.forEach { e ->
            tokenUses[e.resource] = (tokenUses[e.resource] ?: 0) + e.spendsOrUses
            tokenGains[e.resource] = (tokenGains[e.resource] ?: 0) + e.successfulGains
            if (e.reachedZero) tokenZeroGames[e.resource] = (tokenZeroGames[e.resource] ?: 0) + 1
        }
    }
}

private fun printReport(a: InteractionAccumulator, n: Int) {
    fun avg(v: Long) = v.toDouble()/n
    fun fmt(v: Double) = "%.3f".format(v)
    fun pct(v: Double) = "%.2f%%".format(v*100)
    println("OUTCOME")
    println("  win share=${pct(a.win/n)} total VP=${fmt(avg(a.totalVp))} Plant VP=${fmt(avg(a.plantVp))} Battle VP=${fmt(avg(a.battleVp))} Wisp VP=${fmt(avg(a.wispVp))} other VP=${fmt(avg(a.otherVp))} wounds=${fmt(avg(a.wounds))}")
    println("  final dice/game=${fmt(avg(a.finalDice))}; final dice power/game=${fmt(avg(a.finalDicePower))}")
    println()
    println("ECONOMY")
    println("  Plant purchases/game=${fmt(avg(a.plantPurchases))}; dice purchases/game=${fmt(avg(a.diePurchases))}")
    println("  Plant purchase distribution:")
    a.plantPurchaseByName.toList().sortedByDescending { it.second }.forEach { (k,v) -> println("    $k: total=$v per-game=${fmt(v.toDouble()/n)}") }
    println("  Die purchase distribution:")
    a.diePurchaseByName.toList().sortedByDescending { it.second }.forEach { (k,v) -> println("    $k: total=$v per-game=${fmt(v.toDouble()/n)}") }
    println("  Token economy (whole-table finite supply; affected player policy can change table use):")
    SharedTokenResource.entries.forEach { r -> println("    ${r.name}: gains/game=${fmt((a.tokenGains[r]?:0).toDouble()/n)} uses/game=${fmt((a.tokenUses[r]?:0).toDouble()/n)} reached-zero=${a.tokenZeroGames[r]?:0}/$n") }
    println()
    println("CULTIVATION")
    MainActionKind.entries.forEach { k -> println("  ${k.name}: ${fmt((a.cultivationActions[k]?:0).toDouble()/n)}/game") }
    println("  Plant activations/game=${fmt(avg(a.cultivationPlantActivations))}")
    println("  Optional Support decision windows/game=${fmt(avg(a.cultivationSupportWindows))}; PASS/game=${fmt(avg(a.cultivationSupportPasses))}")
    println("  Cultivation Support use/game:")
    SupportActionKind.entries.forEach { k -> println("    ${k.name}: ${fmt((a.cultivationSupportActions[k]?:0).toDouble()/n)}") }
    println("  Round-effect resolutions/game:")
    a.roundEffectUses.toList().sortedByDescending { it.second }.forEach { (k,v) -> println("    $k: ${fmt(v.toDouble()/n)}") }
    println("  Gain Sunlight take rate=${pct(if(a.sunlightGainOpp==0L) 0.0 else a.sunlightGainUses.toDouble()/a.sunlightGainOpp)} (${a.sunlightGainUses}/${a.sunlightGainOpp} executable opportunities)")
    println()
    println("BATTLE")
    println("  Support action distribution/game:")
    SupportActionKind.entries.forEach { k -> println("    ${k.name}: ${fmt((a.supportActions[k]?:0).toDouble()/n)}") }
    println("  Sunlight uses/game=${fmt(avg(a.sunlightUses))}; extra Main Actions/game=${fmt(avg(a.sunlightExtraMains))}")
    println("  Battle Plant activations/game=${fmt(avg(a.battlePlantActivations))}; Strike wins/game=${fmt(avg(a.strikeWins))}")
    println("  Sunlight immediate contributions/game=${fmt(avg(a.sunlightImmediate))}; winner-decisive/game=${fmt(avg(a.sunlightDecisive))}; wound-decisive/game=${fmt(avg(a.sunlightWoundDecisive))}")
    println("  Battle VP/game=${fmt(avg(a.battleVp))}; wounds/game=${fmt(avg(a.wounds))}")
}
