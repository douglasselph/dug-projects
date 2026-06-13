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
import dugsolutions.leaf.v30.game.domain.MainActionException
import dugsolutions.leaf.v30.grove.Grove
import dugsolutions.leaf.v30.player.Player
import dugsolutions.leaf.v30.player.decision.domain.ExecuteTarget
import dugsolutions.leaf.v30.player.decision.domain.ActionBattleMain
import dugsolutions.leaf.v30.player.decision.domain.ActionCultivation
import dugsolutions.leaf.v30.random.Randomizer
import dugsolutions.leaf.v30.random.die.Dice
import dugsolutions.leaf.v30.random.die.Die
import dugsolutions.leaf.v30.random.die.DieSides
import dugsolutions.leaf.v30.random.die.di.DieFactory
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
    fun cultivationInvoke_whenCardEffectIsUnknown_recordsChronicleWarning() {
        val chronicle = GameChronicle()
        val executor = GameCardEffectExecutorCultivation(chronicle)
        val table = createTable(numBattle = 0, numCultivation = 1)
        val player = Player(id = 7)
        val card = loadGameCard().copy(effect = CardEffect.UNKNOWN)

        executor(table, player, ActionCultivation.ExecuteCard(card))

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

        executor(table, player, ActionCultivation.ExecuteCard(card))

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
        val action = ActionBattleMain.ExecuteCard(
            card = loadGameCard().copy(effect = CardEffect.PLACE_BULWARK_TOKEN),
            target = ExecuteTarget(player = target, dice = diceOf(targetDie))
        )

        executor(table, Player(id = 9), action)

        assertEquals(
            BattleItem.BulwarkToken,
            table.battle.grid.getSquare(target.id, BattleStrikeRow.STRIKE_1).all[1]
        )
    }

    @Test
    fun cultivationInvoke_whenRerollDieUntilThreeOrHigher_rerollsHandDieUntilValueIsAtLeastThree() {
        val chronicle = GameChronicle()
        val executor = GameCardEffectExecutorCultivation(chronicle)
        val table = createTable(numBattle = 0, numCultivation = 1)
        val die = SequenceDie(6, initial = 1, rolls = listOf(1, 2, 3))
        val player = Player(id = 7).apply {
            addDieToHand(die)
        }
        val card = loadGameCard().copy(effect = CardEffect.REROLL_DIE_UNTIL_THREE_OR_HIGHER)
        val action = ActionCultivation.ExecuteCard(
            card = card,
            target = ExecuteTarget(player = player, dice = diceOf(FixedDie(6, 1)))
        )

        executor(table, player, action)

        assertEquals(3, die.rollCount)
        assertEquals(3, die.value)
        val entry = assertIs<GameEntry.GameCardEffect>(chronicle.getEntries().single())
        assertEquals(card.effect, entry.effect)
        assertEquals(3, entry.dice.firstOrNull()?.value)
    }

    @Test
    fun cultivationInvoke_whenRerollDieUntilThreeOrHigherHasNoTarget_recordsWarning() {
        val chronicle = GameChronicle()
        val executor = GameCardEffectExecutorCultivation(chronicle)
        val table = createTable(numBattle = 0, numCultivation = 1)
        val player = Player(id = 7)
        val card = loadGameCard().copy(effect = CardEffect.REROLL_DIE_UNTIL_THREE_OR_HIGHER)

        executor(table, player, ActionCultivation.ExecuteCard(card))

        val entry = assertIs<GameEntry.Warning>(chronicle.getEntries().single())
        assertEquals(WarningType.REROLL_TARGET_MISSING, entry.type)
        assertEquals(card.id, entry.cardId)
    }

    @Test
    fun cultivationInvoke_whenRerollDieUntilThreeOrHigherDieIsNotInHand_recordsWarning() {
        val chronicle = GameChronicle()
        val executor = GameCardEffectExecutorCultivation(chronicle)
        val table = createTable(numBattle = 0, numCultivation = 1)
        val player = Player(id = 7).apply {
            addDieToHand(FixedDie(6, 1))
        }
        val card = loadGameCard().copy(effect = CardEffect.REROLL_DIE_UNTIL_THREE_OR_HIGHER)
        val action = ActionCultivation.ExecuteCard(
            card = card,
            target = ExecuteTarget(player = player, dice = diceOf(FixedDie(8, 1)))
        )

        executor(table, player, action)

        val entry = assertIs<GameEntry.Warning>(chronicle.getEntries().single())
        assertEquals(WarningType.REROLL_DIE_NOT_FOUND, entry.type)
        assertEquals(card.id, entry.cardId)
    }

    @Test
    fun cultivationInvoke_whenRerollDieUntilThreeOrHigherNeverReachesThree_throwsException() {
        val executor = GameCardEffectExecutorCultivation()
        val table = createTable(numBattle = 0, numCultivation = 1)
        val die = SequenceDie(6, initial = 1, rolls = List(11) { 1 })
        val player = Player(id = 7).apply {
            addDieToHand(die)
        }
        val action = ActionCultivation.ExecuteCard(
            card = loadGameCard().copy(effect = CardEffect.REROLL_DIE_UNTIL_THREE_OR_HIGHER),
            target = ExecuteTarget(player = player, dice = diceOf(FixedDie(6, 1)))
        )

        assertThrows<MainActionException> {
            executor(table, player, action)
        }
        assertEquals(10, die.rollCount)
    }

    @Test
    fun battleInvoke_whenRerollDieUntilThreeOrHigher_rerollsBattleGridDieUntilValueIsAtLeastThree() {
        val chronicle = GameChronicle()
        val executor = GameCardEffectExecutorBattle(chronicle)
        val table = createTable(numBattle = 1, numCultivation = 0)
        val targetDie = SequenceDie(8, initial = 6, rolls = listOf(1, 2, 4))
        val target = playerWithDice(1, targetDie, FixedDie(6, 3), FixedDie(10, 1))
        table.battle.setup(
            listOf(
                target,
                playerWithDice(2, FixedDie(4, 1), FixedDie(6, 1), FixedDie(8, 1)),
                playerWithDice(3, FixedDie(4, 1), FixedDie(6, 1), FixedDie(8, 1)),
                playerWithDice(4, FixedDie(4, 1), FixedDie(6, 1), FixedDie(8, 1))
            )
        )
        val card = loadGameCard().copy(effect = CardEffect.REROLL_DIE_UNTIL_THREE_OR_HIGHER)
        val action = ActionBattleMain.ExecuteCard(
            card = card,
            target = ExecuteTarget(player = target, dice = diceOf(FixedDie(8, 6))),
            rows = listOf(BattleStrikeRow.STRIKE_1)
        )

        executor(table, Player(id = 9), action)

        assertEquals(3, targetDie.rollCount)
        assertEquals(4, targetDie.value)
        val entry = assertIs<GameEntry.GameCardEffect>(chronicle.getEntries().single())
        assertEquals(card.effect, entry.effect)
        assertEquals(4, entry.dice.firstOrNull()?.value)
    }

    @Test
    fun battleInvoke_whenRerollDieUntilThreeOrHigherHasNoRow_throwsException() {
        val executor = GameCardEffectExecutorBattle()
        val table = createTable(numBattle = 1, numCultivation = 0)
        val player = Player(id = 7)
        val action = ActionBattleMain.ExecuteCard(
            card = loadGameCard().copy(effect = CardEffect.REROLL_DIE_UNTIL_THREE_OR_HIGHER),
            target = ExecuteTarget(player = player, dice = diceOf(FixedDie(6, 1)))
        )

        assertThrows<MainActionException> {
            executor(table, player, action)
        }
    }

    @Test
    fun cultivationInvoke_whenRaiseDiePlus1AndGainWater_raisesHandDieAndGainsWater() {
        val chronicle = GameChronicle()
        val executor = GameCardEffectExecutorCultivation(chronicle)
        val table = createTable(numBattle = 0, numCultivation = 1)
        val die = FixedDie(6, 5)
        val player = Player(id = 7).apply {
            addDieToHand(die)
        }
        val card = loadGameCard().copy(effect = CardEffect.RAISE_DIE_PLUS_1_AND_GAIN_WATER)
        val action = ActionCultivation.ExecuteCard(
            card = card,
            target = ExecuteTarget(player = player, dice = diceOf(FixedDie(6, 5)))
        )

        executor(table, player, action)

        assertEquals(6, die.value)
        assertEquals(1, player.waterTokenCount)
        assertEquals(7, table.grove.count(Token.WATER))
        val entries = chronicle.getEntries().filterIsInstance<GameEntry.GameCardEffect>()
        assertEquals(2, entries.size)
        assertEquals(6, entries[0].dice.firstOrNull()?.value)
        assertEquals(Token.WATER, entries[1].token)
    }

    @Test
    fun cultivationInvoke_whenRaiseDiePlus1AndGainWaterHasNoTarget_recordsWarning() {
        val chronicle = GameChronicle()
        val executor = GameCardEffectExecutorCultivation(chronicle)
        val table = createTable(numBattle = 0, numCultivation = 1)
        val player = Player(id = 7)
        val card = loadGameCard().copy(effect = CardEffect.RAISE_DIE_PLUS_1_AND_GAIN_WATER)

        executor(table, player, ActionCultivation.ExecuteCard(card))

        val entry = assertIs<GameEntry.Warning>(chronicle.getEntries().single())
        assertEquals(WarningType.RAISE_TARGET_MISSING, entry.type)
        assertEquals(card.id, entry.cardId)
    }

    @Test
    fun cultivationInvoke_whenRaiseDiePlus1AndGainWaterDieIsNotInHand_recordsWarning() {
        val chronicle = GameChronicle()
        val executor = GameCardEffectExecutorCultivation(chronicle)
        val table = createTable(numBattle = 0, numCultivation = 1)
        val player = Player(id = 7).apply {
            addDieToHand(FixedDie(6, 5))
        }
        val card = loadGameCard().copy(effect = CardEffect.RAISE_DIE_PLUS_1_AND_GAIN_WATER)
        val action = ActionCultivation.ExecuteCard(
            card = card,
            target = ExecuteTarget(player = player, dice = diceOf(FixedDie(8, 5)))
        )

        executor(table, player, action)

        val entry = assertIs<GameEntry.Warning>(chronicle.getEntries().single())
        assertEquals(WarningType.RAISE_DIE_NOT_FOUND, entry.type)
        assertEquals(0, player.waterTokenCount)
        assertEquals(8, table.grove.count(Token.WATER))
    }

    @Test
    fun battleInvoke_whenRaiseDiePlus1AndGainWater_raisesBattleGridDieAndGainsWater() {
        val chronicle = GameChronicle()
        val executor = GameCardEffectExecutorBattle(chronicle)
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
        val card = loadGameCard().copy(effect = CardEffect.RAISE_DIE_PLUS_1_AND_GAIN_WATER)
        val action = ActionBattleMain.ExecuteCard(
            card = card,
            target = ExecuteTarget(player = target, dice = diceOf(FixedDie(8, 6))),
            rows = listOf(BattleStrikeRow.STRIKE_1)
        )
        val player = Player(id = 9)

        executor(table, player, action)

        assertEquals(7, targetDie.value)
        assertEquals(1, player.waterTokenCount)
        assertEquals(7, table.grove.count(Token.WATER))
        val entries = chronicle.getEntries().filterIsInstance<GameEntry.GameCardEffect>()
        assertEquals(2, entries.size)
        assertEquals(7, entries[0].dice.firstOrNull()?.value)
        assertEquals(Token.WATER, entries[1].token)
    }

    @Test
    fun battleInvoke_whenRaiseDiePlus1AndGainWaterHasNoRow_throwsException() {
        val executor = GameCardEffectExecutorBattle()
        val table = createTable(numBattle = 1, numCultivation = 0)
        val player = Player(id = 7)
        val action = ActionBattleMain.ExecuteCard(
            card = loadGameCard().copy(effect = CardEffect.RAISE_DIE_PLUS_1_AND_GAIN_WATER),
            target = ExecuteTarget(player = player, dice = diceOf(FixedDie(6, 1)))
        )

        assertThrows<MainActionException> {
            executor(table, player, action)
        }
    }

    @Test
    fun cultivationInvoke_whenDoubleOneDie_doublesHandDie() {
        val chronicle = GameChronicle()
        val executor = GameCardEffectExecutorCultivation(chronicle)
        val table = createTable(numBattle = 0, numCultivation = 1)
        val die = FixedDie(6, 3)
        val player = Player(id = 7).apply {
            addDieToHand(die)
        }
        val card = loadGameCard().copy(effect = CardEffect.DOUBLE_ONE_DIE)
        val action = ActionCultivation.ExecuteCard(
            card = card,
            target = ExecuteTarget(player = player, dice = diceOf(FixedDie(6, 3)))
        )

        executor(table, player, action)

        assertEquals(6, die.value)
        val entry = assertIs<GameEntry.GameCardEffect>(chronicle.getEntries().single())
        assertEquals(card.effect, entry.effect)
        assertEquals(6, entry.dice.single().value)
    }

    @Test
    fun cultivationInvoke_whenDoubleOneDieHasNoTarget_recordsWarning() {
        val chronicle = GameChronicle()
        val executor = GameCardEffectExecutorCultivation(chronicle)
        val table = createTable(numBattle = 0, numCultivation = 1)
        val player = Player(id = 7)
        val card = loadGameCard().copy(effect = CardEffect.DOUBLE_ONE_DIE)

        executor(table, player, ActionCultivation.ExecuteCard(card))

        val entry = assertIs<GameEntry.Warning>(chronicle.getEntries().single())
        assertEquals(WarningType.RAISE_TARGET_MISSING, entry.type)
        assertEquals(card.id, entry.cardId)
    }

    @Test
    fun battleInvoke_whenDoubleOneDie_doublesBattleGridDie() {
        val chronicle = GameChronicle()
        val executor = GameCardEffectExecutorBattle(chronicle)
        val table = createTable(numBattle = 1, numCultivation = 0)
        val targetDie = FixedDie(8, 4)
        val target = playerWithDice(1, targetDie, FixedDie(6, 3), FixedDie(10, 1))
        table.battle.setup(
            listOf(
                target,
                playerWithDice(2, FixedDie(4, 1), FixedDie(6, 1), FixedDie(8, 1)),
                playerWithDice(3, FixedDie(4, 1), FixedDie(6, 1), FixedDie(8, 1)),
                playerWithDice(4, FixedDie(4, 1), FixedDie(6, 1), FixedDie(8, 1))
            )
        )
        val card = loadGameCard().copy(effect = CardEffect.DOUBLE_ONE_DIE)
        val action = ActionBattleMain.ExecuteCard(
            card = card,
            target = ExecuteTarget(player = target, dice = diceOf(FixedDie(8, 4))),
            rows = listOf(BattleStrikeRow.STRIKE_1)
        )
        val player = Player(id = 9)

        executor(table, player, action)

        assertEquals(8, targetDie.value)
        val entry = assertIs<GameEntry.GameCardEffect>(chronicle.getEntries().single())
        assertEquals(card.effect, entry.effect)
        assertEquals(8, entry.dice.single().value)
    }

    @Test
    fun battleInvoke_whenDoubleOneDieHasNoRow_throwsException() {
        val executor = GameCardEffectExecutorBattle()
        val table = createTable(numBattle = 1, numCultivation = 0)
        val player = Player(id = 7)
        val action = ActionBattleMain.ExecuteCard(
            card = loadGameCard().copy(effect = CardEffect.DOUBLE_ONE_DIE),
            target = ExecuteTarget(player = player, dice = diceOf(FixedDie(6, 1)))
        )

        assertThrows<MainActionException> {
            executor(table, player, action)
        }
    }

    @Test
    fun cultivationInvoke_whenUpgradeDieAndUseNow_upgradesTargetHandDie() {
        val chronicle = GameChronicle()
        val executor = GameCardEffectExecutorCultivation(chronicle, DieFactory(ValueRandomizer(3)))
        val table = createTable(numBattle = 0, numCultivation = 1).apply {
            grove.resetDice(2)
        }
        val oldDie = FixedDie(4, 2)
        val player = Player(id = 7).apply {
            addDieToHand(oldDie)
        }
        val card = loadGameCard().copy(effect = CardEffect.UPGRADE_DIE_AND_USE_NOW)
        val action = ActionCultivation.ExecuteCard(
            card = card,
            target = ExecuteTarget(player = player, dice = diceOf(FixedDie(4, 2)))
        )

        executor(table, player, action)

        assertEquals(listOf(6 to 3), player.diceHand.dice.map { it.sides to it.value })
        assertEquals(6, table.grove.count(DieSides.D6))
        assertEquals(1, table.grove.count(DieSides.D4))
        val entry = assertIs<GameEntry.GameCardEffect>(chronicle.getEntries().single())
        assertEquals(card.effect, entry.effect)
        assertEquals(listOf(6 to 3), entry.dice.map { it.sides to it.value })
    }

    @Test
    fun battleInvoke_whenUpgradeDieAndUseNow_replacesTargetBattleGridDie() {
        val chronicle = GameChronicle()
        val executor = GameCardEffectExecutorBattle(chronicle, DieFactory(ValueRandomizer(3)))
        val table = createTable(numBattle = 1, numCultivation = 0).apply {
            grove.resetDice(2)
        }
        val oldDie = FixedDie(4, 4)
        val target = playerWithDice(1, oldDie, FixedDie(6, 3), FixedDie(8, 2))
        table.battle.setup(
            listOf(
                target,
                playerWithDice(2, FixedDie(4, 1), FixedDie(6, 1), FixedDie(8, 1)),
                playerWithDice(3, FixedDie(4, 1), FixedDie(6, 1), FixedDie(8, 1)),
                playerWithDice(4, FixedDie(4, 1), FixedDie(6, 1), FixedDie(8, 1))
            )
        )
        val card = loadGameCard().copy(effect = CardEffect.UPGRADE_DIE_AND_USE_NOW)
        val action = ActionBattleMain.ExecuteCard(
            card = card,
            target = ExecuteTarget(player = target, dice = diceOf(FixedDie(4, 4))),
            rows = listOf(BattleStrikeRow.STRIKE_1)
        )

        executor(table, target, action)

        val strikeOneDice = table.battle.grid.getSquare(target.id, BattleStrikeRow.STRIKE_1).all
            .filterIsInstance<BattleItem.DieItem>()
            .map { it.die.sides to it.die.value }
        assertEquals(listOf(6 to 3), strikeOneDice)
        assertEquals(listOf(6, 6, 8), target.diceHand.dice.map { it.sides })
        val entries = chronicle.getEntries().filterIsInstance<GameEntry.GameCardEffect>()
        assertEquals(2, entries.size)
        assertEquals(listOf(6 to 3), entries.last().dice.map { it.sides to it.value })
    }

    @Test
    fun cultivationInvoke_whenDoubleAllDiceShowingOneToFour_doublesMatchingHandDice() {
        val chronicle = GameChronicle()
        val executor = GameCardEffectExecutorCultivation(chronicle)
        val table = createTable(numBattle = 0, numCultivation = 1)
        val d4 = FixedDie(4, 2)
        val d6 = FixedDie(6, 1)
        val d8 = FixedDie(8, 4)
        val d10 = FixedDie(10, 5)
        val player = Player(id = 7).apply {
            addDieToHand(d4)
            addDieToHand(d6)
            addDieToHand(d8)
            addDieToHand(d10)
        }
        val card = loadGameCard().copy(effect = CardEffect.DOUBLE_ALL_DICE_SHOWING_ONE_TO_FOUR)

        executor(table, player, ActionCultivation.ExecuteCard(card))

        assertEquals(4, d4.value)
        assertEquals(2, d6.value)
        assertEquals(8, d8.value)
        assertEquals(5, d10.value)
        val entry = assertIs<GameEntry.GameCardEffect>(chronicle.getEntries().single())
        assertEquals(card.effect, entry.effect)
        assertEquals(
            listOf(4 to 4, 6 to 2, 8 to 8),
            entry.dice.map { it.sides to it.value }
        )
    }

    @Test
    fun battleInvoke_whenDoubleAllDiceShowingOneToFour_doublesMatchingBattleGridDiceForPlayer() {
        val chronicle = GameChronicle()
        val executor = GameCardEffectExecutorBattle(chronicle)
        val table = createTable(numBattle = 1, numCultivation = 0)
        val d10 = FixedDie(10, 5)
        val d8 = FixedDie(8, 4)
        val d6 = FixedDie(6, 3)
        val target = playerWithDice(1, d10, d8, d6)
        table.battle.setup(
            listOf(
                target,
                playerWithDice(2, FixedDie(4, 1), FixedDie(6, 1), FixedDie(8, 1)),
                playerWithDice(3, FixedDie(4, 1), FixedDie(6, 1), FixedDie(8, 1)),
                playerWithDice(4, FixedDie(4, 1), FixedDie(6, 1), FixedDie(8, 1))
            )
        )
        val card = loadGameCard().copy(effect = CardEffect.DOUBLE_ALL_DICE_SHOWING_ONE_TO_FOUR)

        executor(table, target, ActionBattleMain.ExecuteCard(card))

        assertEquals(5, d10.value)
        assertEquals(8, d8.value)
        assertEquals(6, d6.value)
        val entry = assertIs<GameEntry.GameCardEffect>(chronicle.getEntries().single())
        assertEquals(card.effect, entry.effect)
        assertEquals(
            listOf(6 to 6, 8 to 8),
            entry.dice.map { it.sides to it.value }
        )
    }

    @Test
    fun cultivationInvoke_whenFlipDieToOppositeFace_flipsTargetHandDie() {
        val chronicle = GameChronicle()
        val executor = GameCardEffectExecutorCultivation(chronicle)
        val table = createTable(numBattle = 0, numCultivation = 1)
        val targetDie = FixedDie(8, 3)
        val d4 = FixedDie(4, 3)
        val player = Player(id = 7).apply {
            addDieToHand(targetDie)
            addDieToHand(d4)
        }
        val card = loadGameCard().copy(effect = CardEffect.FLIP_DIE_TO_OPPOSITE_FACE)
        val action = ActionCultivation.ExecuteCard(
            card = card,
            target = ExecuteTarget(player = player, dice = diceOf(FixedDie(8, 3)))
        )

        executor(table, player, action)

        assertEquals(6, targetDie.value)
        assertEquals(3, d4.value)
        val entry = assertIs<GameEntry.GameCardEffect>(chronicle.getEntries().single())
        assertEquals(card.effect, entry.effect)
        assertEquals(listOf(8 to 6), entry.dice.map { it.sides to it.value })
    }

    @Test
    fun battleInvoke_whenFlipDieToOppositeFace_flipsTargetBattleGridDie() {
        val chronicle = GameChronicle()
        val executor = GameCardEffectExecutorBattle(chronicle)
        val table = createTable(numBattle = 1, numCultivation = 0)
        val targetDie = FixedDie(8, 6)
        val target = playerWithDice(1, targetDie, FixedDie(6, 3), FixedDie(4, 2))
        table.battle.setup(
            listOf(
                target,
                playerWithDice(2, FixedDie(4, 1), FixedDie(6, 1), FixedDie(8, 1)),
                playerWithDice(3, FixedDie(4, 1), FixedDie(6, 1), FixedDie(8, 1)),
                playerWithDice(4, FixedDie(4, 1), FixedDie(6, 1), FixedDie(8, 1))
            )
        )
        val card = loadGameCard().copy(effect = CardEffect.FLIP_DIE_TO_OPPOSITE_FACE)
        val action = ActionBattleMain.ExecuteCard(
            card = card,
            target = ExecuteTarget(player = target, dice = diceOf(FixedDie(8, 6))),
            rows = listOf(BattleStrikeRow.STRIKE_1)
        )

        executor(table, Player(id = 9), action)

        assertEquals(3, targetDie.value)
        val entry = assertIs<GameEntry.GameCardEffect>(chronicle.getEntries().single())
        assertEquals(card.effect, entry.effect)
        assertEquals(listOf(8 to 3), entry.dice.map { it.sides to it.value })
    }

    @Test
    fun cultivationInvoke_whenGainWormAndBoostWorms_gainsWormAndBoostsPlayerWorms() {
        val executor = GameCardEffectExecutorCultivation()
        val table = createTable(numBattle = 0, numCultivation = 1)
        val player = Player(id = 7).apply {
            addCritter(Critter.WORM)
        }
        val action = ActionCultivation.ExecuteCard(
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
        val action = ActionBattleMain.ExecuteCard(
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
        val action = ActionCultivation.ExecuteCard(
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
        val action = ActionCultivation.ExecuteCard(
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
        val action = ActionBattleMain.ExecuteCard(
            card = loadGameCard().copy(effect = CardEffect.MULCH_DIE_FROM_DISCARD)
        )

        executor(table, player, action)

        assertEquals(listOf(Token.MULCH(DieSides.D20)), player.mulchTokens)
        assertEquals(listOf(4), player.diceDiscard.dice.map { it.sides })
        assertEquals(7, table.grove.count(Token.MULCH()))
    }

    @Test
    fun cultivationInvoke_whenDrawTwoDice_drawsTwoDiceIntoHand() {
        val chronicle = GameChronicle()
        val executor = GameCardEffectExecutorCultivation(chronicle)
        val table = createTable(numBattle = 0, numCultivation = 1)
        val player = Player(id = 7).apply {
            addDieToSupply(FixedDie(12, 5))
            addDieToSupply(FixedDie(6, 2))
        }
        val card = loadGameCard().copy(effect = CardEffect.DRAW_TWO_DICE)

        executor(table, player, ActionCultivation.ExecuteCard(card))

        assertEquals(listOf(6, 12), player.diceHand.dice.map { it.sides })
        val entry = assertIs<GameEntry.GameCardEffect>(chronicle.getEntries().single())
        assertEquals(CardEffect.DRAW_TWO_DICE, entry.effect)
        assertEquals(listOf(6 to 2, 12 to 5), entry.dice.map { it.sides to it.value })
    }

    @Test
    fun battleInvoke_whenDrawTwoDice_placesDiceOnIndicatedRows() {
        val chronicle = GameChronicle()
        val executor = GameCardEffectExecutorBattle(chronicle)
        val table = createTable(numBattle = 1, numCultivation = 0)
        val player = playerWithDice(1, FixedDie(4, 1), FixedDie(6, 2), FixedDie(8, 3)).apply {
            addDieToSupply(FixedDie(12, 5))
            addDieToSupply(FixedDie(10, 4))
        }
        table.battle.setup(
            listOf(
                player,
                playerWithDice(2, FixedDie(4, 1), FixedDie(6, 1), FixedDie(8, 1)),
                playerWithDice(3, FixedDie(4, 1), FixedDie(6, 1), FixedDie(8, 1)),
                playerWithDice(4, FixedDie(4, 1), FixedDie(6, 1), FixedDie(8, 1))
            )
        )
        val card = loadGameCard().copy(effect = CardEffect.DRAW_TWO_DICE)
        val action = ActionBattleMain.ExecuteCard(
            card = card,
            rows = listOf(BattleStrikeRow.STRIKE_1, BattleStrikeRow.STRIKE_3)
        )

        executor(table, player, action)

        assertEquals(emptyList(), player.diceSupply.dice)
        assertEquals(listOf(4, 6, 8), player.diceHand.dice.map { it.sides })
        val row1Dice = table.battle.grid.getSquare(player.id, BattleStrikeRow.STRIKE_1).all
            .filterIsInstance<BattleItem.DieItem>()
            .map { it.die.sides to it.die.value }
        val row3Dice = table.battle.grid.getSquare(player.id, BattleStrikeRow.STRIKE_3).all
            .filterIsInstance<BattleItem.DieItem>()
            .map { it.die.sides to it.die.value }
        assertEquals(listOf(8 to 3, 10 to 4), row1Dice)
        assertEquals(listOf(4 to 1, 12 to 5), row3Dice)
        val entry = assertIs<GameEntry.GameCardEffect>(chronicle.getEntries().single())
        assertEquals(listOf(10 to 4, 12 to 5), entry.dice.map { it.sides to it.value })
    }

    @Test
    fun battleInvoke_whenDrawTwoDiceHasNoSecondRow_placesBothDiceOnFirstRow() {
        val executor = GameCardEffectExecutorBattle()
        val table = createTable(numBattle = 1, numCultivation = 0)
        val player = playerWithDice(1, FixedDie(4, 1), FixedDie(6, 2), FixedDie(8, 3)).apply {
            addDieToSupply(FixedDie(12, 5))
            addDieToSupply(FixedDie(10, 4))
        }
        table.battle.setup(
            listOf(
                player,
                playerWithDice(2, FixedDie(4, 1), FixedDie(6, 1), FixedDie(8, 1)),
                playerWithDice(3, FixedDie(4, 1), FixedDie(6, 1), FixedDie(8, 1)),
                playerWithDice(4, FixedDie(4, 1), FixedDie(6, 1), FixedDie(8, 1))
            )
        )
        val action = ActionBattleMain.ExecuteCard(
            card = loadGameCard().copy(effect = CardEffect.DRAW_TWO_DICE),
            rows = listOf(BattleStrikeRow.STRIKE_2)
        )

        executor(table, player, action)

        val dice = table.battle.grid.getSquare(player.id, BattleStrikeRow.STRIKE_2).all
            .filterIsInstance<BattleItem.DieItem>()
            .map { it.die.sides to it.die.value }
        assertEquals(listOf(6 to 2, 10 to 4, 12 to 5), dice)
    }

    @Test
    fun cultivationInvoke_whenRaiseDiePlus1AndEndGamePlus1VpPerFlower_raisesTargetDieByOne() {
        val chronicle = GameChronicle()
        val executor = GameCardEffectExecutorCultivation(chronicle)
        val table = createTable(numBattle = 0, numCultivation = 1)
        val die = FixedDie(8, 3)
        val player = Player(id = 7).apply {
            addDieToHand(die)
        }
        val card = loadGameCard().copy(effect = CardEffect.RAISE_DIE_PLUS_1_AND_END_GAME_PLUS_1_VP_PER_FLOWER)

        executor(table, player, ActionCultivation.ExecuteCard(card, target = ExecuteTarget(dice = diceOf(FixedDie(8, 3)))))

        assertEquals(4, die.value)
        val entry = assertIs<GameEntry.GameCardEffect>(chronicle.getEntries().single())
        assertEquals(CardEffect.RAISE_DIE_PLUS_1_AND_END_GAME_PLUS_1_VP_PER_FLOWER, entry.effect)
        assertEquals(listOf(8 to 4), entry.dice.map { it.sides to it.value })
    }

    @Test
    fun battleInvoke_whenRaiseDiePlus1AndEndGamePlus1VpPerFlower_raisesTargetGridDieByOne() {
        val chronicle = GameChronicle()
        val executor = GameCardEffectExecutorBattle(chronicle)
        val table = createTable(numBattle = 1, numCultivation = 0)
        val die = FixedDie(10, 4)
        val player = playerWithDice(1, die, FixedDie(8, 3), FixedDie(6, 1))
        table.battle.setup(
            listOf(
                player,
                playerWithDice(2, FixedDie(4, 1), FixedDie(6, 1), FixedDie(8, 1)),
                playerWithDice(3, FixedDie(4, 1), FixedDie(6, 1), FixedDie(8, 1)),
                playerWithDice(4, FixedDie(4, 1), FixedDie(6, 1), FixedDie(8, 1))
            )
        )
        val card = loadGameCard().copy(effect = CardEffect.RAISE_DIE_PLUS_1_AND_END_GAME_PLUS_1_VP_PER_FLOWER)
        val action = ActionBattleMain.ExecuteCard(
            card = card,
            target = ExecuteTarget(player = player, dice = diceOf(FixedDie(10, 4))),
            rows = listOf(BattleStrikeRow.STRIKE_1)
        )

        executor(table, player, action)

        assertEquals(5, die.value)
        val entry = assertIs<GameEntry.GameCardEffect>(chronicle.getEntries().single())
        assertEquals(CardEffect.RAISE_DIE_PLUS_1_AND_END_GAME_PLUS_1_VP_PER_FLOWER, entry.effect)
        assertEquals(listOf(10 to 5), entry.dice.map { it.sides to it.value })
    }

    @Test
    fun cultivationInvoke_whenRaiseThreeDicePlus1_raisesEachTargetHandDieByOne() {
        val chronicle = GameChronicle()
        val executor = GameCardEffectExecutorCultivation(chronicle)
        val table = createTable(numBattle = 0, numCultivation = 1)
        val d4 = FixedDie(4, 1)
        val d6 = FixedDie(6, 2)
        val d8 = FixedDie(8, 3)
        val player = Player(id = 7).apply {
            addDieToHand(d4)
            addDieToHand(d6)
            addDieToHand(d8)
        }
        val card = loadGameCard().copy(effect = CardEffect.RAISE_THREE_DICE_PLUS_1)
        val action = ActionCultivation.ExecuteCard(
            card = card,
            target = ExecuteTarget(dice = diceOf(FixedDie(4, 1), FixedDie(6, 2), FixedDie(8, 3)))
        )

        executor(table, player, action)

        assertEquals(listOf(2, 3, 4), listOf(d4.value, d6.value, d8.value))
        val entry = assertIs<GameEntry.GameCardEffect>(chronicle.getEntries().single())
        assertEquals(CardEffect.RAISE_THREE_DICE_PLUS_1, entry.effect)
        assertEquals(listOf(4 to 2, 6 to 3, 8 to 4), entry.dice.map { it.sides to it.value })
    }

    @Test
    fun battleInvoke_whenRaiseThreeDicePlus1_raisesEachTargetGridDieByOne() {
        val chronicle = GameChronicle()
        val executor = GameCardEffectExecutorBattle(chronicle)
        val table = createTable(numBattle = 1, numCultivation = 0)
        val d10 = FixedDie(10, 4)
        val d8 = FixedDie(8, 3)
        val d6 = FixedDie(6, 2)
        val player = playerWithDice(1, d10, d8, d6)
        table.battle.setup(
            listOf(
                player,
                playerWithDice(2, FixedDie(4, 1), FixedDie(6, 1), FixedDie(8, 1)),
                playerWithDice(3, FixedDie(4, 1), FixedDie(6, 1), FixedDie(8, 1)),
                playerWithDice(4, FixedDie(4, 1), FixedDie(6, 1), FixedDie(8, 1))
            )
        )
        val card = loadGameCard().copy(effect = CardEffect.RAISE_THREE_DICE_PLUS_1)
        val action = ActionBattleMain.ExecuteCard(
            card = card,
            target = ExecuteTarget(player = player, dice = diceOf(FixedDie(10, 4), FixedDie(8, 3), FixedDie(6, 2))),
            rows = listOf(BattleStrikeRow.STRIKE_1, BattleStrikeRow.STRIKE_2, BattleStrikeRow.STRIKE_3)
        )

        executor(table, player, action)

        assertEquals(listOf(5, 4, 3), listOf(d10.value, d8.value, d6.value))
        val entry = assertIs<GameEntry.GameCardEffect>(chronicle.getEntries().single())
        assertEquals(CardEffect.RAISE_THREE_DICE_PLUS_1, entry.effect)
        assertEquals(listOf(6 to 3, 8 to 4, 10 to 5), entry.dice.map { it.sides to it.value })
    }

    @Test
    fun cultivationInvoke_whenRaiseDiePlus4_raisesTargetHandDieByFour() {
        val chronicle = GameChronicle()
        val executor = GameCardEffectExecutorCultivation(chronicle)
        val table = createTable(numBattle = 0, numCultivation = 1)
        val die = FixedDie(10, 3)
        val player = Player(id = 7).apply {
            addDieToHand(die)
        }
        val card = loadGameCard().copy(effect = CardEffect.RAISE_DIE_PLUS_4)
        val action = ActionCultivation.ExecuteCard(
            card = card,
            target = ExecuteTarget(dice = diceOf(FixedDie(10, 3)))
        )

        executor(table, player, action)

        assertEquals(7, die.value)
        val entry = assertIs<GameEntry.GameCardEffect>(chronicle.getEntries().single())
        assertEquals(CardEffect.RAISE_DIE_PLUS_4, entry.effect)
        assertEquals(listOf(10 to 7), entry.dice.map { it.sides to it.value })
    }

    @Test
    fun battleInvoke_whenRaiseDiePlus4_raisesTargetGridDieByFour() {
        val chronicle = GameChronicle()
        val executor = GameCardEffectExecutorBattle(chronicle)
        val table = createTable(numBattle = 1, numCultivation = 0)
        val die = FixedDie(10, 3)
        val player = playerWithDice(1, die, FixedDie(8, 2), FixedDie(6, 1))
        table.battle.setup(
            listOf(
                player,
                playerWithDice(2, FixedDie(4, 1), FixedDie(6, 1), FixedDie(8, 1)),
                playerWithDice(3, FixedDie(4, 1), FixedDie(6, 1), FixedDie(8, 1)),
                playerWithDice(4, FixedDie(4, 1), FixedDie(6, 1), FixedDie(8, 1))
            )
        )
        val card = loadGameCard().copy(effect = CardEffect.RAISE_DIE_PLUS_4)
        val action = ActionBattleMain.ExecuteCard(
            card = card,
            target = ExecuteTarget(player = player, dice = diceOf(FixedDie(10, 3))),
            rows = listOf(BattleStrikeRow.STRIKE_1)
        )

        executor(table, player, action)

        assertEquals(7, die.value)
        val entry = assertIs<GameEntry.GameCardEffect>(chronicle.getEntries().single())
        assertEquals(CardEffect.RAISE_DIE_PLUS_4, entry.effect)
        assertEquals(listOf(10 to 7), entry.dice.map { it.sides to it.value })
    }

    @Test
    fun cultivationInvoke_whenResolveGraftedRootOrVineEffect_chroniclesFollowUpInstruction() {
        val chronicle = GameChronicle()
        val executor = GameCardEffectExecutorCultivation(chronicle)
        val table = createTable(numBattle = 0, numCultivation = 1)
        val player = Player(id = 7)
        val card = loadGameCard().copy(effect = CardEffect.RESOLVE_GRAFTED_ROOT_OR_VINE_EFFECT)

        executor(table, player, ActionCultivation.ExecuteCard(card, usesAction = false))

        val entry = assertIs<GameEntry.GameCardEffect>(chronicle.getEntries().single())
        assertEquals(CardEffect.RESOLVE_GRAFTED_ROOT_OR_VINE_EFFECT, entry.effect)
        assertEquals(card.id, entry.cardId)
        assertEquals(player.id, entry.playerId)
        assertEquals(
            "Enabled resolving a grafted root or vine effect; follow-up ExecuteCard should perform the chosen effect",
            entry.detail
        )
        assertEquals(emptyList(), entry.dice)
    }

    @Test
    fun battleInvoke_whenResolveGraftedRootOrVineEffect_chroniclesFollowUpInstruction() {
        val chronicle = GameChronicle()
        val executor = GameCardEffectExecutorBattle(chronicle)
        val table = createTable(numBattle = 1, numCultivation = 0)
        val player = Player(id = 7)
        val card = loadGameCard().copy(effect = CardEffect.RESOLVE_GRAFTED_ROOT_OR_VINE_EFFECT)

        executor(table, player, ActionBattleMain.ExecuteCard(card, usesAction = false))

        val entry = assertIs<GameEntry.GameCardEffect>(chronicle.getEntries().single())
        assertEquals(CardEffect.RESOLVE_GRAFTED_ROOT_OR_VINE_EFFECT, entry.effect)
        assertEquals(card.id, entry.cardId)
        assertEquals(player.id, entry.playerId)
        assertEquals(
            "Enabled resolving a grafted root or vine effect; follow-up ExecuteCard should perform the chosen effect",
            entry.detail
        )
        assertEquals(emptyList(), entry.dice)
    }

    @Test
    fun battleInvoke_whenResolveStrikeImmediately_awardsSelectedRowAndMarksItResolved() {
        val chronicle = GameChronicle()
        val executor = GameCardEffectExecutorBattle(chronicle)
        val table = createTable(numBattle = 1, numCultivation = 0)
        val winner = playerWithDice(1, FixedDie(8, 6), FixedDie(6, 2), FixedDie(4, 1))
        val loser2 = playerWithDice(2, FixedDie(8, 2), FixedDie(6, 2), FixedDie(4, 1))
        val loser3 = playerWithDice(3, FixedDie(8, 2), FixedDie(6, 2), FixedDie(4, 1))
        val loser4 = playerWithDice(4, FixedDie(8, 2), FixedDie(6, 2), FixedDie(4, 1))
        val players = listOf(winner, loser2, loser3, loser4)
        players.forEach { table.add(it) }
        table.battle.setup(players)
        val card = loadGameCard().copy(effect = CardEffect.RESOLVE_STRIKE_IMMEDIATELY)

        executor(
            table,
            loser2,
            ActionBattleMain.ExecuteCard(
                card = card,
                rows = listOf(BattleStrikeRow.STRIKE_1)
            )
        )

        assertEquals(2, winner.vp)
        assertEquals(setOf(BattleStrikeRow.STRIKE_1), table.battle.resolved)
        assertEquals(emptyList(), table.battle.computeWinners()[BattleStrikeRow.STRIKE_1].winners)
        val vpEntry = assertIs<GameEntry.VpAward>(chronicle.getEntries().first())
        assertEquals(winner.id, vpEntry.playerId)
        assertEquals(BattleStrikeRow.STRIKE_1, vpEntry.row)
        val effectEntry = assertIs<GameEntry.GameCardEffect>(chronicle.getEntries().last())
        assertEquals(CardEffect.RESOLVE_STRIKE_IMMEDIATELY, effectEntry.effect)
        assertEquals("Resolved STRIKE_1 immediately", effectEntry.detail)
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

    private fun diceOf(vararg dice: Die): Dice {
        return Dice(dice.toList())
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

    private class SequenceDie(
        sides: Int,
        initial: Int,
        private val rolls: List<Int>
    ) : Die(sides) {
        var rollCount = 0

        init {
            adjustTo(initial)
        }

        override fun roll(): Die {
            val value = rolls.getOrElse(rollCount) { rolls.last() }
            rollCount++
            adjustTo(value)
            return this
        }
    }

    private class IdentityRandomizer : Randomizer {
        override fun nextBoolean(): Boolean = throw UnsupportedOperationException()
        override fun nextInt(from: Int, until: Int): Int = throw UnsupportedOperationException()
        override fun nextInt(until: Int): Int = throw UnsupportedOperationException()
        override fun <T> randomOrNull(list: List<T>): T? = throw UnsupportedOperationException()
        override fun <T> shuffled(list: List<T>): List<T> = list
    }

    private class ValueRandomizer(
        private val value: Int
    ) : Randomizer {
        override fun nextBoolean(): Boolean = true
        override fun nextInt(from: Int, until: Int): Int = value.coerceIn(from, until - 1)
        override fun nextInt(until: Int): Int = nextInt(0, until)
        override fun <T> randomOrNull(list: List<T>): T? = list.firstOrNull()
        override fun <T> shuffled(list: List<T>): List<T> = list
    }
}
