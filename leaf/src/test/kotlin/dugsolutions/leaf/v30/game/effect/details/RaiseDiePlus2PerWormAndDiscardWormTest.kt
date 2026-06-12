package dugsolutions.leaf.v30.game.effect.details

import dugsolutions.leaf.v30.battle.Battle
import dugsolutions.leaf.v30.battle.PlayerGridOrder
import dugsolutions.leaf.v30.battle.domain.BattleStrikeRow
import dugsolutions.leaf.v30.cards.GameCardRegistry
import dugsolutions.leaf.v30.cards.domain.CardEffect
import dugsolutions.leaf.v30.cards.domain.GameCard
import dugsolutions.leaf.v30.chronicle.GameChronicle
import dugsolutions.leaf.v30.chronicle.domain.GameEntry
import dugsolutions.leaf.v30.chronicle.domain.WarningType
import dugsolutions.leaf.v30.common.Commons
import dugsolutions.leaf.v30.common.Critter
import dugsolutions.leaf.v30.game.effect.scope.BattleDieEffectScope
import dugsolutions.leaf.v30.game.effect.scope.HandleDieEffectScope
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

class RaiseDiePlus2PerWormAndDiscardWormTest {

    @Test
    fun invoke_withHandScope_raisesDieByTwoPerWormAndDoesNotDiscardWhenFewerThanThreeWorms() {
        val chronicle = GameChronicle()
        val table = createTable()
        val card = loadCard()
        val die = TestDie(12, 2)
        val player = Player(id = 1).apply {
            addDieToHand(die)
            addCritter(Critter.WORM)
            addCritter(Critter.WORM)
            addCritter(Critter.BEE)
        }
        val groveWormsBefore = table.grove.count(Critter.WORM)

        RaiseDiePlus2PerWormAndDiscardWorm(chronicle)(
            scope = HandleDieEffectScope(player),
            grove = table.grove,
            player = player,
            card = card,
            target = ExecuteTarget.PlayerDie(player, diceOf(TestDie(12, 2)))
        )

        assertEquals(6, die.value)
        assertEquals(2, player.critters.count { it == Critter.WORM })
        assertEquals(groveWormsBefore, table.grove.count(Critter.WORM))
        val entry = assertIs<GameEntry.GameCardEffect>(chronicle.getEntries().single())
        assertEquals(card.effect, entry.effect)
        assertEquals(listOf(12 to 6), entry.dice.map { it.sides to it.value })
        assertEquals(null, entry.critter)
    }

    @Test
    fun invoke_withHandScope_whenAtLeastThreeWorms_discardsOneWormToGrove() {
        val chronicle = GameChronicle()
        val table = createTable()
        val card = loadCard()
        val die = TestDie(12, 2)
        val player = Player(id = 1).apply {
            addDieToHand(die)
            addCritter(Critter.WORM)
            addCritter(Critter.WORM)
            addCritter(Critter.WORM)
        }
        val groveWormsBefore = table.grove.count(Critter.WORM)

        RaiseDiePlus2PerWormAndDiscardWorm(chronicle)(
            scope = HandleDieEffectScope(player),
            grove = table.grove,
            player = player,
            card = card,
            target = ExecuteTarget.PlayerDie(player, diceOf(TestDie(12, 2)))
        )

        assertEquals(8, die.value)
        assertEquals(2, player.critters.count { it == Critter.WORM })
        assertEquals(groveWormsBefore + 1, table.grove.count(Critter.WORM))
        val entry = assertIs<GameEntry.GameCardEffect>(chronicle.getEntries().single())
        assertEquals(Critter.WORM, entry.critter)
        assertEquals(listOf(12 to 8), entry.dice.map { it.sides to it.value })
    }

    @Test
    fun invoke_countsBoostedWormsAndDiscardsBoostedWormWhenNoNormalWormExists() {
        val chronicle = GameChronicle()
        val table = createTable()
        val card = loadCard()
        val die = TestDie(20, 3)
        val player = Player(id = 1).apply {
            addDieToHand(die)
            addCritter(Critter.BOOSTED_WORM)
            addCritter(Critter.BOOSTED_WORM)
            addCritter(Critter.BOOSTED_WORM)
        }
        val groveWormsBefore = table.grove.count(Critter.WORM)

        RaiseDiePlus2PerWormAndDiscardWorm(chronicle)(
            scope = HandleDieEffectScope(player),
            grove = table.grove,
            player = player,
            card = card,
            target = ExecuteTarget.PlayerDie(player, diceOf(TestDie(20, 3)))
        )

        assertEquals(9, die.value)
        assertEquals(2, player.critters.count { it == Critter.BOOSTED_WORM })
        assertEquals(groveWormsBefore + 1, table.grove.count(Critter.WORM))
        val entry = assertIs<GameEntry.GameCardEffect>(chronicle.getEntries().single())
        assertEquals(Critter.BOOSTED_WORM, entry.critter)
    }

