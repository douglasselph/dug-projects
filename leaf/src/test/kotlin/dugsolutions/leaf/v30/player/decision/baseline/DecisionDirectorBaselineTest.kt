package dugsolutions.leaf.v30.player.decision.baseline

import dugsolutions.leaf.v30.common.Critter
import dugsolutions.leaf.v30.player.Player
import dugsolutions.leaf.v30.player.decision.domain.Decision
import dugsolutions.leaf.v30.player.decision.domain.MainAction
import dugsolutions.leaf.v30.grove.Grove
import dugsolutions.leaf.v30.random.Randomizer
import dugsolutions.leaf.v30.round.RoundCardManager
import dugsolutions.leaf.v30.round.RoundCardRegistry
import dugsolutions.leaf.v30.round.RoundDeck
import dugsolutions.leaf.v30.round.di.RoundCardsFactory
import dugsolutions.leaf.v30.round.domain.RoundCard
import dugsolutions.leaf.v30.table.Table
import dugsolutions.leaf.v30.wisp.WispCardManager
import dugsolutions.leaf.v30.wisp.WispCardRegistry
import dugsolutions.leaf.v30.wisp.WispDeck
import dugsolutions.leaf.v30.wisp.di.WispCardsFactory
import dugsolutions.leaf.v30.common.Commons
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
        val result = SUT.chooseMainActionCultivation(
            Decision.ChooseMainActionCultivation(
                player = Player(),
                roundCard = sampleRoundCard(),
                table = createTable(),
                actionsRemaining = 2
            )
        )

        assertEquals(MainAction.PullDie, result)
    }

    @Test
    fun chooseItemsToBuy_returnsEmptyItemsToBuy() {
        val result = SUT.chooseItemsToBuy(
            Decision.ChooseItemsToBuy(
                player = Player(),
                grove = createGrove()
            )
        )

        assertEquals(emptyList(), result.dice)
        assertEquals(emptyList(), result.cards.cards)
        assertEquals(emptyList(), result.crittersUsed.all)
    }

    @Test
    fun chooseCardsToRefreshWithWorms_returnsEmptyCardsToRefresh() {
        val result = SUT.chooseCardsToRefreshWithWorms(
            Decision.ChooseCardsToRefreshWithWorms(Player())
        )

        assertEquals(emptyList(), result.cards.cards)
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

    private fun createGrove(): Grove {
        return Grove(createWispDeck())
    }

    private fun createTable(): Table {
        return Table(createGrove(), createRoundDeck())
    }

    private fun createRoundDeck(): RoundDeck {
        val registry = RoundCardRegistry()
        registry.loadFromCsv(Commons.ROUND_CARD_LIST)
        val manager = RoundCardManager(RoundCardsFactory())
        manager.loadCards(registry)
        return RoundDeck(manager, IdentityRandomizer())
    }

    private fun createWispDeck(): WispDeck {
        val registry = WispCardRegistry()
        registry.loadFromCsv(Commons.WISP_LIST)
        val manager = WispCardManager(WispCardsFactory())
        manager.loadCards(registry)
        return WispDeck(manager, IdentityRandomizer())
    }

    private class IdentityRandomizer : Randomizer {
        override fun nextBoolean(): Boolean = throw UnsupportedOperationException()
        override fun nextInt(from: Int, until: Int): Int = from
        override fun nextInt(until: Int): Int = throw UnsupportedOperationException()
        override fun <T> randomOrNull(list: List<T>): T? = throw UnsupportedOperationException()
        override fun <T> shuffled(list: List<T>): List<T> = list
    }
}
