package dugsolutions.leaf.v30.game

import dugsolutions.leaf.v30.common.Commons
import dugsolutions.leaf.v30.cards.GameCardRegistry
import dugsolutions.leaf.v30.cards.domain.GameCards
import dugsolutions.leaf.v30.game.round.RoundBattle
import dugsolutions.leaf.v30.game.round.RoundCultivation
import dugsolutions.leaf.v30.grove.Grove
import dugsolutions.leaf.v30.player.Player
import dugsolutions.leaf.v30.random.Randomizer
import dugsolutions.leaf.v30.random.die.Die
import dugsolutions.leaf.v30.round.RoundCardManager
import dugsolutions.leaf.v30.round.RoundCardRegistry
import dugsolutions.leaf.v30.round.RoundDeck
import dugsolutions.leaf.v30.round.di.RoundCardsFactory
import dugsolutions.leaf.v30.round.domain.RoundCardType
import dugsolutions.leaf.v30.table.Table
import dugsolutions.leaf.v30.table.domain.TableConfig
import dugsolutions.leaf.v30.wisp.WispCardManager
import dugsolutions.leaf.v30.wisp.WispCardRegistry
import dugsolutions.leaf.v30.wisp.WispDeck
import dugsolutions.leaf.v30.wisp.di.WispCardsFactory
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class GameTest {

    @Test
    fun setup_delegatesToTableSetup() {
        val roundDeck = createRoundDeck()
        val table = createTable(roundDeck)
        val game = Game(table)
        val config = TableConfig(
            cards = GameCards(listOf(loadGameCard("Root_05_01"))),
            numPlayers = 2,
            numBattle = 1,
            numCultivation = 2
        )

        game.setup(config)

        assertEquals(3, roundDeck.remaining)
    }

    @Test
    fun run_whenRoundDeckIsEmpty_returnsNull() {
        val roundDeck = createRoundDeck()
        roundDeck.setup(numBattle = 0, numCultivation = 0)
        val game = Game(createTable(roundDeck))

        val result = game.run()

        assertNull(result)
    }

    @Test
    fun run_whenNextRoundCardIsCultivation_runsCultivationRound() {
        val roundDeck = createRoundDeck()
        roundDeck.setup(numBattle = 0, numCultivation = 1)
        val game = Game(createTable(roundDeck))

        val result = game.run()

        assertIs<RoundCultivation>(result)
        assertEquals(RoundCardType.CULTIVATION, result.card.cardType)
    }

    @Test
    fun run_whenNextRoundCardIsBattle_runsBattleRound() {
        val roundDeck = createRoundDeck()
        roundDeck.setup(numBattle = 1, numCultivation = 0)
        val table = createTable(roundDeck)
        repeat(4) { index ->
            table.add(playerWithBattleDice(index + 1))
        }
        val game = Game(table)

        val result = game.run()

        assertIs<RoundBattle>(result)
        assertEquals(RoundCardType.BATTLE, result.card.cardType)
    }

    private fun createTable(roundDeck: RoundDeck): Table {
        return Table(
            grove = Grove(createWispDeck()),
            roundDeck = roundDeck
        )
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

    private fun loadGameCard(name: String) =
        GameCardRegistry()
            .apply { loadFromCsv(Commons.CARD_LIST) }
            .getCard(name)
            ?: error("Missing test card: $name")

    private fun playerWithBattleDice(id: Int): Player {
        return Player(id = id).apply {
            addDiceToSupply(
                listOf(
                    FixedDie(6, 4),
                    FixedDie(8, 3),
                    FixedDie(10, 2),
                    FixedDie(12, 1)
                )
            )
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

    private class IdentityRandomizer : Randomizer {
        override fun nextBoolean(): Boolean = throw UnsupportedOperationException()
        override fun nextInt(from: Int, until: Int): Int = throw UnsupportedOperationException()
        override fun nextInt(until: Int): Int = throw UnsupportedOperationException()
        override fun <T> randomOrNull(list: List<T>): T? = throw UnsupportedOperationException()
        override fun <T> shuffled(list: List<T>): List<T> = list
    }
}
