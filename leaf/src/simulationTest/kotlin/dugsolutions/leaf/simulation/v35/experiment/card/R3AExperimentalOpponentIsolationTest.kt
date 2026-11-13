package dugsolutions.leaf.simulation.v35.experiment.card

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class R3AExperimentalOpponentIsolationTest {
    @Test fun `targeted specialist receives its own intervention`() {
        for (seat in 0..1) {
            assertEquals(seat, R3APlantScreenMain.experimentalTrainingSeat("targeted", false, seat))
            assertEquals(seat, R3APlantScreenMain.experimentalTrainingSeat("targeted", true, seat))
        }
    }

    @Test fun `fixed opponent receives variant during challenger countersearch`() {
        assertEquals(1, R3APlantScreenMain.experimentalTrainingSeat("unrestricted", true, 0))
        assertEquals(0, R3APlantScreenMain.experimentalTrainingSeat("unrestricted", true, 1))
        assertEquals(0, R3APlantScreenMain.experimentalTrainingSeat("unrestricted", false, 0))
        assertEquals(1, R3APlantScreenMain.experimentalTrainingSeat("unrestricted", false, 1))
    }

    @Test fun `invalid seat or objective fails closed`() {
        assertFailsWith<IllegalArgumentException> {
            R3APlantScreenMain.experimentalTrainingSeat("unrestricted", true, 2)
        }
        assertFailsWith<IllegalArgumentException> {
            R3APlantScreenMain.experimentalTrainingSeat("neutral", false, 0)
        }
    }
}
