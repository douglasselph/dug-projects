package dugsolutions.leaf.simulation.v35.learning.battle.main

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
import dugsolutions.leaf.simulation.v35.learning.plant.effectivePlantEffectCatalogCards
import dugsolutions.leaf.v35.chronicle.domain.*
import dugsolutions.leaf.v35.common.FirstGameDefault
import dugsolutions.leaf.v35.di.appModules
import dugsolutions.leaf.v35.game.GameConfig
import dugsolutions.leaf.v35.game.GameRunner
import dugsolutions.leaf.v35.game.PlayerDecisionFactory
import dugsolutions.leaf.v35.game.di.GameFactory
import dugsolutions.leaf.v35.plant.GrovePlantCode
import dugsolutions.leaf.v35.plant.PlantCardManager
import dugsolutions.leaf.v35.player.decision.baseline.HumanBaselineDecisionDirector
import dugsolutions.leaf.v35.player.decision.learned.battle.LearnedBattleSupportCatalog
import dugsolutions.leaf.v35.player.decision.learned.battle.LearnedBattleSupportPolicy
import dugsolutions.leaf.v35.player.decision.learned.battle.LearnedBattleSupportWeights
import dugsolutions.leaf.v35.player.decision.learned.battle.main.*
import dugsolutions.leaf.v35.player.decision.learned.buy.LearnedBuyCardCatalog
import dugsolutions.leaf.v35.player.decision.learned.buy.LearnedBuyStrategy
import dugsolutions.leaf.v35.player.decision.learned.buy.LearnedBuyWeights
import dugsolutions.leaf.v35.player.decision.learned.cultivation.LearnedCultivationMainCatalog
import dugsolutions.leaf.v35.player.decision.learned.cultivation.LearnedCultivationMainPolicy
import dugsolutions.leaf.v35.player.decision.learned.cultivation.LearnedCultivationMainWeights
import dugsolutions.leaf.v35.player.decision.learned.cultivation.support.LearnedCultivationSupportCatalog
import dugsolutions.leaf.v35.player.decision.learned.cultivation.support.LearnedCultivationSupportPolicy
import dugsolutions.leaf.v35.player.decision.learned.cultivation.support.LearnedCultivationSupportWeights
import dugsolutions.leaf.v35.player.decision.learned.plant.LearnedPlantEffectCatalog
import dugsolutions.leaf.v35.player.decision.learned.plant.LearnedPlantEffectPolicy
import dugsolutions.leaf.v35.player.decision.learned.plant.LearnedPlantEffectWeights
import dugsolutions.leaf.v35.player.decision.learned.wisp.LearnedWispPlayCatalog
import dugsolutions.leaf.v35.player.decision.learned.wisp.LearnedWispPlayPolicy
import dugsolutions.leaf.v35.player.decision.learned.wisp.LearnedWispPlayWeights
import dugsolutions.leaf.v35.player.decision.random.StrategyRandomizer
import dugsolutions.leaf.v35.player.decision.trace.DecisionReasoningSink
import dugsolutions.leaf.v35.round.RoundCardManager
import dugsolutions.leaf.v35.wisp.WispCardManager
import org.koin.dsl.koinApplication
import java.nio.file.Path
import java.nio.file.Paths

private data class CompletedBattleMainEvalGame(val summary: GameSummary, val entries: List<GameEntry>)

