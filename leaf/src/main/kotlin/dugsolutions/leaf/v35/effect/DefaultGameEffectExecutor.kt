package dugsolutions.leaf.v35.effect

import dugsolutions.leaf.v35.chronicle.domain.Moment
import dugsolutions.leaf.v35.effect.handler.DieValueEffectHandler
import dugsolutions.leaf.v35.effect.handler.EffectHandler
import dugsolutions.leaf.v35.effect.handler.ResourceEffectHandler
import dugsolutions.leaf.v35.effect.handler.UpgradeEffectHandler
import dugsolutions.leaf.v35.effect.special.WispquakeEffect

/**
 * Top-level effect dispatcher.
 *
 * This class intentionally does NOT contain the mechanics of every GameEffect.
 * Its responsibilities are:
 *
 * 1. provide one obvious index from GameEffect -> implementation family
 * 2. ask the selected handler whether the effect is currently legal
 * 3. delegate execution
 * 4. record the common EFFECT_RESOLVED Chronicle marker
 *
 * Related/simple effects belong in cohesive family handlers. Effects with
 * their own substantial algorithm may live in a dedicated class under
 * effect/special. This keeps the dispatcher readable without returning to one
 * tiny class for every enum value.
 */
class DefaultGameEffectExecutor(
    private val dieValueEffects: EffectHandler = DieValueEffectHandler(),
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
     * With dozens of effects this becomes the searchable index of the effect
     * engine: find an enum value here to see immediately which implementation
     * family owns it. Avoid replacing this with reflection or a runtime handler
     * registry unless the project develops a real need for dynamic routing.
     */
    private fun handlerFor(
        effect: GameEffect
    ): EffectHandler? =
        when (effect) {
            GameEffect.RAISE_DIE_PLUS_3 ->
                dieValueEffects

            GameEffect.GAIN_WATER_TOKEN,
            GameEffect.MULCH_DIE_FROM_HAND,
            GameEffect.GAIN_ONE_VP ->
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