    @Test
    fun invoke_withBattleScope_raisesBattleGridDieByActingPlayersWorms() {
        val chronicle = GameChronicle()
        val table = createTable()
        val card = loadCard()
        val targetDie = TestDie(12, 2)
        val target = player(1, targetDie, TestDie(8, 1), TestDie(6, 1))
        val acting = Player(id = 9).apply {
            addCritter(Critter.WORM)
            addCritter(Critter.WORM)
        }
        table.battle.setup(
            listOf(
                target,
                player(2, TestDie(4, 1), TestDie(6, 1), TestDie(8, 1)),
                player(3, TestDie(4, 1), TestDie(6, 1), TestDie(8, 1)),
                player(4, TestDie(4, 1), TestDie(6, 1), TestDie(8, 1))
            )
        )

        RaiseDiePlus2PerWormAndDiscardWorm(chronicle)(
            scope = BattleDieEffectScope(
                battle = table.battle,
                actingPlayer = acting,
                targetPlayer = target,
                row = BattleStrikeRow.STRIKE_1
            ),
            grove = table.grove,
            player = acting,
            card = card,
            target = ExecuteTarget.PlayerDie(target, diceOf(TestDie(12, 2)))
        )

        assertEquals(6, targetDie.value)
        assertEquals(2, acting.critters.count { it == Critter.WORM })
        val entry = assertIs<GameEntry.GameCardEffect>(chronicle.getEntries().single())
        assertEquals(9, entry.playerId)
        assertEquals(listOf(12 to 6), entry.dice.map { it.sides to it.value })
    }

    @Test
    fun invoke_whenTargetIsMissing_recordsWarning() {
        val chronicle = GameChronicle()
        val table = createTable()
        val player = Player(id = 1)

        RaiseDiePlus2PerWormAndDiscardWorm(chronicle)(
            scope = HandleDieEffectScope(player),
            grove = table.grove,
            player = player,
            card = loadCard(),
            target = null
        )

        val warning = assertIs<GameEntry.Warning>(chronicle.getEntries().single())
        assertEquals(WarningType.RAISE_TARGET_MISSING, warning.type)
        assertEquals(1, warning.playerId)
    }

    @Test
    fun invoke_whenDieIsNotInScope_recordsWarning() {
        val chronicle = GameChronicle()
        val table = createTable()
        val player = Player(id = 1).apply { addDieToHand(TestDie(8, 2)) }

        RaiseDiePlus2PerWormAndDiscardWorm(chronicle)(
            scope = HandleDieEffectScope(player),
            grove = table.grove,
            player = player,
            card = loadCard(),
            target = ExecuteTarget.PlayerDie(player, diceOf(TestDie(6, 2)))
        )

        val warning = assertIs<GameEntry.Warning>(chronicle.getEntries().single())
        assertEquals(WarningType.RAISE_DIE_NOT_FOUND, warning.type)
        assertEquals(1, warning.playerId)
    }

    private fun createTable(): Table {
        val wispManager = WispCardManager(WispCardsFactory()).apply { loadCards(emptyList()) }
        val roundManager = RoundCardManager(RoundCardsFactory()).apply { loadCards(emptyList()) }
        return Table(
            grove = Grove(WispDeck(wispManager, IdentityRandomizer())),
            roundDeck = RoundDeck(roundManager, IdentityRandomizer()),
            battle = Battle(playerGridOrder = PlayerGridOrder(SequentialRandomizer()))
        )
    }

    private fun player(
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
            .copy(effect = CardEffect.RAISE_DIE_PLUS_2_PER_WORM_AND_DISCARD_WORM)
    }

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

    private class SequentialRandomizer : Randomizer {
        private var next = 1

        override fun nextBoolean(): Boolean = true

        override fun nextInt(from: Int, until: Int): Int {
            val result = next.coerceIn(from, until - 1)
            next++
            return result
        }

        override fun nextInt(until: Int): Int = nextInt(0, until)
        override fun <T> randomOrNull(list: List<T>): T? = list.firstOrNull()
        override fun <T> shuffled(list: List<T>): List<T> = list
    }
}
