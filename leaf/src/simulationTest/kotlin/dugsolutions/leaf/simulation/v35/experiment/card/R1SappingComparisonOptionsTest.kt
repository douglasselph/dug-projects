package dugsolutions.leaf.simulation.v35.experiment.card

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class R1SappingComparisonOptionsTest {
    private val grove = "Root_05_02,Root_07_04,Root_09_03,Vine_07_01,Vine_09_01,Vine_11_04,Flower_11_03,Flower_14_02,Flower_17_04"
    private fun options(condition: String = "original", cards: String = grove) = arrayOf(
        "--condition=$condition", "--games=4", "--chunk=2", "--players=2",
        "--seed=123", "--strategy-seed=456", "--grove=$cards", "--output-dir=build/r1-options-test",
        "--fingerprint=0123456789abcdef"
    )

    @Test fun acceptsOnlyExplicitlyNamedConditionAndNineDistinctCards() {
        val opts = R1SappingComparisonMain.Options.parse(options("different-die"))
        assertEquals("different-die", opts.condition)
        assertEquals(9, opts.grove.size)
        assertEquals(4, opts.games)
        assertThrows(IllegalArgumentException::class.java) {
            R1SappingComparisonMain.Options.parse(options("sneaky-other-effect"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            R1SappingComparisonMain.Options.parse(options(cards = grove.replace("Flower_14_02", "Flower_11_03")))
        }
        assertThrows(IllegalArgumentException::class.java) {
            R1SappingComparisonMain.Options.parse(options().dropLast(1).toTypedArray())
        }
    }
}
