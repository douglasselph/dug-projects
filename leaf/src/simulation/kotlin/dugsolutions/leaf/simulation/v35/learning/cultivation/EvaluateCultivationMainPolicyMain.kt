package dugsolutions.leaf.simulation.v35.learning.cultivation

import dugsolutions.leaf.simulation.v35.analysis.GameSummary
import dugsolutions.leaf.simulation.v35.analysis.GameSummaryExtractor
import dugsolutions.leaf.simulation.v35.analysis.SharedTokenEconomySummary
import dugsolutions.leaf.simulation.v35.experiment.diagnostic.SimulationRunContext
import dugsolutions.leaf.simulation.v35.experiment.diagnostic.withSimulationFailureDiagnostics
import dugsolutions.leaf.simulation.v35.experiment.plant.PlantExperimentResearchConfig
import dugsolutions.leaf.simulation.v35.experiment.plant.resolveResearchGroveForSample
import dugsolutions.leaf.simulation.v35.experiment.round.RoundExperimentResearchConfig
import dugsolutions.leaf.simulation.v35.learning.buy.parseRoundSetup
import dugsolutions.leaf.v35.chronicle.domain.ChroniclePhase
import dugsolutions.leaf.v35.chronicle.domain.EffectSourceKind
import dugsolutions.leaf.v35.chronicle.domain.GameEntry
import dugsolutions.leaf.v35.chronicle.domain.MainActionKind
import dugsolutions.leaf.v35.chronicle.domain.PurchaseKind
import dugsolutions.leaf.v35.common.FirstGameDefault
import dugsolutions.leaf.v35.di.appModules
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.game.GameConfig
import dugsolutions.leaf.v35.game.GameRunner
import dugsolutions.leaf.v35.game.PlayerDecisionFactory
import dugsolutions.leaf.v35.game.di.GameFactory
import dugsolutions.leaf.v35.plant.GrovePlantCode
import dugsolutions.leaf.v35.plant.PlantCardManager
import dugsolutions.leaf.v35.plant.PlantCardRegistry
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.learned.buy.LearnedBuyCardCatalog
import dugsolutions.leaf.v35.player.decision.learned.buy.LearnedBuyWeights
import dugsolutions.leaf.v35.player.decision.learned.cultivation.LearnedCultivationMainCatalog
import dugsolutions.leaf.v35.player.decision.learned.cultivation.LearnedCultivationMainWeights
import dugsolutions.leaf.v35.round.RoundCardManager
import dugsolutions.leaf.v35.round.RoundCardRegistry
import dugsolutions.leaf.v35.tokens.SharedTokenResource
import dugsolutions.leaf.v35.wisp.WispCardManager
import dugsolutions.leaf.v35.wisp.WispCardRegistry
import org.koin.dsl.koinApplication
import java.nio.file.Path
import java.nio.file.Paths

private data class CompletedCultivationEvalGame(
    val summary: GameSummary,
    val entries: List<GameEntry>
)

