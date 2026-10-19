package dugsolutions.leaf.v30.game.effect.details

import dugsolutions.leaf.v30.battle.Battle
import dugsolutions.leaf.v30.battle.domain.BattleStrikeRow
import dugsolutions.leaf.v30.cards.GameCardRegistry
import dugsolutions.leaf.v30.cards.domain.CardEffect
import dugsolutions.leaf.v30.cards.domain.GameCard
import dugsolutions.leaf.v30.chronicle.GameChronicle
import dugsolutions.leaf.v30.chronicle.domain.GameEntry
import dugsolutions.leaf.v30.common.Commons
import dugsolutions.leaf.v30.game.effect.scope.BattleDieEffectScope
import dugsolutions.leaf.v30.game.effect.scope.HandleDieEffectScope
import dugsolutions.leaf.v30.player.Player
import dugsolutions.leaf.v30.random.die.Die
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class RollExtraForEachMaxDieTest {
    private companion object {
        const val PLAYER_ID = 1
    }

    @Test
    fun invoke_withHandScope_rerollsAndBoostsEachMaxDie() {
        val chronicle = GameChronicle()
        val maxD6 = TestDie(6, 6, rollValue = 2)
        val maxD8 = TestDie(8, 8, rollValue = 5)
        val notMax = TestDie(10, 9, rollValue = 1)
        val player = Player(id = PLAYER_ID).apply {
            addDieToHand(maxD6)
            addDieToHand(maxD8)
            addDieToHand(notMax)
        }

        RollExtraForEachMaxDie(chronicle)(
            scopes = listOf(HandleDieEffectScope(player)),
            card = loadCard()
        )

        assertEquals(8, maxD6.value)
        assertEquals(13, maxD8.value)
        assertEquals(9, notMax.value)
        assertEquals(1, maxD6.rollCount)
        assertEquals(1, maxD8.rollCount)
        assertEquals(0, notMax.rollCount)
        val entry = assertIs<GameEntry.GameCardEffect>(chronicle.getEntries().single())
        assertEquals(CardEffect.ROLL_EXTRA_FOR_EACH_MAX_DIE, entry.effect)
        assertEquals("Rolled extra for 2 max die/dice", entry.detail)
        assertEquals(listOf(6 to 8, 8 to 13), entry.dice.map { it.sides to it.value }.sortedBy { it.first })
    }

    @Test
    fun invoke_withBattleScopes_rerollsAndBoostsMaxDiceAcrossAllRows() {
        val chronicle = GameChronicle()
        val maxD12 = TestDie(12, 12, rollValue = 4)
        val maxD8 = TestDie(8, 8, rollValue = 3)
        val notMax = TestDie(6, 5, rollValue = 1)
        val player = playerWithDice(PLAYER_ID, maxD12, maxD8, notMax)
        val battle = setupBattle(player)

        RollExtraForEachMaxDie(chronicle)(
            scopes = BattleStrikeRow.entries.map { row ->
                BattleDieEffectScope(
                    battle = battle,
                    actingPlayer = player,
                    targetPlayer = player,
                    row = row
                )
            },
            card = loadCard()
        )

        assertEquals(16, maxD12.value)
        assertEquals(11, maxD8.value)
        assertEquals(5, notMax.value)
        assertEquals(1, maxD12.rollCount)
        assertEquals(1, maxD8.rollCount)
        assertEquals(0, notMax.rollCount)
        val entry = assertIs<GameEntry.GameCardEffect>(chronicle.getEntries().single())
        assertEquals(listOf(8 to 11, 12 to 16), entry.dice.map { it.sides to it.value }.sortedBy { it.first })
    }

    @Test
    fun invoke_whenNoDiceAreMax_chroniclesEmptyResult() {
        val chronicle = GameChronicle()
        val player = Player(id = PLAYER_ID).apply {
            addDieToHand(TestDie(6, 5, rollValue = 1))
        }

        RollExtraForEachMaxDie(chronicle)(
            scopes = listOf(HandleDieEffectScope(player)),
            card = loadCard()
        )

        val entry = assertIs<GameEntry.GameCardEffect>(chronicle.getEntries().single())
        assertEquals("Rolled extra for 0 max die/dice", entry.detail)
        assertEquals(emptyList(), entry.dice)
    }

    private fun setupBattle(player: Player): Battle {
        return Battle().apply {
            setup(
                listOf(
                    player,
                    playerWithDice(2, TestDie(4, 1), TestDie(6, 1), TestDie(8, 1)),
                    playerWithDice(3, TestDie(4, 1), TestDie(6, 1), TestDie(8, 1)),
                    playerWithDice(4, TestDie(4, 1), TestDie(6, 1), TestDie(8, 1))
                )
            )
        }
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
            .copy(effect = CardEffect.ROLL_EXTRA_FOR_EACH_MAX_DIE)
    }

    private class TestDie(
        sides: Int,
        value: Int,
        private val rollValue: Int = 1
    ) : Die(sides) {
        var rollCount = 0

        init {
            adjustTo(value)
        }

        override fun roll(): Die {
            rollCount++
            adjustTo(rollValue)
            return this
        }
    }
}
