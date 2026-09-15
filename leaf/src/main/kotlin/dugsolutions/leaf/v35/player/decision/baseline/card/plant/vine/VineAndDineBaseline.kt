package dugsolutions.leaf.v35.player.decision.baseline.card.plant.vine

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.baseline.card.ConfiguredCardScorer

object VineAndDineBaseline : ConfiguredCardScorer(
    cardNames = setOf("Vine_09_04"),
    effect = GameEffect.TRASH_CRITTER_TO_RAISE_DIE_PLUS_5,
    cultivationPlayBase = 45,
    battlePlayBase = 45
)
