package dugsolutions.leaf.v35.player.decision.learned.plant

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.player.decision.learned.buy.PlantCardManifest

object LearnedPlantEffectCatalog {
    private val decisionKinds = listOf(
        "DIE","BATTLE_DIE","ROOT_WELL","CROSS_SWAP","OPTIONAL_DIE","DICE","DIE_PAIR","OPTIONAL_DIE_PAIR",
        "CRITTER_DIE","PETAL_SOURCE","PETAL_BRANCH","BEE_SOURCE","BUTTERFLY","OPTIONAL_PLANT","OPPONENT_PLANT_WOUND",
        "PLANT_EFFECT","O_EDELWEISS","WISPS_KEEP","DIE_SIZE","PLAYER","STRIKE_ROW"
    )

    fun prepare(seed: LearnedPlantEffectWeights, cards: Collection<PlantCard>): LearnedPlantEffectWeights {
        val manifest=PlantCardManifest.from(cards)
        val keys=buildSet {
            cards.forEach { add(LearnedPlantEffectWeights.plantFeature(it.name)) }
            GameEffect.entries.forEach { add(LearnedPlantEffectWeights.effectFeature(it)) }
            decisionKinds.forEach { add(LearnedPlantEffectWeights.decisionFeature(it)) }
        }
        return LearnedPlantEffectWeights.fromDoubleArray(seed.toDoubleArray(),seed.provenance.copy(cardManifest=manifest),keys.associateWith { seed.named(it) } + seed.namedWeights())
    }

    fun validateCurrentSchema(weights: LearnedPlantEffectWeights, cards: Collection<PlantCard>) {
        val expected=PlantCardManifest.from(cards)
        val actual=requireNotNull(weights.provenance.cardManifest) { "Plant Effect weights have no Plant manifest" }
        require(actual.catalogFingerprint==expected.catalogFingerprint) {
            "Learned Plant Effect Plant catalog mismatch: weights=${actual.catalogFingerprint} current=${expected.catalogFingerprint}"
        }
    }
}
