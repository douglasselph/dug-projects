package dugsolutions.leaf.v30.game.effect.details

import dugsolutions.leaf.v30.battle.Battle
import dugsolutions.leaf.v30.battle.domain.BattleStrikeRow
import dugsolutions.leaf.v30.cards.GameCardRegistry
import dugsolutions.leaf.v30.cards.domain.CardEffect
import dugsolutions.leaf.v30.cards.domain.GameCard
import dugsolutions.leaf.v30.chronicle.GameChronicle
import dugsolutions.leaf.v30.chronicle.domain.GameEntry
import dugsolutions.leaf.v30.common.Commons
import dugsolutions.leaf.v30.common.Token
import dugsolutions.leaf.v30.game.effect.scope.BattleDieEffectScope
import dugsolutions.leaf.v30.grove.Grove
import dugsolutions.leaf.v30.player.Player
import dugsolutions.leaf.v30.player.decision.domain.ExecuteTarget
import dugsolutions.leaf.v30.random.Randomizer
import dugsolutions.leaf.v30.random.die.Dice
import dugsolutions.leaf.v30.random.die.Die
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

class SimpleDieEffectsBattleTest {

    @Test
    fun rerollDieUntilThreeOrHigher_rerollsBattleDieUntilAtLeastThree() {
        val chronicle = GameChronicle()
        val table = createTable()
        val card = loadCard(CardEffect.REROLL_DIE_UNTIL_THREE_OR_HIGHER)
        val targetDie = SequenceDie(8, initial = 6, rolls = listOf(1, 2, 4))
        val target = player(1, targetDie, FixedDie(6, 3), FixedDie(4, 1))
        setupBattle(table, target)

        val actingPlayer = Player(id = 9)
        RerollDieUntilThreeOrHigher(chronicle)(
            scope = BattleDieEffectScope(
                battle = table.battle,
                actingPlayer = actingPlayer,
                targetPlayer = target,
                row = BattleStrikeRow.STRIKE_1
            ),
            card = card,
            target = ExecuteTarget(player = target, dice = diceOf(FixedDie(8, 6))),
        )

        assertEquals(3, targetDie.rollCount)
        assertEquals(4, targetDie.value)
        val entry = assertIs<GameEntry.GameCardEffect>(chronicle.getEntries().single())
        assertEquals(card.effect, entry.effect)
        assertEquals(listOf(8 to 4), entry.dice.map { it.sides to it.value })
    }

    @Test
    fun raiseDiePlus1AndGainWater_raisesBattleDieAndMovesWaterTokenToPlayer() {
        val chronicle = GameChronicle()
        val table = createTable()
        val card = loadCard(CardEffect.RAISE_DIE_PLUS_1_AND_GAIN_WATER)
        val targetDie = FixedDie(8, 6)
        val target = player(1, targetDie, FixedDie(6, 3), FixedDie(4, 1))
        val actingPlayer = Player(id = 9)
        setupBattle(table, target)

        RaiseDiePlus1AndGainWaterBattle(chronicle)(
            table = table,
            player = actingPlayer,
            card = card,
            target = ExecuteTarget(player = target, dice = diceOf(FixedDie(8, 6))),
            row = BattleStrikeRow.STRIKE_1
        )

        assertEquals(7, targetDie.value)
        assertEquals(1, actingPlayer.waterTokenCount)
        assertEquals(7, table.grove.count(Token.WATER))
        val entries = chronicle.getEntries().filterIsInstance<GameEntry.GameCardEffect>()
        assertEquals(listOf(8 to 7), entries[0].dice.map { it.sides to it.value })
        assertEquals(Token.WATER, entries[1].token)
    }

