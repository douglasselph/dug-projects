package dugsolutions.leaf.v35.player.decision.baseline.card.plant.flower

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.baseline.card.ConfiguredCardScorer

object OEdelweissBaseline : ConfiguredCardScorer(
    cardNames = setOf("Flower_17_03"),
    effect = GameEffect.PLAY_OR_FLIP_ANOTHER_CARD_TWICE,
    cultivationPlayBase = 85,
    battlePlayBase = 85
)
