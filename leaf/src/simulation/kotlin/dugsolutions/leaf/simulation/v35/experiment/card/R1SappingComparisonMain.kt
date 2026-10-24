package dugsolutions.leaf.simulation.v35.experiment.card

import dugsolutions.leaf.simulation.v35.experiment.baseline.loadCatalogs
import dugsolutions.leaf.simulation.v35.experiment.orchestration.ExperimentConfig
import dugsolutions.leaf.simulation.v35.experiment.orchestration.ExperimentRunner
import dugsolutions.leaf.simulation.v35.experiment.plant.PlantExperimentConfig
import dugsolutions.leaf.simulation.v35.experiment.plant.PlantExperimentOverride
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
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.round.RoundCardManager
import dugsolutions.leaf.v35.round.RoundCardRegistry
import dugsolutions.leaf.v35.wisp.WispCardManager
import dugsolutions.leaf.v35.wisp.WispCardRegistry
import org.koin.dsl.koinApplication
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest

/**
 * R1: one persistent JVM per condition; both cohorts use independent, seeded Games.
 * This is descriptive baseline-policy A/B evidence, NOT a trained best-response test.
 * The same nine named cards are locked, and only Sapping's Battle effect changes.
 */
object R1SappingComparisonMain {
    private const val CARD_ID = "Flower_11_03"
    private const val TITLE = "Sapping Snapdragon"
    private val groveSlots = listOf(
        PlantType.ROOT to 5, PlantType.ROOT to 7, PlantType.ROOT to 9,
        PlantType.VINE to 7, PlantType.VINE to 9, PlantType.VINE to 11,
        PlantType.FLOWER to 11, PlantType.FLOWER to 14, PlantType.FLOWER to 17
    )
    private val original = GameEffect.RAISE_DIE_PLUS_2_AND_REDUCE_OPPOSING_DICE_IN_STRIKE_ROW
    private val different = GameEffect.RAISE_DIE_PLUS_2_AND_REDUCE_OPPOSING_DICE_AROUND_ANOTHER_DIE_IN_STRIKE_ROW
    const val HEADER = "sample\tcondition\tseat\twin_share\tfinal_vp\tsapping_purchased\tsapping_final_grafted\tsapping_cultivation_activations\tsapping_battle_activations\tstrike_vp\towned_dice\towned_d20\tpurchased_plants_named\tfinal_plants_named\tfinal_dice_shape\tmulch_stored_dice\tmulch_stored_dice_shape"

    data class Options(
        val condition: String,
        val games: Int,
        val chunk: Int,
        val players: Int,
        val seed: Long,
        val strategySeed: Long,
        val grove: List<String>,
        val outputDir: File,
        val fingerprint: String
    ) {
        init {
            require(condition in setOf("original", "different-die")) { "Unknown condition: $condition" }
            require(games > 0 && chunk > 0 && players in 2..4)
            require(grove.size == 9 && grove.distinct().size == 9) { "Exactly nine distinct Grove card IDs required" }
            require(fingerprint.length >= 16) { "Missing experiment fingerprint" }
        }

        companion object {
            fun parse(args: Array<String>): Options {
                val fields = args.associate { arg ->
                    require(arg.startsWith("--") && '=' in arg) { "Expected --name=value: $arg" }
                    arg.substringAfter("--").substringBefore('=') to arg.substringAfter('=')
                }
                val names = setOf("condition", "games", "chunk", "players", "seed", "strategy-seed", "grove", "output-dir", "fingerprint")
                require(fields.keys == names) { "Expected $names; received ${fields.keys}" }
                return Options(
                    fields.getValue("condition"), fields.getValue("games").toInt(),
                    fields.getValue("chunk").toInt(), fields.getValue("players").toInt(),
                    fields.getValue("seed").toLong(), fields.getValue("strategy-seed").toLong(),
                    fields.getValue("grove").split(','), File(fields.getValue("output-dir")),
                    fields.getValue("fingerprint")
                )
            }
        }
    }

    @JvmStatic fun main(args: Array<String>) = run(Options.parse(args))

