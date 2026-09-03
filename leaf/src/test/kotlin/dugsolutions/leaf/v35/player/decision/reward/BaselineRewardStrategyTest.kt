package dugsolutions.leaf.v35.player.decision.reward

import dugsolutions.leaf.v35.tokens.Critter
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BaselineRewardStrategyTest {

    private val strategy = BaselineRewardStrategy()

    @Test
    fun chooseCritter_whenBeeAndWormLegalAndOwnedCountsTie_choosesBee() {
        val request = ChooseCritterRequest(
            legalChoices = listOf(
                Critter.BEE,
                Critter.WORM
            ),
            ownedCritters = emptyList()
        )

        val result = strategy.chooseCritter(request)

        assertEquals(Critter.BEE, result)
    }

    @Test
    fun chooseCritter_whenPlayerOwnsMoreBees_choosesWorm() {
        val request = ChooseCritterRequest(
            legalChoices = listOf(
                Critter.BEE,
                Critter.WORM
            ),
            ownedCritters = listOf(
                Critter.BEE,
                Critter.BEE,
                Critter.WORM
            )
        )

        val result = strategy.chooseCritter(request)

        assertEquals(Critter.WORM, result)
    }

    @Test
    fun chooseCritter_whenPlayerOwnsMoreWorms_choosesBee() {
        val request = ChooseCritterRequest(
            legalChoices = listOf(
                Critter.BEE,
                Critter.WORM
            ),
            ownedCritters = listOf(
                Critter.WORM,
                Critter.WORM,
                Critter.BEE
            )
        )

        val result = strategy.chooseCritter(request)

        assertEquals(Critter.BEE, result)
    }

    @Test
    fun chooseCritter_countsBoostedCrittersAsTheirNormalType() {
        val request = ChooseCritterRequest(
            legalChoices = listOf(
                Critter.BEE,
                Critter.WORM
            ),
            ownedCritters = listOf(
                Critter.BOOSTED_BEE,
                Critter.BEE,
                Critter.BOOSTED_WORM
            )
        )

        val result = strategy.chooseCritter(request)

        assertEquals(Critter.WORM, result)
    }

    @Test
    fun chooseCritter_whenOnlyOneChoiceIsLegal_returnsThatChoice() {
        val request = ChooseCritterRequest(
            legalChoices = listOf(Critter.WORM),
            ownedCritters = listOf(
                Critter.WORM,
                Critter.WORM
            )
        )

        val result = strategy.chooseCritter(request)

        assertEquals(Critter.WORM, result)
    }

    @Test
    fun request_whenNoLegalChoices_throws() {
        assertFailsWith<IllegalArgumentException> {
            ChooseCritterRequest(
                legalChoices = emptyList(),
                ownedCritters = emptyList()
            )
        }
    }

    @Test
    fun request_defensivelyCopiesLists() {
        val legalChoices = mutableListOf(
            Critter.BEE,
            Critter.WORM
        )
        val owned = mutableListOf(Critter.BEE)

        val request = ChooseCritterRequest(
            legalChoices = legalChoices,
            ownedCritters = owned
        )

        legalChoices.clear()
        owned.clear()

        assertEquals(
            listOf(Critter.BEE, Critter.WORM),
            request.legalChoices
        )
        assertEquals(
            listOf(Critter.BEE),
            request.ownedCritters
        )
    }
}
