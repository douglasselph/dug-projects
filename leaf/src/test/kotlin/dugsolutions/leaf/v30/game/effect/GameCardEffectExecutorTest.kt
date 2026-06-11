package dugsolutions.leaf.v30.game.effect

import dugsolutions.leaf.v30.cards.GameCardRegistry
import dugsolutions.leaf.v30.cards.domain.CardEffect
import dugsolutions.leaf.v30.cards.domain.GameCard
import dugsolutions.leaf.v30.battle.domain.BattleItem
import dugsolutions.leaf.v30.battle.domain.BattleStrikeRow
import dugsolutions.leaf.v30.chronicle.GameChronicle
import dugsolutions.leaf.v30.chronicle.domain.GameEntry
import dugsolutions.leaf.v30.chronicle.domain.GameEntryMessage
import dugsolutions.leaf.v30.chronicle.domain.WarningType
import dugsolutions.leaf.v30.common.Commons
import dugsolutions.leaf.v30.common.Critter
import dugsolutions.leaf.v30.common.Token
import dugsolutions.leaf.v30.game.domain.CurrentRoundNotSetException
import dugsolutions.leaf.v30.grove.Grove
import dugsolutions.leaf.v30.player.Player
import dugsolutions.leaf.v30.player.decision.domain.ExecuteTarget
import dugsolutions.leaf.v30.player.decision.domain.MainAction
import dugsolutions.leaf.v30.random.Randomizer
import dugsolutions.leaf.v30.random.die.Die
import dugsolutions.leaf.v30.random.die.DieSides
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
import kotlin.test.assertIs
import kotlin.test.assertTrue

class GameCardEffectExecutorTest {

    @Test
    fun invoke_whenCurrentRoundIsCultivation_dispatchesCultivationExecutor() {
        val table = createTable(numBattle = 0, numCultivation = 1).apply { roundDeck.next() }
        val player = Player()
        val card = loadGameCard()
        val action = MainAction.ExecuteCard(card, ExecuteTarget.PlayerDie(player, FixedDie(6, 3)))
        val cultivation = TrackingCultivationExecutor()
        val battle = TrackingBattleExecutor()
        val executor = GameCardEffectExecutor(cultivation, battle)

        executor(table, player, action)

        assertEquals(listOf(action), cultivation.actions)
        assertEquals(emptyList(), battle.actions)
    }

    @Test
    fun invoke_whenCurrentRoundIsBattle_dispatchesBattleExecutor() {
        val table = createTable(numBattle = 1, numCultivation = 0).apply { roundDeck.next() }
        val player = Player()
        val card = loadGameCard()
        val action = MainAction.ExecuteCard(card, ExecuteTarget.PlayerDie(player, FixedDie(6, 3)))
        val cultivation = TrackingCultivationExecutor()
        val battle = TrackingBattleExecutor()
        val executor = GameCardEffectExecutor(cultivation, battle)

        executor(table, player, action)

        assertEquals(emptyList(), cultivation.actions)
        assertEquals(listOf(action), battle.actions)
    }

    @Test
    fun invoke_whenCurrentRoundIsNotSet_throwsException() {
        val table = createTable(numBattle = 1, numCultivation = 0)
        val executor = GameCardEffectExecutor()

        assertThrows<CurrentRoundNotSetException> {
            executor(table, Player(), MainAction.ExecuteCard(loadGameCard()))
        }
    }

    @Test
    fun cultivationInvoke_whenCardEffectIsUnknown_recordsChronicleWarning() {
        val chronicle = GameChronicle()
        val executor = GameCardEffectExecutorCultivation(chronicle)
        val table = createTable(numBattle = 0, numCultivation = 1)
        val player = Player(id = 7)
        val card = loadGameCard().copy(effect = CardEffect.UNKNOWN)

        executor(table, player, MainAction.ExecuteCard(card))

        val entry = assertIs<GameEntry.Warning>(chronicle.getEntries().single())
        assertEquals(WarningType.UNKNOWN_EFFECT, entry.type)
        assertEquals(player.id, entry.playerId)
        assertEquals(card.id, entry.cardId)
        assertEquals(card.name, entry.cardName)

        val message = GameEntryMessage()(entry)
        assertTrue(message.contains("WARNING"))
        assertTrue(message.contains("player=${player.id}"))
        assertTrue(message.contains("type=${WarningType.UNKNOWN_EFFECT}"))
        assertTrue(message.contains("card=${card.name}"))
    }

