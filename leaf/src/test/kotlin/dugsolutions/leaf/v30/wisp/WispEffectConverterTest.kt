package dugsolutions.leaf.v30.wisp

import dugsolutions.leaf.v30.chronicle.GameChronicle
import dugsolutions.leaf.v30.chronicle.domain.GameEntry
import dugsolutions.leaf.v30.wisp.domain.WispEffect
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class WispEffectConverterTest {

    @Test
    fun invoke_withKnownName_returnsWispEffect() {
        val converter = WispEffectConverter()

        assertEquals(WispEffect.KEEP_2_VP, converter("Wisp_Award_VP", "Wisp of Honor"))
        assertEquals(WispEffect.GAIN_2_CRITTERS, converter("Wisp_Gain_Critters", "Whispering Wings"))
        assertEquals(WispEffect.GAIN_GREEN_BUTTERFLY, converter("Wisp_Gain_Green", "Pollinating Wisp"))
        assertEquals(WispEffect.GAIN_PURPLE_BUTTERFLY, converter("Wisp_Gain_Purple", "Pollinating Wisp"))
        assertEquals(WispEffect.GAIN_RED_BUTTERFLY, converter("Wisp_Gain_Red", "Pollinating Wisp"))
        assertEquals(WispEffect.GAIN_YELLOW_BUTTERFLY, converter("Wisp_Gain_Yellow", "Pollinating Wisp"))
        assertEquals(WispEffect.GAIN_MULCH, converter("Wisp_Mulch_Die", "Pocketed Spark"))
        assertEquals(WispEffect.SWAP_DICE, converter("Wisp_Swap_Die", "Pollen Theft"))
        assertEquals(WispEffect.UPGRADE_2_STEPS, converter("Wisp_Upgrade_Die", "Overgrowth"))
    }

    @Test
    fun invoke_withUnknownName_returnsUnknownAndRecordsLoadingWarning() {
        val chronicle = GameChronicle()
        val converter = WispEffectConverter(chronicle)

        val result = converter("Wisp_Missing", "Mystery Wisp")

        assertEquals(WispEffect.UNKNOWN, result)
        val entry = assertIs<GameEntry.LoadingWarning>(chronicle.getEntries().single())
        assertEquals("Wisp_Missing", entry.name)
        assertEquals("Mystery Wisp", entry.title)
        assertEquals("Unknown wisp card effect", entry.reason)
    }
}