fun main(args: Array<String>) {
    val o = CultivationEvalOptions.parse(args.toList())
    val app = koinApplication { modules(appModules) }
    try {
        val koin = app.koin
        loadCultivationResearchCards(koin.get(), koin.get(), koin.get(), koin.get(), koin.get(), koin.get())
        val plantManager = koin.get<PlantCardManager>()
        val roundManager = koin.get<RoundCardManager>()
        val allPlants = plantManager.getAllCards().cards
        val allRounds = roundManager.getAllCards().cards
        val defaultGrove = FirstGameDefault.PLANT_NAMES.map { requireNotNull(plantManager.getCard(it)) }
        val plantExperiment = PlantExperimentResearchConfig.resolve(o.plantOverridesPath, allPlants)
        val roundExperiment = RoundExperimentResearchConfig.resolve(o.roundOverridesPath, allRounds)
        val weights = LearnedCultivationMainCatalog.prepare(
            LearnedCultivationMainWeights.load(o.weights),
            allPlants,
            plantExperiment.effectiveCosts(allPlants)
        )
        LearnedCultivationMainCatalog.validateCurrentSchema(weights, allPlants, plantExperiment.effectiveCosts(allPlants))
        verifyHeldOutSeeds(weights, o)
        val buyWeights = when (o.buyPolicy) {
            "human" -> null
            "learned" -> LearnedBuyCardCatalog.prepare(
                LearnedBuyWeights.load(o.buyWeights),
                allPlants,
                plantExperiment.effectiveCosts(allPlants)
            )
            else -> error("Unsupported Buy policy ${o.buyPolicy}")
        }
        val factory = koin.get<GameFactory>()
        val runner = koin.get<GameRunner>()
        val control = CultivationEvalAccumulator()
        val learned = CultivationEvalAccumulator()

        println("Leaf & Let Die — Learned Cultivation Main Held-Out Evaluation")
        println("weights=${o.weights}; matched samples=${o.games}; players=${o.players}; rounds=${o.roundLabel}")
        println("mechanical seeds=${o.seed}..${o.seed + o.games - 1}; strategy seeds=${o.strategySeed}..${o.strategySeed + o.games - 1}")
        println("${o.groveDescription()}; Buy=${o.buyPolicy}; Battle Main/Support=Human Baseline; lower-level effect targets=Human Baseline")
        if (plantExperiment.isActive) { println(); println(plantExperiment.render(allPlants)) }
        if (roundExperiment.isActive) { println(); println(roundExperiment.render(allRounds)) }
        println()

        repeat(o.games) { sample ->
            val seat = affectedSeat(sample, o.players)
            val grove = resolveResearchGroveForSample(
                grovePattern = o.grovePattern,
                groveSeed = o.groveSeed,
                sample = sample,
                plantManager = plantManager,
                defaultGrove = defaultGrove,
                allPlants = allPlants,
                plantValues = plantExperiment.values
            )
            val mechanicalSeed = o.seed + sample
            val strategySeed = o.strategySeed + sample
            val humanAffected = humanCultivationFactory(buyWeights)
            val learnedAffected = learnedCultivationFactory(weights, buyWeights)
            val controlFactories = List(o.players) { index ->
                if (index == seat) humanAffected else PlayerDecisionFactory.humanBaseline()
            }
            val learnedFactories = List(o.players) { index ->
                if (index == seat) learnedAffected else PlayerDecisionFactory.humanBaseline()
            }
            val controlGame = runOne(
                factory, runner, grove, controlFactories, o, sample, seat,
                mechanicalSeed, strategySeed, "CONTROL", plantExperiment, roundExperiment
            )
            val learnedGame = runOne(
                factory, runner, grove, learnedFactories, o, sample, seat,
                mechanicalSeed, strategySeed, "LEARNED_CULTIVATION_MAIN", plantExperiment, roundExperiment
            )
            control.add(controlGame, seat)
            learned.add(learnedGame, seat)
        }

        printCultivationEvalReport("CONTROL — Human Cultivation Main", control, o.games)
        println()
        printCultivationEvalReport("LEARNED — Learned Cultivation Main", learned, o.games)
        println()
        println("DELTA LEARNED - CONTROL")
        println("  win share: ${pp(learned.winShare / o.games - control.winShare / o.games)}")
        println("  final VP: ${fmt(learned.totalVp.toDouble() / o.games - control.totalVp.toDouble() / o.games)}")
        println("  Plant VP: ${fmt(learned.plantVp.toDouble() / o.games - control.plantVp.toDouble() / o.games)}")
        println("  Battle VP: ${fmt(learned.battleVp.toDouble() / o.games - control.battleVp.toDouble() / o.games)}")
        println()
        println("Raw learned coefficients are diagnostics only; interpret held-out action behavior and outcomes instead.")
    } finally {
        app.close()
    }
}

