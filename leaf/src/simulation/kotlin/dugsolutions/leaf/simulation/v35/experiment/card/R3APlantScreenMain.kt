package dugsolutions.leaf.simulation.v35.experiment.card

import dugsolutions.leaf.simulation.v35.analysis.GameSummaryExtractor
import dugsolutions.leaf.simulation.v35.experiment.baseline.loadCatalogs
import dugsolutions.leaf.simulation.v35.experiment.plant.PlantExperimentConfig
import dugsolutions.leaf.simulation.v35.experiment.plant.PlantExperimentOverride
import dugsolutions.leaf.simulation.v35.learning.buy.BuyEvolutionConfig
import dugsolutions.leaf.simulation.v35.learning.buy.BuyPolicyEvolution
import dugsolutions.leaf.simulation.v35.learning.buy.EvaluatedBuyPolicy
import dugsolutions.leaf.v35.chronicle.domain.ChroniclePhase
import dugsolutions.leaf.v35.chronicle.domain.EffectSourceKind
import dugsolutions.leaf.v35.chronicle.domain.GameEntry
import dugsolutions.leaf.v35.chronicle.domain.PurchaseKind
import dugsolutions.leaf.v35.di.appModules
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.game.GameConfig
import dugsolutions.leaf.v35.game.GameRunner
import dugsolutions.leaf.v35.game.PlayerDecisionFactory
import dugsolutions.leaf.v35.game.di.GameFactory
import dugsolutions.leaf.v35.plant.PlantCardManager
import dugsolutions.leaf.v35.plant.PlantCardRegistry
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.decision.learned.buy.LearnedBuy
import dugsolutions.leaf.v35.player.decision.learned.buy.BuyFeature
import dugsolutions.leaf.v35.player.decision.learned.buy.LearnedBuyCardCatalog
import dugsolutions.leaf.v35.player.decision.learned.buy.LearnedBuyWeights
import dugsolutions.leaf.v35.player.decision.random.StrategyRandomizer
import dugsolutions.leaf.v35.player.decision.trace.DecisionReasoningSink
import dugsolutions.leaf.v35.round.RoundCardManager
import dugsolutions.leaf.v35.round.RoundCardRegistry
import dugsolutions.leaf.v35.wisp.WispCardManager
import dugsolutions.leaf.v35.wisp.WispCardRegistry
import org.koin.dsl.koinApplication
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import kotlin.math.max

/**
 * R3A: generic official-card Buy-only population screen.
 * Each invocation fixes exactly one official card and a legal nine-card Grove.
 * Training-only purchase shaping encourages exploration; evaluation rewards only wins.
 * Activation counts are observations, NOT causally credited effect contributions.
 * Non-Buy decisions remain Human Baseline; this is NOT a balance certification.
 */
object R3APlantScreenMain {
    private val slots = listOf(
        PlantType.ROOT to 5, PlantType.ROOT to 7, PlantType.ROOT to 9,
        PlantType.VINE to 7, PlantType.VINE to 9, PlantType.VINE to 11,
        PlantType.FLOWER to 11, PlantType.FLOWER to 14, PlantType.FLOWER to 17
    )
    private const val SCORES = "generation\tcandidate\twin_share\tpurchaser_game_rate\tbattle_activations_per_game\ttraining_fitness"
    private const val EVAL = "matchup\tsample\trole\tseat\twin_share\tfinal_vp\tstrike_vp\tplant_vp\ttarget_purchases\ttarget_cultivation_resolutions\ttarget_battle_resolutions\ttarget_other_resolutions\ttarget_grafted\tnamed_plant_purchases\tfinal_named_plants\tfinal_dice\tstored_mulch_dice"

