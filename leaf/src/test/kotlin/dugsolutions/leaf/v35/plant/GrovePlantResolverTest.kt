package dugsolutions.leaf.v35.plant

import dugsolutions.leaf.v35.common.CardDataFiles
import dugsolutions.leaf.v35.common.FirstGameDefault
import dugsolutions.leaf.v35.effect.GameEffectConverter
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.random.Randomizer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class GrovePlantResolverTest {
    private lateinit var manager: PlantCardManager
    private lateinit var resolver: GrovePlantResolver

    @BeforeEach
    fun setup() {
        val registry = PlantCardRegistry(GameEffectConverter())
        registry.loadFromCsv(
            CardDataFiles.dataPath(CardDataFiles.ROOT_CARD_LIST),
            CardDataFiles.dataPath(CardDataFiles.VF_CARD_LIST)
        )
        manager = PlantCardManager().also { it.loadCards(registry) }
        resolver = GrovePlantResolver(manager)
    }

    @Test
    fun noOverrides_resolvesExactlyOneCardForEveryRequiredSlot() {
        val cards = resolver.resolve(emptyList(), Randomizer.create(12000L))
        assertEquals(9, cards.size)
        assertEquals(
            listOf(
                PlantType.ROOT to 5, PlantType.ROOT to 7, PlantType.ROOT to 9,
                PlantType.VINE to 7, PlantType.VINE to 9, PlantType.VINE to 11,
                PlantType.FLOWER to 11, PlantType.FLOWER to 14, PlantType.FLOWER to 17
            ),
            cards.map { it.type to it.cost }
        )
    }

    @Test
    fun sameSeed_reproducesRandomGrove() {
        val first = resolver.resolve(emptyList(), Randomizer.create(12000L)).map { it.name }
        val second = resolver.resolve(emptyList(), Randomizer.create(12000L)).map { it.name }
        assertEquals(first, second)
    }

    @Test
    fun differentMechanicalChoices_canProduceDifferentGroves() {
        val first = resolver.resolve(emptyList(), IndexedRandomizer(0)).map { it.name }
        val second = resolver.resolve(emptyList(), IndexedRandomizer(Int.MAX_VALUE)).map { it.name }
        assertTrue(first != second)
    }

    @Test
    fun partialOverride_isPreservedAndRemainingSlotsAreFilled() {
        val root5 = requireNotNull(manager.getCard("Root_05_02"))
        val cards = resolver.resolve(listOf(root5), Randomizer.create(12000L))
        assertEquals(9, cards.size)
        assertEquals("Root_05_02", cards.first().name)
    }

    @Test
    fun allNineFirstGameOverrides_arePreservedExactly() {
        val overrides = FirstGameDefault.PLANT_NAMES.map { requireNotNull(manager.getCard(it)) }
        val cards = resolver.resolve(overrides, Randomizer.create(12000L))
        assertEquals(FirstGameDefault.PLANT_NAMES, cards.map { it.name })
    }

    @Test
    fun unavailablePlant_isNeverChosenForRandomSlot() {
        val blocked = requireNotNull(manager.getCard("Vine_07_01"))
        val values = availabilityValues(setOf(blocked.name))

        val cards = resolver.resolve(emptyList(), IndexedRandomizer(0), values)

        assertTrue(cards.none { it.name == blocked.name })
        assertEquals(9, cards.size)
    }

    @Test
    fun explicitlyFixedUnavailablePlant_failsClearly() {
        val blocked = requireNotNull(manager.getCard("Vine_07_01"))
        val error = assertFailsWith<IllegalArgumentException> {
            resolver.resolve(
                listOf(blocked),
                Randomizer.create(12000L),
                availabilityValues(setOf(blocked.name))
            )
        }

        assertTrue(error.message.orEmpty().contains("unavailable"))
        assertTrue(error.message.orEmpty().contains(blocked.name))
    }

    @Test
    fun unavailableEntireRequiredSlot_failsClearly() {
        val root5 = manager.getCardsByType(PlantType.ROOT)
            .filter { it.cost == 5 }
            .map { it.name }
            .toSet()

        val error = assertFailsWith<IllegalStateException> {
            resolver.resolve(
                emptyList(),
                Randomizer.create(12000L),
                availabilityValues(root5)
            )
        }

        assertTrue(error.message.orEmpty().contains("ROOT 5"))
        assertTrue(error.message.orEmpty().contains("availability"))
    }

    @Test
    fun sameSeedWithAvailabilityOverride_reproducesRandomGrove() {
        val values = availabilityValues(setOf("Vine_07_01", "Vine_07_04"))
        val first = resolver.resolve(emptyList(), Randomizer.create(12000L), values).map { it.name }
        val second = resolver.resolve(emptyList(), Randomizer.create(12000L), values).map { it.name }
        assertEquals(first, second)
    }

    @Test
    fun duplicateSlotOverrides_failClearly() {
        val overrides = listOf(
            requireNotNull(manager.getCard("Root_05_01")),
            requireNotNull(manager.getCard("Root_05_02"))
        )
        assertFailsWith<IllegalArgumentException> {
            resolver.resolve(overrides, Randomizer.create(12000L))
        }
    }

    private fun availabilityValues(unavailable: Set<String>): PlantValueResolver =
        object : PlantValueResolver {
            override fun costFor(card: PlantCard): Int = card.cost
            override fun isAvailable(card: PlantCard): Boolean = card.name !in unavailable
        }

    private class IndexedRandomizer(private val requestedIndex: Int) : Randomizer {
        override fun nextBoolean(): Boolean = false
        override fun nextInt(from: Int, until: Int): Int = from
        override fun nextInt(until: Int): Int = 0
        override fun <T> randomOrNull(list: List<T>): T? =
            list.takeIf { it.isNotEmpty() }?.let { it[requestedIndex.mod(it.size)] }
        override fun <T> shuffled(list: List<T>): List<T> = list
    }

}
