package dugsolutions.leaf.v30.game.effect

import dugsolutions.leaf.v30.cards.GameCardRegistry
import dugsolutions.leaf.v30.cards.domain.GameCard
import dugsolutions.leaf.v30.common.Commons
import dugsolutions.leaf.v30.game.domain.CurrentRoundNotSetException
import dugsolutions.leaf.v30.grove.Grove
import dugsolutions.leaf.v30.player.Player
import dugsolutions.leaf.v30.random.Randomizer
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

class GameCardEffectExecutorTest {

    @Test
    fun invoke_whenCurrentRoundIsCultivation_dispatchesCultivationExecutor() {
        val table = createTable(numBattle = 0, numCultivation = 1).apply { roundDeck.next() }
        val player = Player()
        val card = loadGameCard()
        val cultivation = TrackingCultivationExecutor()
        val battle = TrackingBattleExecutor()
        val executor = GameCardEffectExecutor(cultivation, battle)

        executor(table, player, card)

        assertEquals(listOf(card), cultivation.cards)
        assertEquals(emptyList(), battle.cards)
    }

    @Test
    fun invoke_whenCurrentRoundIsBattle_dispatchesBattleExecutor() {
        val table = createTable(numBattle = 1, numCultivation = 0).apply { roundDeck.next() }
        val player = Player()
        val card = loadGameCard()
        val cultivation = TrackingCultivationExecutor()
        val battle = TrackingBattleExecutor()
        val executor = GameCardEffectExecutor(cultivation, battle)

        executor(table, player, card)

        assertEquals(emptyList(), cultivation.cards)
        assertEquals(listOf(card), battle.cards)
    }

    @Test
    fun invoke_whenCurrentRoundIsNotSet_throwsException() {
        val table = createTable(numBattle = 1, numCultivation = 0)
        val executor = GameCardEffectExecutor()

        assertThrows<CurrentRoundNotSetException> {
            executor(table, Player(), loadGameCard())
        }
    }

    private fun createTable(
        numBattle: Int,
        numCultivation: Int
    ): Table {
        val roundDeck = createRoundDeck().apply {
            setup(numBattle = numBattle, numCultivation = numCultivation)
        }
        return Table(Grove(createWispDeck()), roundDeck)
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

    private fun loadGameCard(): GameCard {
        return GameCardRegistry()
            .apply { loadFromCsv(Commons.CARD_LIST) }
            .getAllCards()
            .first()
    }

    private class TrackingCultivationExecutor : GameCardEffectExecutorCultivation() {
        val cards = mutableListOf<GameCard>()

        override fun invoke(
            table: Table,
            player: Player,
            card: GameCard
        ) {
            cards.add(card)
        }
    }

    private class TrackingBattleExecutor : GameCardEffectExecutorBattle() {
        val cards = mutableListOf<GameCard>()

        override fun invoke(
            table: Table,
            player: Player,
            card: GameCard
        ) {
            cards.add(card)
        }
    }

    private class IdentityRandomizer : Randomizer {
        override fun nextBoolean(): Boolean = throw UnsupportedOperationException()
        override fun nextInt(from: Int, until: Int): Int = throw UnsupportedOperationException()
        override fun nextInt(until: Int): Int = throw UnsupportedOperationException()
        override fun <T> randomOrNull(list: List<T>): T? = throw UnsupportedOperationException()
        override fun <T> shuffled(list: List<T>): List<T> = list
    }
}
