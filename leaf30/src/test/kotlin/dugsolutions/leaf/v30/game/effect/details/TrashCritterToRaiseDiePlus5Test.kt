package dugsolutions.leaf.v30.game.effect.details

import dugsolutions.leaf.v30.battle.Battle
import dugsolutions.leaf.v30.battle.domain.BattleStrikeRow
import dugsolutions.leaf.v30.cards.GameCardRegistry
import dugsolutions.leaf.v30.cards.domain.CardEffect
import dugsolutions.leaf.v30.cards.domain.GameCard
import dugsolutions.leaf.v30.chronicle.GameChronicle
import dugsolutions.leaf.v30.chronicle.domain.GameEntry
import dugsolutions.leaf.v30.common.Commons
import dugsolutions.leaf.v30.common.Critter
import dugsolutions.leaf.v30.game.effect.scope.BattleDieEffectScope
import dugsolutions.leaf.v30.game.effect.scope.HandleDieEffectScope
import dugsolutions.leaf.v30.player.Player
import dugsolutions.leaf.v30.player.decision.domain.ExecuteTarget
import dugsolutions.leaf.v30.random.die.Dice
import dugsolutions.leaf.v30.random.die.Die
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class TrashCritterToRaiseDiePlus5Test {

    @Test
    fun invoke_withHandScope_removesCritterAndRaisesDieByFive() {
        val chronicle = GameChronicle()
        val card = loadCard()
        val die = TestDie(12, 4)
        val player = Player(id = 1).apply {
            addCritter(Critter.WORM)
            addDieToHand(die)
        }

        TrashCritterToRaiseDiePlus5(chronicle)(
            scope = HandleDieEffectScope(player),
            player = player,
            card = card,
            target = ExecuteTarget(
                dice = diceOf(TestDie(12, 4)),
                critter = listOf(Critter.WORM)
            )
        )

        assertEquals(9, die.value)
        assertEquals(emptyList(), player.critters)
        val entry = assertIs<GameEntry.GameCardEffect>(chronicle.getEntries().single())
        assertEquals(CardEffect.TRASH_CRITTER_TO_RAISE_DIE_PLUS_5, entry.effect)
        assertEquals(Critter.WORM, entry.critter)
        assertEquals(listOf(12 to 9), entry.dice.map { it.sides to it.value })
    }

    @Test
    fun invoke_withBattleScope_removesCritterAndRaisesBattleDieByFive() {
        val chronicle = GameChronicle()
        val card = loadCard()
        val die = TestDie(10, 3)
        val player = playerWithDice(1, die, TestDie(8, 2), TestDie(6, 1)).apply {
            addCritter(Critter.BEE)
        }
        val battle = setupBattle(player)

        TrashCritterToRaiseDiePlus5(chronicle)(
            scope = BattleDieEffectScope(
                battle = battle,
                actingPlayer = player,
                targetPlayer = player,
                row = BattleStrikeRow.STRIKE_1
            ),
            player = player,
            card = card,
            target = ExecuteTarget(
                dice = diceOf(TestDie(10, 3)),
                critter = listOf(Critter.BEE)
            )
        )

        assertEquals(8, die.value)
        assertEquals(emptyList(), player.critters)
        val entry = assertIs<GameEntry.GameCardEffect>(chronicle.getEntries().single())
        assertEquals(CardEffect.TRASH_CRITTER_TO_RAISE_DIE_PLUS_5, entry.effect)
        assertEquals(Critter.BEE, entry.critter)
        assertEquals(listOf(10 to 8), entry.dice.map { it.sides to it.value })
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
            .copy(effect = CardEffect.TRASH_CRITTER_TO_RAISE_DIE_PLUS_5)
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
