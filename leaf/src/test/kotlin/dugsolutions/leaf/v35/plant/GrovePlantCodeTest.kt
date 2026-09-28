package dugsolutions.leaf.v35.plant

import dugsolutions.leaf.v35.random.Randomizer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class GrovePlantCodeTest {
    @Test
    fun `exact code maps digits to rules-order card names`() {
        assertEquals(
            listOf("Root_05_03", "Root_07_01", "Root_09_04", "Vine_07_02", "Vine_09_04", "Vine_11_01", "Flower_11_01", "Flower_14_02", "Flower_17_03"),
            GrovePlantCode.overrideNames("314241123")
        )
    }

    @Test
    fun `zero means no override for that slot`() {
        assertEquals(listOf("Root_09_04"), GrovePlantCode.overrideNames("004000000"))
    }

    @Test
    fun `generation preserves fixed digits and fills zeros reproducibly`() {
        val a = GrovePlantCode.generate("004000000", Randomizer.create(77L))
        val b = GrovePlantCode.generate("004000000", Randomizer.create(77L))
        assertEquals(a, b)
        assertEquals('4', a[2])
        assert(a.all { it in '1'..'4' })
    }

    @Test
    fun `invalid code is rejected`() {
        assertThrows(IllegalArgumentException::class.java) { GrovePlantCode.validate("123") }
        assertThrows(IllegalArgumentException::class.java) { GrovePlantCode.validate("123456789") }
    }
}
