package dugsolutions.leaf.simulation.v35.experiment.plant

import dugsolutions.leaf.v35.plant.domain.PlantCard
import java.nio.file.Path

/**
 * Shared research-facing resolution of an optional Plant override file.
 *
 * Training and evaluation deliberately use this same implementation so the
 * same file cannot acquire runner-specific semantics.
 */
data class PlantExperimentResearchConfig(
    val sourcePath: Path?,
    val values: PlantExperimentConfig
) {
    val isActive: Boolean
        get() = sourcePath != null

    fun effectiveCosts(cards: Collection<PlantCard>): Set<Int> =
        cards.map(values::costFor).toSet()

    fun render(canonicalCards: Collection<PlantCard>): String {
        if (!isActive) return ""

        val interventions = canonicalCards
            .sortedBy { it.name }
            .mapNotNull { card ->
                val override = values.overrideFor(card) ?: return@mapNotNull null
                val lines = buildList {
                    override.cost?.let { cost ->
                        if (cost != card.cost) add("  cost: ${card.cost} -> $cost")
                    }
                    override.available?.let { available ->
                        if (!available) add("  available: true -> false")
                    }
                }
                if (lines.isEmpty()) null else card.name to lines
            }

        return buildString {
            appendLine("PLANT EXPERIMENT OVERRIDES")
            appendLine("source: $sourcePath")
            if (interventions.isEmpty()) {
                appendLine("(no effective cost or availability interventions)")
            } else {
                interventions.forEachIndexed { index, (cardId, lines) ->
                    if (index > 0) appendLine()
                    appendLine(cardId)
                    lines.forEach { appendLine(it) }
                }
            }
            append("All unspecified Plant properties canonical.")
        }
    }

    companion object {
        fun resolve(
            sourcePath: Path?,
            canonicalCards: Collection<PlantCard>
        ): PlantExperimentResearchConfig {
            val values = if (sourcePath == null) {
                PlantExperimentConfig.EMPTY
            } else {
                PlantExperimentConfigLoader(canonicalCards).load(sourcePath.toString())
            }
            return PlantExperimentResearchConfig(sourcePath, values)
        }
    }
}
