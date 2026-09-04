package dugsolutions.leaf.v35.random.die.di

import dugsolutions.leaf.v35.random.Randomizer
import dugsolutions.leaf.v35.random.die.DieSides
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class DieFactoryConstructionTest {

    @Test
    fun randomFactory_creationDoesNotRollUntilRollIsRequested() {
        val randomizer = CountingRandomizer()
        val factory = DieFactoryRandom(randomizer)

        val die = factory(DieSides.D6)

        assertEquals(0, randomizer.nextIntCalls)

        die.roll()

        assertEquals(1, randomizer.nextIntCalls)
    }

    @Test
    fun uniformFactory_creationDoesNotConsumeBagUntilRollIsRequested() {
        val randomizer = CountingRandomizer()
        val factory = DieFactoryOneOfEachFaceBag(randomizer)

        val die = factory(DieSides.D6)

        assertEquals(0, randomizer.nextIntCalls)

        die.roll()

        assertEquals(1, randomizer.nextIntCalls)
    }

    private class CountingRandomizer : Randomizer {
        var nextIntCalls: Int = 0
            private set

        override fun nextBoolean(): Boolean = false

        override fun nextInt(from: Int, until: Int): Int {
            nextIntCalls++
            return from
        }

        override fun nextInt(until: Int): Int {
            nextIntCalls++
            return 0
        }

        override fun <T> randomOrNull(list: List<T>): T? = list.firstOrNull()

        override fun <T> shuffled(list: List<T>): List<T> = list
    }
}
