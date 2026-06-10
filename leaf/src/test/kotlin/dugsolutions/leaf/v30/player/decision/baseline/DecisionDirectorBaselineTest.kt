package dugsolutions.leaf.v30.player.decision.baseline

import dugsolutions.leaf.v30.common.Critter
import dugsolutions.leaf.v30.player.Player
import dugsolutions.leaf.v30.player.decision.domain.Decision
import dugsolutions.leaf.v30.player.decision.domain.MainAction
import dugsolutions.leaf.v30.round.domain.RoundCard
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

    @Test
    fun chooseMainAction_returnsPullDie() {
        val result = SUT.chooseMainAction(
            Decision.ChooseMainAction(
                player = Player(),
                roundCard = sampleRoundCard(),
                actionsRemaining = 2
            )
        )

        assertEquals(MainAction.PullDie, result)
    }

    private fun sampleRoundCard() = RoundCard(
        id = 1,
        quantity = 1,
        name = "Resource_Test",
        title = "Cultivation",
        effect1Title = "One",
        effect1Text = "One",
        effect1Bg = "000000",
        effect1TextFg = "ffffff",
        effect1Image = null,
        effect1Icon = null,
        effect2Title = "Two",
        effect2Text = "Two",
        effect2Bg = "000000",
        effect2TextFg = "ffffff",
        effect2Image = null,
        effect2Icon = null,
        backImage = null
    )
}