fun main(args: Array<String>) {
    val o = BattleMainEvalOptions.parse(args.toList())
    val app = koinApplication { modules(appModules) }
    try {
        val koin = app.koin
        loadCultivationResearchCards(koin.get(), koin.get(), koin.get(), koin.get(), koin.get(), koin.get())
        val plantManager = koin.get<PlantCardManager>()
        val roundManager = koin.get<RoundCardManager>()
        val wispManager = koin.get<WispCardManager>()
        val plants = plantManager.getAllCards().cards
        val rounds = roundManager.getAllCards().cards
        val wisps = wispManager.getAllCards().cards
        val defaults = FirstGameDefault.PLANT_NAMES.map { requireNotNull(plantManager.getCard(it)) }
        val plantExp = PlantExperimentResearchConfig.resolve(o.plantOverrides, plants)
        val roundExp = RoundExperimentResearchConfig.resolve(o.roundOverrides, rounds)
        val effectivePlants = effectivePlantEffectCatalogCards(plants, plantExp.values)
        val weights = LearnedBattleMainCatalog.prepare(LearnedBattleMainWeights.load(o.weights), effectivePlants)
        LearnedBattleMainCatalog.validateCurrentSchema(weights, effectivePlants)
        val companions = FrozenBattleMainCompanions(
            buy = if (o.buyPolicy == "learned") LearnedBuyCardCatalog.prepare(LearnedBuyWeights.load(o.buyWeights), plants, plantExp.effectiveCosts(plants)) else null,
            cultivationMain = if (o.cultivationMainPolicy == "learned") LearnedCultivationMainCatalog.prepare(LearnedCultivationMainWeights.load(o.cultivationMainWeights), plants, plantExp.effectiveCosts(plants)) else null,
            cultivationSupport = if (o.cultivationSupportPolicy == "learned") LearnedCultivationSupportCatalog.prepare(LearnedCultivationSupportWeights.load(o.cultivationSupportWeights), plants) else null,
            wisp = if (o.wispPolicy == "learned") LearnedWispPlayCatalog.prepare(LearnedWispPlayWeights.load(o.wispWeights), wisps) else null,
            plantEffect = if (o.plantEffectPolicy == "learned") LearnedPlantEffectCatalog.prepare(LearnedPlantEffectWeights.load(o.plantEffectWeights), effectivePlants) else null,
            battleSupport = if (o.battleSupportPolicy == "learned") LearnedBattleSupportCatalog.prepare(LearnedBattleSupportWeights.load(o.battleSupportWeights), plants) else null
        )
        val factory = koin.get<GameFactory>()
        val runner = koin.get<GameRunner>()
        val control = Accumulator()
        val learned = Accumulator()

        println("Leaf & Let Die — Learned Battle Main Held-Out Evaluation")
        println("weights=${o.weights}; matched samples=${o.games}; players=${o.players}; rounds=${o.roundLabel}")
        println("Intervention changes Battle Main only; requested companion learned policies stay frozen in BOTH control and intervention.")
        println("companions: Buy=${o.buyPolicy} CultMain=${o.cultivationMainPolicy} CultSupport=${o.cultivationSupportPolicy} Wisp=${o.wispPolicy} PlantEffect=${o.plantEffectPolicy} BattleSupport=${o.battleSupportPolicy}")

        repeat(o.games) { sample ->
            val seat = affectedSeat(sample, o.players)
            val grove = resolveResearchGroveForSample(o.grovePattern, o.groveSeed, sample, plantManager, defaults, plants, plantExp.values)
            val humanAffected = companionFactory(companions)
            val learnedAffected = learnedBattleMainFactory(weights, companions)
            val controlFactories = List(o.players) { if (it == seat) humanAffected else PlayerDecisionFactory.humanBaseline() }
            val learnedFactories = List(o.players) { if (it == seat) learnedAffected else PlayerDecisionFactory.humanBaseline() }
            control.add(runOne(factory, runner, grove, controlFactories, o, sample, seat, "CONTROL", plantExp, roundExp), seat)
            learned.add(runOne(factory, runner, grove, learnedFactories, o, sample, seat, "LEARNED_BATTLE_MAIN", plantExp, roundExp), seat)
        }
        printReport("CONTROL — Human Battle Main", control, o.games)
        println()
        printReport("LEARNED — Learned Battle Main", learned, o.games)
        println()
        println("DELTA LEARNED - CONTROL")
        println("  win share: ${pp(learned.win / o.games - control.win / o.games)}")
        println("  total VP: ${fmt(learned.totalVp / o.games - control.totalVp / o.games)}")
        println("  Battle VP: ${fmt(learned.battleVp / o.games - control.battleVp / o.games)}")
    } finally { app.close() }
}

