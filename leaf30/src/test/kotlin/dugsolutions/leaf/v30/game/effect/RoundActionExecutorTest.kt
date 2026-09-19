package dugsolutions.leaf.v30.game.effect

import dugsolutions.leaf.v30.cards.GameCardRegistry
import dugsolutions.leaf.v30.chronicle.GameChronicle
import dugsolutions.leaf.v30.chronicle.domain.GameEntry
import dugsolutions.leaf.v30.common.Commons
import dugsolutions.leaf.v30.common.Critter
import dugsolutions.leaf.v30.common.Token
import dugsolutions.leaf.v30.grove.Grove
import dugsolutions.leaf.v30.player.Player
import dugsolutions.leaf.v30.player.decision.domain.ActionRound
import dugsolutions.leaf.v30.player.decision.domain.ExecuteTarget
import dugsolutions.leaf.v30.random.Randomizer
import dugsolutions.leaf.v30.random.die.Dice
import dugsolutions.leaf.v30.random.die.Die
import dugsolutions.leaf.v30.random.die.DieSides
import dugsolutions.leaf.v30.random.die.di.DieFactory
import dugsolutions.leaf.v30.round.RoundCardManager
import dugsolutions.leaf.v30.round.RoundDeck
import dugsolutions.leaf.v30.round.di.RoundCardsFactory
import dugsolutions.leaf.v30.round.domain.GenRoundCardID
import dugsolutions.leaf.v30.round.domain.RoundCard
import dugsolutions.leaf.v30.round.domain.RoundEffect
import dugsolutions.leaf.v30.table.Table
import dugsolutions.leaf.v30.wisp.WispCardManager
import dugsolutions.leaf.v30.wisp.WispDeck
import dugsolutions.leaf.v30.wisp.di.WispCardsFactory
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class RoundActionExecutorTest {

    @Test
    fun invoke_raiseBy3_raisesTargetHandDieAndChronicles() {
        val chronicle = GameChronicle()
        val executor = RoundActionExecutor(chronicle, DieFactory(IdentityRandomizer()))
        val player = Player(id = 1)
        val die = FixedDie(8, 2)
        player.addDieToHand(die)

        executor(
            table = createTable(),
            player = player,
            card = roundCard(effect1 = RoundEffect.RAISE_BY_3),
            action = ActionRound.ACTION_1,
            target = ExecuteTarget(dice = Dice(listOf(FixedDie(8, 2))))
        )

        assertEquals(5, die.value)
        val entry = assertIs<GameEntry.MainAction>(chronicle.getEntries().single())
        assertTrue(entry.detail.contains("RAISE_BY_3"))
    }

    @Test
    fun invoke_gainWater_addsWaterToken() {
        val executor = RoundActionExecutor()
        val player = Player(id = 1)

        executor(createTable(), player, roundCard(effect1 = RoundEffect.GAIN_WATER), ActionRound.ACTION_1)

        assertEquals(1, player.waterTokenCount)
    }

    @Test
    fun invoke_gainMulch_removesTargetDieAndAddsPendingMulch() {
        val executor = RoundActionExecutor()
        val player = Player(id = 1)
        player.addDieToHand(FixedDie(10, 4))

        executor(
            table = createTable(),
            player = player,
            card = roundCard(effect1 = RoundEffect.GAIN_MULCH),
            action = ActionRound.ACTION_1,
            target = ExecuteTarget(dice = Dice(listOf(FixedDie(10, 4))))
        )

        assertEquals(0, player.diceHand.size)
        assertEquals(1, player.remove(Token.PENDING_MULCH(DieSides.D10)).let { if (it) 1 else 0 })
    }

    @Test
    fun invoke_gainWorms_respectsGroveSupplyAndAddsTwoWorms() {
        val table = createTable()
        val player = Player(id = 1)

        RoundActionExecutor()(table, player, roundCard(effect1 = RoundEffect.GAIN_WORMS), ActionRound.ACTION_1)

        assertEquals(2, player.critters.count { it == Critter.WORM })
        assertEquals(7, table.grove.count(Critter.WORM))
    }

    @Test
    fun invoke_gainRoot_takesHighestAvailableRootAndAddsFaceDownToCreature() {
        val table = createTable()
        val card = requireNotNull(
            GameCardRegistry()
                .apply { loadFromCsv(Commons.CARD_LIST) }
                .getCard("Root_09_01")
        )
        table.grove.setCard(card)
        val player = Player(id = 1)

        RoundActionExecutor()(table, player, roundCard(effect1 = RoundEffect.GAIN_ROOT), ActionRound.ACTION_1)

        assertEquals(card, player.creatureCards.single().card)
        assertTrue(player.creatureCards.single().isFaceDown)
        assertEquals(7, table.grove.cardStacks.getCount(dugsolutions.leaf.v30.grove.domain.GroveCardStackID.ROOT_9))
    }

    private fun roundCard(
        effect1: RoundEffect = RoundEffect.UNKNOWN,
        effect2: RoundEffect = RoundEffect.UNKNOWN
    ): RoundCard {
        return RoundCard(
            id = GenRoundCardID.generateId("Resource Test $effect1 $effect2"),
            quantity = 1,
            name = "Resource Test",
            title = "Test",
            effect1Title = "Effect 1",
            effect1Text = "",
            effect1Bg = "",
            effect1TextFg = "",
            effect1Image = null,
            effect1Icon = null,
            effect1 = effect1,
            effect2Title = "Effect 2",
            effect2Text = "",
            effect2Bg = "",
            effect2TextFg = "",
            effect2Image = null,
            effect2Icon = null,
            effect2 = effect2,
            backImage = null
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
