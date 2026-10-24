package dugsolutions.leaf.simulation.v35.experiment.card

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class R3APlantScreenOptionsTest {
    private val grove = listOf("Root_05_02", "Root_07_04", "Root_09_03", "Vine_07_01",
        "Vine_09_01", "Vine_11_04", "Flower_11_03", "Flower_14_01", "Flower_17_04")

    private fun args(phase: String = "train", objective: String = "targeted", target: String = "Flower_14_01") = arrayOf(
        "--phase=$phase", "--objective=$objective", "--target-id=$target", "--generations=2",
        "--population=4", "--elites=1", "--games=3", "--sigma=0.6", "--mutations=12",
        "--evolution-seed=620000", "--seed=720000", "--strategy-seed=820000", "--holdout-games=10",
        "--target-boost=15.0", "--target-min-purchase-rate=0.15", "--target-purchase-bonus=0.2", "--flower-enabler-boost=8.0",
        "--grove=${grove.joinToString(",")}", "--output-dir=output/experiments/r3a-test",
        "--fingerprint=${"b".repeat(64)}"
    )

    @Test fun `Bee-loved Bloom is a valid focus for a generic Grove`() {
        val options = R3APlantScreenMain.Options.parse(args())
        assertEquals("Flower_14_01", options.targetId)
        assertEquals(grove, options.grove)
    }

    @Test fun `Sapping works with same generic research policy`() {
        val options = R3APlantScreenMain.Options.parse(args(target="Flower_11_03", objective="unrestricted"))
        assertEquals("Flower_11_03", options.targetId)
        assertEquals("unrestricted", options.objective)
    }

    @Test fun `rejects malformed card ID`() {
        assertThrows(IllegalArgumentException::class.java) {
            R3APlantScreenMain.Options.parse(args(target="Not_A_Plant"))
        }
    }

    @Test fun `rejects zero training games`() {
        val bad = args().map { if (it.startsWith("--games=")) "--games=0" else it }
        assertThrows(IllegalArgumentException::class.java) { R3APlantScreenMain.Options.parse(bad.toTypedArray()) }
    }

    @Test fun `evaluation requires both frozen policies`() {
        assertThrows(IllegalArgumentException::class.java) {
            R3APlantScreenMain.Options.parse(args(phase="evaluate"))
        }
    }

    @Test fun `rejects invalid target exploration threshold`() {
        val bad = args().map { if (it.startsWith("--target-min-purchase-rate=")) "--target-min-purchase-rate=2.0" else it }
        assertThrows(IllegalArgumentException::class.java) { R3APlantScreenMain.Options.parse(bad.toTypedArray()) }
    }
}