    data class Options(
        val phase: String, val objective: String, val targetId: String,
        val generations: Int, val population: Int, val elites: Int, val games: Int,
        val sigma: Double, val mutations: Int, val evolutionSeed: Long,
        val seed: Long, val strategySeed: Long, val holdoutGames: Int,
        val targetBoost: Double, val targetMinPurchaseRate: Double, val targetPurchaseBonus: Double,
        val flowerEnablerBoost: Double, val effectVariant: String,
        val grove: List<String>, val outputDir: File, val fingerprint: String,
        val targeted: File?, val unrestricted: File?
    ) {
        init {
            require(phase in setOf("train", "evaluate"))
            require(effectVariant in setOf("official", "disabled", "root-raise20", "queen-draw4"))
            require(effectVariant != "root-raise20" || targetId == "Root_05_02")
            require(effectVariant != "queen-draw4" || targetId == "Flower_17_04")
            require(objective in setOf("targeted", "unrestricted"))
            require(targetId.matches(Regex("(?:Root|Vine|Flower)_[0-9]{2}_[0-9]{2}")))
            require(generations > 0 && population >= 3 && elites in 1 until population && games > 0 && holdoutGames > 0)
            require(sigma.isFinite() && sigma > 0 && mutations > 0)
            require(targetBoost.isFinite() && targetBoost >= 0)
            require(targetMinPurchaseRate.isFinite() && targetMinPurchaseRate in 0.0..1.0)
            require(targetPurchaseBonus.isFinite() && targetPurchaseBonus >= 0)
            require(flowerEnablerBoost.isFinite() && flowerEnablerBoost >= 0)
            require(seed >= 0 && strategySeed >= 0)
            require(grove.size == 9 && grove.distinct().size == 9)
            require(fingerprint.length >= 16)
            if (phase == "evaluate") require(targeted?.isFile == true && unrestricted?.isFile == true)
        }

        companion object {
            fun parse(args: Array<String>): Options {
                val fields = args.associate { arg ->
                    require(arg.startsWith("--") && '=' in arg) { "Expected --key=value: $arg" }
                    arg.substringAfter("--").substringBefore('=') to arg.substringAfter('=')
                }
                val names = setOf("phase", "objective", "target-id", "generations", "population", "elites", "games",
                    "sigma", "mutations", "evolution-seed", "seed", "strategy-seed", "holdout-games",
                    "target-boost", "target-min-purchase-rate", "target-purchase-bonus", "flower-enabler-boost", "effect-variant", "grove", "output-dir", "fingerprint")
                require(fields.keys - names - setOf("targeted", "unrestricted") == emptySet<String>())
                require((names - "effect-variant").all(fields::containsKey)) { "Missing options: ${names - fields.keys}" }
                return Options(
                    fields.getValue("phase"), fields.getValue("objective"), fields.getValue("target-id"),
                    fields.getValue("generations").toInt(), fields.getValue("population").toInt(),
                    fields.getValue("elites").toInt(), fields.getValue("games").toInt(), fields.getValue("sigma").toDouble(),
                    fields.getValue("mutations").toInt(), fields.getValue("evolution-seed").toLong(),
                    fields.getValue("seed").toLong(), fields.getValue("strategy-seed").toLong(),
                    fields.getValue("holdout-games").toInt(), fields.getValue("target-boost").toDouble(),
                    fields.getValue("target-min-purchase-rate").toDouble(), fields.getValue("target-purchase-bonus").toDouble(),
                    fields.getValue("flower-enabler-boost").toDouble(),
                    fields["effect-variant"] ?: "official", fields.getValue("grove").split(','),
                    File(fields.getValue("output-dir")), fields.getValue("fingerprint"),
                    fields["targeted"]?.let(::File), fields["unrestricted"]?.let(::File)
                )
            }
        }
    }

    @JvmStatic fun main(args: Array<String>) = run(Options.parse(args))

