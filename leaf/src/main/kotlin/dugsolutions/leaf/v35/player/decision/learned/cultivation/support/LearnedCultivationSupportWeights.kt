package dugsolutions.leaf.v35.player.decision.learned.cultivation.support

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.learned.buy.PlantCardManifest
import dugsolutions.leaf.v35.player.decision.learned.buy.PlantCardManifestEntry
import java.nio.file.Files
import java.nio.file.Path

data class LearnedCultivationSupportProvenance(
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
    val cardManifest: PlantCardManifest? = null,
    val roundConfiguration: String? = null
)

class LearnedCultivationSupportWeights private constructor(
    private val values: DoubleArray,
    private val namedValues: Map<String, Double>,
    val provenance: LearnedCultivationSupportProvenance
) {
    operator fun get(feature: CultivationSupportFeature): Double = values[feature.ordinal]
    fun named(key: String): Double = namedValues[key] ?: 0.0
    fun namedWeights(): Map<String, Double> = namedValues.toSortedMap()
    fun toDoubleArray(): DoubleArray = values.copyOf()

    fun withNamed(key: String, value: Double): LearnedCultivationSupportWeights {
        require(isNamedFeatureKey(key) && value.isFinite())
        return LearnedCultivationSupportWeights(values.copyOf(), namedValues + (key to value), provenance)
    }

    fun withProvenance(value: LearnedCultivationSupportProvenance) =
        LearnedCultivationSupportWeights(values.copyOf(), namedValues, value)

    fun save(path: Path) {
        path.parent?.let(Files::createDirectories)
        Files.writeString(path, buildString {
            appendLine("formatVersion=1")
            appendLine("policy=cultivation-support-v1")
            appendLine("trainingStatus=${provenance.trainingStatus}")
            appendLine("trainedRoundPattern=${provenance.roundPattern}")
            appendLine("trainedGrove=${provenance.grove}")
            provenance.roundConfiguration?.let { appendLine("trainedRoundConfiguration=${escape(it)}") }
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
            CultivationSupportFeature.entries.forEach { appendLine("${it.name}=${this@LearnedCultivationSupportWeights[it]}") }
            namedValues.toSortedMap().forEach { (k,v) -> appendLine("$k=$v") }
        })
    }

    companion object {
        private val metadataKeys = setOf(
            "formatVersion", "policy", "trainingStatus", "trainedRoundPattern", "trainedGrove", "trainedRoundConfiguration",
            "trainingGenerations", "trainingGamesPerPolicy", "trainingPopulation", "trainingPlayerCount",
            "trainingMutationSigma", "trainingMutationsPerChild", "trainingEvolutionSeed", "trainingMechanicalSeedStart",
            "trainingStrategySeedStart", "trainingFitness", "cardManifestFormatVersion", "cardCatalogFingerprint"
        )
        fun plantFeature(cardId: String) = "PLANT_$cardId"
        fun effectFeature(effect: GameEffect) = "EFFECT_${effect.name}"
        fun wispFeature(name: String) = "WISP_$name"
        fun actionEffectFeature(kind: String, effect: GameEffect) = "ACTION_${kind}__EFFECT_${effect.name}"
        fun isNamedFeatureKey(key: String) = key.startsWith("PLANT_") || key.startsWith("EFFECT_") || key.startsWith("WISP_") || key.matches(Regex("ACTION_[A-Z_]+__EFFECT_[A-Z0-9_]+"))

        fun zeros(provenance: LearnedCultivationSupportProvenance = LearnedCultivationSupportProvenance(), namedFeatureKeys: Collection<String> = emptyList()) =
            LearnedCultivationSupportWeights(DoubleArray(CultivationSupportFeature.entries.size), namedFeatureKeys.associateWith { 0.0 }, provenance)

        fun fromDoubleArray(values: DoubleArray, provenance: LearnedCultivationSupportProvenance = LearnedCultivationSupportProvenance(), namedValues: Map<String,Double> = emptyMap()): LearnedCultivationSupportWeights {
            require(values.size == CultivationSupportFeature.entries.size)
            require(values.all { it.isFinite() })
            require(namedValues.all { isNamedFeatureKey(it.key) && it.value.isFinite() })
            return LearnedCultivationSupportWeights(values.copyOf(), namedValues.toMap(), provenance)
        }

        fun load(path: Path): LearnedCultivationSupportWeights {
            val entries = linkedMapOf<String,String>()
            Files.readAllLines(path).filter { it.isNotBlank() && !it.trimStart().startsWith("#") }.forEach { line ->
                val idx=line.indexOf('='); require(idx>0) { "Invalid Cultivation Support weight line: $line" }
                entries[line.substring(0,idx).trim()] = line.substring(idx+1).trim()
            }
            require(entries["formatVersion"] == "1")
            require(entries["policy"] == "cultivation-support-v1")
            val namedKeys = entries.keys.filter(::isNamedFeatureKey).toSet()
            val cardKeys = entries.keys.filter { it.startsWith("card.") }.toSet()
            val allowed = metadataKeys + CultivationSupportFeature.entries.map { it.name } + namedKeys + cardKeys
            require((entries.keys - allowed).isEmpty()) { "Unknown Cultivation Support weight keys: ${(entries.keys-allowed).sorted()}" }
            val missing = CultivationSupportFeature.entries.filter { it.name !in entries }
            require(missing.isEmpty()) { "Missing Cultivation Support weights: ${missing.map { it.name }}" }
            val manifest = entries["cardCatalogFingerprint"]?.let { fp ->
                val ids=entries.keys.filter { it.startsWith("card.") && it.endsWith(".fingerprint") }.map { it.removePrefix("card.").removeSuffix(".fingerprint") }
                PlantCardManifest(ids.map { id -> PlantCardManifestEntry(id, unescape(entries["card.$id.title"] ?: id), 0, "unknown", 0, "unknown", "unknown", entries.getValue("card.$id.fingerprint")) })
                    .also { require(it.catalogFingerprint == fp) { "Cultivation Support Plant manifest fingerprint mismatch inside weight file" } }
            }
            val standard = DoubleArray(CultivationSupportFeature.entries.size) { entries.getValue(CultivationSupportFeature.entries[it].name).toDouble() }
            val named = entries.filterKeys(::isNamedFeatureKey).mapValues { it.value.toDouble() }
            return LearnedCultivationSupportWeights(standard, named, LearnedCultivationSupportProvenance(
                trainingStatus=entries["trainingStatus"] ?: "unknown", roundPattern=entries["trainedRoundPattern"] ?: "unknown", grove=entries["trainedGrove"] ?: "unknown",
                generations=entries["trainingGenerations"]?.toInt(), gamesPerPolicy=entries["trainingGamesPerPolicy"]?.toInt(), population=entries["trainingPopulation"]?.toInt(), playerCount=entries["trainingPlayerCount"]?.toInt(),
                mutationSigma=entries["trainingMutationSigma"]?.toDouble(), mutationsPerChild=entries["trainingMutationsPerChild"]?.toInt(), evolutionSeed=entries["trainingEvolutionSeed"]?.toLong(),
                mechanicalSeedStart=entries["trainingMechanicalSeedStart"]?.toLong(), strategySeedStart=entries["trainingStrategySeedStart"]?.toLong(), fitness=entries["trainingFitness"]?.toDouble(),
                cardManifest=manifest, roundConfiguration=entries["trainedRoundConfiguration"]?.let(::unescape)
            ))
        }

        private fun escape(s:String)=s.replace("\\","\\\\").replace("\n","\\n").replace("=","\\=")
        private fun unescape(s:String)=s.replace("\\=","=").replace("\\n","\n").replace("\\\\","\\")
    }
}
