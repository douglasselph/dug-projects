package dugsolutions.leaf.v35.game.operation

import dugsolutions.leaf.v35.chronicle.GameChronicle
import dugsolutions.leaf.v35.chronicle.domain.GameEntry
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.Player
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.creature.CreatureCard
import dugsolutions.leaf.v35.player.creature.CreaturePosition
import dugsolutions.leaf.v35.player.creature.CreatureSide
import dugsolutions.leaf.v35.player.creature.GraftPlacement
import dugsolutions.leaf.v35.player.decision.DecisionDirector
import dugsolutions.leaf.v35.tokens.Butterfly
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RefreshResolverTest {

    private lateinit var chronicle: GameChronicle
    private lateinit var resolver: RefreshResolver
    private lateinit var player: Player

    @BeforeEach
    fun setup() {
        chronicle = GameChronicle()
        resolver = RefreshResolver(chronicle)
        player = newPlayer(1)
    }

    @Test
    fun refreshIfReady_whenAllPlantsFaceDown_turnsAllPlantsFaceUp() {
        // Arrange
        val root = graft(player, root(), -1, -1)
        val vine = graft(player, vine(), 1, 0)

        // Act
        val refreshed = resolver.refreshIfReady(player)

        // Assert
        assertTrue(refreshed)
        assertTrue(player.creature.get(root.id)!!.isFaceUp)
        assertTrue(player.creature.get(vine.id)!!.isFaceUp)
    }

    @Test
    fun refreshIfReady_refreshesButterfliesWithPlants() {
        // Arrange
        graft(player, root(), -1, -1)
        player.butterflies.add(Butterfly.GREEN).add(Butterfly.RED)
        player.butterflies.faceDown(Butterfly.GREEN)
        player.butterflies.faceDown(Butterfly.RED)

        // Act
        resolver.refreshIfReady(player)

        // Assert
        assertTrue(player.butterflies.isFaceUp(Butterfly.GREEN))
        assertTrue(player.butterflies.isFaceUp(Butterfly.RED))
    }

    @Test
    fun refreshIfReady_whenOnePlantFaceUp_doesNothing() {
        // Arrange
        val faceUp = graft(player, root(), -1, -1)
        val faceDown = graft(player, vine(), 1, 0)
        player.creature.faceUp(faceUp.id)
        player.butterflies.add(Butterfly.YELLOW)
        player.butterflies.faceDown(Butterfly.YELLOW)

        // Act
        val refreshed = resolver.refreshIfReady(player)

        // Assert
        assertFalse(refreshed)
        assertTrue(player.creature.get(faceUp.id)!!.isFaceUp)
        assertTrue(player.creature.get(faceDown.id)!!.isFaceDown)
        assertTrue(player.butterflies.isFaceDown(Butterfly.YELLOW))
    }

    @Test
    fun refreshIfReady_whenCreatureEmpty_doesNotRefresh() {
        // Arrange
        player.butterflies.add(Butterfly.PURPLE)
        player.butterflies.faceDown(Butterfly.PURPLE)

        // Act
        val refreshed = resolver.refreshIfReady(player)

        // Assert
        assertFalse(player.creature.allFaceDown)
        assertFalse(refreshed)
        assertTrue(player.butterflies.isFaceDown(Butterfly.PURPLE))
    }

    @Test
    fun refresh_forcedRefreshTurnsMixedFacingPlantsFaceUp() {
        // Arrange
        val faceUp = graft(player, root(), -1, -1)
        val faceDown = graft(player, vine(), 1, 0)
        player.creature.faceUp(faceUp.id)

        // Act
        val refreshed = resolver.refresh(player)

        // Assert
        assertTrue(refreshed)
        assertTrue(player.creature.get(faceUp.id)!!.isFaceUp)
        assertTrue(player.creature.get(faceDown.id)!!.isFaceUp)
    }

    @Test
    fun refresh_forcedRefreshTurnsButterfliesFaceUp() {
        // Arrange
        player.butterflies.add(Butterfly.GREEN)
        player.butterflies.faceDown(Butterfly.GREEN)

        // Act
        val refreshed = resolver.refresh(player)

        // Assert
        assertTrue(refreshed)
        assertTrue(player.butterflies.isFaceUp(Butterfly.GREEN))
    }

    @Test
    fun refresh_whenEverythingAlreadyFaceUp_returnsFalseAndKeepsValidState() {
        // Arrange
        val card = graft(player, root(), -1, -1)
        player.creature.faceUp(card.id)
        player.butterflies.add(Butterfly.RED)

        // Act
        val refreshed = resolver.refresh(player)

        // Assert
        assertFalse(refreshed)
        assertTrue(player.creature.get(card.id)!!.isFaceUp)
        assertTrue(player.butterflies.isFaceUp(Butterfly.RED))
    }

    @Test
    fun refresh_recordsChronicleEntryOnlyWhenStateChanges() {
        // Arrange / Act
        assertFalse(resolver.refresh(player))
        graft(player, root(), -1, -1)
        assertTrue(resolver.refresh(player))
        assertFalse(resolver.refresh(player))

        // Assert
        val entries = chronicle.entries.filterIsInstance<GameEntry.Refresh>()
        assertEquals(1, entries.size)
        assertEquals(PlayerId(1), entries.single().playerId)
    }

    @Test
    fun refresh_onlyAffectsRequestedPlayer() {
        // Arrange
        graft(player, root(), -1, -1)
        val other = newPlayer(2)
        val otherCard = graft(other, root("Other_Root"), -1, -1)
        other.butterflies.add(Butterfly.GREEN)
        other.butterflies.faceDown(Butterfly.GREEN)

        // Act
        resolver.refresh(player)

        // Assert
        assertTrue(player.creature.cards.single().isFaceUp)
        assertTrue(other.creature.get(otherCard.id)!!.isFaceDown)
        assertTrue(other.butterflies.isFaceDown(Butterfly.GREEN))
    }

    private fun newPlayer(id: Int): Player =
        Player(
            id = PlayerId(id),
            decisions = DecisionDirector.baseline()
        )

    private fun graft(
        target: Player,
        card: PlantCard,
        x: Int,
        y: Int
    ): CreatureCard =
        target.creature.graft(
            card,
            GraftPlacement(
                side = if (x < 0) CreatureSide.LEFT else CreatureSide.RIGHT,
                position = CreaturePosition(x, y)
            )
        )

    private fun root(name: String = "Root_Test"): PlantCard =
        card(name, PlantType.ROOT)

    private fun vine(name: String = "Vine_Test"): PlantCard =
        card(name, PlantType.VINE)

    private fun card(name: String, type: PlantType): PlantCard =
        PlantCard(
            quantity = 4,
            name = name,
            title = name,
            type = type,
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
