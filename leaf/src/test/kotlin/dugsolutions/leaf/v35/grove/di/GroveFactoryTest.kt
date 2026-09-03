package dugsolutions.leaf.v35.grove.di

import dugsolutions.leaf.v35.common.CardDataFiles
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.GameEffectConverter
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.random.Randomizer
import dugsolutions.leaf.v35.random.die.DieSides
import dugsolutions.leaf.v35.wisp.WispCardManager
import dugsolutions.leaf.v35.wisp.WispCardRegistry
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GroveFactoryTest {

    private lateinit var factory: GroveFactory

    @BeforeEach
    fun setup() {
        val registry =
            WispCardRegistry(
                GameEffectConverter()
            )

        registry.loadFromCsv(
            dataPath(CardDataFiles.WISP_LIST)
        )

        val manager =
            WispCardManager()

        manager.loadCards(registry)

        factory =
            GroveFactory(manager)
    }

    @Test
    fun invoke_createsInitializedGrove() {
        val grove =
            factory(
                selectedPlantCards =
                    selectedCards(),
                randomizer =
                    ReversingRandomizer()
            )

        assertEquals(
            9,
            grove.plantMarket.size
        )
        assertEquals(
            9,
            grove.graftBed.count(
                DieSides.D6
            )
        )
        assertEquals(
            36,
            grove.wispDeck.remaining
        )
    }

    @Test
    fun invoke_eachTimeCreatesIndependentMutableGrove() {
        val cards = selectedCards()

        val first =
            factory(
                selectedPlantCards = cards,
                randomizer =
                    ReversingRandomizer()
            )

        val second =
            factory(
                selectedPlantCards = cards,
                randomizer =
                    ReversingRandomizer()
            )

        assertTrue(first !== second)
        assertTrue(
            first.wispDeck !==
                second.wispDeck
        )
        assertTrue(
            first.graftBed !==
                second.graftBed
        )
        assertTrue(
            first.plantMarket !==
                second.plantMarket
        )

        first.graftBed.take(
            DieSides.D6
        )

        assertEquals(
            8,
            first.graftBed.count(
                DieSides.D6
            )
        )
        assertEquals(
            9,
            second.graftBed.count(
                DieSides.D6
            )
        )
    }

    private fun selectedCards(): List<PlantCard> =
        listOf(
            plantCard("Root_05_01", PlantType.ROOT, 5),
            plantCard("Root_07_01", PlantType.ROOT, 7),
            plantCard("Root_09_01", PlantType.ROOT, 9),
            plantCard("Vine_07_01", PlantType.VINE, 7),
            plantCard("Vine_09_01", PlantType.VINE, 9),
            plantCard("Vine_11_01", PlantType.VINE, 11),
            plantCard("Flower_11_01", PlantType.FLOWER, 11),
            plantCard("Flower_14_01", PlantType.FLOWER, 14),
            plantCard("Flower_17_01", PlantType.FLOWER, 17)
        )

    private fun plantCard(
        name: String,
        type: PlantType,
        cost: Int
    ): PlantCard =
        PlantCard(
            quantity = 4,
            name = name,
            title = name,
            type = type,
            cost = cost,
            lineIcon = null,
            vpIcon = "",
            typeIcon = "",
            fgColor = "",
            textColor = "",
            fullImage = "",
            backgroundImage = "",
            cardBackgroundImage = "",
            effect = GameEffect.UNKNOWN
        )

    private fun dataPath(
        fileName: String
    ): String =
        Path.of(
            "data",
            "v35",
            fileName
        ).toString()

    private class ReversingRandomizer :
        Randomizer {

        override fun nextBoolean(): Boolean =
            throw UnsupportedOperationException()

        override fun nextInt(
            from: Int,
            until: Int
        ): Int =
            throw UnsupportedOperationException()

        override fun nextInt(
            until: Int
        ): Int =
            throw UnsupportedOperationException()

        override fun <T> randomOrNull(
            list: List<T>
        ): T? =
            throw UnsupportedOperationException()

        override fun <T> shuffled(
            list: List<T>
        ): List<T> =
            list.reversed()
    }
}
