package dugsolutions.leaf.v35.player.decision.learned.battle.main

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.player.decision.learned.buy.LearnedPolicyCardManifestMismatchException
import dugsolutions.leaf.v35.player.decision.learned.buy.PlantCardManifest

object LearnedBattleMainCatalog {
    fun namedFeatureKeys(cards: Collection<PlantCard>): Set<String> = buildSet {
        cards.forEach { add(LearnedBattleMainWeights.plantFeature(it.name)) }
        GameEffect.entries.forEach { add(LearnedBattleMainWeights.effectFeature(it)) }
    }

    fun prepare(seed: LearnedBattleMainWeights, cards: Collection<PlantCard>): LearnedBattleMainWeights {
        val manifest = PlantCardManifest.from(cards)
        val historical = seed.provenance.cardManifest
        if (seed.provenance.trainingStatus == "trained" && historical != null && historical.catalogFingerprint != manifest.catalogFingerprint) {
            throw LearnedPolicyCardManifestMismatchException(
                "Learned Battle Main Plant definitions differ from the current catalog; migrate/reset identity weights and retrain."
            )
        }
        val named = seed.namedWeights().toMutableMap()
        namedFeatureKeys(cards).forEach { named.putIfAbsent(it, 0.0) }
        return LearnedBattleMainWeights.fromDoubleArray(
            seed.toDoubleArray(),
            seed.provenance.copy(cardManifest = manifest),
            named
        )
    }

    fun validateCurrentSchema(weights: LearnedBattleMainWeights, cards: Collection<PlantCard>) {
        val expectedManifest = PlantCardManifest.from(cards)
        val actualManifest = requireNotNull(weights.provenance.cardManifest) { "Battle Main weights have no Plant manifest" }
        require(actualManifest.catalogFingerprint == expectedManifest.catalogFingerprint) {
            "Learned Battle Main Plant catalog mismatch: weights=${actualManifest.catalogFingerprint} current=${expectedManifest.catalogFingerprint}"
        }
        val missing = namedFeatureKeys(cards) - weights.namedWeights().keys
        require(missing.isEmpty()) { "Battle Main weights missing named features: ${missing.sorted()}" }
    }
}
