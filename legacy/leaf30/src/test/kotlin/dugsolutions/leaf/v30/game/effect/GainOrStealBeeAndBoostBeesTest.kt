package dugsolutions.leaf.v30.game.effect

import dugsolutions.leaf.v30.battle.Battle
import dugsolutions.leaf.v30.battle.domain.BattleItem
import dugsolutions.leaf.v30.battle.domain.BattleStrikeRow
import dugsolutions.leaf.v30.cards.GameCardRegistry
import dugsolutions.leaf.v30.cards.domain.CardEffect
import dugsolutions.leaf.v30.cards.domain.GameCard
import dugsolutions.leaf.v30.chronicle.GameChronicle
import dugsolutions.leaf.v30.chronicle.domain.GameEntry
import dugsolutions.leaf.v30.common.Commons
import dugsolutions.leaf.v30.common.Critter
import dugsolutions.leaf.v30.grove.Grove
import dugsolutions.leaf.v30.player.Player
import dugsolutions.leaf.v30.player.decision.domain.ActionBattleMain
import dugsolutions.leaf.v30.player.decision.domain.ActionCultivation
import dugsolutions.leaf.v30.player.decision.domain.ExecuteTarget
import dugsolutions.leaf.v30.random.Randomizer
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

class GainOrStealBeeAndBoostBeesTest {

    @Test
    fun cultivation_whenTargetPlayerHasBee_stealsBeeAndBoostsPlayersBees() {
        val chronicle = GameChronicle()
        val table = createTable()
        val card = loadCard()
        val player = Player(id = 1)
        val target = Player(id = 2).apply {
            addCritter(Critter.BEE)
            addCritter(Critter.WORM)
        }
        val executor = GameCardEffectExecutorCultivation(chronicle)

        executor(
            table = table,
            player = player,
            action = ActionCultivation.ExecuteCard(
                card = card,
                target = ExecuteTarget(player = target)
            )
        )

        assertEquals(emptyList(), player.critters.filter { it == Critter.BEE })
        assertEquals(1, player.critters.count { it == Critter.BOOSTED_BEE })
        assertEquals(0, target.critters.count { it == Critter.BEE })
        assertEquals(1, target.critters.count { it == Critter.WORM })
        val entry = assertIs<GameEntry.GameCardEffect>(chronicle.getEntries().single())
        assertEquals(CardEffect.GAIN_OR_STEAL_BEE_AND_BOOST_BEES, entry.effect)
        assertEquals(Critter.BOOSTED_BEE, entry.critter)
    }

    @Test
    fun cultivation_whenNoTarget_gainsBeeFromGroveAndBoostsIt() {
        val chronicle = GameChronicle()
        val table = createTable()
        val card = loadCard()
        val player = Player(id = 1)
        val groveBeesBefore = table.grove.count(Critter.BEE)
        val executor = GameCardEffectExecutorCultivation(chronicle)

        executor(
            table = table,
            player = player,
            action = ActionCultivation.ExecuteCard(card = card)
        )

        assertEquals(1, player.critters.count { it == Critter.BOOSTED_BEE })
        assertEquals(groveBeesBefore - 1, table.grove.count(Critter.BEE))
        val entry = assertIs<GameEntry.GameCardEffect>(chronicle.getEntries().single())
        assertEquals(Critter.BOOSTED_BEE, entry.critter)
        assertEquals(
            "Gained a bee from the Grove and boosted this player's bees for the round",
            entry.detail
        )
    }

    @Test
    fun cultivation_whenTargetHasNullPlayer_gainsBeeFromGroveAndBoostsIt() {
        val chronicle = GameChronicle()
        val table = createTable()
        val card = loadCard()
        val player = Player(id = 1)
        val groveBeesBefore = table.grove.count(Critter.BEE)
        val executor = GameCardEffectExecutorCultivation(chronicle)

        executor(
            table = table,
            player = player,
            action = ActionCultivation.ExecuteCard(
                card = card,
                target = ExecuteTarget()
            )
        )

        assertEquals(1, player.critters.count { it == Critter.BOOSTED_BEE })
        assertEquals(groveBeesBefore - 1, table.grove.count(Critter.BEE))
        val entry = assertIs<GameEntry.GameCardEffect>(chronicle.getEntries().single())
        assertEquals(Critter.BOOSTED_BEE, entry.critter)
        assertEquals(
            "Gained a bee from the Grove and boosted this player's bees for the round",
            entry.detail
        )
    }

    @Test
    fun battle_boostsPlayerBeesInSupplyAndOnBattleGrid() {
        val chronicle = GameChronicle()
        val table = createTable()
        val card = loadCard()
        val player = playerWithDice(1, TestDie(8, 6), TestDie(6, 3), TestDie(4, 1)).apply {
            addCritter(Critter.BEE)
        }
        val source = Player(id = 9).apply { addCritter(Critter.BEE) }
        table.battle.setup(
            listOf(
                player,
                playerWithDice(2, TestDie(4, 1), TestDie(6, 1), TestDie(8, 1)),
                playerWithDice(3, TestDie(4, 1), TestDie(6, 1), TestDie(8, 1)),
                playerWithDice(4, TestDie(4, 1), TestDie(6, 1), TestDie(8, 1))
            )
        )
        table.battle.add(player, BattleStrikeRow.STRIKE_1, Critter.BEE)
        val executor = GameCardEffectExecutorBattle(chronicle)

        executor(
            table = table,
            player = player,
            action = ActionBattleMain.ExecuteCard(
                card = card,
                target = ExecuteTarget(player = source),
                rows = listOf(BattleStrikeRow.STRIKE_1)
            )
        )

        assertEquals(2, player.critters.count { it == Critter.BOOSTED_BEE })
        assertEquals(0, source.critters.count { it == Critter.BEE })
        val gridCritter = table.battle.grid.getSquare(player.id, BattleStrikeRow.STRIKE_1).all
            .filterIsInstance<BattleItem.CritterItem>()
            .single()
            .critter
        assertEquals(Critter.BOOSTED_BEE, gridCritter)
        val entry = assertIs<GameEntry.GameCardEffect>(chronicle.getEntries().single())
        assertEquals(Critter.BOOSTED_BEE, entry.critter)
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

    private fun loadCard(): GameCard {
        return GameCardRegistry()
            .apply { loadFromCsv(Commons.CARD_LIST) }
            .getAllCards()
            .first()
            .copy(effect = CardEffect.GAIN_OR_STEAL_BEE_AND_BOOST_BEES)
    }

    private fun playerWithDice(
        id: Int,
        vararg dice: Die
    ): Player {
        return Player(id = id).apply {
            dice.forEach { addDieToHand(it) }
        }
    }

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
