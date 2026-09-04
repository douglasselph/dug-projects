package dugsolutions.leaf.v35.effect

import dugsolutions.leaf.v35.error.effectNotNull
import dugsolutions.leaf.v35.error.effectCheck
import dugsolutions.leaf.v35.error.stateCheck
import dugsolutions.leaf.v35.chronicle.domain.Moment
import dugsolutions.leaf.v35.effect.handler.CrossPlayerEffectHandler
import dugsolutions.leaf.v35.effect.handler.DieValueEffectHandler
import dugsolutions.leaf.v35.effect.handler.DrawEffectHandler
import dugsolutions.leaf.v35.effect.handler.EffectHandler
import dugsolutions.leaf.v35.effect.handler.ResourceEffectHandler
import dugsolutions.leaf.v35.effect.handler.UpgradeEffectHandler
import dugsolutions.leaf.v35.effect.special.AlluringNectarEffect
import dugsolutions.leaf.v35.effect.special.BeeLovedBloomEffect
import dugsolutions.leaf.v35.effect.special.OEdelweissEffect
import dugsolutions.leaf.v35.effect.special.OvergrowthEffect
import dugsolutions.leaf.v35.effect.special.PartingThornEffect
import dugsolutions.leaf.v35.effect.special.PetalToDie4Effect
import dugsolutions.leaf.v35.effect.special.SnipHappensEffect
import dugsolutions.leaf.v35.effect.special.VineAndAgainEffect
import dugsolutions.leaf.v35.effect.special.VineAndDineEffect
import dugsolutions.leaf.v35.effect.special.WispReckoningEffect
import dugsolutions.leaf.v35.effect.special.WispLastWordEffect
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
    private val crossPlayerEffects: EffectHandler = CrossPlayerEffectHandler(),
    private val upgradeEffects: EffectHandler = UpgradeEffectHandler(),
    private val vineAndDineEffect: EffectHandler = VineAndDineEffect(),
    private val petalToDie4Effect: EffectHandler = PetalToDie4Effect(),
    private val beeLovedBloomEffect: EffectHandler = BeeLovedBloomEffect(),
    private val alluringNectarEffect: EffectHandler = AlluringNectarEffect(),
    private val partingThornEffect: EffectHandler = PartingThornEffect(),
    private val snipHappensEffect: EffectHandler = SnipHappensEffect(),
    private val vineAndAgainEffect: EffectHandler = VineAndAgainEffect(),
    private val oEdelweissEffect: EffectHandler = OEdelweissEffect(),
    private val wispReckoningEffect: EffectHandler = WispReckoningEffect(),
    private val wispLastWordEffect: EffectHandler = WispLastWordEffect(),
    private val overgrowthEffect: EffectHandler = OvergrowthEffect(),
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
        val handler = effectNotNull(
            handlerFor(request.effect)
        ) {
            "GameEffect is not currently supported: ${request.effect}"
        }

        effectCheck(handler.canExecute(request)) {
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
            GameEffect.SET_DIE_TO_MATCH_ANOTHER,
            GameEffect.SET_DIE_UP_TO_D12_TO_MAX,
            GameEffect.SET_LOWEST_VALUE_DIE_TO_MAX ->
                dieValueEffects

            GameEffect.DISCARD_ANY_NUMBER_OF_DICE_AND_REDRAW_OR_REROLL_ONE_IN_BATTLE,
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
            GameEffect.GAIN_ANY_DIE_TO_DISCARD,
            GameEffect.GAIN_ANY_TWO_CRITTERS,
            GameEffect.GAIN_MULCH_AND_STORE_DIE_FROM_DISCARD,
            GameEffect.GAIN_ONE_VP,
            GameEffect.GAIN_ONE_WISP,
            GameEffect.GAIN_TWO_WORMS,
            GameEffect.GAIN_WORM_AND_BOOST_WORMS_THIS_ROUND,
            GameEffect.GAIN_OR_REFRESH_GREEN_BUTTERFLY,
            GameEffect.GAIN_OR_REFRESH_PURPLE_BUTTERFLY,
            GameEffect.GAIN_OR_REFRESH_RED_BUTTERFLY,
            GameEffect.GAIN_OR_REFRESH_YELLOW_BUTTERFLY,
            GameEffect.GAIN_WATER_TOKEN,
            GameEffect.STEAL_RANDOM_WISP_FROM_ONE_OPPONENT,
            GameEffect.STEAL_RANDOM_WISP_FROM_ALL_OPPONENTS,
            GameEffect.MULCH_DIE_FROM_DISCARD,
            GameEffect.MULCH_DIE_FROM_HAND ->
                resourceEffects

            GameEffect.GAIN_WATER_AND_SPEND_3_TO_REROLL_BATTLE_DIE,
            GameEffect.SWAP_OWN_DIE_WITH_OPPONENT_SAME_SIZE ->
                crossPlayerEffects

            GameEffect.UPGRADE_DIE_AND_USE_NOW,
            GameEffect.UPGRADE_DIE_FROM_HAND ->
                upgradeEffects

            GameEffect.TRASH_CRITTER_TO_RAISE_DIE_PLUS_5 ->
                vineAndDineEffect

            GameEffect.GAIN_D4_SET_TO_4_OR_TRASH_D4_RAISE_ALL_DICE_PLUS_4 ->
                petalToDie4Effect

            GameEffect.GAIN_OR_STEAL_BEE_AND_BOOST_BEES_THIS_ROUND ->
                beeLovedBloomEffect

            GameEffect.STEAL_BUTTERFLY_AND_REFRESH_ALL_BUTTERFLIES ->
                alluringNectarEffect

            GameEffect.FLIP_OWN_PLANT_OR_WOUND_EACH_OPPONENT_IN_BATTLE ->
                partingThornEffect

            GameEffect.WOUND_OPPONENT_PLANT_OF_YOUR_CHOICE ->
                snipHappensEffect

            GameEffect.REUSE_SPENT_ROOT_OR_VINE_EFFECT ->
                vineAndAgainEffect

            GameEffect.PLAY_OR_FLIP_ANOTHER_CARD_TWICE ->
                oEdelweissEffect

            GameEffect.LIMIT_WISPS_AND_TRASH_EXCESS ->
                wispReckoningEffect

            GameEffect.RESOLVE_STRIKE_IMMEDIATELY_AND_CLEAR_ROW ->
                wispLastWordEffect

            GameEffect.UPGRADE_DIE_TWO_STEPS_SKIP_MISSING_AND_USE_NOW ->
                overgrowthEffect

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