    @Test
    fun flipDieToOppositeFace_flipsTargetBattleDie() {
        val chronicle = GameChronicle()
        val table = createTable()
        val card = loadCard(CardEffect.FLIP_DIE_TO_OPPOSITE_FACE)
        val targetDie = FixedDie(8, 6)
        val target = player(1, targetDie, FixedDie(6, 3), FixedDie(4, 1))
        setupBattle(table, target)

        val actingPlayer = Player(id = 9)
        FlipDieToOppositeFace(chronicle)(
            scope = BattleDieEffectScope(
                battle = table.battle,
                actingPlayer = actingPlayer,
                targetPlayer = target,
                row = BattleStrikeRow.STRIKE_1
            ),
            card = card,
            target = ExecuteTarget(player = target, dice = diceOf(FixedDie(8, 6))),
        )

        assertEquals(3, targetDie.value)
        val entry = assertIs<GameEntry.GameCardEffect>(chronicle.getEntries().single())
        assertEquals(listOf(8 to 3), entry.dice.map { it.sides to it.value })
    }

    @Test
    fun setDieToMatchAnother_setsSecondTargetBattleDieToFirstTargetDieValue() {
        val chronicle = GameChronicle()
        val table = createTable()
        val card = loadCard(CardEffect.SET_DIE_TO_MATCH_ANOTHER)
        val source = FixedDie(8, 6)
        val targetDie = FixedDie(6, 2)
        val target = player(1, source, targetDie, FixedDie(4, 1))
        setupBattle(table, target)
        table.battle.remove(target, BattleStrikeRow.STRIKE_2, targetDie)
        table.battle.add(target, BattleStrikeRow.STRIKE_1, targetDie)

        SetDieToMatchAnother(chronicle)(
            scope = BattleDieEffectScope(
                battle = table.battle,
                actingPlayer = Player(id = 9),
                targetPlayer = target,
                row = BattleStrikeRow.STRIKE_1
            ),
            card = card,
            target = ExecuteTarget(player = target, dice = diceOf(FixedDie(8, 6), FixedDie(6, 2))),
        )

        assertEquals(6, source.value)
        assertEquals(6, targetDie.value)
        val entry = assertIs<GameEntry.GameCardEffect>(chronicle.getEntries().single())
        assertEquals(card.effect, entry.effect)
        assertEquals(listOf(6 to 6, 8 to 6), entry.dice.map { it.sides to it.value })
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

    private fun setupBattle(table: Table, target: Player) {
        table.battle.setup(
            listOf(
                target,
                player(2, FixedDie(4, 1), FixedDie(6, 1), FixedDie(8, 1)),
                player(3, FixedDie(4, 1), FixedDie(6, 1), FixedDie(8, 1)),
                player(4, FixedDie(4, 1), FixedDie(6, 1), FixedDie(8, 1))
            )
        )
    }

    private fun player(
        id: Int,
        vararg dice: Die
    ): Player {
        return Player(id = id).apply {
            dice.forEach { addDieToSupply(it) }
            repeat(dice.size) { drawDie() }
        }
    }

    private fun loadCard(effect: CardEffect): GameCard {
        return GameCardRegistry()
            .apply { loadFromCsv(Commons.CARD_LIST) }
            .getAllCards()
            .first()
            .copy(effect = effect)
    }

    private fun diceOf(vararg dice: Die): Dice = Dice(dice.toList())

    private class FixedDie(
        sides: Int,
        value: Int
    ) : Die(sides) {
        init { adjustTo(value) }
        override fun roll(): Die = this
    }

    private class SequenceDie(
        sides: Int,
        initial: Int,
        private val rolls: List<Int>
    ) : Die(sides) {
        var rollCount = 0

        init { adjustTo(initial) }

        override fun roll(): Die {
            adjustTo(rolls.getOrElse(rollCount) { rolls.last() })
            rollCount++
            return this
        }
    }

    private class IdentityRandomizer : Randomizer {
        override fun nextBoolean(): Boolean = true
        override fun nextInt(from: Int, until: Int): Int = from
        override fun nextInt(until: Int): Int = 0
        override fun <T> randomOrNull(list: List<T>): T? = list.firstOrNull()
        override fun <T> shuffled(list: List<T>): List<T> = list
    }
}
