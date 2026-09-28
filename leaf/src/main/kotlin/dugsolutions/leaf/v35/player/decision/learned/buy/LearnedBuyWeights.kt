package dugsolutions.leaf.v35.player.decision.learned.buy

import java.nio.file.Files
import java.nio.file.Path

/** Research provenance travels with a saved policy but does not affect scoring. */
data class LearnedBuyProvenance(
    val trainingStatus: String = "untrained",
    val roundPattern: String = "unknown",
    val grove: String = "unknown",
    val generations: Int? = null,
    val gamesPerPolicy: Int? = null,
    val population: Int? = null,
    val mutationSigma: Double? = null,
    val mutationsPerChild: Int? = null,
    val evolutionSeed: Long? = null,
    val mechanicalSeedStart: Long? = null,
    val strategySeedStart: Long? = null,
    val fitness: Double? = null
)

/** Versioned, strict, Git-friendly persistence for learned Buy weights. */
class LearnedBuyWeights private constructor(
    private val values: DoubleArray,
    val provenance: LearnedBuyProvenance
) {
    operator fun get(feature: BuyFeature): Double = values[feature.ordinal]
    fun toDoubleArray(): DoubleArray = values.copyOf()

    fun with(feature: BuyFeature, value: Double): LearnedBuyWeights {
        require(value.isFinite()) { "Weight $feature must be finite: $value" }
        return LearnedBuyWeights(values.copyOf().also { it[feature.ordinal] = value }, provenance)
    }

    fun withProvenance(value: LearnedBuyProvenance): LearnedBuyWeights =
        LearnedBuyWeights(values.copyOf(), value)

    fun save(path: Path) {
        path.parent?.let(Files::createDirectories)
        val text = buildString {
            appendLine("formatVersion=2")
            appendLine("policy=buy-v1")
            appendLine("trainingStatus=${provenance.trainingStatus}")
            appendLine("trainedRoundPattern=${provenance.roundPattern}")
            appendLine("trainedGrove=${provenance.grove}")
            provenance.generations?.let { appendLine("trainingGenerations=$it") }
            provenance.gamesPerPolicy?.let { appendLine("trainingGamesPerPolicy=$it") }
            provenance.population?.let { appendLine("trainingPopulation=$it") }
            provenance.mutationSigma?.let { appendLine("trainingMutationSigma=$it") }
            provenance.mutationsPerChild?.let { appendLine("trainingMutationsPerChild=$it") }
            provenance.evolutionSeed?.let { appendLine("trainingEvolutionSeed=$it") }
            provenance.mechanicalSeedStart?.let { appendLine("trainingMechanicalSeedStart=$it") }
            provenance.strategySeedStart?.let { appendLine("trainingStrategySeedStart=$it") }
            provenance.fitness?.let { appendLine("trainingFitness=$it") }
            BuyFeature.entries.forEach { appendLine("${it.name}=${this@LearnedBuyWeights[it]}") }
        }
        Files.writeString(path, text)
    }

    companion object {
        private val metadataKeys = setOf(
            "formatVersion", "policy", "trainingStatus", "trainedRoundPattern", "trainedGrove",
            "trainingGenerations", "trainingGamesPerPolicy", "trainingPopulation",
            "trainingMutationSigma", "trainingMutationsPerChild", "trainingEvolutionSeed", "trainingMechanicalSeedStart", "trainingStrategySeedStart", "trainingFitness"
        )

        fun zeros(provenance: LearnedBuyProvenance = LearnedBuyProvenance()): LearnedBuyWeights =
            LearnedBuyWeights(DoubleArray(BuyFeature.entries.size), provenance)

        fun fromDoubleArray(values: DoubleArray, provenance: LearnedBuyProvenance = LearnedBuyProvenance()): LearnedBuyWeights {
            require(values.size == BuyFeature.entries.size) {
                "Expected ${BuyFeature.entries.size} learned Buy weights, got ${values.size}"
            }
            values.forEachIndexed { index, value ->
                require(value.isFinite()) { "Weight ${BuyFeature.entries[index]} must be finite: $value" }
            }
            return LearnedBuyWeights(values.copyOf(), provenance)
        }

        fun load(path: Path): LearnedBuyWeights {
            val entries = Files.readAllLines(path).filter { it.isNotBlank() && !it.trimStart().startsWith("#") }
                .associate { line ->
                    val parts = line.split('=', limit = 2)
                    require(parts.size == 2) { "Invalid learned Buy weight line: $line" }
                    parts[0].trim() to parts[1].trim()
                }
            val version = entries["formatVersion"]
            require(version == "1" || version == "2") { "Unsupported learned Buy weight formatVersion=$version" }
            require(entries["policy"] == "buy-v1") { "Unsupported learned Buy policy=${entries["policy"]}" }
            val allowed = BuyFeature.entries.map { it.name }.toSet() + metadataKeys
            val unknown = entries.keys - allowed
            require(unknown.isEmpty()) { "Unknown learned Buy weight keys: ${unknown.sorted()}" }

            // v1 predates stage interactions; those new weights migrate as zero.
            val oldFeatures = BuyFeature.entries.filterNot { "_STAGE_" in it.name }
            val required = if (version == "1") oldFeatures else BuyFeature.entries
            val missing = required.filter { it.name !in entries }
            require(missing.isEmpty()) { "Missing learned Buy weights: ${missing.map { it.name }}" }

            val provenance = if (version == "2") LearnedBuyProvenance(
                trainingStatus = entries["trainingStatus"] ?: "unknown",
                roundPattern = entries["trainedRoundPattern"] ?: "unknown",
                grove = entries["trainedGrove"] ?: "unknown",
                generations = entries["trainingGenerations"]?.toInt(),
                gamesPerPolicy = entries["trainingGamesPerPolicy"]?.toInt(),
                population = entries["trainingPopulation"]?.toInt(),
                mutationSigma = entries["trainingMutationSigma"]?.toDouble()?.also { require(it.isFinite()) },
                mutationsPerChild = entries["trainingMutationsPerChild"]?.toInt(),
                evolutionSeed = entries["trainingEvolutionSeed"]?.toLong(),
                mechanicalSeedStart = entries["trainingMechanicalSeedStart"]?.toLong(),
                strategySeedStart = entries["trainingStrategySeedStart"]?.toLong(),
                fitness = entries["trainingFitness"]?.toDouble()?.also { require(it.isFinite()) }
            ) else LearnedBuyProvenance()

            return LearnedBuyWeights(DoubleArray(BuyFeature.entries.size) { index ->
                val feature = BuyFeature.entries[index]
                entries[feature.name]?.toDouble()?.also {
                    require(it.isFinite()) { "Weight $feature must be finite: $it" }
                } ?: 0.0
            }, provenance)
        }
    }
}
