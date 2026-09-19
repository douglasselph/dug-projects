package dugsolutions.leaf.v30.game.effect

import dugsolutions.leaf.v30.battle.Battle
import dugsolutions.leaf.v30.battle.domain.BattleStrikeRow
import dugsolutions.leaf.v30.cards.GameCardRegistry
import dugsolutions.leaf.v30.cards.domain.CardEffect
import dugsolutions.leaf.v30.cards.domain.GameCard
import dugsolutions.leaf.v30.chronicle.GameChronicle
import dugsolutions.leaf.v30.chronicle.domain.GameEntry
import dugsolutions.leaf.v30.common.Commons
import dugsolutions.leaf.v30.grove.Grove
import dugsolutions.leaf.v30.player.Player
import dugsolutions.leaf.v30.player.decision.domain.ActionBattleMain
import dugsolutions.leaf.v30.player.decision.domain.ActionCultivation
import dugsolutions.leaf.v30.player.domain.CreatureCard
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

class WoundWinnerOfStrikeRowTest {

    @Test
    fun battle_woundsWinnerOfTargetStrikeRowAndChroniclesEffect() {
        val chronicle = GameChronicle()
        val table = createTable()
        val card = loadCard()
        val winnerCard = loadGameCard()
        val winner = playerWithDice(1, TestDie(8, 8), TestDie(6, 3), TestDie(4, 1), chronicle).apply {
            addCardToCreature(CreatureCard(winnerCard, CreatureCard.Facing.FACE_UP))
        }
        val loser2 = playerWithDice(2, TestDie(8, 2), TestDie(6, 1), TestDie(4, 1), chronicle)
        val loser3 = playerWithDice(3, TestDie(8, 2), TestDie(6, 1), TestDie(4, 1), chronicle)
        val loser4 = playerWithDice(4, TestDie(8, 2), TestDie(6, 1), TestDie(4, 1), chronicle)
        setupPlayersAndBattle(table, listOf(winner, loser2, loser3, loser4))
        val executor = GameCardEffectExecutorBattle(chronicle)

        executor(
            table = table,
            player = loser2,
            action = ActionBattleMain.ExecuteCard(
                card = card,
                rows = listOf(BattleStrikeRow.STRIKE_1)
            )
        )

        assertEquals(true, winner.creatureCards.single().isFaceDown)
        val entries = chronicle.getEntries()
        val woundEntry = assertIs<GameEntry.WoundCard>(entries[0])
        assertEquals(winner.id, woundEntry.playerId)
        assertEquals(true, woundEntry.wasFlipped)
        val effectEntry = assertIs<GameEntry.GameCardEffect>(entries[1])
        assertEquals(loser2.id, effectEntry.playerId)
        assertEquals(CardEffect.WOUND_WINNER_OF_STRIKE_ROW, effectEntry.effect)
        assertEquals("Wounded 1 winner(s) on STRIKE_1", effectEntry.detail)
    }

    @Test
    fun battle_whenTargetRowHasMultipleWinners_woundsEachWinner() {
        val chronicle = GameChronicle()
        val table = createTable()
        val card = loadCard()
        val winnerCard = loadGameCard()
        val winner1 = playerWithDice(1, TestDie(8, 8), TestDie(6, 3), TestDie(4, 1), chronicle).apply {
            addCardToCreature(CreatureCard(winnerCard, CreatureCard.Facing.FACE_UP))
        }
        val winner2 = playerWithDice(2, TestDie(10, 8), TestDie(6, 3), TestDie(4, 1), chronicle).apply {
            addCardToCreature(CreatureCard(winnerCard, CreatureCard.Facing.FACE_UP))
        }
        val loser3 = playerWithDice(3, TestDie(8, 2), TestDie(6, 1), TestDie(4, 1), chronicle)
        val loser4 = playerWithDice(4, TestDie(8, 2), TestDie(6, 1), TestDie(4, 1), chronicle)
        setupPlayersAndBattle(table, listOf(winner1, winner2, loser3, loser4))
        val executor = GameCardEffectExecutorBattle(chronicle)

        executor(
            table = table,
            player = loser3,
            action = ActionBattleMain.ExecuteCard(
                card = card,
                rows = listOf(BattleStrikeRow.STRIKE_1)
            )
        )

        assertEquals(true, winner1.creatureCards.single().isFaceDown)
        assertEquals(true, winner2.creatureCards.single().isFaceDown)
        val woundEntries = chronicle.getEntries().filterIsInstance<GameEntry.WoundCard>()
        assertEquals(listOf(1, 2), woundEntries.map { it.playerId })
        val effectEntry = chronicle.getEntries().filterIsInstance<GameEntry.GameCardEffect>().single()
        assertEquals("Wounded 2 winner(s) on STRIKE_1", effectEntry.detail)
    }

    @Test
    fun cultivation_doesNothingForWoundWinnerEffect() {
        val chronicle = GameChronicle()
        val table = createTable()
        val card = loadCard()
        val player = Player(chronicle = chronicle, id = 1)
        val executor = GameCardEffectExecutorCultivation(chronicle)

        executor(
            table = table,
            player = player,
            action = ActionCultivation.ExecuteCard(card = card)
        )

        assertEquals(emptyList(), chronicle.getEntries())
    }

    private fun setupPlayersAndBattle(
        table: Table,
        players: List<Player>
    ) {
        players.forEach { table.add(it) }
        table.battle.setup(players)
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
        return loadGameCard().copy(effect = CardEffect.WOUND_WINNER_OF_STRIKE_ROW)
    }

    private fun loadGameCard(): GameCard {
        return GameCardRegistry()
            .apply { loadFromCsv(Commons.CARD_LIST) }
            .getAllCards()
            .first()
    }

    private fun playerWithDice(
        id: Int,
        die1: Die,
        die2: Die,
        die3: Die,
        chronicle: GameChronicle
    ): Player {
        return Player(chronicle = chronicle, id = id).apply {
            addDieToHand(die1)
            addDieToHand(die2)
            addDieToHand(die3)
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
