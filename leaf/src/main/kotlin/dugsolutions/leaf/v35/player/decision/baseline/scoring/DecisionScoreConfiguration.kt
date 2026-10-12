package dugsolutions.leaf.v35.player.decision.baseline.scoring

import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties

/** A parameter catalog: old Human Baseline scores are preserved unless an override is supplied. */
class HumanScoringConfiguration private constructor(private val values: Map<String, ScoreValue>) {
    fun value(id: String, fallback: ScoreValue): ScoreValue = values[id] ?: fallback
    fun influence(id: String, description: String, fallback: ScoreValue): ScoreInfluence =
        ScoreInfluence(id, description, value(id, fallback))

    /** Override a calibrated score without adding a second external-weight system. */
    fun withValue(id: String, value: ScoreValue): HumanScoringConfiguration =
        HumanScoringConfiguration(values + (id to value))
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

/** Configures policy routing only; score values belong in HumanScoringConfiguration. */
class DecisionExperimentConfiguration private constructor(
    val cultivationMainPolicy: CultivationMainPolicyMode
) {
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
            require(props.stringPropertyNames().none { it.startsWith("external.") }) {
                "external.* preferences have been removed; edit data/ai/human-scoring.properties instead (e.g. compost.base=85)"
            }
            return DecisionExperimentConfiguration(mode)
        }
    }
}
