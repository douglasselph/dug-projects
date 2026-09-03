package dugsolutions.leaf.v35.player.decision.wound

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.creature.CreatureCard
import dugsolutions.leaf.v35.player.creature.CreatureCardId
import dugsolutions.leaf.v35.player.creature.CreaturePosition
import dugsolutions.leaf.v35.player.creature.CreatureSide
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BaselineWoundStrategyTest {

    private val strategy = BaselineWoundStrategy()

    @Test
    fun choose_returnsFirstLegalChoice() {
        val first = WoundChoice.Flip(
            creatureCard(
                id = 1,
                name = "First",
                facing = CreatureCard.Facing.FACE_UP
            )
        )
        val second = WoundChoice.Flip(
            creatureCard(
                id = 2,
                name = "Second",
                facing = CreatureCard.Facing.FACE_UP
            )
        )

        val result = strategy.choose(
            ChooseWoundRequest(
                legalChoices = listOf(first, second)
            )
        )

        assertEquals(first, result)
    }

    @Test
    fun choose_canReturnSnipChoice() {
        val snip = WoundChoice.Snip(
            creatureCard(
                id = 3,
                name = "Outer",
                facing = CreatureCard.Facing.FACE_DOWN
            )
        )

        val result = strategy.choose(
            ChooseWoundRequest(
                legalChoices = listOf(snip)
            )
        )

        assertEquals(snip, result)
    }

    @Test
    fun request_whenNoLegalChoices_throws() {
        assertFailsWith<IllegalArgumentException> {
            ChooseWoundRequest(
                legalChoices = emptyList()
            )
        }
    }

    @Test
    fun request_defensivelyCopiesLegalChoices() {
        val first = WoundChoice.Flip(
            creatureCard(
                id = 1,
                name = "First",
                facing = CreatureCard.Facing.FACE_UP
            )
        )
        val incoming = mutableListOf<WoundChoice>(first)

        val request = ChooseWoundRequest(incoming)
        incoming.clear()

        assertEquals(
            listOf(first),
            request.legalChoices
        )
    }

    private fun creatureCard(
        id: Int,
        name: String,
        facing: CreatureCard.Facing
    ): CreatureCard =
        CreatureCard(
            id = CreatureCardId(id),
            card = plantCard(name),
            side = CreatureSide.LEFT,
            position = CreaturePosition(-id, 0),
            facing = facing
        )

    private fun plantCard(
        name: String
    ): PlantCard =
        PlantCard(
            quantity = 1,
            name = name,
            title = name,
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
