package dugsolutions.leaf.simulation.v35.experiment.round

import dugsolutions.leaf.v35.effect.RoundEffectSlot
import dugsolutions.leaf.v35.round.domain.RoundCard
import java.nio.file.Path

/** Shared train/evaluate resolution and reporting for optional Round overrides. */
data class RoundExperimentResearchConfig(
    val sourcePath: Path?,
    val values: RoundExperimentConfig
) {
    val isActive: Boolean get() = sourcePath != null

    fun render(canonicalCards: Collection<RoundCard>): String {
        if (!isActive) return ""
        val byName = canonicalCards.associateBy { it.name }
        return buildString {
            appendLine("ROUND EXPERIMENT OVERRIDES")
            appendLine("source: $sourcePath")
            if (values.overrides.isEmpty()) {
                appendLine("(no effective Round interventions)")
            } else {
                values.overrides.forEachIndexed { index, override ->
                    if (index > 0) appendLine()
                    val card = requireNotNull(byName[override.roundCardName])
                    val canonical = when (override.slot) {
                        RoundEffectSlot.FIRST -> card.firstEffect.effect
                        RoundEffectSlot.SECOND -> card.secondEffect.effect
                    }
                    appendLine(override.roundCardName)
                    appendLine("  effect ${if (override.slot == RoundEffectSlot.FIRST) 1 else 2}: $canonical -> ${override.effect}")
                }
            }
            append("All unspecified Round effects canonical.")
        }
    }

    companion object {
        fun resolve(sourcePath: Path?, canonicalCards: Collection<RoundCard>): RoundExperimentResearchConfig {
            val values = if (sourcePath == null) {
                RoundExperimentConfig.EMPTY
            } else {
                RoundExperimentConfigLoader(canonicalCards).load(sourcePath.toString())
            }
            return RoundExperimentResearchConfig(sourcePath, values)
        }
    }
}