private fun runOne(
    factory: GameFactory,
    runner: GameRunner,
    grove: List<PlantCard>,
    decisions: List<PlayerDecisionFactory>,
    o: CultivationEvalOptions,
    sample: Int,
    seat: Int,
    mechanicalSeed: Long,
    strategySeed: Long,
    variant: String,
    plantExperiment: PlantExperimentResearchConfig,
    roundExperiment: RoundExperimentResearchConfig
): CompletedCultivationEvalGame {
    val game = factory(
        GameConfig(
            selectedPlantCards = grove,
            playerDecisionFactories = decisions,
            roundSetup = o.roundSetup,
            seed = mechanicalSeed,
            strategySeed = strategySeed,
            recordDecisionReasoning = false,
            plantValues = plantExperiment.values,
            roundValues = roundExperiment.values
        )
    )
    val result = withSimulationFailureDiagnostics(
        game,
        SimulationRunContext(
            experiment = "evaluate_cultivation_main_policy",
            sample = sample,
            variant = variant,
            affectedSeat = seat,
            mechanicalSeed = mechanicalSeed,
            strategySeed = strategySeed,
            grove = GrovePlantCode.encode(grove),
            roundStructure = o.roundLabel
        )
    ) { runner.run(game) }
    return CompletedCultivationEvalGame(
        GameSummaryExtractor.extract(game, result),
        game.chronicle.entries.toList()
    )
}

private data class RoundEffectStats(var opportunities: Long = 0, var uses: Long = 0)

private data class SunlightChoiceStats(
    var opportunities: Long = 0,
    var uses: Long = 0,
    var finalVpSum: Long = 0,
    var winShareSum: Double = 0.0
) {
    fun add(used: Boolean, finalVp: Int, winShare: Double) {
        opportunities++
        if (used) uses++
        finalVpSum += finalVp
        winShareSum += winShare
    }

    fun rate(): Double = if (opportunities == 0L) 0.0 else uses.toDouble() / opportunities
    fun avgVp(): Double = if (opportunities == 0L) 0.0 else finalVpSum.toDouble() / opportunities
    fun avgWinShare(): Double = if (opportunities == 0L) 0.0 else winShareSum / opportunities
}

private data class TokenStats(
    var games: Long = 0,
    var starting: Long = 0,
    var gains: Long = 0,
    var spends: Long = 0,
    var finalHeld: Long = 0,
    var minSupply: Long = 0,
    var zeroGames: Long = 0,
    var failedEmpty: Long = 0
)

private class CultivationEvalAccumulator {
    var winShare = 0.0
    var totalVp = 0L
    var plantVp = 0L
    var battleVp = 0L
    var wounds = 0L
    var finalDice = 0L
    var finalDicePower = 0L
    val actions = linkedMapOf<MainActionKind, Long>()
    val actionsBattleNext = linkedMapOf<MainActionKind, Long>()
    val actionsBattleNotNext = linkedMapOf<MainActionKind, Long>()
    val plantActivations = sortedMapOf<String, Long>()
    val battlePlantActivations = sortedMapOf<String, Long>()
    val roundEffects = sortedMapOf<String, RoundEffectStats>()
    val tokenEconomy = SharedTokenResource.entries.associateWith { TokenStats() }.toMutableMap()
    var plantPurchases = 0L
    var diePurchases = 0L

    val sunlightOverall = SunlightChoiceStats()
    val sunlightAlreadyHeld = SunlightChoiceStats()
    val sunlightBattleNext = SunlightChoiceStats()
    val sunlightBattleDistant = SunlightChoiceStats()
    val sunlightHumanPreferredCompetitor = SunlightChoiceStats()
    val sunlightByBattlesRemaining = sortedMapOf<Int, SunlightChoiceStats>()
    val sunlightDeclinedAlternatives = sortedMapOf<String, Long>()

    var sunlightGained = 0L
    var sunlightSpent = 0L
    var sunlightRetained = 0L
    var sunlightSupportOpportunities = 0L
    var sunlightSupportUses = 0L
    var sunlightExtraMains = 0L
    var sunlightExtraPlants = 0L
    var sunlightImmediateStrikeContributions = 0L
    var sunlightWinningStrikeContributions = 0L
    var sunlightWinnerDecisiveContributions = 0L
    var sunlightWoundDecisiveContributions = 0L
    var sunlightAssociatedBattleVp = 0L