    fun run(o: Options) {
        val app = koinApplication { modules(appModules) }
        try {
            val koin = app.koin
            loadCatalogs(koin.get<PlantCardRegistry>(), koin.get<PlantCardManager>(),
                koin.get<WispCardRegistry>(), koin.get<WispCardManager>(),
                koin.get<RoundCardRegistry>(), koin.get<RoundCardManager>())
            val all = koin.get<PlantCardManager>().getAllCards().cards
            val grove = o.grove.mapIndexed { i, id ->
                val card = requireNotNull(koin.get<PlantCardManager>().getCard(id)) { "Missing Grove card $id" }
                require(card.type == slots[i].first && card.cost == slots[i].second) { "Wrong Grove slot $i: ${card.title}" }
                card
            }
            val target = grove.singleOrNull { it.name == o.targetId }
            requireNotNull(target) { "Target ${o.targetId} is not present in the supplied Grove" }
            // R3B calibration modes are strictly research-only: price, VP, and Grove stay canonical.
            val calibratedEffect = when (o.effectVariant) {
                "official" -> null
                "disabled" -> GameEffect.RESEARCH_NO_EFFECT
                "root-raise20" -> GameEffect.RESEARCH_RAISE_DIE_PLUS_20
                "queen-draw4" -> GameEffect.RESEARCH_DRAW_FOUR_DICE
                else -> error("Unsupported calibration effect: ${o.effectVariant}")
            }
            val values = if (calibratedEffect == null) PlantExperimentConfig.EMPTY else
                PlantExperimentConfig.of(o.targetId to PlantExperimentOverride(effect = calibratedEffect))
            require(o.outputDir.isDirectory || o.outputDir.mkdirs())
            val initial = LearnedBuyCardCatalog.prepare(LearnedBuyWeights.zeros(), all)
            println("R3A ${o.phase}: ${o.targetId} / ${target.title} / ${o.objective} / effect=${o.effectVariant} / nine official cards")
            println("Grove: " + grove.joinToString("; ") { it.title })
            if (o.phase == "train") train(o, initial, grove, values, koin.get(), koin.get())
            else evaluate(o, all, grove, values, koin.get(), koin.get())
        } finally { app.close() }
    }