    fun run(options: Options) {
        val app = koinApplication { modules(appModules) }
        try {
            val koin = app.koin
            loadCatalogs(koin.get<PlantCardRegistry>(), koin.get<PlantCardManager>(),
                koin.get<WispCardRegistry>(), koin.get<WispCardManager>(),
                koin.get<RoundCardRegistry>(), koin.get<RoundCardManager>())
            val cards = koin.get<PlantCardManager>()
            val grove = options.grove.mapIndexed { index, id ->
                val card = requireNotNull(cards.getCard(id)) { "No Grove card $id" }
                val (type, cost) = groveSlots[index]
                require(card.type == type && card.cost == cost) { "Wrong slot $index: ${card.title} (${card.type} ${card.cost}); expected $type $cost" }
                card
            }
            val sapping = grove.single { it.name == CARD_ID }
            require(sapping.title == TITLE && sapping.cost == 11 && sapping.effect == original) {
                "Canonical Sapping definition changed; review experimental variant"
            }
            val effect = if (options.condition == "original") original else different
            val values = if (effect == original) PlantExperimentConfig.EMPTY else
                PlantExperimentConfig.of(CARD_ID to PlantExperimentOverride(effect = effect))
            val factory = koin.get<GameFactory>()
            val runner = koin.get<GameRunner>()
            val directory = options.outputDir
            require(directory.isDirectory || directory.mkdirs()) { "Cannot create ${directory.path}" }
            val chunks = ExperimentConfig(0, options.games, options.chunk)
            println("R1 ${options.condition}: $effect; ${options.players} players; ${options.games} games; nine locked cards:")
            grove.forEachIndexed { index, card -> println("  ${groveSlots[index]}: ${card.title} [${card.name}]") }
            var writer: java.io.PrintWriter? = null
            var active: Pair<File, File>? = null
            try {
                ExperimentRunner(chunks).run(
                    isComplete = { c ->
                        val path = File(directory, stem(c.firstSample, c.endSampleExclusive) + ".tsv")
                        val marker = File(directory, path.name + ".complete")
                        if (!path.isFile || !marker.isFile) false
                        else {
                            val valid = marker.readText().trim() == "${options.fingerprint}:${sha256(path)}:${c.games * options.players}" &&
                                path.bufferedReader().use { reader ->
                                    reader.readLine() == HEADER &&
                                        reader.lineSequence().count() == c.games * options.players
                                }
                            if (!valid) println("INVALID checkpoint ${path.name}: rerunning")
                            valid
                        }
                    },
                    beginChunk = { c ->
                        val stem = stem(c.firstSample, c.endSampleExclusive)
                        val path = File(directory, "$stem.tsv")
                        val temp = File(directory, "$stem.tsv.tmp")
                        File(directory, "$stem.tsv.complete").delete()
                        temp.delete()
                        active = temp to path
                        writer = temp.printWriter()
                        checkNotNull(writer).println(HEADER)
                    },
                    runSample = { sample ->
                        val game = factory(GameConfig(
                            selectedPlantCards = grove,
                            playerDecisionFactories = List(options.players) { PlayerDecisionFactory.humanBaseline() },
                            seed = options.seed + sample,
                            strategySeed = options.strategySeed + sample,
                            recordDecisionReasoning = false,
                            plantValues = values
                        ))
                        val result = runner.run(game)
                        require(game.isComplete) { "Game not complete, sample=$sample" }
                        val entries = game.chronicle.entries
                        game.players.forEachIndexed { seat, player ->
                            val won = player.id in result.finalScoring.winnerIds
                            val share = if (won) 1.0 / result.finalScoring.winnerIds.size else 0.0
                            val vp = result.finalScoring.scores.single { it.playerId == player.id }.totalVp
                            val purchased = entries.count {
                                it is GameEntry.Purchase && it.playerId == player.id &&
                                    it.kind == PurchaseKind.PLANT && it.itemName == CARD_ID
                            }
                            val activated = entries.filterIsInstance<GameEntry.EffectResolved>().filter {
                                it.playerId == player.id && it.sourceKind == EffectSourceKind.PLANT && it.sourceName == CARD_ID
                            }
                            val strikeVp = entries.filterIsInstance<GameEntry.StrikeResolved>().sumOf {
                                if (player.id in it.winnerIds) it.vpPerWinner else 0
                            }
                            val owned = player.dice.supply + player.dice.hand + player.dice.discard
                            fun namedCopies(names: List<String>): String = names.groupingBy { it }.eachCount()
                                .toSortedMap().entries.joinToString("|") { (name, count) -> "$name*$count" }
                            val purchasesNamed = entries.filterIsInstance<GameEntry.Purchase>()
                                .filter { it.playerId == player.id && it.kind == PurchaseKind.PLANT }
                                .map { purchase -> grove.firstOrNull { it.name == purchase.itemName }?.title ?: purchase.itemName }
                            val finalPlantsNamed = player.creature.cards.map { it.card.title }
                            val diceShape = listOf(4, 6, 8, 10, 12, 20).joinToString("|") { sides ->
                                "D$sides:${owned.count { it.sides == sides }}"
                            }
                            val mulchCount = player.tokens.mulchTokens.size + player.tokens.pendingMulchTokens.size
                            val mulchShape = listOf(4, 6, 8, 10, 12, 20).joinToString("|") { sides ->
                                val current = player.tokens.mulchTokens.count { it.sides?.value == sides }
                                val pending = player.tokens.pendingMulchTokens.count { it.sides?.value == sides }
                                "D$sides:${current + pending}"
                            }
                            checkNotNull(writer).println(listOf(
                                sample, options.condition, seat, share, vp, purchased,
                                player.creature.cards.count { it.card.name == CARD_ID },
                                activated.count { it.phase == ChroniclePhase.CULTIVATION },
                                activated.count { it.phase == ChroniclePhase.BATTLE },
                                strikeVp, owned.size, owned.count { it.sides == 20 },
                                namedCopies(purchasesNamed), namedCopies(finalPlantsNamed), diceShape,
                                mulchCount, mulchShape
                            ).joinToString("\t"))
                        }
                        if ((sample + 1) % 25 == 0) {
                            checkNotNull(writer).flush()
                            check(!checkNotNull(writer).checkError()) { "Write failure sample=$sample" }
                        }
                    },
                    commitChunk = { c ->
                        val (temp, path) = checkNotNull(active)
                        checkNotNull(writer).flush()
                        check(!checkNotNull(writer).checkError()) { "Chunk flush failed" }
                        checkNotNull(writer).close()
                        writer = null
                        Files.move(temp.toPath(), path.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING)
                        File(directory, path.name + ".complete").writeText("${options.fingerprint}:${sha256(path)}:${c.games * options.players}\n")
                        active = null
                    }
                )
            } finally {
                writer?.close()
                active?.first?.delete()
            }
        } finally { app.close() }
    }

    private fun stem(first: Int, end: Int) = "sample-%06d-%06d".format(first, end - 1)

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(65536)
            while (true) {
                val n = input.read(buffer)
                if (n < 0) break
                digest.update(buffer, 0, n)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}

fun main(args: Array<String>) = R1SappingComparisonMain.main(args)
