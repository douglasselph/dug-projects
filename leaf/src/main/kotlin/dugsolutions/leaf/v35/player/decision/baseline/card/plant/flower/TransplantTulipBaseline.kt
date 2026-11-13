package dugsolutions.leaf.v35.player.decision.baseline.card.plant.flower

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.baseline.card.ConfiguredCardScorer

// The original source filename is retained to support unzip-only changed-files patches.
// This scorer now represents the authoritative Flower_11_04: Someday Sunflower.
object SomedaySunflowerBaseline : ConfiguredCardScorer(
    cardNames = setOf("Flower_11_04"),
    effect = GameEffect.GAIN_TWO_SUNLIGHT_TOKENS,
    cultivationPlayBase = 52,
    battlePlayBase = 42
)
