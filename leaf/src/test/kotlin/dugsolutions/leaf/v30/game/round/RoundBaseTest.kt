package dugsolutions.leaf.v30.game.round

import dugsolutions.leaf.v30.common.Commons
import dugsolutions.leaf.v30.common.Critter
import dugsolutions.leaf.v30.grove.Grove
import dugsolutions.leaf.v30.player.Player
import dugsolutions.leaf.v30.player.decision.domain.Decision
import dugsolutions.leaf.v30.player.decision.domain.DecisionDirector
import dugsolutions.leaf.v30.player.decision.domain.MainAction
import dugsolutions.leaf.v30.player.domain.OutOfDiceException
import dugsolutions.leaf.v30.random.Randomizer
import dugsolutions.leaf.v30.random.die.Die
import dugsolutions.leaf.v30.random.die.SampleDie
import dugsolutions.leaf.v30.round.RoundCardManager
import dugsolutions.leaf.v30.round.RoundCardRegistry
import dugsolutions.leaf.v30.round.RoundDeck
import dugsolutions.leaf.v30.round.di.RoundCardsFactory
import dugsolutions.leaf.v30.table.Table
import dugsolutions.leaf.v30.wisp.WispCardManager
import dugsolutions.leaf.v30.wisp.WispCardRegistry
import dugsolutions.leaf.v30.wisp.WispDeck
import dugsolutions.leaf.v30.wisp.di.WispCardsFactory
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RoundBaseTest {

    @Test
    fun drawDice_discardsExistingHandThenDrawsThreeDiceForEachPlayer() {
        val dice = SampleDie(Randomizer.create(seed = 1L))
        val player1 = Player()
        val player2 = Player()
        player1.addDiceToSupply(listOf(dice.d4, dice.d6, dice.d8, dice.d10))
        player2.addDiceToSupply(listOf(dice.d4, dice.d6, dice.d8))
        player1.drawDie()
        val table = createTable().add(player1).add(player2)
        val round = RoundCultivation(table, loadCultivationCard())

        round.drawDice()

        assertEquals(3, player1.diceHand.size)
        assertEquals(3, player2.diceHand.size)
        assertEquals(1, player1.diceDiscard.size)
        assertTrue(player2.diceDiscard.isEmpty())
    }

    @Test
    fun drawDice_whenPlayerHasNoDiceAvailable_throwsException() {
        val table = createTable().add(Player())
        val round = RoundCultivation(table, loadCultivationCard())

        assertThrows<OutOfDiceException> {
            round.drawDice()
        }
    }

    @Test
    fun rollDice_rollsEachPlayersHandDice() {
        val player1Die = TrackingDie(6)
        val player2Die = TrackingDie(8)
        val player1 = Player().apply {
            addDieToSupply(player1Die)
            drawDie()
        }
        val player2 = Player().apply {
            addDieToSupply(player2Die)
            drawDie()
        }
        val table = createTable().add(player1).add(player2)
        val round = RoundCultivation(table, loadCultivationCard())

        round.rollDice()

        assertEquals(1, player1Die.rollCount)
        assertEquals(1, player2Die.rollCount)
    }

    @Test
    fun resolveRewards_withOneRolled_givesPlayerBeeOnTie() {
        val player = Player()
        player.addDieToSupply(FixedDie(6, 1))
        player.drawDie()
        val table = createTable().add(player)
        val round = RoundCultivation(table, loadCultivationCard())

        round.resolveRewards()

        assertEquals(listOf(Critter.BEE), player.critters)
        assertEquals(8, table.grove.count(Critter.BEE))
    }

    @Test
    fun resolveRewards_withOneRolled_givesPlayerCritterTheyHaveLessOf() {
        val player = Player()
        player.addCritter(Critter.BEE)
        player.addDieToSupply(FixedDie(6, 1))
        player.drawDie()
        val table = createTable().add(player)
        val round = RoundCultivation(table, loadCultivationCard())

        round.resolveRewards()

        assertEquals(listOf(Critter.BEE, Critter.WORM), player.critters)
        assertEquals(8, table.grove.count(Critter.WORM))
    }

    @Test
    fun resolveRewards_withOneRolled_usesPlayersDecisionDirector() {
        val player = Player(AlwaysWormDecisionDirector())
        player.addDieToSupply(FixedDie(6, 1))
        player.drawDie()
        val table = createTable().add(player)
        val round = RoundCultivation(table, loadCultivationCard())

        round.resolveRewards()

        assertEquals(listOf(Critter.WORM), player.critters)
        assertEquals(8, table.grove.count(Critter.WORM))
    }

    @Test
    fun resolveRewards_withTwoRolled_givesPlayerNextWispCard() {
        val player = Player()
        player.addDieToSupply(FixedDie(6, 2))
        player.drawDie()
        val table = createTable().add(player)
        val startingWispCount = table.grove.wispDeck.remaining
        val round = RoundCultivation(table, loadCultivationCard())

        round.resolveRewards()

        assertEquals(1, player.wispCards.size)
        assertEquals(startingWispCount - 1, table.grove.wispDeck.remaining)
    }

    private fun createTable(): Table {
        return Table(
            grove = Grove(createWispDeck()),
            roundDeck = createRoundDeck()
        )
    }

    private fun loadCultivationCard() =
        RoundCardRegistry()
            .apply { loadFromCsv(Commons.ROUND_CARD_LIST) }
            .getCard("Resource_Compost_Mulch")
            ?: error("Missing test round card")

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
        override fun nextInt(from: Int, until: Int): Int = throw UnsupportedOperationException()
        override fun nextInt(until: Int): Int = throw UnsupportedOperationException()
        override fun <T> randomOrNull(list: List<T>): T? = throw UnsupportedOperationException()
        override fun <T> shuffled(list: List<T>): List<T> = list
    }

    private class TrackingDie(sides: Int) : Die(sides) {
        var rollCount = 0

        override fun roll(): Die {
            rollCount++
            return this
        }
    }

    private class FixedDie(
        sides: Int,
        value: Int
    ) : Die(sides) {
        init {
            adjustTo(value)
        }

        override fun roll(): Die = this
    }

    private class AlwaysWormDecisionDirector : DecisionDirector {
        override fun chooseCritter(input: Decision.ChooseCritter): Critter {
            return Critter.WORM
        }

        override fun chooseMainAction(input: Decision.ChooseMainAction): MainAction {
            return MainAction.PullDie
        }
    }
}