    fun add(game: CompletedCultivationEvalGame, seat: Int) {
        val player = game.summary.players.single { it.seat == seat }
        winShare += player.winShare
        totalVp += player.totalVp
        plantVp += player.plantVp
        battleVp += player.battleStrikeVp
        wounds += player.woundsTaken
        finalDice += player.finalDiceCount
        finalDicePower += player.finalDicePower
        sunlightGained += player.sunlightGained
        sunlightSpent += player.sunlightSpent
        sunlightRetained += player.finalSunlightCount
        sunlightSupportOpportunities += player.sunlightSupportOpportunities
        sunlightSupportUses += player.sunlightSupportUses
        sunlightExtraMains += player.sunlightExtraMainActions
        sunlightExtraPlants += player.sunlightExtraPlantActions
        sunlightImmediateStrikeContributions += player.sunlightImmediateStrikeContributions
        sunlightWinningStrikeContributions += player.sunlightWinningStrikeContributions
        sunlightWinnerDecisiveContributions += player.sunlightWinnerDecisiveContributions
        sunlightWoundDecisiveContributions += player.sunlightWoundDecisiveContributions
        sunlightAssociatedBattleVp += player.sunlightAssociatedBattleVp
        val playerId = player.playerId

        game.entries.filterIsInstance<GameEntry.RoundEffectChoice>()
            .filter {
                it.playerId == playerId &&
                    it.phase == ChroniclePhase.CULTIVATION &&
                    it.selectedMainAction != null
            }
            .forEach { choice ->
                val action = requireNotNull(choice.selectedMainAction)
                actions.bump(action)
                if (choice.battleNext) actionsBattleNext.bump(action) else actionsBattleNotNext.bump(action)
                collectRoundSlot(choice, 1, choice.firstEffect, choice.firstExecutable, player.totalVp, player.winShare)
                collectRoundSlot(choice, 2, choice.secondEffect, choice.secondExecutable, player.totalVp, player.winShare)
            }

        game.entries.filterIsInstance<GameEntry.EffectResolved>()
            .filter { it.playerId == playerId && it.sourceKind == EffectSourceKind.PLANT }
            .forEach { effect ->
                when (effect.phase) {
                    ChroniclePhase.CULTIVATION -> plantActivations.bump(effect.sourceName)
                    ChroniclePhase.BATTLE -> battlePlantActivations.bump(effect.sourceName)
                    else -> Unit
                }
            }

        game.entries.filterIsInstance<GameEntry.Purchase>()
            .filter { it.playerId == playerId }
            .forEach { purchase ->
                when (purchase.kind) {
                    PurchaseKind.PLANT -> plantPurchases++
                    PurchaseKind.DIE -> diePurchases++
                }
            }

        game.summary.sharedTokenEconomy.forEach { summary -> tokenEconomy.getValue(summary.resource).add(summary) }
    }

    private fun collectRoundSlot(
        choice: GameEntry.RoundEffectChoice,
        slot: Int,
        effect: GameEffect,
        executable: Boolean,
        finalVp: Int,
        winShare: Double
    ) {
        if (!executable) return
        val stats = roundEffects.getOrPut(effect.name) { RoundEffectStats() }
        stats.opportunities++
        val sunlightAction = when (slot) {
            1 -> MainActionKind.ROUND_EFFECT_1
            2 -> MainActionKind.ROUND_EFFECT_2
            else -> error("Round effect slot must be 1 or 2: $slot")
        }
        val used = choice.selectedMainAction == sunlightAction
        if (used) stats.uses++
        if (effect != GameEffect.GAIN_SUNLIGHT_TOKEN) return

        sunlightOverall.add(used, finalVp, winShare)
        if (choice.sunlightHeld > 0) sunlightAlreadyHeld.add(used, finalVp, winShare)
        if (choice.battleNext) sunlightBattleNext.add(used, finalVp, winShare)
        else sunlightBattleDistant.add(used, finalVp, winShare)
        sunlightByBattlesRemaining.getOrPut(choice.battlesRemaining) { SunlightChoiceStats() }
            .add(used, finalVp, winShare)

        // Operational strong-competing-action proxy: at this exact state, the
        // certified Human Baseline preferred another legal Main Action over
        // Gain Sunlight before the learned Main policy was allowed to replace it.
        val humanPreferred = choice.humanBaselineSelectedMainAction
        if (humanPreferred != null && humanPreferred != sunlightAction) {
            sunlightHumanPreferredCompetitor.add(used, finalVp, winShare)
        }

        if (!used) {
            val selectedAction = choice.selectedMainAction
            val alternative = when (selectedAction) {
                MainActionKind.ACTIVATE_PLANT -> choice.selectedPlantCardName?.let { "PLANT:$it" } ?: "PLANT"
                null -> "NONE"
                else -> selectedAction.name
            }
            sunlightDeclinedAlternatives.bump(alternative)
        }
    }

