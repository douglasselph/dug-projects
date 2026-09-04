package dugsolutions.leaf.v35.effect

import dugsolutions.leaf.v35.chronicle.domain.Moment
import dugsolutions.leaf.v35.effect.handler.DieValueEffectHandler
import dugsolutions.leaf.v35.effect.handler.DrawEffectHandler
import dugsolutions.leaf.v35.effect.handler.EffectHandler
import dugsolutions.leaf.v35.effect.handler.ResourceEffectHandler
import dugsolutions.leaf.v35.effect.handler.UpgradeEffectHandler
import dugsolutions.leaf.v35.effect.special.WispquakeEffect

/**
 * Top-level effect dispatcher.
 *
 * This class intentionally does NOT contain the mechanics of every GameEffect.
 * It remains the searchable index from GameEffect to a cohesive implementation
 * family or a dedicated complex-effect class.
 */
class DefaultGameEffectExecutor(
    private val dieValueEffects: EffectHandler = DieValueEffectHandler(),
    private val drawEffects: EffectHandler = DrawEffectHandler(),
    private val resourceEffects: EffectHandler = ResourceEffectHandler(),
    private val upgradeEffects: EffectHandler = UpgradeEffectHandler(),
    private val wispquakeEffect: EffectHandler = WispquakeEffect()
) : GameEffectExecutor {

    override fun canExecute(
        request: GameEffectRequest
    ): Boolean =
        handlerFor(request.effect)
            ?.canExecute(request)
            ?: false

    override fun execute(
        request: GameEffectRequest
    ) {
        val handler = checkNotNull(
            handlerFor(request.effect)
        ) {
            "GameEffect is not currently supported: ${request.effect}"
        }

        check(handler.canExecute(request)) {
            "GameEffect is not currently executable: ${request.effect}"
        }

        handler.execute(
            request = request,
            executor = this
        )

        request.game.chronicle.record(
            Moment.Marker(
                "EFFECT_RESOLVED player=${request.actor.id.value} " +
                    "effect=${request.effect} source=${sourceName(request.source)} " +
                    "phase=${request.phase}"
            )
        )
    }

    /**
     * Deliberately explicit routing table.
     *
     * Find an enum value here to see immediately which implementation family
     * owns it. Unsupported / more involved effects intentionally remain absent
     * until their complete rule can be implemented.
     */
    private fun handlerFor(
        effect: GameEffect
    ): EffectHandler? =
        when (effect) {
            GameEffect.DOUBLE_ONE_DIE,
            GameEffect.FLIP_OWN_DIE_TO_OPPOSITE_FACE,
            GameEffect.RAISE_ALL_DICE_PLUS_2,
            GameEffect.RAISE_ANY_DIE_PLUS_1,
            GameEffect.RAISE_DIE_PLUS_1_AND_FLIP_HIGHER_OPPOSING_DICE_IN_STRIKE_ROW,
            GameEffect.RAISE_DIE_PLUS_1_AND_WITHDRAW_FROM_STRIKE_SQUARE,
            GameEffect.RAISE_DIE_PLUS_1_PER_GRAFTED_VINE_OR_FLOWER,
            GameEffect.RAISE_DIE_PLUS_1_PER_ROOT_OR_VINE,
            GameEffect.RAISE_DIE_PLUS_2_AND_REDUCE_OPPOSING_DICE_IN_STRIKE_ROW,
            GameEffect.RAISE_DIE_PLUS_3,
            GameEffect.RAISE_DIE_PLUS_4,
            GameEffect.SET_ANY_DIE_TO_3_OR_REDUCE_OPPOSING_STRIKE_ROW_BY_3,
            GameEffect.SET_DIE_SHOWING_2_PLUS_TO_1_AND_GAIN_VP_PER_ONE,
            GameEffect.SET_DIE_UP_TO_D12_TO_MAX,
            GameEffect.SET_LOWEST_VALUE_DIE_TO_MAX ->
                dieValueEffects

            GameEffect.DISCARD_ONE_DIE_DRAW_ONE_AND_SWAP_TWO_OWN_DICE_IN_BATTLE,
            GameEffect.DISCARD_ONE_DIE_DRAW_TWO_AND_PLACE_DRAWN_DIE_IN_STRIKE_SQUARE,
            GameEffect.DRAW_TWO_DICE,
            GameEffect.RAISE_DIE_PLUS_1_AND_DRAW_ONE_PER_MAX_DIE,
            GameEffect.REROLL_DIE_UNTIL_3_PLUS_IGNORE_ROLL_REWARDS,
            GameEffect.REROLL_ONE_DIE_AND_REROLL_HIGHER_OPPOSING_DICE_IN_STRIKE_ROW,
            GameEffect.ROLL_DIE_FROM_DISCARD_INTO_HAND ->
                drawEffects

            GameEffect.GAIN_D10_TO_DISCARD,
            GameEffect.GAIN_D12_TO_DISCARD,
            GameEffect.GAIN_D20_TO_DISCARD,
            GameEffect.GAIN_ANY_TWO_CRITTERS,
            GameEffect.GAIN_MULCH_AND_STORE_DIE_FROM_DISCARD,
            GameEffect.GAIN_ONE_VP,
            GameEffect.GAIN_ONE_WISP,
            GameEffect.GAIN_TWO_WORMS,
            GameEffect.GAIN_OR_REFRESH_GREEN_BUTTERFLY,
            GameEffect.GAIN_OR_REFRESH_PURPLE_BUTTERFLY,
            GameEffect.GAIN_OR_REFRESH_RED_BUTTERFLY,
            GameEffect.GAIN_OR_REFRESH_YELLOW_BUTTERFLY,
            GameEffect.GAIN_WATER_AND_SPEND_3_TO_REROLL_BATTLE_DIE,
            GameEffect.GAIN_WATER_TOKEN,
            GameEffect.MULCH_DIE_FROM_DISCARD,
            GameEffect.MULCH_DIE_FROM_HAND ->
                resourceEffects

            GameEffect.UPGRADE_DIE_FROM_HAND ->
                upgradeEffects

            GameEffect.REROLL_ALL_PLAYERS_DICE_KEEP_ONE_OWN ->
                wispquakeEffect

            else -> null
        }

    private fun sourceName(
        source: GameEffectSource
    ): String =
        when (source) {
            is GameEffectSource.Plant -> "PLANT_${source.card.card.name}"
            is GameEffectSource.Round -> "ROUND_${source.slot}"
            is GameEffectSource.Wisp -> "WISP_${source.card.name}"
        }
}
