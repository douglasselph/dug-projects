package dugsolutions.leaf.v30.game.effect.details

import dugsolutions.leaf.v30.battle.Battle
import dugsolutions.leaf.v30.battle.domain.BattleItem
import dugsolutions.leaf.v30.battle.domain.BattleStrikeRow
import dugsolutions.leaf.v30.cards.GameCardRegistry
import dugsolutions.leaf.v30.cards.domain.CardEffect
import dugsolutions.leaf.v30.cards.domain.GameCard
import dugsolutions.leaf.v30.chronicle.GameChronicle
import dugsolutions.leaf.v30.chronicle.domain.GameEntry
import dugsolutions.leaf.v30.common.Commons
import dugsolutions.leaf.v30.grove.Grove
import dugsolutions.leaf.v30.player.Player
import dugsolutions.leaf.v30.player.decision.domain.ExecuteTarget
import dugsolutions.leaf.v30.random.Randomizer
import dugsolutions.leaf.v30.random.die.Dice
import dugsolutions.leaf.v30.random.die.Die
import dugsolutions.leaf.v30.random.die.DieSides
import dugsolutions.leaf.v30.random.die.di.DieFactory
import dugsolutions.leaf.v30.round.RoundCardManager
import dugsolutions.leaf.v30.round.RoundDeck
import dugsolutions.leaf.v30.round.di.RoundCardsFactory
import dugsolutions.leaf.v30.table.Table
import dugsolutions.leaf.v30.wisp.WispCardManager
import dugsolutions.leaf.v30.wisp.WispDeck
import dugsolutions.leaf.v30.wisp.di.WispCardsFactory
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class GainD4OrReturnD4RaiseDiePlus4Test {

    @Test
    fun cultivation_whenTargetHasNoDice_gainsD4AtValueFourIntoHand() {
        val chronicle = GameChronicle()
        val table = createTable()
        table.grove.add(DieSides.D4)
        val player = Player(id = 1)
        val card = loadCard()

        GainD4OrReturnD4RaiseDiePlus4Cultivation(chronicle, dieFactory())(
            grove = table.grove,
            player = player,
            card = card,
            target = ExecuteTarget()
        )

        assertEquals(0, table.grove.count(DieSides.D4))
        val d4 = player.diceHand.dice.single()
        assertEquals(4, d4.sides)
        assertEquals(4, d4.value)
        val entry = assertIs<GameEntry.GameCardEffect>(chronicle.getEntries().single())
        assertEquals(CardEffect.GAIN_D4_OR_RETURN_D4_RAISE_DIE_PLUS_4, entry.effect)
        assertEquals(listOf(4 to 4), entry.dice.map { it.sides to it.value })
    }

    @Test
    fun cultivation_whenTargetHasDie_returnsLowestHandD4AndRaisesTargetByFour() {
        val chronicle = GameChronicle()
        val table = createTable()
        val targetDie = TestDie(12, 5)
        val lowD4 = TestDie(4, 1)
        val highD4 = TestDie(4, 3)
        val player = Player(id = 1).apply {
            addDieToHand(targetDie)
            addDieToHand(highD4)
            addDieToHand(lowD4)
        }
        val card = loadCard()

        GainD4OrReturnD4RaiseDiePlus4Cultivation(chronicle, dieFactory())(
            grove = table.grove,
            player = player,
            card = card,
            target = ExecuteTarget(dice = diceOf(TestDie(12, 5)))
        )

        assertEquals(9, targetDie.value)
        assertEquals(1, table.grove.count(DieSides.D4))
        assertEquals(false, player.diceHand.hasDie(lowD4))
        assertTrue(player.diceHand.hasDie(highD4))
        val entry = assertIs<GameEntry.GameCardEffect>(chronicle.getEntries().single())
        assertEquals(listOf(4 to 1, 12 to 9), entry.dice.map { it.sides to it.value })
    }

    @Test
    fun battle_whenTargetHasNoDice_gainsD4AtValueFourOntoTargetRow() {
        val chronicle = GameChronicle()
        val table = createTable()
        table.grove.add(DieSides.D4)
        val player = playerWithDice(1, TestDie(12, 6), TestDie(8, 3), TestDie(6, 1))
        setupBattle(table, player)
        val card = loadCard()

        GainD4OrReturnD4RaiseDiePlus4Battle(chronicle, dieFactory())(
            battle = table.battle,
            grove = table.grove,
            player = player,
            card = card,
            target = ExecuteTarget(),
            row = BattleStrikeRow.STRIKE_1
        )

        assertEquals(0, table.grove.count(DieSides.D4))
        val dice = table.battle.grid.getSquare(player.id, BattleStrikeRow.STRIKE_1).all
            .filterIsInstance<BattleItem.DieItem>()
            .map { it.die.sides to it.die.value }
        assertEquals(listOf(12 to 6, 4 to 4), dice)
        val entry = assertIs<GameEntry.GameCardEffect>(chronicle.getEntries().single())
        assertEquals(listOf(4 to 4), entry.dice.map { it.sides to it.value })
    }

    @Test
    fun battle_whenSecondTargetDieIsD4_removesMatchingD4AndRaisesFirstDie() {
        val chronicle = GameChronicle()
        val table = createTable()
        val targetDie = TestDie(12, 6)
        val matchingD4 = TestDie(4, 2)
        val player = playerWithDice(1, targetDie, TestDie(8, 3), TestDie(6, 1))
        setupBattle(table, player)
        table.battle.add(player, BattleStrikeRow.STRIKE_2, matchingD4)
        val card = loadCard()

        GainD4OrReturnD4RaiseDiePlus4Battle(chronicle, dieFactory())(
            battle = table.battle,
            grove = table.grove,
            player = player,
            card = card,
            target = ExecuteTarget(dice = diceOf(TestDie(12, 6), TestDie(4, 2))),
            row = BattleStrikeRow.STRIKE_1
        )

        assertEquals(10, targetDie.value)
        assertEquals(1, table.grove.count(DieSides.D4))
        assertEquals(false, table.battle.hasDie(player, BattleStrikeRow.STRIKE_2, matchingD4))
        val entry = assertIs<GameEntry.GameCardEffect>(chronicle.getEntries().single())
        assertEquals(listOf(4 to 2, 12 to 10), entry.dice.map { it.sides to it.value })
    }

    @Test
    fun battle_whenSecondTargetIsNotD4_removesLowestGridD4AndRaisesFirstDie() {
        val chronicle = GameChronicle()
        val table = createTable()
        val targetDie = TestDie(12, 6)
        val lowD4 = TestDie(4, 1)
        val highD4 = TestDie(4, 3)
        val player = playerWithDice(1, targetDie, TestDie(8, 3), TestDie(6, 1))
        setupBattle(table, player)
        table.battle.add(player, BattleStrikeRow.STRIKE_2, highD4)
        table.battle.add(player, BattleStrikeRow.STRIKE_3, lowD4)
        val card = loadCard()

        GainD4OrReturnD4RaiseDiePlus4Battle(chronicle, dieFactory())(
            battle = table.battle,
            grove = table.grove,
            player = player,
            card = card,
            target = ExecuteTarget(dice = diceOf(TestDie(12, 6), TestDie(8, 3))),
            row = BattleStrikeRow.STRIKE_1
        )

        assertEquals(10, targetDie.value)
        assertEquals(1, table.grove.count(DieSides.D4))
        assertEquals(false, table.battle.hasDie(player, BattleStrikeRow.STRIKE_3, lowD4))
        assertTrue(table.battle.hasDie(player, BattleStrikeRow.STRIKE_2, highD4))
        val entry = assertIs<GameEntry.GameCardEffect>(chronicle.getEntries().single())
        assertEquals(listOf(4 to 1, 12 to 10), entry.dice.map { it.sides to it.value })
    }

    private fun createTable(): Table {
        val wispManager = WispCardManager(WispCardsFactory()).apply { loadCards(emptyList()) }
        val roundManager = RoundCardManager(RoundCardsFactory()).apply { loadCards(emptyList()) }
        return Table(
            grove = Grove(WispDeck(wispManager, IdentityRandomizer())),
            roundDeck = RoundDeck(roundManager, IdentityRandomizer()),
            battle = Battle()
        )
    }

    private fun setupBattle(
        table: Table,
        player: Player
    ) {
        table.battle.setup(
            listOf(
                player,
                playerWithDice(2, TestDie(4, 1), TestDie(6, 1), TestDie(8, 1)),
                playerWithDice(3, TestDie(4, 1), TestDie(6, 1), TestDie(8, 1)),
                playerWithDice(4, TestDie(4, 1), TestDie(6, 1), TestDie(8, 1))
            )
        )
    }

    private fun playerWithDice(
        id: Int,
        vararg dice: Die
    ): Player {
        return Player(id = id).apply {
            dice.forEach { addDieToHand(it) }
        }
    }

    private fun loadCard(): GameCard {
        return GameCardRegistry()
            .apply { loadFromCsv(Commons.CARD_LIST) }
            .getAllCards()
            .first()
            .copy(effect = CardEffect.GAIN_D4_OR_RETURN_D4_RAISE_DIE_PLUS_4)
    }

    private fun dieFactory(): DieFactory = DieFactory(IdentityRandomizer())

    private fun diceOf(vararg dice: Die): Dice = Dice(dice.toList())

    private class TestDie(
        sides: Int,
        value: Int
    ) : Die(sides) {
        init {
            adjustTo(value)
        }

        override fun roll(): Die = this
    }

    private class IdentityRandomizer : Randomizer {
        override fun nextBoolean(): Boolean = true
        override fun nextInt(from: Int, until: Int): Int = from
        override fun nextInt(until: Int): Int = 0
        override fun <T> randomOrNull(list: List<T>): T? = list.firstOrNull()
        override fun <T> shuffled(list: List<T>): List<T> = list
    }
}
