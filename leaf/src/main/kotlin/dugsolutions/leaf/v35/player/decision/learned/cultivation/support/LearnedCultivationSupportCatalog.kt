package dugsolutions.leaf.v35.player.decision.learned.cultivation.support

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.player.decision.learned.buy.LearnedPolicyCardManifestMismatchException
import dugsolutions.leaf.v35.player.decision.learned.buy.PlantCardManifest

object LearnedCultivationSupportCatalog {
    fun namedFeatureKeys(cards: Collection<PlantCard>): Set<String> = buildSet {
        cards.forEach { add(LearnedCultivationSupportWeights.plantFeature(it.name)) }
        GameEffect.entries.filter { it != GameEffect.UNKNOWN }.forEach { effect ->
            add(LearnedCultivationSupportWeights.effectFeature(effect))
            listOf("PASS","WISP","WATER_REROLL","WATER_REFRESH","MULCH","WORM_FLIP","BUTTERFLY").forEach { kind ->
                add(LearnedCultivationSupportWeights.actionEffectFeature(kind, effect))
            }
        }
    }

    fun prepare(weights: LearnedCultivationSupportWeights, cards: Collection<PlantCard>): LearnedCultivationSupportWeights {
        val current=PlantCardManifest.from(cards)
        val historical=weights.provenance.cardManifest
        if(weights.provenance.trainingStatus=="trained" && historical!=null && historical.catalogFingerprint!=current.catalogFingerprint) {
            throw LearnedPolicyCardManifestMismatchException("Learned Cultivation Support policy Plant definitions differ from the current CardDataFiles catalog; retrain the policy.")
        }
        var prepared=LearnedCultivationSupportWeights.fromDoubleArray(weights.toDoubleArray(), weights.provenance.copy(cardManifest=current), weights.namedWeights())
        namedFeatureKeys(cards).forEach { if(it !in prepared.namedWeights()) prepared=prepared.withNamed(it,0.0) }
        return prepared
    }

    fun validateCurrentSchema(weights: LearnedCultivationSupportWeights, cards: Collection<PlantCard>) {
        val expected=namedFeatureKeys(cards); val actual=weights.namedWeights().keys
        require(expected==actual) { "CULTIVATION SUPPORT FEATURE CATALOG IS OUT OF DATE. Missing=${(expected-actual).sorted()} obsolete=${(actual-expected).sorted()}" }
    }
}