    @Test
    fun cultivationInvoke_whenCardEffectIsPlaceBulwarkToken_ignoresEffect() {
        val chronicle = GameChronicle()
        val executor = GameCardEffectExecutorCultivation(chronicle)
        val table = createTable(numBattle = 0, numCultivation = 1)
        val player = Player(id = 7)
        val card = loadGameCard().copy(effect = CardEffect.PLACE_BULWARK_TOKEN)

        executor(table, player, MainAction.ExecuteCard(card))

        assertEquals(emptyList(), chronicle.getEntries())
    }

    @Test
    fun battleInvoke_whenCardEffectIsPlaceBulwarkToken_addsTokenToTargetDieRow() {
        val executor = GameCardEffectExecutorBattle()
        val table = createTable(numBattle = 1, numCultivation = 0)
        val targetDie = FixedDie(8, 6)
        val target = playerWithDice(1, targetDie, FixedDie(6, 3), FixedDie(10, 1))
        table.battle.setup(
            listOf(
                target,
                playerWithDice(2, FixedDie(4, 1), FixedDie(6, 1), FixedDie(8, 1)),
                playerWithDice(3, FixedDie(4, 1), FixedDie(6, 1), FixedDie(8, 1)),
                playerWithDice(4, FixedDie(4, 1), FixedDie(6, 1), FixedDie(8, 1))
            )
        )
        val action = MainAction.ExecuteCard(
            card = loadGameCard().copy(effect = CardEffect.PLACE_BULWARK_TOKEN),
            target = ExecuteTarget.PlayerDie(target, targetDie)
        )

        executor(table, Player(id = 9), action)

        assertEquals(
            BattleItem.BulwarkToken,
            table.battle.grid.getSquare(target.id, BattleStrikeRow.STRIKE_1).all[1]
        )
    }

    @Test
    fun cultivationInvoke_whenGainWormAndBoostWorms_gainsWormAndBoostsPlayerWorms() {
        val executor = GameCardEffectExecutorCultivation()
        val table = createTable(numBattle = 0, numCultivation = 1)
        val player = Player(id = 7).apply {
            addCritter(Critter.WORM)
        }
        val action = MainAction.ExecuteCard(
            card = loadGameCard().copy(effect = CardEffect.GAIN_WORM_AND_BOOST_WORMS)
        )

        executor(table, player, action)

        assertEquals(listOf(Critter.BOOSTED_WORM, Critter.BOOSTED_WORM), player.critters)
        assertEquals(8, table.grove.count(Critter.WORM))
        assertEquals(0, table.grove.count(Critter.BOOSTED_WORM))
    }

    @Test
    fun battleInvoke_whenGainWormAndBoostWorms_boostsPlayerAndBattleGridWorms() {
        val executor = GameCardEffectExecutorBattle()
        val table = createTable(numBattle = 1, numCultivation = 0)
        val targetDie = FixedDie(8, 6)
        val target = playerWithDice(1, targetDie, FixedDie(6, 3), FixedDie(10, 1)).apply {
            addCritter(Critter.WORM)
        }
        table.battle.setup(
            listOf(
                target,
                playerWithDice(2, FixedDie(4, 1), FixedDie(6, 1), FixedDie(8, 1)),
                playerWithDice(3, FixedDie(4, 1), FixedDie(6, 1), FixedDie(8, 1)),
                playerWithDice(4, FixedDie(4, 1), FixedDie(6, 1), FixedDie(8, 1))
            )
        )
        table.battle.add(target, BattleStrikeRow.STRIKE_1, Critter.WORM)
        val action = MainAction.ExecuteCard(
            card = loadGameCard().copy(effect = CardEffect.GAIN_WORM_AND_BOOST_WORMS)
        )

        executor(table, target, action)

        assertEquals(listOf(Critter.BOOSTED_WORM, Critter.BOOSTED_WORM), target.critters)
        assertEquals(
            BattleItem.CritterItem(Critter.BOOSTED_WORM),
            table.battle.grid.getSquare(target.id, BattleStrikeRow.STRIKE_1).all[1]
        )
    }

