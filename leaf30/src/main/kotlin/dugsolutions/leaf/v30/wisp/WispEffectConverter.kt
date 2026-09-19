package dugsolutions.leaf.v30.wisp

import dugsolutions.leaf.v30.chronicle.Chronicle
import dugsolutions.leaf.v30.chronicle.GameChronicle
import dugsolutions.leaf.v30.chronicle.domain.Moment
import dugsolutions.leaf.v30.wisp.domain.WispEffect

class WispEffectConverter(
    private val chronicle: Chronicle = GameChronicle()
) {
    operator fun invoke(
        name: String,
        title: String
    ): WispEffect {
        val effect = effectsByName[name.trim()]
        if (effect != null) return effect

        chronicle(
            Moment.LoadingWarning(
                name = name,
                title = title,
                reason = "Unknown wisp card effect"
            )
        )
        return WispEffect.UNKNOWN
    }

    private companion object {
        val effectsByName = mapOf(
            "Wisp_Award_VP" to WispEffect.KEEP_2_VP,
            "Wisp_Gain_Critters" to WispEffect.GAIN_2_CRITTERS,
            "Wisp_Gain_Green" to WispEffect.GAIN_GREEN_BUTTERFLY,
            "Wisp_Gain_Purple" to WispEffect.GAIN_PURPLE_BUTTERFLY,
            "Wisp_Gain_Red" to WispEffect.GAIN_RED_BUTTERFLY,
            "Wisp_Gain_Yellow" to WispEffect.GAIN_YELLOW_BUTTERFLY,
            "Wisp_Mulch_Die" to WispEffect.GAIN_MULCH,
            "Wisp_Swap_Die" to WispEffect.SWAP_DICE,
            "Wisp_Upgrade_Die" to WispEffect.UPGRADE_2_STEPS,
        )
    }
}
