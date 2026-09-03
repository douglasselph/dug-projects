package dugsolutions.leaf.v35.grove.plant

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.plant.domain.PlantType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlantMarketTest {

    @Test
    fun constructor_createsNineStacksInIncomingOrder() {
        val cards = selectedCards()

        val market = PlantMarket(cards)

        assertEquals(9, market.size)
        assertEquals(
            cards,
            market.stacks.map { it.card }
        )
    }

    @Test
    fun constructor_requiresExactlyNineCards() {
        assertFailsWith<IllegalArgumentException> {
            PlantMarket(
                selectedCards().dropLast(1)
            )
        }
    }

    @Test
    fun constructor_requiresThreeCardsOfEachType() {
        val invalid =
            selectedCards()
                .toMutableList()

        invalid[8] = plantCard(
            name = "Root_Extra",
            type = PlantType.ROOT,
            cost = 9
        )

        assertFailsWith<IllegalArgumentException> {
            PlantMarket(invalid)
        }
    }

    @Test
    fun constructor_rejectsDuplicateNamesIgnoringCaseAndWhitespace() {
        val cards =
            selectedCards().toMutableList()

        cards[1] = plantCard(
            name = "  ${cards[0].name.uppercase()}  ",
            type = PlantType.ROOT,
            cost = 7
        )

        assertFailsWith<IllegalArgumentException> {
            PlantMarket(cards)
        }
    }

    @Test
    fun everyStack_beginsAtItsCardsQuantity() {
        val cards = selectedCards()

        val market = PlantMarket(cards)

        market.stacks.forEach { stack ->
            assertEquals(
                stack.card.quantity,
                stack.remaining
            )
        }
    }

    @Test
    fun stackFor_findsSelectedCardByStableName() {
        val cards = selectedCards()
        val market = PlantMarket(cards)
        val target = cards[4]

        assertEquals(
            target,
            market.stackFor(
                "  ${target.name.uppercase()}  "
            )?.card
        )
    }

    @Test
    fun stackFor_unknownCard_returnsNull() {
        val market =
            PlantMarket(selectedCards())

        assertNull(
            market.stackFor("Not_Selected")
        )
    }

    @Test
    fun take_decrementsOnlyMatchingStack() {
        val cards = selectedCards()
        val market = PlantMarket(cards)
        val target = cards[4]
        val targetBefore =
            market.stackFor(target)!!.remaining
        val other = cards[0]
        val otherBefore =
            market.stackFor(other)!!.remaining

        val result =
            market.take(target)

        assertEquals(target, result)
        assertEquals(
            targetBefore - 1,
            market.stackFor(target)!!.remaining
        )
        assertEquals(
            otherBefore,
            market.stackFor(other)!!.remaining
        )
    }

    @Test
    fun take_untilEmpty_thenReturnsNull() {
        val cards = selectedCards()
        val market = PlantMarket(cards)
        val target = cards[0]

        repeat(target.quantity) {
            assertEquals(
                target,
                market.take(target)
            )
        }

        assertNull(
            market.take(target)
        )
        assertTrue(
            market.stackFor(target)!!.isEmpty
        )
    }

    @Test
    fun availableStacks_excludesEmptyStack() {
        val cards = selectedCards()
        val market = PlantMarket(cards)
        val target = cards[0]

        repeat(target.quantity) {
            market.take(target)
        }

        assertFalse(
            market.availableStacks.any {
                it.card.name == target.name
            }
        )
        assertEquals(
            8,
            market.availableStacks.size
        )
    }

    @Test
    fun returnCard_restoresMatchingStack() {
        val cards = selectedCards()
        val market = PlantMarket(cards)
        val target = cards[0]

        market.take(target)

        val result =
            market.returnCard(target)

        assertTrue(result)
        assertEquals(
            target.quantity,
            market.stackFor(target)!!.remaining
        )
    }

    @Test
    fun returnCard_unknownCard_returnsFalse() {
        val market =
            PlantMarket(selectedCards())

        val unknown = plantCard(
            name = "Root_Not_Selected",
            type = PlantType.ROOT,
            cost = 5
        )

        assertFalse(
            market.returnCard(unknown)
        )
    }

    @Test
    fun reset_restoresAllStackQuantities() {
        val cards = selectedCards()
        val market = PlantMarket(cards)

        cards.take(4).forEach {
            market.take(it)
        }

        market.reset()

        market.stacks.forEach {
            assertEquals(
                it.card.quantity,
                it.remaining
            )
        }
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
}