    private fun train(o: Options, initial: LearnedBuyWeights, grove: List<PlantCard>, values: PlantExperimentConfig,
                      factory: GameFactory, runner: GameRunner) {
        val initialTarget = if (o.objective == "targeted") {
            val key = LearnedBuyWeights.cardFeature(o.targetId)
            val boosted = initial.withNamed(key, initial.named(key) + o.targetBoost)
            // Flowers must graft to a Vine. A purchase-only boost is otherwise
            // meaningless when the policy never builds a legal Flower support.
            // Enabling preferences are exploration scaffolding during training;
            // the actual held-out evaluation applies no reward or intervention.
            if (grove.any { it.name == o.targetId && it.type == PlantType.FLOWER })
                boosted.with(BuyFeature.ACTION_IS_VINE_STAGE_1, o.flowerEnablerBoost)
            else boosted
        } else initial
        val config = BuyEvolutionConfig(o.population, o.elites, o.sigma, o.mutations, o.evolutionSeed)
        var population = BuyPolicyEvolution(config).initialPopulation(initialTarget)
        var previousChampion: LearnedBuyWeights? = null
        for (generation in 0 until o.generations) {
            val folder = File(o.outputDir, "generation-%03d".format(generation + 1))
            val records: List<Score>
            if (validGeneration(folder, o.fingerprint, o.population)) {
                population = (0 until o.population).map { LearnedBuyWeights.load(File(folder, "candidate-%03d.weights".format(it)).toPath()) }
                records = parseScores(File(folder, "scores.tsv"))
                println("R3A_PROGRESS target=${o.targetId} objective=${o.objective} generation=${generation+1}/${o.generations} skipped=1")
            } else {
                // Later generations depend on this one's winners/mutations. Invalidate
                // the entire suffix rather than silently mixing incompatible lineages.
                for (later in generation until o.generations) {
                    File(o.outputDir, "generation-%03d".format(later + 1)).deleteRecursively()
                }
                File(o.outputDir, "champion.weights").delete()
                require(folder.mkdirs())
                val scores = population.mapIndexed { ci, weights ->
                    var win = 0.0
                    var purchased = 0
                    var activations = 0
                    repeat(o.games) { sample ->
                        val opponent = when (sample % 3) {
                            0 -> null
                            1 -> population[(ci + 1 + (sample / 3) % (o.population - 1)) % o.population]
                            else -> previousChampion ?: population[(ci + 1) % o.population]
                        }
                        val targetSeat = sample % 2
                        val policies = List(2) { seat -> when {
                            seat == targetSeat -> buyFactory(weights)
                            opponent != null -> buyFactory(opponent)
                            else -> PlayerDecisionFactory.humanBaseline()
                        } }
                        val game = factory(GameConfig(
                            selectedPlantCards = grove, playerDecisionFactories = policies,
                            seed = o.seed + sample, strategySeed = o.strategySeed + sample,
                            plantValues = values
                        ))
                        val result = runner.run(game)
                        val player = game.players[targetSeat]
                        win += GameSummaryExtractor.extract(game, result).players[targetSeat].winShare
                        val purchasedNow = game.chronicle.entries.any { e -> e is GameEntry.Purchase && e.playerId == player.id &&
                            e.kind == PurchaseKind.PLANT && e.itemName == o.targetId }
                        if (purchasedNow) purchased++
                        activations += game.chronicle.entries.count { e -> e is GameEntry.EffectResolved && e.playerId == player.id &&
                            e.sourceKind == EffectSourceKind.PLANT && e.sourceName == o.targetId && e.phase == ChroniclePhase.BATTLE }
                    }
                    val winShare = win / o.games
                    val purchaseRate = purchased.toDouble() / o.games
                    // ONLY training has shaping; evaluation rewards wins exclusively.
                    // The targeted search must actually explore the card. Candidates
                    // below the minimum usage rate cannot outrank any nonnegative
                    // win-share policy satisfying that minimum. This penalty is
                    // TRAINING-ONLY and is absent from all held-out matchups.
                    val fitness = if (o.objective == "targeted") {
                        winShare + o.targetPurchaseBonus * purchaseRate -
                            if (purchaseRate < o.targetMinPurchaseRate) 2.0 else 0.0
                    } else winShare
                    Score(generation + 1, ci, winShare, purchaseRate, activations.toDouble() / o.games, fitness)
                }
                population.forEachIndexed { ci, weights ->
                    weights.save(File(folder, "candidate-%03d.weights".format(ci)).toPath())
                }
                val scoreFile = File(folder, "scores.tsv")
                scoreFile.writeText(SCORES + "\n" + scores.joinToString("\n") { it.tsv() } + "\n")
                File(folder, "generation.complete").writeText(generationDigest(folder, o.fingerprint, o.population) + "\n")
                records = scores
                println("R3A_PROGRESS target=${o.targetId} objective=${o.objective} generation=${generation+1}/${o.generations} skipped=0 bestWin=${"%.3f".format(records.maxOf { it.winShare })} bestTrainingFitness=${"%.3f".format(records.maxOf { it.fitness })}")
            }
            val ranked = records.sortedWith(compareByDescending<Score> { it.fitness }.thenBy { it.candidate })
            previousChampion = population[ranked.first().candidate]
            if (generation + 1 < o.generations) {
                val evaluated = records.map { EvaluatedBuyPolicy(population[it.candidate], it.fitness) }
                population = BuyPolicyEvolution(config.copy(evolutionSeed = o.evolutionSeed + generation + 1))
                    .nextPopulation(evaluated)
            }
        }
        val champ = checkNotNull(previousChampion)
        champ.save(File(o.outputDir, "champion.weights").toPath())
        println("R3A_TRAIN_COMPLETE ${o.targetId} ${o.objective} champion=${File(o.outputDir, "champion.weights").absolutePath}")
    }

