package dugsolutions.leaf.v35.player.decision.placement

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.creature.CreaturePosition
import dugsolutions.leaf.v35.player.creature.CreatureSide
import dugsolutions.leaf.v35.player.creature.GraftPlacement
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BaselineCreaturePlacementStrategyTest {

    private val strategy =
        BaselineCreaturePlacementStrategy()

    @Test
    fun choose_returnsFirstLegalPlacement() {
        val first = placement(
            CreatureSide.LEFT,
            -1,
            -1
        )
        val second = placement(
            CreatureSide.RIGHT,
            1,
            -1
        )

        val result = strategy.choose(
            ChooseCreaturePlacementRequest(
                card = root(),
                legalPlacements = listOf(
                    first,
                    second
                )
            )
        )

        assertEquals(first, result)
    }

    @Test
    fun request_preservesCard() {
        val card = root()
        val legal = listOf(
            placement(
                CreatureSide.LEFT,
                -1,
                -1
            )
        )

        val request =
            ChooseCreaturePlacementRequest(
                card = card,
                legalPlacements = legal
            )

        assertEquals(card, request.card)
    }

    @Test
    fun request_whenNoLegalPlacements_throws() {
        assertFailsWith<IllegalArgumentException> {
            ChooseCreaturePlacementRequest(
                card = root(),
                legalPlacements = emptyList()
            )
        }
    }

    @Test
    fun request_defensivelyCopiesLegalPlacements() {
        val first = placement(
            CreatureSide.LEFT,
            -1,
            -1
        )
        val incoming = mutableListOf(first)

        val request =
            ChooseCreaturePlacementRequest(
                card = root(),
                legalPlacements = incoming
            )

        incoming.clear()

        assertEquals(
            listOf(first),
            request.legalPlacements
        )
    }

    private fun placement(
        side: CreatureSide,
        x: Int,
        y: Int
    ): GraftPlacement =
        GraftPlacement(
            side = side,
            position = CreaturePosition(x, y)
        )

    private fun root(): PlantCard =
        PlantCard(
            quantity = 1,
            name = "Root_Test",
            title = "Root Test",
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
