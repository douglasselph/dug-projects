package dugsolutions.leaf.v30.round

import dugsolutions.leaf.v30.chronicle.Chronicle
import dugsolutions.leaf.v30.chronicle.GameChronicle
import dugsolutions.leaf.v30.chronicle.domain.Moment
import dugsolutions.leaf.v30.round.domain.RoundEffect

class RoundEffectConverter(
    private val chronicle: Chronicle = GameChronicle()
) {
    operator fun invoke(
        name: String,
        title: String
    ): RoundEffect {
        val effect = effectsByTitle[title.trim()]
        if (effect != null) return effect

        chronicle(
            Moment.LoadingWarning(
                name = name,
                title = title,
                reason = "Unknown round card effect"
            )
        )
        return RoundEffect.UNKNOWN
    }

    private companion object {
        val effectsByTitle = mapOf(
            "Sunlight" to RoundEffect.RAISE_BY_3,
            "Water" to RoundEffect.GAIN_WATER,
            "Mulch" to RoundEffect.GAIN_MULCH,
            "Compost" to RoundEffect.UPGRADE_DIE,
            "Burrow" to RoundEffect.GAIN_WORMS,
            "Bloom" to RoundEffect.GAIN_VP,
            "Surge" to RoundEffect.GAIN_D20,
            "Swell" to RoundEffect.GAIN_D12,
            "Rootcall" to RoundEffect.GAIN_ROOT,
            "Vinecall" to RoundEffect.GAIN_VINE,
            "Flowercall" to RoundEffect.GAIN_FLOWER,
        )
    }
}
