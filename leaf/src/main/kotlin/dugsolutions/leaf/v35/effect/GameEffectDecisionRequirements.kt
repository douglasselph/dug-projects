package dugsolutions.leaf.v35.effect

/**
 * Stable names for the player-decision seams an effect may ask for.
 *
 * This is audit/strategy metadata, not execution logic. It lets integration
 * tests and future simulation agents verify that every CSV-backed effect has a
 * deliberately understood decision surface instead of discovering a missing
 * decision only after a rare card is played.
 *
 * Generic consequences are included when the effect itself delegates to a
 * different decision family (for example a Battle placement for a newly drawn
 * die, a Critter choice, or normal Wound resolution).
 */
enum class EffectDecisionMechanism {
    EFFECT_DIE,
    EFFECT_BATTLE_DIE,
    EFFECT_CROSS_PLAYER_DIE_SWAP,
    EFFECT_OPTIONAL_DIE,
    EFFECT_DICE_SET,
    EFFECT_DIE_PAIR,
    EFFECT_OPTIONAL_DIE_PAIR,
    EFFECT_CRITTER_AND_DIE,
    EFFECT_PETAL_TO_DIE_4,
    EFFECT_BEE_SOURCE,
    EFFECT_BUTTERFLY_TARGET,
    EFFECT_OPTIONAL_PLANT,
    EFFECT_OPPONENT_PLANT_WOUND,
    EFFECT_PLANT_EFFECT,
    EFFECT_O_EDELWEISS,
    EFFECT_WISPS_TO_KEEP,
    EFFECT_DIE_SIZE,
    EFFECT_PLAYER,
    EFFECT_STRIKE_ROW,
    REWARD_CRITTER,
    WOUND_RESOLUTION,
    BATTLE_DIE_PLACEMENT
}

/** Phase-aware decision surface for one [GameEffect]. */
data class GameEffectDecisionRequirement(
    val cultivation: Set<EffectDecisionMechanism> = emptySet(),
    val battle: Set<EffectDecisionMechanism> = cultivation
) {
    fun forPhase(phase: GameEffectPhase): Set<EffectDecisionMechanism> =
        when (phase) {
            GameEffectPhase.CULTIVATION -> cultivation
            GameEffectPhase.BATTLE -> battle
        }
}

/**
 * Exhaustive decision contract for every defined v35 effect.
 *
 * An empty set is intentional: it means the effect is deterministic once it
 * is chosen/triggered. UNKNOWN deliberately has no contract.
 */
object GameEffectDecisionRequirements {