private fun companionFactory(c: FrozenBattleMainCompanions): PlayerDecisionFactory = object : PlayerDecisionFactory {
    override fun create() = create(StrategyRandomizer.create(), DecisionReasoningSink.NONE)
    override fun create(strategyRandomizer: StrategyRandomizer) = create(strategyRandomizer, DecisionReasoningSink.NONE)
    override fun create(strategyRandomizer: StrategyRandomizer, reasoningSink: DecisionReasoningSink) =
        HumanBaselineDecisionDirector(strategyRandomizer = strategyRandomizer, reasoningSink = reasoningSink).createDirector().let { baseline ->
            baseline.copy(
                buy = c.buy?.let { LearnedBuyStrategy(it, baseline.buy) } ?: baseline.buy,
                cultivationMain = c.cultivationMain?.let { LearnedCultivationMainPolicy(it) } ?: baseline.cultivationMain,
                cultivationSupport = c.cultivationSupport?.let { LearnedCultivationSupportPolicy(it) } ?: baseline.cultivationSupport,
                wispPlay = c.wisp?.let { LearnedWispPlayPolicy(it) } ?: baseline.wispPlay,
                plantEffect = c.plantEffect?.let { LearnedPlantEffectPolicy(it) } ?: baseline.plantEffect,
                battleSupport = c.battleSupport?.let { LearnedBattleSupportPolicy(it) } ?: baseline.battleSupport
            )
        }
}

private fun runOne(
    factory: GameFactory, runner: GameRunner, grove: List<dugsolutions.leaf.v35.plant.domain.PlantCard>, decisions: List<PlayerDecisionFactory>,
    o: BattleMainEvalOptions, sample: Int, seat: Int, variant: String,
    plantExp: PlantExperimentResearchConfig, roundExp: RoundExperimentResearchConfig
): CompletedBattleMainEvalGame {
    val game = factory(GameConfig(selectedPlantCards = grove, playerDecisionFactories = decisions, roundSetup = o.roundSetup, seed = o.seed + sample, strategySeed = o.strategySeed + sample, plantValues = plantExp.values, roundValues = roundExp.values))
    val result = withSimulationFailureDiagnostics(game, SimulationRunContext("evaluate_battle_main_policy", sample, variant, seat, o.seed + sample, o.strategySeed + sample, GrovePlantCode.describe(grove), o.roundLabel)) { runner.run(game) }
    return CompletedBattleMainEvalGame(GameSummaryExtractor.extract(game, result), game.chronicle.entries.toList())
}

