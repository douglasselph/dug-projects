package dugsolutions.leaf.v35.game.operation

import dugsolutions.leaf.v35.chronicle.GameChronicle
import dugsolutions.leaf.v35.chronicle.domain.GameEntry
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.Player
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.creature.CreaturePosition
import dugsolutions.leaf.v35.player.creature.CreatureSide
import dugsolutions.leaf.v35.player.creature.GraftPlacement
import dugsolutions.leaf.v35.player.decision.DecisionDirector
import dugsolutions.leaf.v35.player.decision.placement.ChooseCreaturePlacementRequest
import dugsolutions.leaf.v35.player.decision.placement.CreaturePlacementStrategy
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GraftResolverTest {

    @Test
    fun resolve_graftsRootAtChosenLegalPlacementFaceDown() {
        val chronicle = GameChronicle()
        val chosen = GraftPlacement(CreatureSide.RIGHT, CreaturePosition(1, -1))
        val player = player(FixedPlacementStrategy(chosen))

        val result = GraftResolver(chronicle).resolve(player, card(PlantType.ROOT))!!

        assertEquals(chosen, GraftPlacement(result.side, result.position))
        assertTrue(result.isFaceDown)
        assertEquals(result, player.creature.get(result.id))
    }

    @Test
    fun resolve_graftsVineAndThenFlowerUsingCreatureGeometry() {
        val chronicle = GameChronicle()
        val vinePosition = GraftPlacement(CreatureSide.LEFT, CreaturePosition(-1, 0))
        val player = player(FixedPlacementStrategy(vinePosition))
        val resolver = GraftResolver(chronicle)
        resolver.resolve(player, card(PlantType.VINE))
        val flowerPosition = GraftPlacement(CreatureSide.LEFT, CreaturePosition(-1, 1))
        val flowerPlayer = player.copyDecisions(FixedPlacementStrategy(flowerPosition))

        val flower = resolver.resolve(flowerPlayer, card(PlantType.FLOWER))

        assertEquals(flowerPosition.position, flower!!.position)
    }

    @Test
    fun prepare_whenStrategyReturnsIllegalPlacement_rejectsBeforeMutation() {
        val player = player(
            FixedPlacementStrategy(
                GraftPlacement(CreatureSide.LEFT, CreaturePosition(99, 99))
            )
        )

        assertFailsWith<IllegalStateException> {
            GraftResolver(GameChronicle()).prepare(player, card(PlantType.ROOT))
        }

        assertTrue(player.creature.isEmpty)
    }

    @Test
    fun prepare_whenNoLegalFlowerPlacement_returnsNullWithoutAskingStrategy() {
        val strategy = FixedPlacementStrategy(
            GraftPlacement(CreatureSide.LEFT, CreaturePosition(-1, 1))
        )
        val player = player(strategy)

        val result = GraftResolver(GameChronicle()).prepare(player, card(PlantType.FLOWER))

        assertNull(result)
        assertEquals(0, strategy.calls)
    }

    @Test
    fun resolve_recordsSuccessfulGraftOnlyAfterMutation() {
        val chronicle = GameChronicle()
        val player = player(
            FixedPlacementStrategy(
                GraftPlacement(CreatureSide.LEFT, CreaturePosition(-1, -1))
            )
        )

        GraftResolver(chronicle).resolve(player, card(PlantType.ROOT, "Root_A"))

        val marker = chronicle.entries.filterIsInstance<GameEntry.Marker>().single()
        assertEquals("GRAFT player=1 plant=Root_A", marker.message)
    }

    private fun player(strategy: CreaturePlacementStrategy): Player = Player(
        PlayerId(1),
        DecisionDirector.baseline().copy(placement = strategy)
    )

    private fun Player.copyDecisions(strategy: CreaturePlacementStrategy): Player =
        Player(id, decisions.copy(placement = strategy), creature = creature)

    private class FixedPlacementStrategy(
        private val placement: GraftPlacement
    ) : CreaturePlacementStrategy {
        var calls = 0
        override fun choose(request: ChooseCreaturePlacementRequest): GraftPlacement {
            calls++
            return placement
        }
    }

    private fun card(type: PlantType, name: String = type.name) = PlantCard(
        quantity = 1,
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
