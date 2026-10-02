package dugsolutions.leaf.simulation.v35.experiment.round

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.RoundEffectSlot
import dugsolutions.leaf.v35.round.RoundValueResolver
import dugsolutions.leaf.v35.round.domain.RoundCard

/** One immutable research-only replacement of a Round-card effect slot. */
data class RoundExperimentOverride(
    val roundCardName: String,
    val slot: RoundEffectSlot,
    val effect: GameEffect
)

/**
 * Immutable per-game Round-card effect interventions.
 *
 * Keys are normalized only for lookup; authored RoundCard names remain the
 * reporting/source-of-truth identifiers.
 */
class RoundExperimentConfig private constructor(
    overrides: Collection<RoundExperimentOverride>
) : RoundValueResolver {
    private data class Key(val normalizedName: String, val slot: RoundEffectSlot)

    private val overridesByKey: Map<Key, RoundExperimentOverride> =
        overrides.associateBy { Key(it.roundCardName.normalizedRoundId(), it.slot) }.toMap()

    val overrides: List<RoundExperimentOverride>
        get() = overridesByKey.values.sortedWith(
            compareBy<RoundExperimentOverride> { it.roundCardName }
                .thenBy { it.slot.ordinal }
        )

    override fun effectFor(card: RoundCard, slot: RoundEffectSlot): GameEffect =
        overridesByKey[Key(card.name.normalizedRoundId(), slot)]?.effect
            ?: RoundValueResolver.CANONICAL.effectFor(card, slot)

    companion object {
        val EMPTY = RoundExperimentConfig(emptyList())

        fun of(overrides: Collection<RoundExperimentOverride>): RoundExperimentConfig =
            if (overrides.isEmpty()) EMPTY else RoundExperimentConfig(overrides)
    }
}

internal fun String.normalizedRoundId(): String = trim().lowercase()
