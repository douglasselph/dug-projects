package dugsolutions.leaf.player.decisions.local

import dugsolutions.leaf.components.CardEffect

/**
 * Return a score to help determine if this card should be preferred to be used to absorb damage.
 * A lower score is more likely.
 * A die is sides. That is a D4 is worth 4, 6 is 6, etc. So scores should be relative to this.
 */
class EffectBattleScore {

    operator fun invoke(effect: CardEffect?, value: Int): Int {
        effect ?: return 0
        return when (effect) {
            CardEffect.ADD_TO_DIE -> if (value <= 2) value else value + 1
            CardEffect.DEFLECT,
            CardEffect.ADD_TO_TOTAL -> if (value <= 2) value + 1 else value + 3
            CardEffect.ADJUST_BY -> if (value <= 2) value + 1 else value + 2
            CardEffect.ADJUST_TO_MAX -> 18
            CardEffect.ADJUST_TO_MIN_OR_MAX -> 20
            CardEffect.ADORN -> 10
            CardEffect.DISCARD -> 7 * value
            CardEffect.DISCARD_CARD -> 6 * value
            CardEffect.DISCARD_DIE -> 9 * value
            CardEffect.DRAW_CARD -> 8 * value
            CardEffect.DRAW_CARD_COMPOST -> 8 * value
            CardEffect.DRAW_DIE -> 10 * value
            CardEffect.DRAW_DIE_ANY -> 15 * value
            CardEffect.DRAW_DIE_COMPOST -> 16 * value
            CardEffect.DRAW_THEN_DISCARD -> 5 * value
            CardEffect.FLOURISH_OVERRIDE -> 0
            CardEffect.GAIN_FREE_ROOT -> 6 * value
            CardEffect.GAIN_FREE_CANOPY -> 12 * value
            CardEffect.GAIN_FREE_VINE -> 20 * value
            CardEffect.REDUCE_COST_ROOT -> 0
            CardEffect.REDUCE_COST_CANOPY -> 0
            CardEffect.REDUCE_COST_VINE -> 0
            CardEffect.REROLL_ACCEPT_2ND -> 5 * value
            CardEffect.REROLL_ALL_MAX -> 3 * value
            CardEffect.REROLL_TAKE_BETTER -> 9 * value
            CardEffect.REPLAY_VINE -> 4 * value
            CardEffect.RESILIENCE_BOOST -> 10 * value
            CardEffect.RETAIN_CARD -> 2 * value
            CardEffect.RETAIN_DIE -> 3 * value
            CardEffect.RETAIN_DIE_REROLL -> 4 * value
            CardEffect.REUSE_CARD -> 9 * value
            CardEffect.REUSE_DIE -> 11 * value
            CardEffect.REUSE_ANY -> 13 * value
            CardEffect.UPGRADE_ANY_RETAIN -> 14 * value
            CardEffect.UPGRADE_ANY -> 11 * value
            CardEffect.UPGRADE_D4 -> 4 * value
            CardEffect.UPGRADE_D4_D6 -> 6 * value
            CardEffect.USE_OPPONENT_CARD -> 7 * value
            CardEffect.USE_OPPONENT_DIE -> 9 * value
        }
    }
}
