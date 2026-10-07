package dugsolutions.leaf.v35.player.decision.learned.buy

import dugsolutions.leaf.v35.plant.domain.PlantCard

class LearnedPolicyCardManifestMismatchException(message: String) : IllegalStateException(message)

/** Keeps learned card identity schema aligned with the canonical parsed CardDataFiles catalog. */
object LearnedBuyCardCatalog {
    fun namedFeatureKeys(
        cards: Collection<PlantCard>,
        additionalPlantCosts: Collection<Int> = emptyList()
    ): Set<String> = buildSet {
        cards.forEach { card ->
            add(LearnedBuyWeights.cardFeature(card.name))
        }
        (cards.map { it.cost } + additionalPlantCosts).distinct().forEach { cost ->
            add(LearnedBuyWeights.costFeature(cost))
            (1..3).forEach { stage -> add("${LearnedBuyWeights.costFeature(cost)}_STAGE_$stage") }
            add("${LearnedBuyWeights.costFeature(cost)}_STAGE_4_PLUS")
        }
    }

    fun prepare(
        weights: LearnedBuyWeights,
        cards: Collection<PlantCard>,
        additionalPlantCosts: Collection<Int> = emptyList()
    ): LearnedBuyWeights {
        val current=PlantCardManifest.from(cards)
        val historical=weights.provenance.cardManifest
        if (weights.provenance.trainingStatus == "trained" && historical != null && historical.catalogFingerprint != current.catalogFingerprint) {
            throw mismatch(historical,current)
        }
        var prepared=LearnedBuyWeights.fromDoubleArray(weights.toDoubleArray(), weights.provenance.copy(cardManifest=current), weights.namedWeights())
        namedFeatureKeys(cards, additionalPlantCosts).forEach { key -> if (key !in prepared.namedWeights()) prepared=prepared.withNamed(key,0.0) }
        return prepared
    }

    fun validateCurrentSchema(weights: LearnedBuyWeights, cards: Collection<PlantCard>) {
        val expected=namedFeatureKeys(cards); val actual=weights.namedWeights().keys
        val missing=expected-actual; val obsolete=actual-expected
        require(missing.isEmpty() && obsolete.isEmpty()) {
            buildString {
                appendLine("AI CARD FEATURE CATALOG IS OUT OF DATE")
                if(missing.isNotEmpty()) appendLine("Missing learned features: ${missing.sorted().joinToString()}")
                if(obsolete.isNotEmpty()) appendLine("Obsolete learned features: ${obsolete.sorted().joinToString()}")
                append("Regenerate/update data/ai/4p/buy-policy-v1.weights from the current CardDataFiles catalog before training.")
            }
        }
    }

    private fun mismatch(old:PlantCardManifest,current:PlantCardManifest): LearnedPolicyCardManifestMismatchException {
        val a=old.byId(); val b=current.byId(); val added=b.keys-a.keys; val removed=a.keys-b.keys
        val changed=(a.keys intersect b.keys).filter { a.getValue(it).fingerprint!=b.getValue(it).fingerprint }
        return LearnedPolicyCardManifestMismatchException(buildString {
            appendLine("Learned Buy policy Plant card definitions differ from the current CardDataFiles catalog.")
            if(changed.isNotEmpty()) appendLine("Changed: ${changed.sorted().joinToString()}")
            if(added.isNotEmpty()) appendLine("Added: ${added.sorted().joinToString()}")
            if(removed.isNotEmpty()) appendLine("Removed: ${removed.sorted().joinToString()}")
            append("The CSV files are authoritative. Migrate/reset affected identity weights as appropriate, then retrain and revalidate the policy.")
        })
    }
}
