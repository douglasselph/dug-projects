@file:OptIn(kotlin.contracts.ExperimentalContracts::class)

package dugsolutions.leaf.v35.error

import kotlin.contracts.contract

/**
 * Base type for runtime failures that indicate a v35 game-engine invariant,
 * decision, lifecycle, configuration, or effect-execution problem.
 *
 * These are intentionally separate from IllegalArgumentException produced by
 * require(...). A failed require means the caller supplied an invalid API/data
 * argument. A LeafGameException means the game engine reached a state or
 * decision that should never occur during legal play.
 */
sealed class LeafGameException(
    val context: String,
    val reason: String,
    cause: Throwable? = null
) : RuntimeException("[$context] $reason", cause)

/** A strategy/director returned a choice that was not legal when offered. */
class InvalidDecisionException(
    context: String,
    reason: String
) : LeafGameException(context, reason)

/** Mutable game state violated an invariant after legality had been checked. */
class InvalidGameStateException(
    context: String,
    reason: String
) : LeafGameException(context, reason)

/** A Game lifecycle transition was attempted from an invalid status. */
class GameLifecycleException(
    context: String,
    reason: String
) : LeafGameException(context, reason)

/** Effect execution failed because the effect is not legal/executable now. */
open class EffectExecutionException(
    context: String,
    reason: String
) : LeafGameException(context, reason)

/** An effect reached a handler that does not implement that effect. */
class UnsupportedGameEffectException(
    context: String,
    reason: String
) : EffectExecutionException(context, reason)

/** Static game/card/effect configuration is internally inconsistent. */
class GameConfigurationException(
    context: String,
    reason: String
) : LeafGameException(context, reason)

inline fun decisionCheck(
    condition: Boolean,
    context: String = "Decision",
    lazyReason: () -> Any
) {
    contract { returns() implies condition }
    if (!condition) {
        throw InvalidDecisionException(
            context = context,
            reason = lazyReason().toString()
        )
    }
}

inline fun stateCheck(
    condition: Boolean,
    context: String = "GameState",
    lazyReason: () -> Any
) {
    contract { returns() implies condition }
    if (!condition) {
        throw InvalidGameStateException(
            context = context,
            reason = lazyReason().toString()
        )
    }
}

inline fun lifecycleCheck(
    condition: Boolean,
    context: String = "GameLifecycle",
    lazyReason: () -> Any
) {
    contract { returns() implies condition }
    if (!condition) {
        throw GameLifecycleException(
            context = context,
            reason = lazyReason().toString()
        )
    }
}

inline fun effectCheck(
    condition: Boolean,
    context: String = "EffectExecution",
    lazyReason: () -> Any
) {
    contract { returns() implies condition }
    if (!condition) {
        throw EffectExecutionException(
            context = context,
            reason = lazyReason().toString()
        )
    }
}

inline fun configurationCheck(
    condition: Boolean,
    context: String = "GameConfiguration",
    lazyReason: () -> Any
) {
    contract { returns() implies condition }
    if (!condition) {
        throw GameConfigurationException(
            context = context,
            reason = lazyReason().toString()
        )
    }
}

inline fun <T : Any> decisionNotNull(
    value: T?,
    context: String = "Decision",
    lazyReason: () -> Any
): T {
    contract { returns() implies (value != null) }
    return value ?: throw InvalidDecisionException(
        context = context,
        reason = lazyReason().toString()
    )
}

inline fun <T : Any> stateNotNull(
    value: T?,
    context: String = "GameState",
    lazyReason: () -> Any
): T {
    contract { returns() implies (value != null) }
    return value ?: throw InvalidGameStateException(
        context = context,
        reason = lazyReason().toString()
    )
}

inline fun <T : Any> effectNotNull(
    value: T?,
    context: String = "EffectExecution",
    lazyReason: () -> Any
): T {
    contract { returns() implies (value != null) }
    return value ?: throw EffectExecutionException(
        context = context,
        reason = lazyReason().toString()
    )
}

fun unsupportedGameEffect(
    reason: String,
    context: String = "EffectExecution"
): Nothing =
    throw UnsupportedGameEffectException(
        context = context,
        reason = reason
    )
