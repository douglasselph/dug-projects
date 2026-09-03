package dugsolutions.leaf.v35.player.creature

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class CreatureGeometryTest {

    @Test
    fun move_left_decrementsX() {
        assertEquals(
            CreaturePosition(2, 4),
            CreaturePosition(3, 4).move(GraftDirection.LEFT)
        )
    }

    @Test
    fun move_right_incrementsX() {
        assertEquals(
            CreaturePosition(4, 4),
            CreaturePosition(3, 4).move(GraftDirection.RIGHT)
        )
    }

    @Test
    fun move_above_incrementsY() {
        assertEquals(
            CreaturePosition(3, 5),
            CreaturePosition(3, 4).move(GraftDirection.ABOVE)
        )
    }

    @Test
    fun graftPlacement_preservesSideAndPosition() {
        val placement = GraftPlacement(
            side = CreatureSide.LEFT,
            position = CreaturePosition(-2, 1)
        )

        assertEquals(CreatureSide.LEFT, placement.side)
        assertEquals(CreaturePosition(-2, 1), placement.position)
    }
}