    private fun evaluate(o: Options, all: List<PlantCard>, grove: List<PlantCard>, values: PlantExperimentConfig,
                         factory: GameFactory, runner: GameRunner) {
        val targeted = LearnedBuyCardCatalog.prepare(LearnedBuyWeights.load(requireNotNull(o.targeted).toPath()), all)
        val unrestricted = LearnedBuyCardCatalog.prepare(LearnedBuyWeights.load(requireNotNull(o.unrestricted).toPath()), all)
        val path = File(o.outputDir, "holdout.tsv")
        val marker = File(o.outputDir, "holdout.tsv.complete")
        val evidenceHash = o.fingerprint + ":" + sha256(checkNotNull(o.targeted)) + ":" + sha256(checkNotNull(o.unrestricted))
        if (path.isFile && marker.isFile && marker.readText().trim() == "$evidenceHash:${sha256(path)}" &&
            path.bufferedReader().use { it.lineSequence().count() == 1 + o.holdoutGames * 3 * 2 }) {
            println("R3A_EVALUATION_COMPLETE ${o.targetId} skipped=1 rows=${o.holdoutGames * 3 * 2}")
            return
        }
        val temp = File(o.outputDir, "holdout.tsv.tmp")
        marker.delete()
        temp.printWriter().use { out ->
            out.println(EVAL)
            val pairings = listOf(
                Triple("targeted_vs_unrestricted", "targeted", "unrestricted"),
                Triple("targeted_vs_baseline", "targeted", "baseline"),
                Triple("unrestricted_vs_baseline", "unrestricted", "baseline")
            )
            for ((mi, matchup) in pairings.withIndex()) {
                val (name, a, b) = matchup
                repeat(o.holdoutGames) { sample ->
                    val seatA = sample % 2
                    fun getWeights(role: String): LearnedBuyWeights? = when (role) {
                        "targeted" -> targeted
                        "unrestricted" -> unrestricted
                        else -> null
                    }
                    val roleAt = List(2) { seat -> if (seat == seatA) a else b }
                    val game = factory(GameConfig(
                        selectedPlantCards = grove,
                        playerDecisionFactories = roleAt.map { role -> getWeights(role)?.let(::buyFactory) ?: PlayerDecisionFactory.humanBaseline() },
                        seed = o.seed + 1000000L + mi * o.holdoutGames + sample,
                        strategySeed = o.strategySeed + 1000000L + mi * o.holdoutGames + sample,
                        plantValues = values
                    ))
                    val result = runner.run(game)
                    val summaries = GameSummaryExtractor.extract(game, result)
                    for (seat in 0..1) {
                        val player = game.players[seat]
                        val entries = game.chronicle.entries
                        val purchased = entries.filterIsInstance<GameEntry.Purchase>().filter {
                            it.playerId == player.id && it.kind == PurchaseKind.PLANT
                        }
                        val resolutions = entries.filterIsInstance<GameEntry.EffectResolved>().filter { e ->
                            e.playerId == player.id && e.sourceKind == EffectSourceKind.PLANT && e.sourceName == o.targetId
                        }
                        val cultivation = resolutions.count { it.phase == ChroniclePhase.CULTIVATION }
                        val battle = resolutions.count { it.phase == ChroniclePhase.BATTLE }
                        val other = resolutions.size - cultivation - battle
                        val dice = player.dice.supply + player.dice.hand + player.dice.discard
                        val diceShape = listOf(4,6,8,10,12,20).joinToString("|") { s -> "D$s:${dice.count { it.sides == s }}" }
                        val mulchShape = listOf(4,6,8,10,12,20).joinToString("|") { s ->
                            "D$s:${player.tokens.mulchTokens.count { it.sides?.value == s } + player.tokens.pendingMulchTokens.count { it.sides?.value == s }}"
                        }
                        val named = purchased.map { p -> grove.firstOrNull { it.name == p.itemName }?.title ?: p.itemName }
                        val summary = summaries.players[seat]
                        out.println(listOf(name, sample, roleAt[seat], seat, summary.winShare,
                            summary.totalVp, summary.battleStrikeVp, summary.plantVp,
                            purchased.count { it.itemName == o.targetId }, cultivation, battle, other,
                            player.creature.cards.count { it.card.name == o.targetId },
                            copies(named), copies(player.creature.cards.map { it.card.title }), diceShape, mulchShape
                        ).joinToString("\t"))
                    }
                }
                println("R3A_EVAL_PROGRESS target=${o.targetId} matchup=${mi+1}/3 name=$name")
            }
        }
        Files.move(temp.toPath(), path.toPath(), StandardCopyOption.REPLACE_EXISTING)
        marker.writeText("$evidenceHash:${sha256(path)}\n")
        println("R3A_EVALUATION_COMPLETE ${o.targetId} skipped=0 rows=${o.holdoutGames * 3 * 2}")
    }