private class Accumulator {
    var win = 0.0; var totalVp = 0.0; var plantVp = 0.0; var battleVp = 0.0; var wispVp = 0.0; var wounds = 0.0
    var plantPurchases = 0; var diePurchases = 0; var strikeWins = 0; var winnerDecisiveRows = 0; var winnerDecisiveContributions = 0; var woundDecisiveContributions = 0
    val first = linkedMapOf<MainActionKind, Int>(); val final = linkedMapOf<MainActionKind, Int>()
    var sunlightMains = 0; var battlePlantActivations = 0; var battleRoundEffects = 0; var battleDraws = 0
    var supports = 0
    fun add(g: CompletedBattleMainEvalGame, seat: Int) {
        val p = g.summary.players.single { it.seat == seat }
        win += p.winShare; totalVp += p.totalVp; plantVp += p.plantVp; battleVp += p.battleStrikeVp; wispVp += p.unplayedWispVp; wounds += p.woundsTaken
        g.entries.filterIsInstance<GameEntry.Purchase>().filter { it.playerId == p.playerId }.forEach { if (it.kind == PurchaseKind.PLANT) plantPurchases++ else diePurchases++ }
        g.entries.filterIsInstance<GameEntry.MainAction>().filter { it.playerId == p.playerId && it.phase == ChroniclePhase.BATTLE }.forEach { e ->
            when (e.battleStage) {
                BattleMainStage.FIRST -> first[e.action] = (first[e.action] ?: 0) + 1
                BattleMainStage.FINAL -> final[e.action] = (final[e.action] ?: 0) + 1
                BattleMainStage.SUNLIGHT -> sunlightMains++
                null -> Unit
            }
            when (e.action) {
                MainActionKind.DRAW -> battleDraws++
                MainActionKind.ACTIVATE_PLANT -> battlePlantActivations++
                MainActionKind.ROUND_EFFECT_1, MainActionKind.ROUND_EFFECT_2 -> battleRoundEffects++
            }
        }
        supports += g.entries.filterIsInstance<GameEntry.SupportAction>().count { it.playerId == p.playerId && it.phase == ChroniclePhase.BATTLE }
        g.entries.filterIsInstance<GameEntry.StrikeResolved>().forEach { e ->
            if (p.playerId in e.winnerIds) strikeWins++
            val own = e.contributionLedger?.contributions.orEmpty().filter { it.playerId == p.playerId }
            val winnerDecisive = own.count { it.individuallyWinnerDecisive }
            val woundDecisive = own.count { it.individuallyWoundDecisive }
            winnerDecisiveContributions += winnerDecisive
            woundDecisiveContributions += woundDecisive
            if (winnerDecisive > 0) winnerDecisiveRows++
        }
    }
}

private fun printReport(label: String, a: Accumulator, n: Int) {
    println(label)
    println("  win share: ${pp(a.win / n)}")
    println("  total VP=${fmt(a.totalVp / n)} Plant VP=${fmt(a.plantVp / n)} Battle VP=${fmt(a.battleVp / n)} Wisp VP=${fmt(a.wispVp / n)} wounds=${fmt(a.wounds / n)}")
    println("  FIRST Main/game: ${MainActionKind.entries.joinToString { "${it.name}=${fmt((a.first[it] ?: 0).toDouble() / n)}" }}")
    println("  FINAL Main/game: ${MainActionKind.entries.joinToString { "${it.name}=${fmt((a.final[it] ?: 0).toDouble() / n)}" }}")
    println("  Battle interactions/game: supports=${fmt(a.supports.toDouble()/n)} sunlight-funded=${fmt(a.sunlightMains.toDouble()/n)} draws=${fmt(a.battleDraws.toDouble()/n)} Plant activations=${fmt(a.battlePlantActivations.toDouble()/n)} Round effects=${fmt(a.battleRoundEffects.toDouble()/n)} Strike wins=${fmt(a.strikeWins.toDouble()/n)} winner-decisive rows=${fmt(a.winnerDecisiveRows.toDouble()/n)} decisive contributions=${fmt(a.winnerDecisiveContributions.toDouble()/n)} wound-decisive contributions=${fmt(a.woundDecisiveContributions.toDouble()/n)}")
    println("  Economy/game: Plants bought=${fmt(a.plantPurchases.toDouble()/n)} dice bought=${fmt(a.diePurchases.toDouble()/n)}")
}

private fun fmt(v: Double) = "%.2f".format(v)
private fun pp(v: Double) = "%.2f%%".format(v * 100)

