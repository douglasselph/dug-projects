package dugsolutions.leaf.simulation.v35.experiment.card

import dugsolutions.leaf.simulation.v35.strategy.planned.TargetCard
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CardFocusExperimentSpecTest {
    @Test
    fun totalGames_multipliesGamesPerSeatByPlayerCount() {
        val spec = CardFocusExperimentSpec(
            targetCard = TargetCard("Root_07_02"),
            numPlayers = 4,
            gamesPerSeat = 1_000
        )

        assertEquals(4_000, spec.totalGames)
    }

    @Test
    fun rejectsInvalidCounts() {
        assertFailsWith<IllegalArgumentException> {
            CardFocusExperimentSpec(
                targetCard = TargetCard("Root_07_02"),
                targetCount = 0,
                gamesPerSeat = 1
            )
        }
        assertFailsWith<IllegalArgumentException> {
            CardFocusExperimentSpec(
                targetCard = TargetCard("Root_07_02"),
                numPlayers = 1,
                gamesPerSeat = 1
            )
        }
    }
}
