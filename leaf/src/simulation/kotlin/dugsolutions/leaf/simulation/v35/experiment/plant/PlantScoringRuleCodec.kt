package dugsolutions.leaf.simulation.v35.experiment.plant

import dugsolutions.leaf.v35.plant.domain.PlantScoringRule

/** Human-readable research-file syntax for structured Plant scoring rules. */
internal object PlantScoringRuleCodec {
    fun parse(expression: String): PlantScoringRule {
        val value = expression.trim()
        val normalized = value.uppercase()

        return when (normalized) {
            "PER_GRAFTED_VINE" -> PlantScoringRule.PerGraftedVine
            "PER_GRAFTED_FLOWER" -> PlantScoringRule.PerGraftedFlower
            "PER_BUTTERFLY" -> PlantScoringRule.PerButterfly
            "PER_OWNED_D4" -> PlantScoringRule.PerOwnedD4
            else -> parseFixed(value, normalized)
        }
    }

    fun format(rule: PlantScoringRule): String =
        when (rule) {
            is PlantScoringRule.Fixed -> "FIXED:${rule.points}"
            PlantScoringRule.PerGraftedVine -> "PER_GRAFTED_VINE"
            PlantScoringRule.PerGraftedFlower -> "PER_GRAFTED_FLOWER"
            PlantScoringRule.PerButterfly -> "PER_BUTTERFLY"
            PlantScoringRule.PerOwnedD4 -> "PER_OWNED_D4"
        }

    private fun parseFixed(value: String, normalized: String): PlantScoringRule {
        if (!normalized.startsWith("FIXED:")) {
            throw IllegalArgumentException("Unknown Plant scoring expression '$value'")
        }

        val rawPoints = value.substringAfter(':').trim()
        val points = rawPoints.toIntOrNull()
            ?: throw IllegalArgumentException(
                "Invalid FIXED Plant scoring expression '$value'; expected FIXED:<non-negative integer>"
            )
        require(points >= 0) {
            "Invalid FIXED Plant scoring expression '$value'; points must be non-negative"
        }
        return PlantScoringRule.Fixed(points)
    }
}