internal data class BattleMainEvalOptions(
    val games: Int, val seed: Long, val strategySeed: Long, val weights: Path, val plantOverrides: Path?, val roundOverrides: Path?,
    val grovePattern: String?, val groveSeed: Long, val players: Int, val roundLabel: String,
    val buyPolicy: String, val buyWeights: Path,
    val cultivationMainPolicy: String, val cultivationMainWeights: Path,
    val cultivationSupportPolicy: String, val cultivationSupportWeights: Path,
    val wispPolicy: String, val wispWeights: Path,
    val plantEffectPolicy: String, val plantEffectWeights: Path,
    val battleSupportPolicy: String, val battleSupportWeights: Path
) {
    val roundSetup = parseRoundSetup(roundLabel)
    companion object {
        fun parse(args: List<String>): BattleMainEvalOptions {
            var games = 300; var seed = 94000L; var strategy = 104000L; var weights = Paths.get("output/ai/battle-main-policy-v1-trained.weights")
            var plant: Path? = null; var round: Path? = null; var grove: String? = null; var groveSeed = 114000L; var players = 4; var rounds = "3/2/2"
            var buyPolicy = "human"; var buyWeights = Paths.get("data/ai/buy-policy-v1.weights")
            var cmPolicy = "human"; var cmWeights = Paths.get("data/ai/cultivation-main-policy-v1.weights")
            var csPolicy = "human"; var csWeights = Paths.get("data/ai/cultivation-support-policy-v1.weights")
            var wispPolicy = "human"; var wispWeights = Paths.get("data/ai/wisp-play-policy-v1.weights")
            var pePolicy = "human"; var peWeights = Paths.get("data/ai/plant-effect-policy-v1.weights")
            var bsPolicy = "human"; var bsWeights = Paths.get("data/ai/battle-support-policy-v1.weights")
            var i = 0; fun value(a:String)=if('=' in a)a.substringAfter('=') else args[++i]
            while(i<args.size){ val a=args[i]; when {
                a.startsWith("--games") || a.startsWith("--samples") -> games=value(a).toInt()
                a.startsWith("--seed") -> seed=value(a).toLong(); a.startsWith("--strategy-seed") -> strategy=value(a).toLong(); a.startsWith("--weights") -> weights=Paths.get(value(a))
                a.startsWith("--plant-overrides") -> plant=Paths.get(value(a)); a.startsWith("--round-overrides") -> round=Paths.get(value(a)); a.startsWith("--grove-seed") -> groveSeed=value(a).toLong(); a.startsWith("--players") -> players=value(a).toInt(); a.startsWith("--rounds") -> rounds=value(a)
                a.startsWith("--grove") -> grove=GrovePlantCode.validate(value(a)); a=="--random-grove" -> grove=GrovePlantCode.RANDOM_PATTERN; a=="--first-game-grove" -> grove=null
                a.startsWith("--buy-policy") -> buyPolicy=value(a).lowercase(); a.startsWith("--buy-weights") -> buyWeights=Paths.get(value(a))
                a.startsWith("--cultivation-main-policy") -> cmPolicy=value(a).lowercase(); a.startsWith("--cultivation-main-weights") -> cmWeights=Paths.get(value(a))
                a.startsWith("--cultivation-support-policy") -> csPolicy=value(a).lowercase(); a.startsWith("--cultivation-support-weights") -> csWeights=Paths.get(value(a))
                a.startsWith("--wisp-policy") -> wispPolicy=value(a).lowercase(); a.startsWith("--wisp-weights") -> wispWeights=Paths.get(value(a))
                a.startsWith("--plant-effect-policy") -> pePolicy=value(a).lowercase(); a.startsWith("--plant-effect-weights") -> peWeights=Paths.get(value(a))
                a.startsWith("--battle-support-policy") -> bsPolicy=value(a).lowercase(); a.startsWith("--battle-support-weights") -> bsWeights=Paths.get(value(a))
                else -> error("Unknown option: $a") }; i++ }
            listOf(buyPolicy,cmPolicy,csPolicy,wispPolicy,pePolicy,bsPolicy).forEach{require(it in setOf("human","learned"))}
            require(players in 2..4)
            return BattleMainEvalOptions(games,seed,strategy,weights,plant,round,grove,groveSeed,players,rounds,buyPolicy,buyWeights,cmPolicy,cmWeights,csPolicy,csWeights,wispPolicy,wispWeights,pePolicy,peWeights,bsPolicy,bsWeights)
        }
    }
}
