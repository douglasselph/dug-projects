package dugsolutions.leaf.v35.grove

import dugsolutions.leaf.v35.common.CardDataFiles
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.GameEffectConverter
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.random.Randomizer
import dugsolutions.leaf.v35.random.die.DieSides
import dugsolutions.leaf.v35.tokens.Butterfly
import dugsolutions.leaf.v35.tokens.Critter
import dugsolutions.leaf.v35.tokens.Token
import dugsolutions.leaf.v35.wisp.WispCardManager
import dugsolutions.leaf.v35.wisp.WispCardRegistry
import dugsolutions.leaf.v35.wisp.WispDeck
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GroveTest {

    private lateinit var wispManager: WispCardManager

    @BeforeEach
    fun setup() {
        val registry =
            WispCardRegistry(
                GameEffectConverter()
            )

        registry.loadFromCsv(
            dataPath(CardDataFiles.WISP_LIST)
        )

        wispManager =
            WispCardManager()

        wispManager.loadCards(registry)
    }

    @Test
    fun newGrove_hasCompleteV35StartingState() {
        val grove = grove()

        assertEquals(
            9,
            grove.plantMarket.size
        )

        assertEquals(
            0,
            grove.graftBed.count(DieSides.D4)
        )

        DieSides.entries
            .filterNot { it == DieSides.D4 }
            .forEach {
                assertEquals(
                    9,
                    grove.graftBed.count(it)
                )
            }

        assertEquals(
            9,
            grove.critters.count(Critter.BEE)
        )
        assertEquals(
            9,
            grove.critters.count(Critter.WORM)
        )

        assertEquals(
            9,
            grove.tokens.waterCount
        )
        assertEquals(
            9,
            grove.tokens.mulchCount
        )
        assertEquals(
            0,
            grove.tokens.pendingMulchCount
        )

        assertEquals(
            Butterfly.entries.toSet(),
            grove.butterflies.all.toSet()
        )
        assertEquals(
            4,
            grove.butterflies.size
        )

        assertEquals(
            36,
            grove.wispDeck.remaining
        )
    }

    @Test
    fun newGrove_plantStacksUseAuthoredQuantity() {
        val cards = selectedCards()
        val grove = grove(cards)

        grove.plantMarket.stacks.forEach {
            assertEquals(
                it.card.quantity,
                it.remaining
            )
        }
    }

    @Test
    fun reset_restoresEveryOwnedComponent() {
        val cards = selectedCards()
        val grove = grove(cards)

        grove.plantMarket.take(cards[0])
        grove.graftBed.take(DieSides.D8)
        grove.graftBed.returnD4()

        grove.critters.remove(Critter.BEE)
        grove.tokens.pull(Token.WATER)
        grove.tokens.pull(Token.MULCH())

        grove.butterflies.remove(
            Butterfly.GREEN
        )

        repeat(5) {
            grove.wispDeck.draw()
        }

        grove.reset()

        grove.plantMarket.stacks.forEach {
            assertEquals(
                it.card.quantity,
                it.remaining
            )
        }

        assertEquals(
            0,
            grove.graftBed.count(DieSides.D4)
        )
        assertEquals(
            9,
            grove.graftBed.count(DieSides.D8)
        )

        assertEquals(
            9,
            grove.critters.count(Critter.BEE)
        )
        assertEquals(
            9,
            grove.critters.count(Critter.WORM)
        )

        assertEquals(
            9,
            grove.tokens.waterCount
        )
        assertEquals(
            9,
            grove.tokens.mulchCount
        )

        assertTrue(
            grove.butterflies.all.contains(
                Butterfly.GREEN
            )
        )

        assertEquals(
            36,
            grove.wispDeck.remaining
        )
    }

    @Test
    fun reset_preservesOriginalSelectedPlantDefinitions() {
        val cards = selectedCards()
        val grove = grove(cards)

        grove.plantMarket.take(cards[0])

        grove.reset()

        assertEquals(
            cards,
            grove.plantMarket.stacks
                .map { it.card }
        )
    }

    @Test
    fun separateGroves_doNotShareMutableState() {
        val first = grove()
        val second = grove()

        first.graftBed.take(DieSides.D20)
        first.critters.remove(Critter.BEE)
        first.tokens.pull(Token.WATER)
        first.butterflies.remove(
            Butterfly.PURPLE
        )
        first.wispDeck.draw()

        assertEquals(
            8,
            first.graftBed.count(DieSides.D20)
        )
        assertEquals(
            9,
            second.graftBed.count(DieSides.D20)
        )

        assertEquals(
            8,
            first.critters.count(Critter.BEE)
        )
        assertEquals(
            9,
            second.critters.count(Critter.BEE)
        )

        assertEquals(
            8,
            first.tokens.waterCount
        )
        assertEquals(
            9,
            second.tokens.waterCount
        )

        assertFalse(
            first.butterflies.all.contains(
                Butterfly.PURPLE
            )
        )
        assertTrue(
            second.butterflies.all.contains(
                Butterfly.PURPLE
            )
        )

        assertEquals(
            35,
            first.wispDeck.remaining
        )
        assertEquals(
            36,
            second.wispDeck.remaining
        )
    }

    private fun grove(
        cards: List<PlantCard> = selectedCards()
    ): Grove =
        Grove(
            selectedPlantCards = cards,
            wispDeck = WispDeck(
                wispCardManager = wispManager,
                randomizer =
                    ReversingRandomizer()
            )
        )

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