    private fun <K> MutableMap<K, Long>.bump(key: K) { this[key] = (this[key] ?: 0L) + 1L }
}

private fun TokenStats.add(summary: SharedTokenEconomySummary) {
    games++
    starting += summary.startingGroveSupply
    gains += summary.successfulGains
    spends += summary.spendsOrUses
    finalHeld += summary.finalHeldByPlayers
    minSupply += summary.minimumGroveSupply
    if (summary.reachedZero) zeroGames++
    failedEmpty += summary.failedGainsEmptyGrove
}

private fun printCultivationEvalReport(label: String, a: CultivationEvalAccumulator, games: Int) {
    println(label)
    println("  win share: ${pct(a.winShare / games)}")
    println("  final VP: ${fmt(a.totalVp.toDouble() / games)}")
    println("  Plant VP: ${fmt(a.plantVp.toDouble() / games)}")
    println("  Battle VP: ${fmt(a.battleVp.toDouble() / games)}")
    println("  wounds: ${fmt(a.wounds.toDouble() / games)}")
    println("  final dice: ${fmt(a.finalDice.toDouble() / games)}; die-side power=${fmt(a.finalDicePower.toDouble() / games)}")
    println("  CULTIVATION MAIN ACTIONS")
    MainActionKind.entries.forEach { kind ->
        println("    $kind: ${a.actions[kind] ?: 0} (Battle next=${a.actionsBattleNext[kind] ?: 0}, not next=${a.actionsBattleNotNext[kind] ?: 0})")
    }
    println("  PLANT ACTIVATIONS (Cultivation)")
    if (a.plantActivations.isEmpty()) println("    none")
    else a.plantActivations.entries.sortedByDescending { it.value }.forEach { println("    ${it.key}: ${it.value}") }
    println("  PLANT ACTIVATIONS (Battle)")
    if (a.battlePlantActivations.isEmpty()) println("    none")
    else a.battlePlantActivations.entries.sortedByDescending { it.value }.forEach { println("    ${it.key}: ${it.value}") }
    println("  PURCHASES (affected player)")
    println("    Plants/game=${fmt(a.plantPurchases.toDouble()/games)} dice/game=${fmt(a.diePurchases.toDouble()/games)}")
    println("  ROUND EFFECT UTILIZATION")
    if (a.roundEffects.isEmpty()) println("    none")
    else a.roundEffects.forEach { (effect, stats) ->
        val rate = if (stats.opportunities == 0L) 0.0 else stats.uses.toDouble() / stats.opportunities
        println("    $effect: opportunities=${stats.opportunities} uses=${stats.uses} use/legal=${pct(rate)}")
    }
    println("  SUNLIGHT GAIN: opportunities=${a.sunlightOverall.opportunities} uses=${a.sunlightOverall.uses} take rate=${pct(a.sunlightOverall.rate())}")
    println("  SUNLIGHT BATTLE USE")
    println(
        "    gained/game=${fmt(a.sunlightGained.toDouble()/games)} spent/game=${fmt(a.sunlightSpent.toDouble()/games)} " +
            "retained/game=${fmt(a.sunlightRetained.toDouble()/games)}"
    )
    println(
        "    Support opportunities/game=${fmt(a.sunlightSupportOpportunities.toDouble()/games)} " +
            "uses/game=${fmt(a.sunlightSupportUses.toDouble()/games)} extra Mains/game=${fmt(a.sunlightExtraMains.toDouble()/games)} " +
            "extra Plant Mains/game=${fmt(a.sunlightExtraPlants.toDouble()/games)}"
    )
    println(
        "    immediate Strike contributions=${a.sunlightImmediateStrikeContributions} " +
            "winning-row=${a.sunlightWinningStrikeContributions} winner-decisive=${a.sunlightWinnerDecisiveContributions} " +
            "Wound-decisive=${a.sunlightWoundDecisiveContributions} associated Battle VP=${a.sunlightAssociatedBattleVp}"
    )
    println("  SUNLIGHT MANDATORY-ACTION CHECK")
    printSunlightChoiceBucket("all legal Main decisions", a.sunlightOverall)
    printSunlightChoiceBucket("already holding Sunlight", a.sunlightAlreadyHeld)
    printSunlightChoiceBucket("Battle next", a.sunlightBattleNext)
    printSunlightChoiceBucket("Battle distant (Battle not next)", a.sunlightBattleDistant)
    printSunlightChoiceBucket(
        "Human Baseline preferred another Main (strong competing-action proxy)",
        a.sunlightHumanPreferredCompetitor
    )
    if (a.sunlightByBattlesRemaining.isNotEmpty()) {
        println("    by Battles remaining:")
        a.sunlightByBattlesRemaining.forEach { (remaining, stats) ->
            printSunlightChoiceBucket("$remaining", stats, indent = "      ")
        }
    }
    println("    alternatives chosen when Sunlight declined:")
    if (a.sunlightDeclinedAlternatives.isEmpty()) println("      none")
    else a.sunlightDeclinedAlternatives.entries.sortedByDescending { it.value }.forEach {
        println("      ${it.key}: ${it.value}")
    }
    println(
        "SUNLIGHT_MANDATORY_METRICS label=${machineLabel(label)} " +
            "opportunities=${a.sunlightOverall.opportunities} uses=${a.sunlightOverall.uses} " +
            "take_rate=${fmt(a.sunlightOverall.rate())} held_opportunities=${a.sunlightAlreadyHeld.opportunities} " +
            "held_take_rate=${fmt(a.sunlightAlreadyHeld.rate())} distant_opportunities=${a.sunlightBattleDistant.opportunities} " +
            "distant_take_rate=${fmt(a.sunlightBattleDistant.rate())} competitor_opportunities=${a.sunlightHumanPreferredCompetitor.opportunities} " +
            "competitor_take_rate=${fmt(a.sunlightHumanPreferredCompetitor.rate())} win_share=${fmt(a.winShare / games)} " +
            "final_vp=${fmt(a.totalVp.toDouble()/games)}"
    )
    println("  TOKEN ECONOMY (whole matched games)")
    SharedTokenResource.entries.forEach { resource ->
        val t = a.tokenEconomy.getValue(resource)
        if (t.games == 0L) return@forEach
        println(
            "    $resource: start=${fmt(t.starting.toDouble()/t.games)} gains=${fmt(t.gains.toDouble()/t.games)} " +
                "spends=${fmt(t.spends.toDouble()/t.games)} finalHeld=${fmt(t.finalHeld.toDouble()/t.games)} " +
                "avgMinGrove=${fmt(t.minSupply.toDouble()/t.games)} zeroGames=${pct(t.zeroGames.toDouble()/t.games)} " +
                "failedEmpty=${t.failedEmpty}"
        )
    }
}

