package dugsolutions.leaf.v35.player.creature

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.plant.domain.PlantType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CreatureTest {

    @Test
    fun newCreature_isEmpty() {
        val creature = Creature()

        assertTrue(creature.isEmpty)
        assertEquals(0, creature.size)
        assertEquals(emptyList(), creature.cards)
        assertFalse(creature.allFaceDown)
    }

    @Test
    fun initialRootPlacements_areDirectlyLeftAndRightOfDiceSupply() {
        val creature = Creature()

        val result = creature.legalPlacements(root())

        assertEquals(
            setOf(
                placement(CreatureSide.LEFT, -1, -1),
                placement(CreatureSide.RIGHT, 1, -1)
            ),
            result.toSet()
        )
    }

    @Test
    fun initialVinePlacements_areDirectlyLeftAndRightOfPlantCore() {
        val creature = Creature()

        val result = creature.legalPlacements(vine())

        assertEquals(
            setOf(
                placement(CreatureSide.LEFT, -1, 0),
                placement(CreatureSide.RIGHT, 1, 0)
            ),
            result.toSet()
        )
    }

    @Test
    fun flower_hasNoLegalPlacementUntilAVineExists() {
        val creature = Creature()

        assertEquals(
            emptyList(),
            creature.legalPlacements(flower())
        )
    }

    @Test
    fun graft_createsFaceDownCreatureCardWithSequentialIdentity() {
        val creature = Creature()

        val left = creature.graft(
            root("Root_A"),
            placement(CreatureSide.LEFT, -1, -1)
        )
        val right = creature.graft(
            root("Root_B"),
            placement(CreatureSide.RIGHT, 1, -1)
        )

        assertEquals(CreatureCardId(1), left.id)
        assertEquals(CreatureCardId(2), right.id)
        assertTrue(left.isFaceDown)
        assertTrue(right.isFaceDown)
        assertEquals(2, creature.size)
        assertTrue(creature.allFaceDown)
    }

    @Test
    fun graft_rejectsIllegalPlacement() {
        val creature = Creature()

        assertFailsWith<IllegalArgumentException> {
            creature.graft(
                root(),
                placement(CreatureSide.LEFT, 0, 0)
            )
        }
    }

    @Test
    fun canGraft_requiresBothLegalPositionAndCorrectSide() {
        val creature = Creature()
        val vine = vine()

        assertTrue(
            creature.canGraft(
                vine,
                placement(CreatureSide.LEFT, -1, 0)
            )
        )

        assertFalse(
            creature.canGraft(
                vine,
                placement(CreatureSide.LEFT, 1, 0)
            )
        )
    }

    @Test
    fun roots_extendHorizontallyFromExistingRoot() {
        val creature = Creature()
        creature.graft(
            root("Root_A"),
            placement(CreatureSide.LEFT, -1, -1)
        )

        val result = creature.legalPlacements(root("Root_B"))

        assertEquals(
            setOf(
                placement(CreatureSide.LEFT, -2, -1),
                placement(CreatureSide.RIGHT, 1, -1)
            ),
            result.toSet()
        )
    }

    @Test
    fun vines_extendLeftRightOrAboveExistingVine() {
        val creature = Creature()
        creature.graft(
            vine("Vine_A"),
            placement(CreatureSide.LEFT, -1, 0)
        )

        val result = creature.legalPlacements(vine("Vine_B"))

        assertEquals(
            setOf(
                placement(CreatureSide.LEFT, -2, 0),
                placement(CreatureSide.LEFT, -1, 1),
                placement(CreatureSide.RIGHT, 1, 0)
            ),
            result.toSet()
        )
    }

    @Test
    fun flowers_mayGraftLeftRightOrAboveVineWhereSpaceIsOpen() {
        val creature = Creature()
        creature.graft(
            vine(),
            placement(CreatureSide.LEFT, -1, 0)
        )

        val result = creature.legalPlacements(flower())

        assertEquals(
            setOf(
                placement(CreatureSide.LEFT, -2, 0),
                placement(CreatureSide.LEFT, -1, 1)
            ),
            result.toSet()
        )
    }

    @Test
    fun occupiedPosition_isRemovedFromFutureLegalPlacements() {
        val creature = Creature()
        creature.graft(
            vine("Vine_A"),
            placement(CreatureSide.LEFT, -1, 0)
        )
        creature.graft(
            flower("Flower_A"),
            placement(CreatureSide.LEFT, -1, 1)
        )

        val vinePlacements = creature.legalPlacements(vine("Vine_B"))
        val flowerPlacements = creature.legalPlacements(flower("Flower_B"))

        assertFalse(
            placement(CreatureSide.LEFT, -1, 1) in vinePlacements
        )
        assertFalse(
            placement(CreatureSide.LEFT, -1, 1) in flowerPlacements
        )
    }

    @Test
    fun flower_isEndpointAndDoesNotGenerateNewPlacements() {
        val creature = Creature()
        creature.graft(
            vine(),
            placement(CreatureSide.LEFT, -1, 0)
        )
        creature.graft(
            flower(),
            placement(CreatureSide.LEFT, -1, 1)
        )

        val result = creature.legalPlacements(flower("Flower_B"))

        assertFalse(
            placement(CreatureSide.LEFT, -1, 2) in result
        )
        assertFalse(
            placement(CreatureSide.LEFT, -2, 1) in result
        )
        assertFalse(
            placement(CreatureSide.LEFT, 0, 1) in result
        )
    }

    @Test
    fun cards_areGroupedBySideAndPlantType() {
        val creature = Creature()

        val leftRoot = creature.graft(
            root(),
            placement(CreatureSide.LEFT, -1, -1)
        )
        val rightVine = creature.graft(
            vine(),
            placement(CreatureSide.RIGHT, 1, 0)
        )
        val rightFlower = creature.graft(
            flower(),
            placement(CreatureSide.RIGHT, 1, 1)
        )

        assertEquals(listOf(leftRoot), creature.leftCards)
        assertEquals(
            listOf(rightVine, rightFlower),
            creature.rightCards
        )
        assertEquals(listOf(leftRoot), creature.roots)
        assertEquals(listOf(rightVine), creature.vines)
        assertEquals(listOf(rightFlower), creature.flowers)
    }

    @Test
    fun get_returnsCreatureCardByRuntimeIdentity() {
        val creature = Creature()
        val grafted = creature.graft(
            root(),
            placement(CreatureSide.LEFT, -1, -1)
        )

        assertEquals(grafted, creature.get(grafted.id))
        assertNull(creature.get(CreatureCardId(999)))
    }

    @Test
    fun duplicatePlantDefinitions_receiveDifferentCreatureCardIds() {
        val creature = Creature()
        val sameDefinition = root()

        val first = creature.graft(
            sameDefinition,
            placement(CreatureSide.LEFT, -1, -1)
        )
        val second = creature.graft(
            sameDefinition,
            placement(CreatureSide.RIGHT, 1, -1)
        )

        assertEquals(first.card, second.card)
        assertFalse(first.id == second.id)
    }

    @Test
    fun faceOperations_updateStoredCreatureCard() {
        val creature = Creature()
        val grafted = creature.graft(
            root(),
            placement(CreatureSide.LEFT, -1, -1)
        )

        assertTrue(creature.faceUp(grafted.id))
        assertTrue(creature.get(grafted.id)!!.isFaceUp)
        assertFalse(creature.allFaceDown)

        assertTrue(creature.flip(grafted.id))
        assertTrue(creature.get(grafted.id)!!.isFaceDown)
        assertTrue(creature.allFaceDown)

        assertTrue(creature.faceUp(grafted.id))
        assertTrue(creature.faceDown(grafted.id))
        assertTrue(creature.get(grafted.id)!!.isFaceDown)
    }

    @Test
    fun faceOperations_onMissingCard_returnFalse() {
        val creature = Creature()
        val missing = CreatureCardId(99)

        assertFalse(creature.faceUp(missing))
        assertFalse(creature.faceDown(missing))
        assertFalse(creature.flip(missing))
    }

    @Test
    fun faceUpAll_turnsEveryGraftedCardFaceUp() {
        val creature = Creature()

        val root = creature.graft(
            root(),
            placement(CreatureSide.LEFT, -1, -1)
        )
        val vine = creature.graft(
            vine(),
            placement(CreatureSide.RIGHT, 1, 0)
        )

        creature.faceUpAll()

        assertTrue(creature.get(root.id)!!.isFaceUp)
        assertTrue(creature.get(vine.id)!!.isFaceUp)
        assertFalse(creature.allFaceDown)
    }

    @Test
    fun outerRoot_isSnippableButInnerRootIsNot() {
        val creature = Creature()

        val inner = creature.graft(
            root("Root_1"),
            placement(CreatureSide.LEFT, -1, -1)
        )
        val middle = creature.graft(
            root("Root_2"),
            placement(CreatureSide.LEFT, -2, -1)
        )
        val outer = creature.graft(
            root("Root_3"),
            placement(CreatureSide.LEFT, -3, -1)
        )

        assertFalse(creature.canSnip(inner.id))
        assertFalse(creature.canSnip(middle.id))
        assertTrue(creature.canSnip(outer.id))
        assertEquals(listOf(outer), creature.snippableCards)
    }

    @Test
    fun snippingOuterRoot_makesNextRootOuter() {
        val creature = Creature()

        val inner = creature.graft(
            root("Root_1"),
            placement(CreatureSide.LEFT, -1, -1)
        )
        val outer = creature.graft(
            root("Root_2"),
            placement(CreatureSide.LEFT, -2, -1)
        )

        assertEquals(outer, creature.snip(outer.id))
        assertTrue(creature.canSnip(inner.id))
        assertEquals(1, creature.size)
    }

    @Test
    fun vineSupportingAnotherVine_isNotSnippable() {
        val creature = Creature()

        val inner = creature.graft(
            vine("Vine_1"),
            placement(CreatureSide.LEFT, -1, 0)
        )
        val outer = creature.graft(
            vine("Vine_2"),
            placement(CreatureSide.LEFT, -2, 0)
        )

        assertFalse(creature.canSnip(inner.id))
        assertTrue(creature.canSnip(outer.id))
    }

    @Test
    fun vineSupportingFlower_isNotSnippableUntilFlowerIsRemoved() {
        val creature = Creature()

        creature.graft(
            vine("Vine_1"),
            placement(CreatureSide.LEFT, -1, 0)
        )
        val supportingVine = creature.graft(
            vine("Vine_2"),
            placement(CreatureSide.LEFT, -2, 0)
        )
        val flower = creature.graft(
            flower(),
            placement(CreatureSide.LEFT, -2, 1)
        )

        assertFalse(creature.canSnip(supportingVine.id))
        assertTrue(creature.canSnip(flower.id))

        assertEquals(flower, creature.snip(flower.id))
        assertTrue(creature.canSnip(supportingVine.id))
    }

    @Test
    fun snip_nonOuterCard_returnsNullAndLeavesCreatureUnchanged() {
        val creature = Creature()

        val inner = creature.graft(
            root("Root_1"),
            placement(CreatureSide.LEFT, -1, -1)
        )
        creature.graft(
            root("Root_2"),
            placement(CreatureSide.LEFT, -2, -1)
        )

        val result = creature.snip(inner.id)

        assertNull(result)
        assertEquals(2, creature.size)
        assertEquals(inner, creature.get(inner.id))
    }

    @Test
    fun snip_missingCard_returnsNull() {
        val creature = Creature()

        assertNull(creature.snip(CreatureCardId(99)))
    }

    @Test
    fun cardsGetter_returnsDefensiveSnapshot() {
        val creature = Creature()
        creature.graft(
            root("Root_1"),
            placement(CreatureSide.LEFT, -1, -1)
        )
        val snapshot = creature.cards

        creature.graft(
            root("Root_2"),
            placement(CreatureSide.RIGHT, 1, -1)
        )

        assertEquals(1, snapshot.size)
        assertEquals(2, creature.cards.size)
    }

    @Test
    fun clear_removesCardsAndRestartsCreatureCardIds() {
        val creature = Creature()

        creature.graft(
            root(),
            placement(CreatureSide.LEFT, -1, -1)
        )

        creature.clear()

        assertTrue(creature.isEmpty)
        assertEquals(0, creature.size)
        assertFalse(creature.allFaceDown)

        val firstAfterClear = creature.graft(
            vine(),
            placement(CreatureSide.RIGHT, 1, 0)
        )

        assertEquals(
            CreatureCardId(1),
            firstAfterClear.id
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

    private fun root(
        name: String = "Root_Test"
    ): PlantCard =
        plantCard(
            name = name,
            type = PlantType.ROOT,
            cost = 5
        )

    private fun vine(
        name: String = "Vine_Test"
    ): PlantCard =
        plantCard(
            name = name,
            type = PlantType.VINE,
            cost = 7
        )

    private fun flower(
        name: String = "Flower_Test"
    ): PlantCard =
        plantCard(
            name = name,
            type = PlantType.FLOWER,
            cost = 11
        )

    private fun plantCard(
        name: String,
        type: PlantType,
        cost: Int
    ): PlantCard =
        PlantCard(
            quantity = 1,
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
