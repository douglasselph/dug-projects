package dugsolutions.leaf.v35.player.decision.baseline.scoring

import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties

/** A parameter catalog: old Human Baseline scores are preserved unless an override is supplied. */
class HumanScoringConfiguration private constructor(private val values: Map<String, ScoreValue>) {
    fun value(id: String, fallback: ScoreValue): ScoreValue = values[id] ?: fallback
    fun influence(id: String, description: String, fallback: ScoreValue): ScoreInfluence =
        ScoreInfluence(id, description, value(id, fallback))
    companion object {
        fun empty(): HumanScoringConfiguration = HumanScoringConfiguration(emptyMap())
        fun load(path: Path): HumanScoringConfiguration {
            val props = Properties()
            Files.newInputStream(path).use(props::load)
            return fromProperties(props)
        }
        fun fromProperties(props: Properties): HumanScoringConfiguration {
            val result = props.stringPropertyNames().associateWith { id ->
                try { ScoreValue.parse(props.getProperty(id)) }
                catch (e: RuntimeException) {
                    throw IllegalArgumentException("Invalid human-scoring property '$id': ${props.getProperty(id)}", e)
                }
            }
            return HumanScoringConfiguration(result)
        }
    }
}

enum class CultivationMainPolicyMode {
    LEARNED, HUMAN_BASELINE, HUMAN_WHEN_COMPOST_AVAILABLE, LEGACY_HUMAN_BASELINE
}

/** External weights are added to Human Baseline scores, not to learned model logits. */
class DecisionExperimentConfiguration private constructor(
    val cultivationMainPolicy: CultivationMainPolicyMode,
    private val externalWeights: Map<String, Int>
) {
    fun externalWeight(actionId: String): Int = externalWeights[actionId] ?: 0
    fun externalInfluence(actionId: String): ScoreInfluence = ScoreInfluence(
        "external.$actionId", "Experiment preference for $actionId", ScoreValue.Fixed(externalWeight(actionId))
    )
    companion object {
        fun load(path: Path): DecisionExperimentConfiguration {
            val props = Properties()
            Files.newInputStream(path).use(props::load)
            return fromProperties(props)
        }
        fun fromProperties(props: Properties): DecisionExperimentConfiguration {
            val mode = CultivationMainPolicyMode.valueOf(
                props.getProperty("cultivation.main.policy", "LEARNED").trim()
            )
            val weights = props.stringPropertyNames().filter { it.startsWith("external.") }
                .associate { key ->
                    val action = key.removePrefix("external.")
                    require(action.isNotBlank()) { "External action name missing: $key" }
                    action to (props.getProperty(key).trim().toIntOrNull()
                        ?: throw IllegalArgumentException("External weight must be an integer: $key"))
                }
            return DecisionExperimentConfiguration(mode, weights)
        }
    }
}