    @Test
    fun cultivationInvoke_whenMulchDieFromDiscard_movesHighestDiscardDieIntoPlayerMulchToken() {
        val executor = GameCardEffectExecutorCultivation()
        val table = createTable(numBattle = 0, numCultivation = 1)
        val player = Player(id = 7).apply {
            addDieToSupply(FixedDie(6, 1))
            addDieToSupply(FixedDie(12, 1))
            addDieToSupply(FixedDie(8, 1))
            repeat(3) { drawDie() }
            discardHandDice()
        }
        val action = MainAction.ExecuteCard(
            card = loadGameCard().copy(effect = CardEffect.MULCH_DIE_FROM_DISCARD)
        )

        executor(table, player, action)

        assertEquals(listOf(Token.MULCH(DieSides.D12)), player.mulchTokens)
        assertEquals(listOf(6, 8), player.diceDiscard.dice.map { it.sides }.sorted())
        assertEquals(7, table.grove.count(Token.MULCH()))
        assertTrue(table.grove.tokens.mulchTokens.all { it.sides == null })
    }

    @Test
    fun cultivationInvoke_whenMulchDieFromDiscardHasNoDiscardDie_returnsGroveToken() {
        val executor = GameCardEffectExecutorCultivation()
        val table = createTable(numBattle = 0, numCultivation = 1)
        val player = Player(id = 7)
        val action = MainAction.ExecuteCard(
            card = loadGameCard().copy(effect = CardEffect.MULCH_DIE_FROM_DISCARD)
        )

        executor(table, player, action)

        assertEquals(emptyList(), player.mulchTokens)
        assertEquals(8, table.grove.count(Token.MULCH()))
        assertTrue(table.grove.tokens.mulchTokens.all { it.sides == null })
    }

    @Test
    fun battleInvoke_whenMulchDieFromDiscard_usesSameImplementation() {
        val executor = GameCardEffectExecutorBattle()
        val table = createTable(numBattle = 1, numCultivation = 0)
        val player = Player(id = 7).apply {
            addDieToSupply(FixedDie(4, 1))
            addDieToSupply(FixedDie(20, 1))
            repeat(2) { drawDie() }
            discardHandDice()
        }
        val action = MainAction.ExecuteCard(
            card = loadGameCard().copy(effect = CardEffect.MULCH_DIE_FROM_DISCARD)
        )

        executor(table, player, action)

        assertEquals(listOf(Token.MULCH(DieSides.D20)), player.mulchTokens)
        assertEquals(listOf(4), player.diceDiscard.dice.map { it.sides })
        assertEquals(7, table.grove.count(Token.MULCH()))
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

    private fun playerWithDice(
        id: Int,
        vararg dice: Die
    ): Player {
        return Player(id = id).apply {
            dice.forEach { addDieToSupply(it) }
            repeat(dice.size) { drawDie() }
        }
    }

    private class TrackingCultivationExecutor : GameCardEffectExecutorCultivation() {
        val actions = mutableListOf<MainAction.ExecuteCard>()

        override fun invoke(
            table: Table,
            player: Player,
            action: MainAction.ExecuteCard
        ) {
            actions.add(action)
        }
    }

    private class TrackingBattleExecutor : GameCardEffectExecutorBattle() {
        val actions = mutableListOf<MainAction.ExecuteCard>()

        override fun invoke(
            table: Table,
            player: Player,
            action: MainAction.ExecuteCard
        ) {
            actions.add(action)
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
