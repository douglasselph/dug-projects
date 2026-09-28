package dugsolutions.leaf.v35.player.decision.learned.buy

import java.nio.file.Files
import java.nio.file.Path

/** Research provenance travels with a saved policy but does not affect scoring. */
data class LearnedBuyProvenance(
    val trainingStatus: String = "untrained", val roundPattern: String = "unknown", val grove: String = "unknown",
    val generations: Int? = null, val gamesPerPolicy: Int? = null, val population: Int? = null,
    val mutationSigma: Double? = null, val mutationsPerChild: Int? = null, val evolutionSeed: Long? = null,
    val mechanicalSeedStart: Long? = null, val strategySeedStart: Long? = null, val fitness: Double? = null,
    val cardManifest: PlantCardManifest? = null
)

/** Versioned, strict, Git-friendly persistence for learned Buy weights. */
class LearnedBuyWeights private constructor(
    private val values: DoubleArray,
    private val namedValues: Map<String, Double>,
    val provenance: LearnedBuyProvenance
) {
    operator fun get(feature: BuyFeature): Double = values[feature.ordinal]
    fun named(key: String): Double = namedValues[key] ?: 0.0
    fun namedWeights(): Map<String, Double> = namedValues.toSortedMap()
    fun toDoubleArray(): DoubleArray = values.copyOf()

    fun with(feature: BuyFeature, value: Double): LearnedBuyWeights {
        require(value.isFinite()) { "Weight $feature must be finite: $value" }
        return LearnedBuyWeights(values.copyOf().also { it[feature.ordinal] = value }, namedValues, provenance)
    }
    fun withNamed(key: String, value: Double): LearnedBuyWeights {
        require(isNamedFeatureKey(key)) { "Unknown learned Buy named feature: $key" }
        require(value.isFinite()) { "Weight $key must be finite: $value" }
        return LearnedBuyWeights(values.copyOf(), namedValues + (key to value), provenance)
    }
    fun withProvenance(value: LearnedBuyProvenance): LearnedBuyWeights = LearnedBuyWeights(values.copyOf(), namedValues, value)

    fun save(path: Path) {
        path.parent?.let(Files::createDirectories)
        val text = buildString {
            appendLine("formatVersion=3"); appendLine("policy=buy-v1")
            appendLine("trainingStatus=${provenance.trainingStatus}"); appendLine("trainedRoundPattern=${provenance.roundPattern}"); appendLine("trainedGrove=${provenance.grove}")
            provenance.generations?.let { appendLine("trainingGenerations=$it") }; provenance.gamesPerPolicy?.let { appendLine("trainingGamesPerPolicy=$it") }
            provenance.population?.let { appendLine("trainingPopulation=$it") }; provenance.mutationSigma?.let { appendLine("trainingMutationSigma=$it") }
            provenance.mutationsPerChild?.let { appendLine("trainingMutationsPerChild=$it") }; provenance.evolutionSeed?.let { appendLine("trainingEvolutionSeed=$it") }
            provenance.mechanicalSeedStart?.let { appendLine("trainingMechanicalSeedStart=$it") }; provenance.strategySeedStart?.let { appendLine("trainingStrategySeedStart=$it") }
            provenance.fitness?.let { appendLine("trainingFitness=$it") }
            provenance.cardManifest?.let { manifest ->
                appendLine("cardManifestFormatVersion=${PlantCardManifest.FORMAT_VERSION}")
                appendLine("cardCatalogFingerprint=${manifest.catalogFingerprint}")
                manifest.entries.sortedBy { it.cardId }.forEach { e ->
                    appendLine("card.${e.cardId}.title=${escape(e.title)}")
                    appendLine("card.${e.cardId}.fingerprint=${e.fingerprint}")
                }
            }
            BuyFeature.entries.forEach { appendLine("${it.name}=${this@LearnedBuyWeights[it]}") }
            namedValues.toSortedMap().forEach { (key, value) -> appendLine("$key=$value") }
        }
        Files.writeString(path, text)
    }

    companion object {
        private val metadataKeys = setOf("formatVersion","policy","trainingStatus","trainedRoundPattern","trainedGrove","trainingGenerations","trainingGamesPerPolicy","trainingPopulation","trainingMutationSigma","trainingMutationsPerChild","trainingEvolutionSeed","trainingMechanicalSeedStart","trainingStrategySeedStart","trainingFitness","cardManifestFormatVersion","cardCatalogFingerprint")
        fun cardFeature(cardId: String) = "CARD_$cardId"
        fun costFeature(cost: Int) = "ACTION_COST_$cost"
        fun isNamedFeatureKey(key: String) = key.startsWith("CARD_") || key.matches(Regex("ACTION_COST_[0-9]+(?:_STAGE_(?:1|2|3|4_PLUS))?"))

        fun zeros(provenance: LearnedBuyProvenance = LearnedBuyProvenance(), namedFeatureKeys: Collection<String> = emptyList()): LearnedBuyWeights =
            LearnedBuyWeights(DoubleArray(BuyFeature.entries.size), namedFeatureKeys.associateWith { 0.0 }, provenance)

        fun fromDoubleArray(values: DoubleArray, provenance: LearnedBuyProvenance = LearnedBuyProvenance(), namedValues: Map<String, Double> = emptyMap()): LearnedBuyWeights {
            require(values.size == BuyFeature.entries.size) { "Expected ${BuyFeature.entries.size} learned Buy weights, got ${values.size}" }
            values.forEachIndexed { i,v -> require(v.isFinite()) { "Weight ${BuyFeature.entries[i]} must be finite: $v" } }
            namedValues.forEach { (k,v) -> require(isNamedFeatureKey(k) && v.isFinite()) { "Invalid named learned Buy weight $k=$v" } }
            return LearnedBuyWeights(values.copyOf(), namedValues.toMap(), provenance)
        }

        fun load(path: Path): LearnedBuyWeights {
            val entries = Files.readAllLines(path).filter { it.isNotBlank() && !it.trimStart().startsWith("#") }.associate { line ->
                val parts=line.split('=',limit=2); require(parts.size==2) { "Invalid learned Buy weight line: $line" }; parts[0].trim() to parts[1].trim()
            }
            val version=entries["formatVersion"]; require(version in setOf("1","2","3")) { "Unsupported learned Buy weight formatVersion=$version" }
            require(entries["policy"]=="buy-v1") { "Unsupported learned Buy policy=${entries["policy"]}" }
            val cardMetadataKeys=entries.keys.filter { it.startsWith("card.") }.toSet()
            val namedKeys=entries.keys.filter(::isNamedFeatureKey).toSet()
            val allowed=BuyFeature.entries.map { it.name }.toSet()+metadataKeys+cardMetadataKeys+namedKeys
            val unknown=entries.keys-allowed; require(unknown.isEmpty()) { "Unknown learned Buy weight keys: ${unknown.sorted()}" }
            val required=if(version=="1") BuyFeature.entries.filterNot { "_STAGE_" in it.name } else BuyFeature.entries
            val missing=required.filter { it.name !in entries }; require(missing.isEmpty()) { "Missing learned Buy weights: ${missing.map { it.name }}" }
            val manifest = if (version=="3" && entries["cardManifestFormatVersion"] != null) parseManifest(entries) else null
            val provenance=if(version!="1") LearnedBuyProvenance(
                trainingStatus=entries["trainingStatus"]?:"unknown", roundPattern=entries["trainedRoundPattern"]?:"unknown", grove=entries["trainedGrove"]?:"unknown",
                generations=entries["trainingGenerations"]?.toInt(), gamesPerPolicy=entries["trainingGamesPerPolicy"]?.toInt(), population=entries["trainingPopulation"]?.toInt(),
                mutationSigma=entries["trainingMutationSigma"]?.toDouble()?.also{require(it.isFinite())}, mutationsPerChild=entries["trainingMutationsPerChild"]?.toInt(),
                evolutionSeed=entries["trainingEvolutionSeed"]?.toLong(), mechanicalSeedStart=entries["trainingMechanicalSeedStart"]?.toLong(), strategySeedStart=entries["trainingStrategySeedStart"]?.toLong(),
                fitness=entries["trainingFitness"]?.toDouble()?.also{require(it.isFinite())}, cardManifest=manifest) else LearnedBuyProvenance()
            val values=DoubleArray(BuyFeature.entries.size){ i -> entries[BuyFeature.entries[i].name]?.toDouble()?.also{require(it.isFinite())}?:0.0 }
            val named=namedKeys.associateWith { entries.getValue(it).toDouble().also { v->require(v.isFinite()) } }
            return LearnedBuyWeights(values,named,provenance)
        }

        private fun parseManifest(entries: Map<String,String>): PlantCardManifest {
            require(entries["cardManifestFormatVersion"]==PlantCardManifest.FORMAT_VERSION.toString()) { "Unsupported cardManifestFormatVersion=${entries["cardManifestFormatVersion"]}" }
            val ids=entries.keys.filter { it.startsWith("card.") && it.endsWith(".fingerprint") }.map { it.removePrefix("card.").removeSuffix(".fingerprint") }.sorted()
            // Historical snapshot intentionally stores compact title+fingerprint only. Detailed current fields come from CardDataFiles.
            val manifest=PlantCardManifest(ids.map { id -> PlantCardManifestEntry(id, unescape(entries["card.$id.title"]?:""),0,"",0,"","",entries.getValue("card.$id.fingerprint")) })
            require(entries["cardCatalogFingerprint"]==manifest.catalogFingerprint) { "Stored card catalog fingerprint does not match card manifest entries" }
            return manifest
        }
        private fun escape(v:String)=v.replace("\\","\\\\").replace("\n","\\n").replace("=","\\=")
        private fun unescape(v:String)=v.replace("\\=","=").replace("\\n","\n").replace("\\\\","\\")
    }
}
