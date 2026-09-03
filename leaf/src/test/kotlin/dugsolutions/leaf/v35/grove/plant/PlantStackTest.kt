package dugsolutions.leaf.v35.grove.plant

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.plant.domain.PlantType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlantStackTest {

    @Test
    fun newStack_usesCardQuantity() {
        val card = card(
            name = "Root_05_01",
            quantity = 4
        )

        val stack = PlantStack(card)

        assertEquals(card, stack.card)
        assertEquals(4, stack.remaining)
        assertFalse(stack.isEmpty)
        assertTrue(stack.isNotEmpty)
    }

    @Test
    fun take_returnsCardAndDecrementsRemaining() {
        val card = card(
            name = "Root_05_01",
            quantity = 2
        )
        val stack = PlantStack(card)

        val result = stack.take()

        assertEquals(card, result)
        assertEquals(1, stack.remaining)
    }

    @Test
    fun take_whenEmpty_returnsNullWithoutGoingNegative() {
        val card = card(
            name = "Root_05_01",
            quantity = 1
        )
        val stack = PlantStack(card)

        assertEquals(card, stack.take())
        assertNull(stack.take())

        assertEquals(0, stack.remaining)
        assertTrue(stack.isEmpty)
        assertFalse(stack.isNotEmpty)
    }

    @Test
    fun returnCard_afterTake_restoresOneCopy() {
        val card = card(
            name = "Root_05_01",
            quantity = 2
        )
        val stack = PlantStack(card)
        stack.take()

        val result = stack.returnCard(card)

        assertTrue(result)
        assertEquals(2, stack.remaining)
    }

    @Test
    fun returnCard_whenStackAlreadyFull_returnsFalse() {
        val card = card(
            name = "Root_05_01",
            quantity = 2
        )
        val stack = PlantStack(card)

        val result = stack.returnCard(card)

        assertFalse(result)
        assertEquals(2, stack.remaining)
    }

    @Test
    fun returnCard_whenDifferentName_returnsFalse() {
        val stackCard = card(
            name = "Root_05_01",
            quantity = 2
        )
        val otherCard = card(
            name = "Root_05_02",
            quantity = 2
        )
        val stack = PlantStack(stackCard)
        stack.take()

        val result =
            stack.returnCard(otherCard)

        assertFalse(result)
        assertEquals(1, stack.remaining)
    }

    @Test
    fun matchingName_isCaseAndWhitespaceInsensitive() {
        val stackCard = card(
            name = "Root_05_01",
            quantity = 2
        )
        val equivalent = card(
            name = "  ROOT_05_01  ",
            quantity = 2
        )
        val stack = PlantStack(stackCard)
        stack.take()

        assertTrue(
            stack.returnCard(equivalent)
        )
        assertEquals(
            2,
            stack.remaining
        )
    }

    @Test
    fun reset_restoresAuthoredQuantity() {
        val card = card(
            name = "Root_05_01",
            quantity = 4
        )
        val stack = PlantStack(card)

        repeat(3) {
            stack.take()
        }

        stack.reset()

        assertEquals(
            4,
            stack.remaining
        )
    }

    private fun card(
        name: String,
        quantity: Int
    ): PlantCard =
        PlantCard(
            quantity = quantity,
            name = name,
            title = name,
            type = PlantType.ROOT,
            cost = 5,
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