private fun printSunlightChoiceBucket(
    label: String,
    stats: SunlightChoiceStats,
    indent: String = "    "
) {
    val outcomes = if (stats.opportunities == 0L) "n/a" else
        "descriptive avgFinalVP=${fmt(stats.avgVp())} winShare=${pct(stats.avgWinShare())}"
    println(
        "$indent$label: opportunities=${stats.opportunities} uses=${stats.uses} " +
            "take=${pct(stats.rate())}; $outcomes"
    )
}

private fun machineLabel(label: String): String =
    if (label.startsWith("CONTROL")) "CONTROL" else "LEARNED"

private fun verifyHeldOutSeeds(weights: LearnedCultivationMainWeights, o: CultivationEvalOptions) {
    val n = weights.provenance.gamesPerPolicy ?: return
    fun overlaps(a0: Long, a1: Long, b0: Long, b1: Long) = a0 <= b1 && b0 <= a1
    weights.provenance.mechanicalSeedStart?.let { start ->
        require(!overlaps(o.seed, o.seed + o.games - 1, start, start + n - 1)) {
            "Evaluation mechanical seeds overlap training seeds $start..${start + n - 1}; choose held-out --seed values."
        }
    }
    weights.provenance.strategySeedStart?.let { start ->
        require(!overlaps(o.strategySeed, o.strategySeed + o.games - 1, start, start + n - 1)) {
            "Evaluation strategy seeds overlap training seeds $start..${start + n - 1}; choose held-out --strategy-seed values."
        }
    }
}

