package dugsolutions.leaf.v35.player.decision.learned.battle.main

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.learned.buy.PlantCardManifest
import dugsolutions.leaf.v35.player.decision.learned.buy.PlantCardManifestEntry
import java.nio.file.Files
import java.nio.file.Path

data class LearnedBattleMainProvenance(
    val trainingStatus: String = "untrained",
    val roundPattern: String = "unknown",
    val grove: String = "unknown",
    val generations: Int? = null,
    val gamesPerPolicy: Int? = null,
    val population: Int? = null,
    val playerCount: Int? = null,
    val mutationSigma: Double? = null,
    val mutationsPerChild: Int? = null,
    val evolutionSeed: Long? = null,
    val mechanicalSeedStart: Long? = null,
    val strategySeedStart: Long? = null,
    val fitness: Double? = null,
    val cardManifest: PlantCardManifest? = null
)

class LearnedBattleMainWeights private constructor(
    private val values: DoubleArray,
    private val namedValues: Map<String, Double>,
    val provenance: LearnedBattleMainProvenance
) {
    operator fun get(feature: BattleMainFeature): Double = values[feature.ordinal]
    fun named(key: String): Double = namedValues[key] ?: 0.0
    fun namedWeights(): Map<String, Double> = namedValues.toSortedMap()
    fun toDoubleArray(): DoubleArray = values.copyOf()
    fun withProvenance(p: LearnedBattleMainProvenance) = LearnedBattleMainWeights(values.copyOf(), namedValues, p)

    fun save(path: Path) {
        path.parent?.let(Files::createDirectories)
        Files.writeString(path, buildString {
            appendLine("formatVersion=1")
            appendLine("policy=battle-main-v1")
            appendLine("trainingStatus=${provenance.trainingStatus}")
            appendLine("trainedRoundPattern=${provenance.roundPattern}")
            appendLine("trainedGrove=${provenance.grove}")
            provenance.generations?.let { appendLine("trainingGenerations=$it") }
            provenance.gamesPerPolicy?.let { appendLine("trainingGamesPerPolicy=$it") }
            provenance.population?.let { appendLine("trainingPopulation=$it") }
            provenance.playerCount?.let { appendLine("trainingPlayerCount=$it") }
            provenance.mutationSigma?.let { appendLine("trainingMutationSigma=$it") }
            provenance.mutationsPerChild?.let { appendLine("trainingMutationsPerChild=$it") }
            provenance.evolutionSeed?.let { appendLine("trainingEvolutionSeed=$it") }
            provenance.mechanicalSeedStart?.let { appendLine("trainingMechanicalSeedStart=$it") }
            provenance.strategySeedStart?.let { appendLine("trainingStrategySeedStart=$it") }
            provenance.fitness?.let { appendLine("trainingFitness=$it") }
            provenance.cardManifest?.let { manifest ->
                appendLine("cardManifestFormatVersion=${PlantCardManifest.FORMAT_VERSION}")
                appendLine("cardCatalogFingerprint=${manifest.catalogFingerprint}")
                manifest.entries.sortedBy { it.cardId }.forEach { e ->
                    appendLine("card.${e.cardId}.title=${escape(e.title)}")
                    appendLine("card.${e.cardId}.fingerprint=${e.fingerprint}")
                }
            }
            BattleMainFeature.entries.forEach { appendLine("${it.name}=${this@LearnedBattleMainWeights[it]}") }
            namedValues.toSortedMap().forEach { (key, value) -> appendLine("$key=$value") }
        })
    }

    companion object {
        fun plantFeature(id: String) = "PLANT_$id"
        fun effectFeature(effect: GameEffect) = "EFFECT_${effect.name}"
        fun isNamedFeatureKey(key: String) = key.startsWith("PLANT_") || key.startsWith("EFFECT_")

        fun zeros(
            provenance: LearnedBattleMainProvenance = LearnedBattleMainProvenance(),
            namedFeatureKeys: Collection<String> = emptyList()
        ) = LearnedBattleMainWeights(
            DoubleArray(BattleMainFeature.entries.size),
            namedFeatureKeys.associateWith { 0.0 },
            provenance
        )

        fun fromDoubleArray(
            values: DoubleArray,
            provenance: LearnedBattleMainProvenance = LearnedBattleMainProvenance(),
            namedValues: Map<String, Double> = emptyMap()
        ): LearnedBattleMainWeights {
            require(values.size == BattleMainFeature.entries.size)
            require(values.all { it.isFinite() })
            require(namedValues.all { isNamedFeatureKey(it.key) && it.value.isFinite() })
            return LearnedBattleMainWeights(values.copyOf(), namedValues.toMap(), provenance)
        }

        fun load(path: Path): LearnedBattleMainWeights {
            val entries = linkedMapOf<String, String>()
            Files.readAllLines(path).filter { it.isNotBlank() && !it.trimStart().startsWith("#") }.forEach { line ->
                val idx = line.indexOf('=')
                require(idx > 0) { "Invalid Battle Main weight line: $line" }
                entries[line.substring(0, idx).trim()] = line.substring(idx + 1).trim()
            }
            require(entries["formatVersion"] == "1")
            require(entries["policy"] == "battle-main-v1")
            val missing = BattleMainFeature.entries.filter { it.name !in entries }
            require(missing.isEmpty()) { "Missing Battle Main weights: ${missing.map { it.name }}" }
            val values = DoubleArray(BattleMainFeature.entries.size) { i ->
                entries.getValue(BattleMainFeature.entries[i].name).toDouble().also { require(it.isFinite()) }
            }
            val named = entries.filterKeys(::isNamedFeatureKey).mapValues { it.value.toDouble().also { v -> require(v.isFinite()) } }
            val manifest = entries["cardCatalogFingerprint"]?.let { fp ->
                val ids = entries.keys.filter { it.startsWith("card.") && it.endsWith(".fingerprint") }
                    .map { it.removePrefix("card.").removeSuffix(".fingerprint") }.sorted()
                PlantCardManifest(ids.map { id ->
                    PlantCardManifestEntry(
                        cardId = id,
                        title = unescape(entries["card.$id.title"] ?: id),
                        quantity = 0,
                        type = "unknown",
                        cost = 0,
                        effect = "unknown",
                        scoringRule = "unknown",
                        fingerprint = entries.getValue("card.$id.fingerprint")
                    )
                }).also { require(it.catalogFingerprint == fp) { "Battle Main Plant manifest fingerprint mismatch inside weight file" } }
            }
            return LearnedBattleMainWeights(values, named, LearnedBattleMainProvenance(
                trainingStatus = entries["trainingStatus"] ?: "unknown",
                roundPattern = entries["trainedRoundPattern"] ?: "unknown",
                grove = entries["trainedGrove"] ?: "unknown",
                generations = entries["trainingGenerations"]?.toInt(),
                gamesPerPolicy = entries["trainingGamesPerPolicy"]?.toInt(),
                population = entries["trainingPopulation"]?.toInt(),
                playerCount = entries["trainingPlayerCount"]?.toInt(),
                mutationSigma = entries["trainingMutationSigma"]?.toDouble(),
                mutationsPerChild = entries["trainingMutationsPerChild"]?.toInt(),
                evolutionSeed = entries["trainingEvolutionSeed"]?.toLong(),
                mechanicalSeedStart = entries["trainingMechanicalSeedStart"]?.toLong(),
                strategySeedStart = entries["trainingStrategySeedStart"]?.toLong(),
                fitness = entries["trainingFitness"]?.toDouble(),
                cardManifest = manifest
            ))
        }

        private fun escape(value: String): String = value.replace("\\", "\\\\").replace("\n", "\\n").replace("=", "\\=")
        private fun unescape(value: String): String = value.replace("\\=", "=").replace("\\n", "\n").replace("\\\\", "\\")
    }
}
