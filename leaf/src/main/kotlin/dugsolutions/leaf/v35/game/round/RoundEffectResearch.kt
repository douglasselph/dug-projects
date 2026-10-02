package dugsolutions.leaf.v35.game.round

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.game.Game
import dugsolutions.leaf.v35.tokens.Critter

/** Observation-only explanation for a subset of Round-effect illegality caused by finite Grove supply. */
internal fun blockedByEmptySharedResource(game: Game, effect: GameEffect): Boolean = when (effect) {
    GameEffect.GAIN_WATER_TOKEN -> !game.grove.tokens.hasWater
    GameEffect.GAIN_SUNLIGHT_TOKEN -> !game.grove.tokens.hasSunlight
    GameEffect.MULCH_DIE_FROM_HAND,
    GameEffect.MULCH_DIE_FROM_DISCARD,
    GameEffect.GAIN_MULCH_AND_STORE_DIE_FROM_DISCARD -> !game.grove.tokens.hasMulch
    GameEffect.GAIN_TWO_WORMS -> game.grove.critters.count(Critter.WORM) == 0
    GameEffect.GAIN_ANY_TWO_CRITTERS -> game.grove.critters.isEmpty
    else -> false
}

internal fun battlesRemaining(game: Game): Int =
    game.roundDeck.cards.cards.count { it.type == dugsolutions.leaf.v35.round.domain.RoundCardType.BATTLE }

internal fun battleIsNext(game: Game): Boolean =
    game.roundDeck.cards.getOrNull(0)?.type == dugsolutions.leaf.v35.round.domain.RoundCardType.BATTLE