    fun forEffect(effect: GameEffect): GameEffectDecisionRequirement? =
        when (effect) {
            GameEffect.UNKNOWN -> null

            GameEffect.DISCARD_ANY_NUMBER_OF_DICE_AND_REDRAW_OR_REROLL_ONE_IN_BATTLE ->
                phased(
                    cultivation = setOf(EffectDecisionMechanism.EFFECT_DICE_SET),
                    battle = setOf(EffectDecisionMechanism.EFFECT_DIE)
                )

            GameEffect.DISCARD_ONE_DIE_DRAW_ONE_AND_SWAP_TWO_OWN_DICE_IN_BATTLE ->
                phased(
                    cultivation = setOf(EffectDecisionMechanism.EFFECT_DIE),
                    battle = setOf(
                        EffectDecisionMechanism.EFFECT_DIE,
                        EffectDecisionMechanism.EFFECT_OPTIONAL_DIE_PAIR
                    )
                )

            GameEffect.DISCARD_ONE_DIE_DRAW_TWO_AND_PLACE_DRAWN_DIE_IN_STRIKE_SQUARE ->
                phased(
                    cultivation = setOf(EffectDecisionMechanism.EFFECT_DIE),
                    battle = setOf(
                        EffectDecisionMechanism.EFFECT_DIE,
                        EffectDecisionMechanism.BATTLE_DIE_PLACEMENT
                    )
                )

            GameEffect.DOUBLE_ONE_DIE -> same(EffectDecisionMechanism.EFFECT_DIE)

            GameEffect.DRAW_TWO_DICE ->
                phased(
                    cultivation = emptySet(),
                    battle = setOf(EffectDecisionMechanism.BATTLE_DIE_PLACEMENT)
                )

            GameEffect.WOUND_OPPONENT_PLANT_OF_YOUR_CHOICE ->
                same(EffectDecisionMechanism.EFFECT_OPPONENT_PLANT_WOUND)

            GameEffect.FLIP_OWN_DIE_TO_OPPOSITE_FACE ->
                same(EffectDecisionMechanism.EFFECT_DIE)

            GameEffect.FLIP_OWN_PLANT_OR_WOUND_EACH_OPPONENT_IN_BATTLE ->
                phased(
                    cultivation = setOf(EffectDecisionMechanism.EFFECT_OPTIONAL_PLANT),
                    battle = setOf(EffectDecisionMechanism.WOUND_RESOLUTION)
                )

            GameEffect.GAIN_ANY_DIE_TO_DISCARD ->
                same(EffectDecisionMechanism.EFFECT_DIE_SIZE)

            GameEffect.GAIN_ANY_TWO_CRITTERS ->
                same(EffectDecisionMechanism.REWARD_CRITTER)

            GameEffect.GAIN_D10_TO_DISCARD,
            GameEffect.GAIN_D12_TO_DISCARD,
            GameEffect.GAIN_D20_TO_DISCARD,
            GameEffect.GAIN_ONE_VP,
            GameEffect.GAIN_ONE_WISP,
            GameEffect.GAIN_OR_REFRESH_GREEN_BUTTERFLY,
            GameEffect.GAIN_OR_REFRESH_PURPLE_BUTTERFLY,
            GameEffect.GAIN_OR_REFRESH_RED_BUTTERFLY,
            GameEffect.GAIN_OR_REFRESH_YELLOW_BUTTERFLY,
            GameEffect.GAIN_TWO_WORMS,
            GameEffect.GAIN_WATER_TOKEN,
            GameEffect.GAIN_WORM_AND_BOOST_WORMS_THIS_ROUND,
            GameEffect.RAISE_ALL_DICE_PLUS_2,
            GameEffect.STEAL_RANDOM_WISP_FROM_ALL_OPPONENTS ->
                same()

            GameEffect.GAIN_D4_SET_TO_4_OR_TRASH_D4_RAISE_ALL_DICE_PLUS_4 ->
                phased(
                    cultivation = setOf(EffectDecisionMechanism.EFFECT_PETAL_TO_DIE_4),
                    battle = setOf(
                        EffectDecisionMechanism.EFFECT_PETAL_TO_DIE_4,
                        EffectDecisionMechanism.BATTLE_DIE_PLACEMENT
                    )
                )

            GameEffect.GAIN_MULCH_AND_STORE_DIE_FROM_DISCARD,
            GameEffect.MULCH_DIE_FROM_DISCARD,
            GameEffect.MULCH_DIE_FROM_HAND ->
                same(EffectDecisionMechanism.EFFECT_DIE)

            GameEffect.GAIN_OR_STEAL_BEE_AND_BOOST_BEES_THIS_ROUND ->
                same(EffectDecisionMechanism.EFFECT_BEE_SOURCE)

            GameEffect.GAIN_WATER_AND_SPEND_3_TO_REROLL_BATTLE_DIE ->
                phased(
                    cultivation = emptySet(),
                    battle = setOf(EffectDecisionMechanism.EFFECT_BATTLE_DIE)
                )

            GameEffect.LIMIT_WISPS_AND_TRASH_EXCESS ->
                same(EffectDecisionMechanism.EFFECT_WISPS_TO_KEEP)

            GameEffect.PLAY_OR_FLIP_ANOTHER_CARD_TWICE ->
                same(EffectDecisionMechanism.EFFECT_O_EDELWEISS)

            GameEffect.RAISE_ANY_DIE_PLUS_1,
            GameEffect.RAISE_DIE_PLUS_1_PER_GRAFTED_VINE_OR_FLOWER,
            GameEffect.RAISE_DIE_PLUS_1_PER_ROOT_OR_VINE,
            GameEffect.RAISE_DIE_PLUS_2_AND_REDUCE_OPPOSING_DICE_IN_STRIKE_ROW,
            GameEffect.RAISE_DIE_PLUS_3,
            GameEffect.RAISE_DIE_PLUS_4,
            GameEffect.REROLL_DIE_UNTIL_3_PLUS_IGNORE_ROLL_REWARDS,
            GameEffect.SET_DIE_SHOWING_2_PLUS_TO_1_AND_GAIN_VP_PER_ONE,
            GameEffect.SET_DIE_UP_TO_D12_TO_MAX,
            GameEffect.SET_LOWEST_VALUE_DIE_TO_MAX,
            GameEffect.UPGRADE_DIE_AND_USE_NOW,
            GameEffect.UPGRADE_DIE_FROM_HAND,
            GameEffect.UPGRADE_DIE_TWO_STEPS_SKIP_MISSING_AND_USE_NOW ->
                same(EffectDecisionMechanism.EFFECT_DIE)

            GameEffect.RAISE_DIE_PLUS_1_AND_DRAW_ONE_PER_MAX_DIE ->
                phased(
                    cultivation = setOf(EffectDecisionMechanism.EFFECT_DIE),
                    battle = setOf(
                        EffectDecisionMechanism.EFFECT_DIE,
                        EffectDecisionMechanism.BATTLE_DIE_PLACEMENT
                    )
                )

            GameEffect.RAISE_DIE_PLUS_1_AND_FLIP_HIGHER_OPPOSING_DICE_IN_STRIKE_ROW ->
                same(EffectDecisionMechanism.EFFECT_DIE)

            GameEffect.RAISE_DIE_PLUS_1_AND_WITHDRAW_FROM_STRIKE_SQUARE ->
                phased(
                    cultivation = setOf(EffectDecisionMechanism.EFFECT_DIE),
                    battle = setOf(
                        EffectDecisionMechanism.EFFECT_DIE,
                        EffectDecisionMechanism.EFFECT_STRIKE_ROW
                    )
                )

            GameEffect.REROLL_ALL_PLAYERS_DICE_KEEP_ONE_OWN ->
                same(EffectDecisionMechanism.EFFECT_OPTIONAL_DIE)

            GameEffect.REROLL_ONE_DIE_AND_REROLL_HIGHER_OPPOSING_DICE_IN_STRIKE_ROW ->
                phased(
                    cultivation = setOf(EffectDecisionMechanism.EFFECT_DIE),
                    battle = setOf(
                        EffectDecisionMechanism.EFFECT_DIE,
                        EffectDecisionMechanism.EFFECT_STRIKE_ROW
                    )
                )

            GameEffect.RESOLVE_STRIKE_IMMEDIATELY_AND_CLEAR_ROW ->
                phased(
                    cultivation = emptySet(),
                    battle = setOf(EffectDecisionMechanism.EFFECT_STRIKE_ROW)
                )

            GameEffect.REUSE_SPENT_ROOT_OR_VINE_EFFECT ->
                same(EffectDecisionMechanism.EFFECT_PLANT_EFFECT)

            GameEffect.ROLL_DIE_FROM_DISCARD_INTO_HAND ->
                phased(
                    cultivation = setOf(EffectDecisionMechanism.EFFECT_DIE),
                    battle = setOf(
                        EffectDecisionMechanism.EFFECT_DIE,
                        EffectDecisionMechanism.BATTLE_DIE_PLACEMENT
                    )
                )

            GameEffect.SET_ANY_DIE_TO_3_OR_REDUCE_OPPOSING_STRIKE_ROW_BY_3 ->
                phased(
                    cultivation = setOf(EffectDecisionMechanism.EFFECT_OPTIONAL_DIE),
                    battle = setOf(EffectDecisionMechanism.EFFECT_STRIKE_ROW)
                )

            GameEffect.SET_DIE_TO_MATCH_ANOTHER ->
                same(EffectDecisionMechanism.EFFECT_DIE_PAIR)

            GameEffect.STEAL_BUTTERFLY_AND_REFRESH_ALL_BUTTERFLIES ->
                same(EffectDecisionMechanism.EFFECT_BUTTERFLY_TARGET)

            GameEffect.STEAL_RANDOM_WISP_FROM_ONE_OPPONENT ->
                same(EffectDecisionMechanism.EFFECT_PLAYER)

            GameEffect.SWAP_OWN_DIE_WITH_OPPONENT_SAME_SIZE ->
                phased(
                    cultivation = emptySet(),
                    battle = setOf(EffectDecisionMechanism.EFFECT_CROSS_PLAYER_DIE_SWAP)
                )

            GameEffect.TRASH_CRITTER_TO_RAISE_DIE_PLUS_5 ->
                same(EffectDecisionMechanism.EFFECT_CRITTER_AND_DIE)
        }

    fun forPhase(
        effect: GameEffect,
        phase: GameEffectPhase
    ): Set<EffectDecisionMechanism> =
        requireNotNull(forEffect(effect)) {
            "No decision contract exists for effect: $effect"
        }.forPhase(phase)

    private fun same(
        vararg mechanisms: EffectDecisionMechanism
    ): GameEffectDecisionRequirement =
        GameEffectDecisionRequirement(
            cultivation = mechanisms.toSet()
        )

    private fun phased(
        cultivation: Set<EffectDecisionMechanism>,
        battle: Set<EffectDecisionMechanism>
    ): GameEffectDecisionRequirement =
        GameEffectDecisionRequirement(
            cultivation = cultivation,
            battle = battle
        )
}
