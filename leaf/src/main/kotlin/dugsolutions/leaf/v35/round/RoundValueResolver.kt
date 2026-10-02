package dugsolutions.leaf.v35.round

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.RoundEffectSlot
import dugsolutions.leaf.v35.round.domain.RoundCard

/**
 * Resolves effective Round-card effects for one Game.
 *
 * Canonical gameplay uses [CANONICAL]. Research may provide an immutable
 * resolver through GameConfig without mutating the loaded RoundCard catalog.
 */
interface RoundValueResolver {
    fun effectFor(card: RoundCard, slot: RoundEffectSlot): GameEffect

    /**
     * Returns a RoundCard view whose effect values match this resolver while
     * preserving every other authored property from the canonical card.
     */
    fun cardFor(card: RoundCard): RoundCard {
        val first = effectFor(card, RoundEffectSlot.FIRST)
        val second = effectFor(card, RoundEffectSlot.SECOND)
        if (first == card.firstEffect.effect && second == card.secondEffect.effect) {
            return card
        }
        return card.copy(
            firstEffect = card.firstEffect.copy(effect = first),
            secondEffect = card.secondEffect.copy(effect = second)
        )
    }

    companion object {
        val CANONICAL: RoundValueResolver = object : RoundValueResolver {
            override fun effectFor(card: RoundCard, slot: RoundEffectSlot): GameEffect =
                when (slot) {
                    RoundEffectSlot.FIRST -> card.firstEffect.effect
                    RoundEffectSlot.SECOND -> card.secondEffect.effect
                }
        }
    }
}
