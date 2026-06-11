package dugsolutions.leaf.v30.cards

import dugsolutions.leaf.v30.cards.domain.CardEffect
import dugsolutions.leaf.v30.chronicle.Chronicle
import dugsolutions.leaf.v30.chronicle.GameChronicle
import dugsolutions.leaf.v30.chronicle.domain.Moment

class CardEffectConverter(
    private val chronicle: Chronicle = GameChronicle()
) {
    operator fun invoke(
        name: String,
        title: String
    ): CardEffect {
        val effect = effectsByTitle[title.trim()]
        if (effect != null) return effect

        chronicle(
            Moment.LoadingWarning(
                name = name,
                title = title,
                reason = "Unknown card effect"
            )
        )
        return CardEffect.UNKNOWN
    }

    private companion object {
        val effectsByTitle = mapOf(
            "Root Bulwark" to CardEffect.PLACE_BULWARK_TOKEN,
            "Root Flourish" to CardEffect.GAIN_WORM_AND_BOOST_WORMS,
            "Root Renewal" to CardEffect.MULCH_DIE_FROM_DISCARD,
            "Root Reservoir" to CardEffect.REROLL_DIE_UNTIL_THREE_OR_HIGHER,
            "Root Nourish" to CardEffect.RAISE_DIE_PLUS_2_AND_GAIN_WATER,
            "Root Reflection" to CardEffect.RAISE_DIE_PLUS_1_AND_DOUBLE_MATCHING_DICE,
            "Root Reinforced" to CardEffect.DOUBLE_ONE_DIE,
            "Root Resonance" to CardEffect.DOUBLE_ALL_DICE_SHOWING_ONE_TO_FOUR,
            "Root Awakening" to CardEffect.UPGRADE_DIE_AND_USE_NOW,
            "Root Cause" to CardEffect.FLIP_DIE_TO_OPPOSITE_FACE,
            "Root Loot" to CardEffect.SET_DIE_TO_MATCH_ANOTHER,
            "Root Reach" to CardEffect.RAISE_DIE_PLUS_2_PER_WORM_AND_DISCARD_WORM,
            "Acutely Floral" to CardEffect.GAIN_OR_STEAL_BEE_AND_BOOST_BEES,
            "Parting Thorn" to CardEffect.WOUND_WINNER_OF_STRIKE_ROW,
            "Petal To Die 4" to CardEffect.GAIN_D4_OR_TRASH_D4_GAIN_D8,
            "Transplant Tulip" to CardEffect.SWAP_TWO_OWN_DICE,
            "Bloom Backbone" to CardEffect.RAISE_DIE_PLUS_1_PER_GRAFTED_ROOT_OR_VINE,
            "Bursting Blossom" to CardEffect.ROLL_EXTRA_FOR_EACH_MAX_DIE,
            "Gust of Petals" to CardEffect.REROLL_HIGHER_OPPOSING_DICE_ON_STRIKE_ROW,
            "Siphon Snapdragon" to CardEffect.DRAIN_HIGHER_DICE_AND_RAISE_OWN_DIE,
            "Bee-trieval" to CardEffect.DRAW_DIE_FROM_DISCARD,
            "Bloom Backflip" to CardEffect.FLIP_HIGHER_OPPOSING_DICE_ON_STRIKE_ROW,
            "Edelweiss²" to CardEffect.PLAY_UP_TO_TWO_OTHER_CARDS,
            "Queen’s Blossom" to CardEffect.DRAW_TWO_DICE,
            "Berry Important" to CardEffect.RAISE_DIE_PLUS_1_AND_END_GAME_PLUS_2_VP,
            "De-Vine Exchange" to CardEffect.RAISE_DIE_PLUS_1_AND_END_GAME_PLUS_1_VP_PER_FLOWER,
            "Twist and Swarm" to CardEffect.RAISE_THREE_DICE_PLUS_1,
            "Vine Lash" to CardEffect.RAISE_DIE_PLUS_3,
            "Vine Again" to CardEffect.REPEAT_FACE_DOWN_VINE_OR_ROOT_EFFECT,
            "No Time To Vine" to CardEffect.RESOLVE_STRIKE_IMMEDIATELY,
            "Reaping Tendrils" to CardEffect.GAIN_MULCH_AND_CLEANUP_MULCH_DIE,
            "Withering Grasp" to CardEffect.TRASH_CRITTER_TO_RAISE_DIE_PLUS_5,
            "Saplink Trellis" to CardEffect.RAISE_DIE_PLUS_2_PER_VINE,
            "Snip Happens" to CardEffect.FLIP_OPPONENT_FACE_UP_VINE_FACE_DOWN,
            "Vine and Dine" to CardEffect.SET_DIE_UP_TO_D12_TO_MAX,
            "Vine and Punishment" to CardEffect.REDUCE_OPPOSING_DICE_ON_STRIKE_ROW_BY_3
        )
    }
}
