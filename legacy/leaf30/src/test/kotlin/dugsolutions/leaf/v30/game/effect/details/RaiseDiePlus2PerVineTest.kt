package dugsolutions.leaf.v30.game.effect.details

import dugsolutions.leaf.v30.battle.Battle
import dugsolutions.leaf.v30.battle.domain.BattleStrikeRow
import dugsolutions.leaf.v30.cards.GameCardRegistry
import dugsolutions.leaf.v30.cards.domain.CardEffect
import dugsolutions.leaf.v30.cards.domain.CardType
import dugsolutions.leaf.v30.cards.domain.GameCard
import dugsolutions.leaf.v30.chronicle.GameChronicle
import dugsolutions.leaf.v30.chronicle.domain.GameEntry
import dugsolutions.leaf.v30.common.Commons
import dugsolutions.leaf.v30.game.effect.scope.BattleDieEffectScope
import dugsolutions.leaf.v30.game.effect.scope.HandleDieEffectScope
import dugsolutions.leaf.v30.player.Player
import dugsolutions.leaf.v30.player.decision.domain.ExecuteTarget
import dugsolutions.leaf.v30.random.die.Dice
import dugsolutions.leaf.v30.random.die.Die
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class RaiseDiePlus2PerVineTest {

    @Test
    fun invoke_withHandScope_raisesEachDieInOrderAndDumpsRemainingRaisesIntoLastDie() {
        val chronicle = GameChronicle()
        val card = loadCard()
        val vine = loadCard().copy(type = CardType.VINE)
        val d8 = TestDie(8, 1)
        val d12 = TestDie(12, 1)
        val player = Player(id = 1).apply {
            addCardToCreature(vine)
            addCardToCreature(vine.copy(name = "Vine_07_99"))
            addCardToCreature(vine.copy(name = "Vine_09_99"))
            addDieToHand(d8)
            addDieToHand(d12)
        }

        RaiseDiePlus2PerVine(chronicle)(
            scope = HandleDieEffectScope(player),
            player = player,
            card = card,
            target = ExecuteTarget(dice = diceOf(TestDie(8, 1), TestDie(12, 1)))
        )

        assertEquals(3, d8.value)
        assertEquals(5, d12.value)
        val entry = assertIs<GameEntry.GameCardEffect>(chronicle.getEntries().single())
        assertEquals(CardEffect.RAISE_DIE_PLUS_2_PER_VINE, entry.effect)
        assertEquals(listOf(8 to 3, 12 to 5), entry.dice.map { it.sides to it.value })
    }

    @Test
    fun invoke_withBattleScope_raisesTargetDiceOnCorrespondingRows() {
        val chronicle = GameChronicle()
        val card = loadCard()
        val vine = loadCard().copy(type = CardType.VINE)
        val d10 = TestDie(10, 3)
        val d8 = TestDie(8, 2)
        val player = playerWithDice(1, d10, d8, TestDie(6, 1)).apply {
            addCardToCreature(vine)
            addCardToCreature(vine.copy(name = "Vine_07_99"))
        }
        val battle = setupBattle(player)

        RaiseDiePlus2PerVine(chronicle)(
            scope = BattleDieEffectScope(
                battle = battle,
                actingPlayer = player,
                targetPlayer = player,
                rows = listOf(BattleStrikeRow.STRIKE_1, BattleStrikeRow.STRIKE_2)
            ),
            player = player,
            card = card,
            target = ExecuteTarget(dice = diceOf(TestDie(10, 3), TestDie(8, 2)))
        )

        assertEquals(5, d10.value)
        assertEquals(4, d8.value)
        val entry = assertIs<GameEntry.GameCardEffect>(chronicle.getEntries().single())
        assertEquals(CardEffect.RAISE_DIE_PLUS_2_PER_VINE, entry.effect)
        assertEquals(listOf(8 to 4, 10 to 5), entry.dice.map { it.sides to it.value })
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
            .copy(effect = CardEffect.RAISE_DIE_PLUS_2_PER_VINE)
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
}
