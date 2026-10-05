package dugsolutions.leaf.v35.player.decision.learned.plant

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.learned.buy.PlantCardManifest
import dugsolutions.leaf.v35.player.decision.learned.buy.PlantCardManifestEntry
import java.nio.file.Files
import java.nio.file.Path

data class LearnedPlantEffectProvenance(
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
    val cardManifest: PlantCardManifest? = null,
    val roundConfiguration: String? = null
)

class LearnedPlantEffectWeights private constructor(
    private val values: DoubleArray,
    private val namedValues: Map<String, Double>,
    val provenance: LearnedPlantEffectProvenance
) {
    operator fun get(feature: PlantEffectFeature): Double = values[feature.ordinal]
    fun named(key: String): Double = namedValues[key] ?: 0.0
    fun namedWeights(): Map<String, Double> = namedValues.toSortedMap()
    fun toDoubleArray(): DoubleArray = values.copyOf()
    fun withProvenance(p: LearnedPlantEffectProvenance) = LearnedPlantEffectWeights(values.copyOf(), namedValues, p)

    fun save(path: Path) {
        path.parent?.let(Files::createDirectories)
        Files.writeString(path, buildString {
            appendLine("formatVersion=1")
            appendLine("policy=plant-effect-v1")
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
            provenance.roundConfiguration?.let { appendLine("trainedRoundConfiguration=${escape(it)}") }
            provenance.cardManifest?.let { manifest ->
                appendLine("cardManifestFormatVersion=${PlantCardManifest.FORMAT_VERSION}")
                appendLine("cardCatalogFingerprint=${manifest.catalogFingerprint}")
                manifest.entries.sortedBy { it.cardId }.forEach { e ->
                    appendLine("card.${e.cardId}.title=${escape(e.title)}")
                    appendLine("card.${e.cardId}.fingerprint=${e.fingerprint}")
                }
            }
            PlantEffectFeature.entries.forEach { appendLine("${it.name}=${this@LearnedPlantEffectWeights[it]}") }
            namedValues.toSortedMap().forEach { (k,v) -> appendLine("$k=$v") }
        })
    }

    companion object {
        fun plantFeature(id: String) = "PLANT_$id"
        fun effectFeature(effect: GameEffect) = "EFFECT_${effect.name}"
        fun decisionFeature(kind: String) = "DECISION_${kind.uppercase()}"
        fun isNamedFeatureKey(k: String) = k.startsWith("PLANT_") || k.startsWith("EFFECT_") || k.startsWith("DECISION_")

        fun zeros(p: LearnedPlantEffectProvenance = LearnedPlantEffectProvenance(), keys: Collection<String> = emptyList()) =
            LearnedPlantEffectWeights(DoubleArray(PlantEffectFeature.entries.size), keys.associateWith { 0.0 }, p)

        fun fromDoubleArray(v: DoubleArray, p: LearnedPlantEffectProvenance = LearnedPlantEffectProvenance(), n: Map<String,Double> = emptyMap()): LearnedPlantEffectWeights {
            require(v.size == PlantEffectFeature.entries.size)
            require(v.all { it.isFinite() })
            require(n.all { isNamedFeatureKey(it.key) && it.value.isFinite() })
            return LearnedPlantEffectWeights(v.copyOf(), n.toMap(), p)
        }

        fun load(path: Path): LearnedPlantEffectWeights {
            val e=linkedMapOf<String,String>()
            Files.readAllLines(path).filter { it.isNotBlank() && !it.trimStart().startsWith("#") }.forEach { line ->
                val i=line.indexOf('='); require(i>0) { "Invalid Plant Effect weight line: $line" }; e[line.substring(0,i).trim()]=line.substring(i+1).trim()
            }
            require(e["formatVersion"]=="1"); require(e["policy"]=="plant-effect-v1")
            val values=DoubleArray(PlantEffectFeature.entries.size){ i -> e.getValue(PlantEffectFeature.entries[i].name).toDouble() }
            val named=e.filterKeys(::isNamedFeatureKey).mapValues { it.value.toDouble() }
            val manifest=e["cardCatalogFingerprint"]?.let { fp ->
                val ids=e.keys.filter { it.startsWith("card.") && it.endsWith(".fingerprint") }.map { it.removePrefix("card.").removeSuffix(".fingerprint") }
                PlantCardManifest(ids.map { id -> PlantCardManifestEntry(id, unescape(e["card.$id.title"]?:id),0,"unknown",0,"unknown","unknown",e.getValue("card.$id.fingerprint")) })
                    .also { require(it.catalogFingerprint==fp) { "Plant Effect Plant manifest fingerprint mismatch inside weight file" } }
            }
            return LearnedPlantEffectWeights(values,named,LearnedPlantEffectProvenance(
                trainingStatus=e["trainingStatus"]?:"unknown",roundPattern=e["trainedRoundPattern"]?:"unknown",grove=e["trainedGrove"]?:"unknown",
                generations=e["trainingGenerations"]?.toInt(),gamesPerPolicy=e["trainingGamesPerPolicy"]?.toInt(),population=e["trainingPopulation"]?.toInt(),playerCount=e["trainingPlayerCount"]?.toInt(),
                evolutionSeed=e["trainingEvolutionSeed"]?.toLong(),mechanicalSeedStart=e["trainingMechanicalSeedStart"]?.toLong(),strategySeedStart=e["trainingStrategySeedStart"]?.toLong(),fitness=e["trainingFitness"]?.toDouble(),cardManifest=manifest,
                roundConfiguration=e["trainedRoundConfiguration"]?.let(::unescape)
            ))
        }
        private fun escape(s:String)=s.replace("\\","\\\\").replace("\n","\\n").replace("=","\\=")
        private fun unescape(s:String)=s.replace("\\n","\n").replace("\\=","=").replace("\\\\","\\")
    }
}
