package dugsolutions.leaf.v35.player.decision.learned.cultivation

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.player.decision.learned.buy.LearnedPolicyCardManifestMismatchException
import dugsolutions.leaf.v35.player.decision.learned.buy.PlantCardManifest

/** Keeps stable Plant/effect identity features aligned with the current card/effect catalog. */
object LearnedCultivationMainCatalog {
    fun namedFeatureKeys(
        cards: Collection<PlantCard>,
        additionalPlantCosts: Collection<Int> = emptyList()
    ): Set<String> = buildSet {
        cards.forEach { add(LearnedCultivationMainWeights.cardFeature(it.name)) }
        (cards.map { it.cost } + additionalPlantCosts).distinct().forEach {
            add(LearnedCultivationMainWeights.costFeature(it))
        }
        GameEffect.entries.filter { it != GameEffect.UNKNOWN }.forEach { effect ->
            add(LearnedCultivationMainWeights.effectFeature(effect))
            add(LearnedCultivationMainWeights.effectBattleNextFeature(effect))
            add(LearnedCultivationMainWeights.effectBattlesRemainingFeature(effect))
            add(LearnedCultivationMainWeights.effectSunlightHeldFeature(effect))
            add(LearnedCultivationMainWeights.effectMainActionsRemainingFeature(effect))
        }
    }

    fun prepare(
        weights: LearnedCultivationMainWeights,
        cards: Collection<PlantCard>,
        additionalPlantCosts: Collection<Int> = emptyList()
    ): LearnedCultivationMainWeights {
        val current = PlantCardManifest.from(cards)
        val historical = weights.provenance.cardManifest
        if (
            weights.provenance.trainingStatus == "trained" &&
            historical != null &&
            historical.catalogFingerprint != current.catalogFingerprint
        ) {
            throw LearnedPolicyCardManifestMismatchException(
                "Learned Cultivation Main policy Plant card definitions differ from the current CardDataFiles catalog. " +
                    "The CSV files are authoritative; migrate/reset affected card identity weights and retrain."
            )
        }
        var prepared = LearnedCultivationMainWeights.fromDoubleArray(
            weights.toDoubleArray(),
            weights.provenance.copy(cardManifest = current),
            weights.namedWeights()
        )
        namedFeatureKeys(cards, additionalPlantCosts).forEach { key ->
            if (key !in prepared.namedWeights()) prepared = prepared.withNamed(key, 0.0)
        }
        return prepared
    }

    fun validateCurrentSchema(
        weights: LearnedCultivationMainWeights,
        cards: Collection<PlantCard>,
        additionalPlantCosts: Collection<Int> = emptyList()
    ) {
        val expected = namedFeatureKeys(cards, additionalPlantCosts)
        val actual = weights.namedWeights().keys
        val missing = expected - actual
        val obsolete = actual - expected
        require(missing.isEmpty() && obsolete.isEmpty()) {
            buildString {
                appendLine("CULTIVATION MAIN FEATURE CATALOG IS OUT OF DATE")
                if (missing.isNotEmpty()) appendLine("Missing features: ${missing.sorted().joinToString()}")
                if (obsolete.isNotEmpty()) appendLine("Obsolete features: ${obsolete.sorted().joinToString()}")
                append("Regenerate/prepare the policy against the current CardDataFiles catalog before use.")
            }
        }
    }
}