private fun pct(value: Double) = "%.2f%%".format(value * 100.0)
private fun pp(value: Double) = "%+.2f pp".format(value * 100.0)
private fun fmt(value: Double) = "%.2f".format(value)

internal data class CultivationEvalOptions(
    val games: Int,
    val seed: Long,
    val strategySeed: Long,
    val weights: Path,
    val grovePattern: String?,
    val groveSeed: Long,
    val roundLabel: String,
    val plantOverridesPath: Path?,
    val roundOverridesPath: Path?,
    val players: Int,
    val buyPolicy: String,
    val buyWeights: Path,
    val battleSupportPolicy: String
) {
    val roundSetup = parseRoundSetup(roundLabel)
    fun groveDescription(): String = grovePattern?.let { "Grove pattern=$it (new resolution per matched sample)" } ?: "Grove=FirstGameDefault"

    companion object {
        fun parse(args: List<String>): CultivationEvalOptions {
            var games = 300
            var seed = 262000L
            var strategySeed = 272000L
            var weights = Paths.get("output/ai/cultivation-main-policy-v1-trained.weights")
            var grovePattern: String? = null
            var groveSeed = 282000L
            var roundLabel = "3/2/2"
            var plantOverridesPath: Path? = null
            var roundOverridesPath: Path? = null
            var players = 4
            var buyPolicy = "human"
            var buyWeights = Paths.get("data/ai/frozen-buy-first-game-default-v1.weights")
            var battleSupportPolicy = "human"
            var positional = false
            var i = 0
            fun value(arg: String): String = if ('=' in arg) arg.substringAfter('=') else args[++i]
            while (i < args.size) {
                val arg = args[i]
                when {
                    !positional && arg.matches(Regex("[1-9][0-9]*")) -> { games = arg.toInt(); positional = true }
                    arg.startsWith("--games") -> games = value(arg).toInt()
                    arg.startsWith("--seed") -> seed = value(arg).toLong()
                    arg.startsWith("--strategy-seed") -> strategySeed = value(arg).toLong()
                    arg.startsWith("--weights") || arg.startsWith("--input") -> weights = Paths.get(value(arg))
                    arg.startsWith("--grove-seed") -> groveSeed = value(arg).toLong()
                    arg.startsWith("--rounds") -> roundLabel = value(arg).trim()
                    arg.startsWith("--plant-overrides") -> plantOverridesPath = Paths.get(value(arg))
                    arg.startsWith("--round-overrides") -> roundOverridesPath = Paths.get(value(arg))
                    arg.startsWith("--players") -> players = value(arg).toInt()
                    arg.startsWith("--buy-policy") -> buyPolicy = value(arg).trim().lowercase()
                    arg.startsWith("--buy-weights") -> buyWeights = Paths.get(value(arg))
                    arg.startsWith("--battle-support-policy") -> battleSupportPolicy = value(arg).trim().lowercase()
                    arg.startsWith("--grove") -> grovePattern = GrovePlantCode.validate(value(arg))
                    arg == "--random-grove" -> grovePattern = GrovePlantCode.RANDOM_PATTERN
                    arg == "--help" -> { usage(); kotlin.system.exitProcess(0) }
                    else -> error("Unknown argument: $arg")
                }
                i++
            }
            require(games > 0)
            require(players in 2..4) { "--players must be 2, 3, or 4" }
            require(buyPolicy in setOf("human", "learned")) { "--buy-policy must be human or learned" }
            require(battleSupportPolicy == "human") { "--battle-support-policy currently supports only human; learned policy is a later task" }
            parseRoundSetup(roundLabel)
            return CultivationEvalOptions(
                games, seed, strategySeed, weights, grovePattern, groveSeed, roundLabel,
                plantOverridesPath, roundOverridesPath, players, buyPolicy, buyWeights, battleSupportPolicy
            )
        }

        private fun usage() = println(
            "evaluate_cultivation_main_policy [N|--games N] [--seed N] [--strategy-seed N] " +
                "[--weights PATH] [--grove CODE|--random-grove] [--grove-seed N] [--rounds PATTERN] " +
                "[--plant-overrides PATH] [--round-overrides PATH] [--players 2|3|4] " +
                "[--buy-policy human|learned] [--buy-weights PATH] [--battle-support-policy human]"
        )
    }
}
