package dugsolutions.leaf.v35.player.decision.learned.battle

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.learned.buy.PlantCardManifest
import dugsolutions.leaf.v35.player.decision.learned.buy.PlantCardManifestEntry
import java.nio.file.Files
import java.nio.file.Path

data class LearnedBattleSupportProvenance(
    val trainingStatus: String = "untrained",
    val roundPattern: String = "unknown",
    val grove: String = "unknown",
    val generations: Int? = null,
    val gamesPerPolicy: Int? = null,
    val population: Int? = null,
    val playerCount: Int? = null,
    val evolutionSeed: Long? = null,
    val mechanicalSeedStart: Long? = null,
    val strategySeedStart: Long? = null,
    val fitness: Double? = null,
    val cardManifest: PlantCardManifest? = null
)

class LearnedBattleSupportWeights private constructor(
    private val values: DoubleArray,
    private val namedValues: Map<String, Double>,
    val provenance: LearnedBattleSupportProvenance
) {
    operator fun get(feature: BattleSupportFeature): Double = values[feature.ordinal]
    fun named(key: String): Double = namedValues[key] ?: 0.0
    fun namedWeights(): Map<String, Double> = namedValues.toSortedMap()
    fun toDoubleArray(): DoubleArray = values.copyOf()
    fun withProvenance(p: LearnedBattleSupportProvenance) = LearnedBattleSupportWeights(values.copyOf(), namedValues, p)

    fun save(path: Path) {
        path.parent?.let(Files::createDirectories)
        Files.writeString(path, buildString {
            appendLine("formatVersion=1")
            appendLine("policy=battle-support-v1")
            appendLine("trainingStatus=${provenance.trainingStatus}")
            appendLine("trainedRoundPattern=${provenance.roundPattern}")
            appendLine("trainedGrove=${provenance.grove}")
            provenance.generations?.let { appendLine("trainingGenerations=$it") }
            provenance.gamesPerPolicy?.let { appendLine("trainingGamesPerPolicy=$it") }
            provenance.population?.let { appendLine("trainingPopulation=$it") }
            provenance.playerCount?.let { appendLine("trainingPlayerCount=$it") }
            provenance.evolutionSeed?.let { appendLine("trainingEvolutionSeed=$it") }
            provenance.mechanicalSeedStart?.let { appendLine("trainingMechanicalSeedStart=$it") }
            provenance.strategySeedStart?.let { appendLine("trainingStrategySeedStart=$it") }
            provenance.fitness?.let { appendLine("trainingFitness=$it") }
            provenance.cardManifest?.let { manifest ->
                appendLine("cardManifestFormatVersion=${PlantCardManifest.FORMAT_VERSION}")
                appendLine("cardCatalogFingerprint=${manifest.catalogFingerprint}")
                manifest.entries.sortedBy { it.cardId }.forEach { e ->
                    appendLine("card.${e.cardId}.title=${e.title.replace("\\", "\\\\").replace("\n", "\\n").replace("=", "\\=")}")
                    appendLine("card.${e.cardId}.fingerprint=${e.fingerprint}")
                }
            }
            BattleSupportFeature.entries.forEach { appendLine("${it.name}=${this@LearnedBattleSupportWeights[it]}") }
            namedValues.toSortedMap().forEach { (k,v) -> appendLine("$k=$v") }
        })
    }

    companion object {
        fun plantFeature(id: String) = "PLANT_$id"
        fun effectFeature(effect: GameEffect) = "EFFECT_${effect.name}"
        fun wispFeature(name: String) = "WISP_$name"
        fun isNamedFeatureKey(key: String) = key.startsWith("PLANT_") || key.startsWith("EFFECT_") || key.startsWith("WISP_")

        fun zeros(provenance: LearnedBattleSupportProvenance = LearnedBattleSupportProvenance(), namedFeatureKeys: Collection<String> = emptyList()) =
            LearnedBattleSupportWeights(DoubleArray(BattleSupportFeature.entries.size), namedFeatureKeys.associateWith { 0.0 }, provenance)

        fun fromDoubleArray(values: DoubleArray, provenance: LearnedBattleSupportProvenance = LearnedBattleSupportProvenance(), namedValues: Map<String,Double> = emptyMap()): LearnedBattleSupportWeights {
            require(values.size == BattleSupportFeature.entries.size)
            require(values.all { it.isFinite() })
            require(namedValues.all { isNamedFeatureKey(it.key) && it.value.isFinite() })
            return LearnedBattleSupportWeights(values.copyOf(), namedValues.toMap(), provenance)
        }

        fun load(path: Path): LearnedBattleSupportWeights {
            val lines = Files.readAllLines(path).filter { it.isNotBlank() && !it.trimStart().startsWith("#") }
            val entries = linkedMapOf<String,String>()
            for (line in lines) {
                val idx = line.indexOf('='); require(idx > 0) { "Invalid Battle Support weight line: $line" }
                entries[line.substring(0, idx).trim()] = line.substring(idx + 1).trim()
            }
            require(entries["formatVersion"] == "1")
            require(entries["policy"] == "battle-support-v1")
            val missing = BattleSupportFeature.entries.filter { it.name !in entries }
            require(missing.isEmpty()) { "Missing Battle Support weights: ${missing.map { it.name }}" }
            val values = DoubleArray(BattleSupportFeature.entries.size) { i -> entries.getValue(BattleSupportFeature.entries[i].name).toDouble() }
            val named = entries.filterKeys(::isNamedFeatureKey).mapValues { it.value.toDouble() }
            val manifest = entries["cardCatalogFingerprint"]?.let { fp ->
                val ids = entries.keys.filter { it.startsWith("card.") && it.endsWith(".fingerprint") }.map { it.removePrefix("card.").removeSuffix(".fingerprint") }
                PlantCardManifest(ids.map { id ->
                    PlantCardManifestEntry(
                        cardId = id,
                        title = entries["card.$id.title"] ?: id,
                        quantity = 0,
                        type = "unknown",
                        cost = 0,
                        effect = "unknown",
                        scoringRule = "unknown",
                        fingerprint = entries.getValue("card.$id.fingerprint")
                    )
                }).also { require(it.catalogFingerprint == fp) { "Battle Support Plant manifest fingerprint mismatch inside weight file" } }
            }
            return LearnedBattleSupportWeights(values, named, LearnedBattleSupportProvenance(
                trainingStatus = entries["trainingStatus"] ?: "unknown",
                roundPattern = entries["trainedRoundPattern"] ?: "unknown",
                grove = entries["trainedGrove"] ?: "unknown",
                generations = entries["trainingGenerations"]?.toInt(), gamesPerPolicy = entries["trainingGamesPerPolicy"]?.toInt(),
                population = entries["trainingPopulation"]?.toInt(), playerCount = entries["trainingPlayerCount"]?.toInt(),
                evolutionSeed = entries["trainingEvolutionSeed"]?.toLong(), mechanicalSeedStart = entries["trainingMechanicalSeedStart"]?.toLong(),
                strategySeedStart = entries["trainingStrategySeedStart"]?.toLong(), fitness = entries["trainingFitness"]?.toDouble(), cardManifest = manifest
            ))
        }
    }
}
