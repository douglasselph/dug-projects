package dugsolutions.leaf.v30.game.effect.details

import dugsolutions.leaf.v30.grove.Grove
import dugsolutions.leaf.v30.random.Randomizer
import dugsolutions.leaf.v30.random.die.Die
import dugsolutions.leaf.v30.random.die.DieSides
import dugsolutions.leaf.v30.random.die.di.DieFactory
import dugsolutions.leaf.v30.wisp.WispCardManager
import dugsolutions.leaf.v30.wisp.WispDeck
import dugsolutions.leaf.v30.wisp.di.WispCardsFactory
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UpgradeDieTest {

    @Test
    fun invoke_whenNextDieExists_removesNextDieAndReturnsUpgrade() {
        val grove = createGrove()

        val result = UpgradeDie(grove, dieFactory())(FixedDie(6, 2))

        assertEquals(8, result?.sides)
        assertEquals(6, grove.count(DieSides.D8))
    }

    @Test
    fun invoke_whenUpgradingD4_returnsD4ToGrove() {
        val grove = createGrove()

        val result = UpgradeDie(grove, dieFactory())(FixedDie(4, 2))

        assertEquals(6, result?.sides)
        assertEquals(6, grove.count(DieSides.D6))
        assertEquals(1, grove.count(DieSides.D4))
    }

    @Test
    fun invoke_whenNextDieIsMissing_returnsNull() {
        val grove = createGrove().apply {
            diceStacks.setCount(DieSides.D8, 0)
        }

        val result = UpgradeDie(grove, dieFactory())(FixedDie(6, 2))

        assertNull(result)
        assertEquals(0, grove.count(DieSides.D8))
    }

    @Test
    fun invokeTwice_skipsMissingSizesAndReturnsSecondAvailableUpgrade() {
        val grove = createGrove().apply {
            diceStacks.setCount(DieSides.D6, 0)
            diceStacks.setCount(DieSides.D8, 0)
        }

        val result = UpgradeDieTwice(grove, dieFactory())(FixedDie(4, 2))

        assertEquals(12, result?.sides)
        assertEquals(6, grove.count(DieSides.D12))
        assertEquals(1, grove.count(DieSides.D4))
    }

    private fun createGrove(): Grove {
        val manager = WispCardManager(WispCardsFactory()).apply {
            loadCards(emptyList())
        }
        return Grove(WispDeck(manager, StaticRandomizer())).apply {
            resetDice(2)
        }
    }

    private fun dieFactory(): DieFactory {
        return DieFactory(StaticRandomizer())
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

    private class StaticRandomizer : Randomizer {
        override fun nextBoolean(): Boolean = true
        override fun nextInt(from: Int, until: Int): Int = 3.coerceIn(from, until - 1)
        override fun nextInt(until: Int): Int = nextInt(0, until)
        override fun <T> randomOrNull(list: List<T>): T? = list.firstOrNull()
        override fun <T> shuffled(list: List<T>): List<T> = list
    }
}
