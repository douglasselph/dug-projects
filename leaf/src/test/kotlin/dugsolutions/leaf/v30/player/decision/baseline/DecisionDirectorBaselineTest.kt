package dugsolutions.leaf.v30.player.decision.baseline

import dugsolutions.leaf.v30.common.Critter
import dugsolutions.leaf.v30.player.Player
import dugsolutions.leaf.v30.player.decision.domain.Decision
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class DecisionDirectorBaselineTest {

    private val SUT = DecisionDirectorBaseline()

    @Test
    fun chooseCritter_whenTieAndBothAvailable_returnsBee() {
        val result = SUT.chooseCritter(
            Decision.ChooseCritter(
                player = Player(),
                availableCritters = listOf(Critter.BEE, Critter.WORM)
            )
        )

        assertEquals(Critter.BEE, result)
    }

    @Test
    fun chooseCritter_whenPlayerHasMoreBees_returnsWorm() {
        val player = Player().apply { addCritter(Critter.BEE) }

        val result = SUT.chooseCritter(
            Decision.ChooseCritter(
                player = player,
                availableCritters = listOf(Critter.BEE, Critter.WORM)
            )
        )

        assertEquals(Critter.WORM, result)
    }

    @Test
    fun chooseCritter_whenOnlyBeeAvailable_returnsBee() {
        val player = Player().apply { addCritter(Critter.BEE) }

        val result = SUT.chooseCritter(
            Decision.ChooseCritter(
                player = player,
                availableCritters = listOf(Critter.BEE)
            )
        )

        assertEquals(Critter.BEE, result)
    }

    @Test
    fun chooseCritter_whenNoCrittersAvailable_throwsException() {
        assertThrows<IllegalArgumentException> {
            SUT.chooseCritter(
                Decision.ChooseCritter(
                    player = Player(),
                    availableCritters = emptyList()
                )
            )
        }
    }
}
