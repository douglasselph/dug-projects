package dugsolutions.leaf.simulation.v35.experiment.card

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class R2SappingDiscoveryOptionsTest {
    private val grove = listOf("Root_05_02", "Root_07_04", "Root_09_03", "Vine_07_01",
        "Vine_09_01", "Vine_11_04", "Flower_11_03", "Flower_14_02", "Flower_17_04")

    private fun args(phase: String = "train", condition: String = "original", objective: String = "targeted") = arrayOf(
        "--phase=$phase", "--condition=$condition", "--objective=$objective", "--generations=2",
        "--population=4", "--elites=1", "--games=3", "--sigma=0.6", "--mutations=12",
        "--evolution-seed=620000", "--seed=720000", "--strategy-seed=820000",
        "--holdout-games=10", "--target-boost=15.0", "--target-min-purchase-rate=0.15",
        "--target-purchase-bonus=0.2", "--grove=${grove.joinToString(",")}",
        "--output-dir=output/experiments/r2-test", "--fingerprint=${"a".repeat(64)}"
    )

    @Test fun `train accepts the exact original Sapping catalog Grove`() {
        val o = R2SappingDiscoveryMain.Options.parse(args())
        assertEquals("original", o.condition)
        assertEquals("targeted", o.objective)
        assertEquals(0.15, o.targetMinPurchaseRate)
        assertEquals(grove, o.grove)
    }

    @Test fun `different-die unrestricted train is distinct`() {
        val o = R2SappingDiscoveryMain.Options.parse(args(condition = "different-die", objective = "unrestricted"))
        assertEquals("different-die", o.condition)
        assertEquals("unrestricted", o.objective)
    }

    @Test fun `rejects out-of-range exploration requirement`() {
        val bad = args().map { if (it.startsWith("--target-min-purchase-rate=")) "--target-min-purchase-rate=1.1" else it }
        assertThrows(IllegalArgumentException::class.java) { R2SappingDiscoveryMain.Options.parse(bad.toTypedArray()) }
    }

    @Test fun `rejects missing evaluation policy evidence`() {
        assertThrows(IllegalArgumentException::class.java) {
            R2SappingDiscoveryMain.Options.parse(args(phase = "evaluate"))
        }
    }

    @Test fun `rejects duplicate Grove card IDs`() {
        val repeated = grove.toMutableList().also { it[8] = it[6] }
        val bad = args().map { if (it.startsWith("--grove=")) "--grove=${repeated.joinToString(",")}" else it }
        assertThrows(IllegalArgumentException::class.java) { R2SappingDiscoveryMain.Options.parse(bad.toTypedArray()) }
    }
}
