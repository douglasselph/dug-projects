package dugsolutions.leaf.v35.player.decision.learned.battle

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.player.decision.learned.buy.PlantCardManifest

object LearnedBattleSupportCatalog {
    fun prepare(seed: LearnedBattleSupportWeights, cards: Collection<PlantCard>): LearnedBattleSupportWeights {
        val manifest = PlantCardManifest.from(cards)
        val keys = buildSet {
            cards.forEach { add(LearnedBattleSupportWeights.plantFeature(it.name)) }
            GameEffect.entries.forEach { add(LearnedBattleSupportWeights.effectFeature(it)) }
        }
        val named = keys.associateWith { seed.named(it) } + seed.namedWeights()
        return LearnedBattleSupportWeights.fromDoubleArray(
            seed.toDoubleArray(),
            seed.provenance.copy(cardManifest = manifest),
            named
        )
    }

    fun validateCurrentSchema(weights: LearnedBattleSupportWeights, cards: Collection<PlantCard>) {
        val expected = PlantCardManifest.from(cards)
        val actual = requireNotNull(weights.provenance.cardManifest) { "Battle Support weights have no Plant manifest" }
        require(actual.catalogFingerprint == expected.catalogFingerprint) {
            "Learned Battle Support Plant catalog mismatch: weights=${actual.catalogFingerprint} current=${expected.catalogFingerprint}"
        }
    }
}
