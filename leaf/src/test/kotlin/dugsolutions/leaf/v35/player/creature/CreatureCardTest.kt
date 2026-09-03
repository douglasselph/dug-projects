package dugsolutions.leaf.v35.player.creature

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.plant.domain.PlantType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CreatureCardTest {

    @Test
    fun defaultFacing_isFaceDown() {
        val result = creatureCard()

        assertTrue(result.isFaceDown)
        assertFalse(result.isFaceUp)
    }

    @Test
    fun faceUp_returnsFaceUpCopyWithoutChangingOriginal() {
        val original = creatureCard()

        val result = original.faceUp()

        assertTrue(result.isFaceUp)
        assertTrue(original.isFaceDown)
        assertEquals(original.id, result.id)
        assertEquals(original.card, result.card)
        assertEquals(original.side, result.side)
        assertEquals(original.position, result.position)
    }

    @Test
    fun faceDown_returnsFaceDownCopyWithoutChangingOriginal() {
        val original = creatureCard(
            facing = CreatureCard.Facing.FACE_UP
        )

        val result = original.faceDown()

        assertTrue(result.isFaceDown)
        assertTrue(original.isFaceUp)
    }

    @Test
    fun flip_changesFacingInBothDirections() {
        val faceDown = creatureCard()

        val faceUp = faceDown.flip()
        val faceDownAgain = faceUp.flip()

        assertTrue(faceUp.isFaceUp)
        assertTrue(faceDownAgain.isFaceDown)
    }

    private fun creatureCard(
        facing: CreatureCard.Facing = CreatureCard.Facing.FACE_DOWN
    ): CreatureCard =
        CreatureCard(
            id = CreatureCardId(7),
            card = plantCard(),
            side = CreatureSide.LEFT,
            position = CreaturePosition(-1, 0),
            facing = facing
        )

    private fun plantCard(): PlantCard =
        PlantCard(
            quantity = 1,
            name = "Vine_Test",
            title = "Test Vine",
            type = PlantType.VINE,
            cost = 7,
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