    private data class Score(val generation: Int, val candidate: Int, val winShare: Double,
                             val purchaserRate: Double, val activations: Double, val fitness: Double) {
        fun tsv() = listOf(generation, candidate, winShare, purchaserRate, activations, fitness).joinToString("\t")
    }

    private fun parseScores(file: File): List<Score> = file.readLines().drop(1).filter { it.isNotBlank() }.map { line ->
        val s = line.split('\t')
        require(s.size == 6)
        Score(s[0].toInt(), s[1].toInt(), s[2].toDouble(), s[3].toDouble(), s[4].toDouble(), s[5].toDouble())
    }

    private fun validGeneration(dir: File, fingerprint: String, population: Int): Boolean {
        val score = File(dir, "scores.tsv")
        val marker = File(dir, "generation.complete")
        if (!score.isFile || !marker.isFile) return false
        val files = (0 until population).map { File(dir, "candidate-%03d.weights".format(it)) }
        if (files.any { !it.isFile }) return false
        return runCatching {
            parseScores(score).let { records -> records.size == population &&
                records.map { it.candidate }.toSet() == (0 until population).toSet() &&
                records.all { it.fitness.isFinite() && it.winShare in 0.0..1.0 && it.purchaserRate in 0.0..1.0 } } &&
                marker.readText().trim() == generationDigest(dir, fingerprint, population)
        }.getOrDefault(false)
    }

    private fun generationDigest(folder: File, fingerprint: String, population: Int): String {
        val paths = listOf(File(folder, "scores.tsv")) + (0 until population).map { File(folder, "candidate-%03d.weights".format(it)) }
        return hash((listOf(fingerprint) + paths.map(::sha256)).joinToString(":" ).toByteArray())
    }

    private fun copies(cards: List<String>): String = cards.groupingBy { it }.eachCount().toSortedMap()
        .entries.joinToString("|") { (name, count) -> "${name.replace('\t',' ')}*$count" }

    private fun buyFactory(weights: LearnedBuyWeights): PlayerDecisionFactory = object : PlayerDecisionFactory {
        override fun create() = LearnedBuy.createDirector(weights)
        override fun create(strategyRandomizer: StrategyRandomizer) = LearnedBuy.createDirector(weights, strategyRandomizer)
        override fun create(strategyRandomizer: StrategyRandomizer, reasoningSink: DecisionReasoningSink) =
            LearnedBuy.createDirector(weights, strategyRandomizer)
    }

    private fun sha256(file: File): String = file.inputStream().use { input ->
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(65536)
        while (true) {
            val n = input.read(buffer)
            if (n < 0) break
            digest.update(buffer, 0, n)
        }
        digest.digest().joinToString("") { "%02x".format(it) }
    }
    private fun hash(value: ByteArray) = MessageDigest.getInstance("SHA-256").digest(value)
        .joinToString("") { "%02x".format(it) }
}

fun main(args: Array<String>) = R3APlantScreenMain.main(args)
