package dugsolutions.leaf.v30.game.effect

import dugsolutions.leaf.v30.chronicle.GameChronicle
import dugsolutions.leaf.v30.chronicle.domain.GameEntry
import dugsolutions.leaf.v30.common.Butterfly
import dugsolutions.leaf.v30.common.Critter
import dugsolutions.leaf.v30.common.Token
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
import dugsolutions.leaf.v30.wisp.domain.GenWispCardID
import dugsolutions.leaf.v30.wisp.domain.WispCard
import dugsolutions.leaf.v30.wisp.domain.WispEffect
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class WispCardEffectExecutorTest {

    @Test
    fun invoke_gainTwoCritters_takesRequestedCrittersFromGrove() {
        val table = createTable()
        val player = Player(id = 1)
        val card = wispCard(WispEffect.GAIN_2_CRITTERS)

        WispCardEffectExecutor()(table, player, card, ExecuteTarget(critter = listOf(Critter.WORM, Critter.BEE)))

        assertEquals(listOf(Critter.WORM, Critter.BEE), player.critters)
        assertEquals(8, table.grove.count(Critter.WORM))
        assertEquals(8, table.grove.count(Critter.BEE))
    }

    @Test
    fun invoke_gainMulch_usesHighestSidedDieFromDiscard() {
        val player = Player(id = 1).apply {
            addDieToDiscard(FixedDie(6, 4))
            addDieToDiscard(FixedDie(12, 5))
        }

        WispCardEffectExecutor()(createTable(), player, wispCard(WispEffect.GAIN_MULCH))

        assertEquals(1, player.diceDiscard.size)
        assertEquals(listOf(Token.MULCH(DieSides.D12)), player.mulchTokens)
    }

    @Test
    fun invoke_gainButterfly_removesFromCurrentOwnerAndAddsFaceUpToPlayer() {
        val table = createTable()
        val owner = Player(id = 2).apply {
            addButterfly(Butterfly.RED)
            faceDownButterfly(Butterfly.RED)
        }
        val player = Player(id = 1)
        table.grove.remove(Butterfly.RED)
        table.add(player).add(owner)

        WispCardEffectExecutor()(table, player, wispCard(WispEffect.GAIN_RED_BUTTERFLY))

        assertEquals(emptyList(), owner.butterflies)
        assertEquals(listOf(Butterfly.RED), player.butterflies)
        assertTrue(player.isButterflyFaceUp(Butterfly.RED))
    }

    @Test
    fun invoke_upgradeTwoSteps_removesTargetDieAndAddsUpgradedRolledDieToHand() {
        val table = createTable()
        table.grove.diceStacks.setCount(DieSides.D6, 1)
        table.grove.diceStacks.setCount(DieSides.D8, 1)
        val player = Player(id = 1)
        player.addDieToHand(FixedDie(4, 3))

        WispCardEffectExecutor(dieFactory = DieFactory(IdentityRandomizer()))(
            table = table,
            player = player,
            card = wispCard(WispEffect.UPGRADE_2_STEPS),
            target = ExecuteTarget(dice = Dice(listOf(FixedDie(4, 3))))
        )

        assertEquals(1, player.diceHand.size)
        assertEquals(8, player.diceHand.dice.single().sides)
    }

    @Test
    fun invoke_swapDice_swapsValuesBetweenPlayerHands() {
        val table = createTable()
        val player = Player(id = 1).apply { addDieToHand(FixedDie(8, 2)) }
        val targetPlayer = Player(id = 2).apply { addDieToHand(FixedDie(8, 7)) }
        table.add(player).add(targetPlayer)

        WispCardEffectExecutor()(
            table = table,
            player = player,
            card = wispCard(WispEffect.SWAP_DICE),
            target = ExecuteTarget(
                player = targetPlayer,
                dice = Dice(listOf(FixedDie(8, 2), FixedDie(8, 7)))
            )
        )

        assertEquals(7, player.diceHand.dice.single().value)
        assertEquals(2, targetPlayer.diceHand.dice.single().value)
    }

    @Test
    fun invoke_unknown_recordsWarningWithEffectDetail() {
        val chronicle = GameChronicle()
        val player = Player(id = 1)

        WispCardEffectExecutor(chronicle)(createTable(), player, wispCard(WispEffect.UNKNOWN))

        val entry = assertIs<GameEntry.Warning>(chronicle.getEntries().single())
        assertEquals("WispEffect.UNKNOWN", entry.detail)
    }

    private fun wispCard(effect: WispEffect): WispCard {
        return WispCard(
            id = GenWispCardID.generateId("Wisp $effect"),
            quantity = 1,
            name = "Wisp $effect",
            title = "Test",
            count = 1,
            description = "",
            lineIcons = null,
            lineIconsHeight = 0,
            mainBackdrop = null,
            effect = effect
        )
    }

    private fun createTable(): Table {
        val wispManager = WispCardManager(WispCardsFactory()).apply { loadCards(emptyList()) }
        val roundManager = RoundCardManager(RoundCardsFactory()).apply { loadCards(emptyList()) }
        return Table(
            grove = Grove(WispDeck(wispManager, IdentityRandomizer())),
            roundDeck = RoundDeck(roundManager, IdentityRandomizer())
        )
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
        override fun nextBoolean(): Boolean = true
        override fun nextInt(from: Int, until: Int): Int = from
        override fun nextInt(until: Int): Int = 0
        override fun <T> randomOrNull(list: List<T>): T? = list.firstOrNull()
        override fun <T> shuffled(list: List<T>): List<T> = list
    }
}
