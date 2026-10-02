package dugsolutions.leaf.v35.player.decision.learned.cultivation

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.learned.buy.PlantCardManifest
import dugsolutions.leaf.v35.player.decision.learned.buy.PlantCardManifestEntry
import java.nio.file.Files
import java.nio.file.Path

/** Research provenance travels with a saved policy but does not affect scoring. */
data class LearnedCultivationMainProvenance(
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

/** Strict, Git-friendly persistence for the transparent Cultivation Main linear policy. */
class LearnedCultivationMainWeights private constructor(
    private val values: DoubleArray,
    private val namedValues: Map<String, Double>,
    val provenance: LearnedCultivationMainProvenance
) {
    operator fun get(feature: CultivationMainFeature): Double = values[feature.ordinal]
    fun named(key: String): Double = namedValues[key] ?: 0.0
    fun namedWeights(): Map<String, Double> = namedValues.toSortedMap()
    fun toDoubleArray(): DoubleArray = values.copyOf()

    fun withNamed(key: String, value: Double): LearnedCultivationMainWeights {
        require(isNamedFeatureKey(key)) { "Unknown learned Cultivation Main named feature: $key" }
        require(value.isFinite()) { "Weight $key must be finite: $value" }
        return LearnedCultivationMainWeights(values.copyOf(), namedValues + (key to value), provenance)
    }

    fun withProvenance(value: LearnedCultivationMainProvenance): LearnedCultivationMainWeights =
        LearnedCultivationMainWeights(values.copyOf(), namedValues, value)

    fun save(path: Path) {
        path.parent?.let(Files::createDirectories)
        val text = buildString {
            appendLine("formatVersion=1")
            appendLine("policy=cultivation-main-v1")
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
                manifest.entries.sortedBy { it.cardId }.forEach { entry ->
                    appendLine("card.${entry.cardId}.title=${escape(entry.title)}")
                    appendLine("card.${entry.cardId}.fingerprint=${entry.fingerprint}")
                }
            }
            CultivationMainFeature.entries.forEach { appendLine("${it.name}=${this@LearnedCultivationMainWeights[it]}") }
            namedValues.toSortedMap().forEach { (key, value) -> appendLine("$key=$value") }
        }
        Files.writeString(path, text)
    }

    companion object {
        private val metadataKeys = setOf(
            "formatVersion", "policy", "trainingStatus", "trainedRoundPattern", "trainedGrove",
            "trainingGenerations", "trainingGamesPerPolicy", "trainingPopulation", "trainingPlayerCount",
            "trainingMutationSigma", "trainingMutationsPerChild", "trainingEvolutionSeed",
            "trainingMechanicalSeedStart", "trainingStrategySeedStart", "trainingFitness",
            "cardManifestFormatVersion", "cardCatalogFingerprint"
        )

        fun cardFeature(cardId: String): String = "CARD_$cardId"
        fun costFeature(cost: Int): String = "ACTION_COST_$cost"
        fun effectFeature(effect: GameEffect): String = "EFFECT_${effect.name}"
        fun effectBattleNextFeature(effect: GameEffect): String = "${effectFeature(effect)}__BATTLE_NEXT"
        fun effectBattlesRemainingFeature(effect: GameEffect): String = "${effectFeature(effect)}__BATTLES_REMAINING"
        fun effectSunlightHeldFeature(effect: GameEffect): String = "${effectFeature(effect)}__SUNLIGHT_HELD"
        fun effectMainActionsRemainingFeature(effect: GameEffect): String = "${effectFeature(effect)}__MAIN_ACTIONS_REMAINING"

        fun isNamedFeatureKey(key: String): Boolean =
            key.startsWith("CARD_") ||
                key.matches(Regex("ACTION_COST_[0-9]+")) ||
                key.matches(Regex("EFFECT_[A-Z0-9_]+(?:__(?:BATTLE_NEXT|BATTLES_REMAINING|SUNLIGHT_HELD|MAIN_ACTIONS_REMAINING))?"))

        fun zeros(
            provenance: LearnedCultivationMainProvenance = LearnedCultivationMainProvenance(),
            namedFeatureKeys: Collection<String> = emptyList()
        ): LearnedCultivationMainWeights = LearnedCultivationMainWeights(
            DoubleArray(CultivationMainFeature.entries.size),
            namedFeatureKeys.associateWith { 0.0 },
            provenance
        )

        fun fromDoubleArray(
            values: DoubleArray,
            provenance: LearnedCultivationMainProvenance = LearnedCultivationMainProvenance(),
            namedValues: Map<String, Double> = emptyMap()
        ): LearnedCultivationMainWeights {
            require(values.size == CultivationMainFeature.entries.size) {
                "Expected ${CultivationMainFeature.entries.size} learned Cultivation Main weights, got ${values.size}"
            }
            values.forEachIndexed { index, value ->
                require(value.isFinite()) { "Weight ${CultivationMainFeature.entries[index]} must be finite: $value" }
            }
            namedValues.forEach { (key, value) ->
                require(isNamedFeatureKey(key) && value.isFinite()) {
                    "Invalid named learned Cultivation Main weight $key=$value"
                }
            }
            return LearnedCultivationMainWeights(values.copyOf(), namedValues.toMap(), provenance)
        }

        fun load(path: Path): LearnedCultivationMainWeights {
            val entries = Files.readAllLines(path)
                .filter { it.isNotBlank() && !it.trimStart().startsWith("#") }
                .associate { line ->
                    val parts = line.split('=', limit = 2)
                    require(parts.size == 2) { "Invalid learned Cultivation Main weight line: $line" }
                    parts[0].trim() to parts[1].trim()
                }
            require(entries["formatVersion"] == "1") {
                "Unsupported learned Cultivation Main weight formatVersion=${entries["formatVersion"]}"
            }
            require(entries["policy"] == "cultivation-main-v1") {
                "Unsupported learned Cultivation Main policy=${entries["policy"]}"
            }
            val cardMetadataKeys = entries.keys.filter { it.startsWith("card.") }.toSet()
            val namedKeys = entries.keys.filter(::isNamedFeatureKey).toSet()
            val allowed = CultivationMainFeature.entries.map { it.name }.toSet() + metadataKeys + cardMetadataKeys + namedKeys
            val unknown = entries.keys - allowed
            require(unknown.isEmpty()) { "Unknown learned Cultivation Main weight keys: ${unknown.sorted()}" }
            val missing = CultivationMainFeature.entries.filter { it.name !in entries }
            require(missing.isEmpty()) { "Missing learned Cultivation Main weights: ${missing.map { it.name }}" }
            val manifest = if (entries["cardManifestFormatVersion"] != null) parseManifest(entries) else null
            val provenance = LearnedCultivationMainProvenance(
                trainingStatus = entries["trainingStatus"] ?: "unknown",
                roundPattern = entries["trainedRoundPattern"] ?: "unknown",
                grove = entries["trainedGrove"] ?: "unknown",
                generations = entries["trainingGenerations"]?.toInt(),
                gamesPerPolicy = entries["trainingGamesPerPolicy"]?.toInt(),
                population = entries["trainingPopulation"]?.toInt(),
                playerCount = entries["trainingPlayerCount"]?.toInt(),
                mutationSigma = entries["trainingMutationSigma"]?.toDouble()?.also { require(it.isFinite()) },
                mutationsPerChild = entries["trainingMutationsPerChild"]?.toInt(),
                evolutionSeed = entries["trainingEvolutionSeed"]?.toLong(),
                mechanicalSeedStart = entries["trainingMechanicalSeedStart"]?.toLong(),
                strategySeedStart = entries["trainingStrategySeedStart"]?.toLong(),
                fitness = entries["trainingFitness"]?.toDouble()?.also { require(it.isFinite()) },
                cardManifest = manifest
            )
            val values = DoubleArray(CultivationMainFeature.entries.size) { index ->
                entries.getValue(CultivationMainFeature.entries[index].name).toDouble().also { require(it.isFinite()) }
            }
            val named = namedKeys.associateWith { key ->
                entries.getValue(key).toDouble().also { require(it.isFinite()) }
            }
            return LearnedCultivationMainWeights(values, named, provenance)
        }

        private fun parseManifest(entries: Map<String, String>): PlantCardManifest {
            require(entries["cardManifestFormatVersion"] == PlantCardManifest.FORMAT_VERSION.toString()) {
                "Unsupported cardManifestFormatVersion=${entries["cardManifestFormatVersion"]}"
            }
            val ids = entries.keys
                .filter { it.startsWith("card.") && it.endsWith(".fingerprint") }
                .map { it.removePrefix("card.").removeSuffix(".fingerprint") }
                .sorted()
            val manifest = PlantCardManifest(ids.map { id ->
                PlantCardManifestEntry(
                    cardId = id,
                    title = unescape(entries["card.$id.title"] ?: ""),
                    quantity = 0,
                    type = "",
                    cost = 0,
                    effect = "",
                    scoringRule = "",
                    fingerprint = entries.getValue("card.$id.fingerprint")
                )
            })
            require(entries["cardCatalogFingerprint"] == manifest.catalogFingerprint) {
                "Stored card catalog fingerprint does not match card manifest entries"
            }
            return manifest
        }

        private fun escape(value: String): String = value.replace("\\", "\\\\").replace("\n", "\\n").replace("=", "\\=")
        private fun unescape(value: String): String = value.replace("\\=", "=").replace("\\n", "\n").replace("\\\\", "\\")
    }
}
