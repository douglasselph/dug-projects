package dugsolutions.leaf.v30.round

import dugsolutions.leaf.v30.chronicle.GameChronicle
import dugsolutions.leaf.v30.chronicle.domain.GameEntry
import dugsolutions.leaf.v30.round.domain.RoundEffect
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class RoundEffectConverterTest {

    @Test
    fun invoke_withKnownTitle_returnsRoundEffect() {
        val converter = RoundEffectConverter()

        assertEquals(RoundEffect.RAISE_BY_3, converter("Resource_Sunlight_Water", "Sunlight"))
        assertEquals(RoundEffect.GAIN_WATER, converter("Resource_Sunlight_Water", "Water"))
        assertEquals(RoundEffect.GAIN_MULCH, converter("Resource_Water_Mulch", "Mulch"))
        assertEquals(RoundEffect.UPGRADE_DIE, converter("Resource_Compost_Mulch", "Compost"))
        assertEquals(RoundEffect.GAIN_WORMS, converter("Battle_Bloom_Burrow", "Burrow"))
        assertEquals(RoundEffect.GAIN_VP, converter("Battle_Bloom_Burrow", "Bloom"))
        assertEquals(RoundEffect.GAIN_D20, converter("Battle_Bloom_Surge", "Surge"))
        assertEquals(RoundEffect.GAIN_D12, converter("Battle_Bloom_Swell", "Swell"))
        assertEquals(RoundEffect.GAIN_ROOT, converter("Battle_Rootcall_Swell", "Rootcall"))
        assertEquals(RoundEffect.GAIN_VINE, converter("Battle_Vinecall_Swell", "Vinecall"))
        assertEquals(RoundEffect.GAIN_FLOWER, converter("Battle_Flowercall_Surge", "Flowercall"))
    }

    @Test
    fun invoke_withUnknownTitle_returnsUnknownAndRecordsLoadingWarning() {
        val chronicle = GameChronicle()
        val converter = RoundEffectConverter(chronicle)

        val result = converter("Round_Missing", "Mystery")

        assertEquals(RoundEffect.UNKNOWN, result)
        val entry = assertIs<GameEntry.LoadingWarning>(chronicle.getEntries().single())
        assertEquals("Round_Missing", entry.name)
        assertEquals("Mystery", entry.title)
        assertEquals("Unknown round card effect", entry.reason)
    }
}
