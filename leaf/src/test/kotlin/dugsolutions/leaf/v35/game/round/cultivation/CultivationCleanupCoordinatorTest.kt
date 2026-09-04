package dugsolutions.leaf.v35.game.round.cultivation

import dugsolutions.leaf.v35.chronicle.domain.GameEntry
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.game.Game
import dugsolutions.leaf.v35.game.GameEngineTestFixture
import dugsolutions.leaf.v35.game.operation.RefreshResolver
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.Player
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.creature.CreaturePosition
import dugsolutions.leaf.v35.player.creature.CreatureSide
import dugsolutions.leaf.v35.player.creature.GraftPlacement
import dugsolutions.leaf.v35.player.decision.DecisionDirector
import dugsolutions.leaf.v35.player.dice.PlayerDice
import dugsolutions.leaf.v35.random.die.Die
import dugsolutions.leaf.v35.random.die.DieSides
import dugsolutions.leaf.v35.tokens.Butterfly
import dugsolutions.leaf.v35.tokens.Critter
import dugsolutions.leaf.v35.tokens.Token
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CultivationCleanupCoordinatorTest {

    @Test
    fun execute_discardsEveryRemainingHandDieForEveryPlayer() {
        val first = player(1, listOf(die(6, 4), die(8, 7)))
        val second = player(2, listOf(die(4, 3)))
        val fixture = fixture(first, second)

        val result = fixture.coordinator.execute(fixture.game)

        assertTrue(first.dice.hand.isEmpty())
        assertTrue(second.dice.hand.isEmpty())
        assertEquals(2, first.dice.discardSize)
        assertEquals(1, second.dice.discardSize)
        assertEquals(3, result.totalDiscardedDice)
        assertEquals(
            listOf(2, 1),
            result.players.map { it.discardedDice }
        )
    }

    @Test
    fun execute_whenAllPlantsFaceDown_refreshesPlantsAndButterfliesTogether() {
        val first = player(1, emptyList())
        val left = graftVine(first, "Left", CreatureSide.LEFT, -1)
        val right = graftVine(first, "Right", CreatureSide.RIGHT, 1)
        first.butterflies.add(Butterfly.GREEN)
        first.butterflies.faceDown(Butterfly.GREEN)
        val fixture = fixture(first, player(2, emptyList()))

        val result = fixture.coordinator.execute(fixture.game)

        assertTrue(first.creature.get(left.id)!!.isFaceUp)
        assertTrue(first.creature.get(right.id)!!.isFaceUp)
        assertTrue(first.butterflies.isFaceUp(Butterfly.GREEN))
        assertEquals(listOf(first.id), result.refreshedPlayers)
    }

    @Test
    fun execute_whenAnyPlantRemainsFaceUp_doesNotRefreshGroup() {
        val first = player(1, emptyList())
        val left = graftVine(first, "Left", CreatureSide.LEFT, -1)
        val right = graftVine(first, "Right", CreatureSide.RIGHT, 1)
        first.creature.faceUp(left.id)
        first.butterflies.add(Butterfly.GREEN)
        first.butterflies.faceDown(Butterfly.GREEN)
        val fixture = fixture(first, player(2, emptyList()))

        val result = fixture.coordinator.execute(fixture.game)

        assertTrue(first.creature.get(left.id)!!.isFaceUp)
        assertTrue(first.creature.get(right.id)!!.isFaceDown)
        assertTrue(first.butterflies.isFaceDown(Butterfly.GREEN))
        assertFalse(result.players.first().refreshed)
    }

    @Test
    fun execute_emptyCreatureDoesNotRefreshButterflyByCleanupCondition() {
        val first = player(1, emptyList())
        first.butterflies.add(Butterfly.PURPLE)
        first.butterflies.faceDown(Butterfly.PURPLE)
        val fixture = fixture(first, player(2, emptyList()))

        val result = fixture.coordinator.execute(fixture.game)

        assertTrue(first.butterflies.isFaceDown(Butterfly.PURPLE))
        assertFalse(result.players.first().refreshed)
    }

    @Test
    fun execute_normalizesPendingMulchForUseBeginningNextRound() {
        val first = player(1, emptyList())
        first.tokens.add(Token.MULCH(DieSides.D6))
        first.tokens.add(Token.PENDING_MULCH(DieSides.D10))
        val fixture = fixture(first, player(2, emptyList()))

        fixture.coordinator.execute(fixture.game)

        assertEquals(0, first.tokens.pendingMulchCount)
        assertEquals(2, first.tokens.mulchCount)
        assertEquals(
            listOf(
                Token.MULCH(DieSides.D6),
                Token.MULCH(DieSides.D10)
            ),
            first.tokens.mulchTokens
        )
    }


    @Test
    fun execute_clearsTemporaryCritterValuesAfterBuyWindowEnds() {
        val first = player(1, emptyList())
        first.critterValues.boostForRound(Critter.WORM, 2)
        first.critterValues.setForRound(Critter.BEE, 4)
        val fixture = fixture(first, player(2, emptyList()))

        fixture.coordinator.execute(fixture.game)

        assertEquals(1, first.critterValues.valueOf(Critter.WORM))
        assertEquals(2, first.critterValues.valueOf(Critter.BEE))
        assertTrue(first.critterValues.overrides.isEmpty())
    }

    @Test
    fun execute_processesPlayersInSeatingOrderAndRecordsOutcome() {
        val first = player(1, listOf(die(6, 3)))
        val second = player(2, listOf(die(8, 5), die(10, 9)))
        val fixture = fixture(first, second)

        val result = fixture.coordinator.execute(fixture.game)

        assertEquals(listOf(first.id, second.id), result.players.map { it.playerId })
        val messages = markerMessages(fixture.game)
            .filter { it.startsWith("CULTIVATION_CLEANUP") }
        assertEquals(2, messages.size)
        assertTrue(messages[0].contains("player=1 discarded=1"))
        assertTrue(messages[1].contains("player=2 discarded=2"))
    }

    private fun fixture(first: Player, second: Player): Fixture {
        val game = GameEngineTestFixture.game(
            cultivationRounds = 1,
            battleRounds = 0,
            players = listOf(first, second)
        )
        return Fixture(
            game = game,
            coordinator = CultivationCleanupCoordinator(
                RefreshResolver(game.chronicle)
            )
        )
    }

    private fun player(id: Int, hand: List<Die>): Player =
        Player(
            id = PlayerId(id),
            decisions = DecisionDirector.baseline(),
            dice = PlayerDice(hand = hand)
        )

    private fun graftVine(
        player: Player,
        name: String,
        side: CreatureSide,
        x: Int
    ) = player.creature.graft(
        plant(name),
        GraftPlacement(
            side = side,
            position = CreaturePosition(x, 0)
        )
    )

    private fun plant(name: String): PlantCard =
        PlantCard(
            quantity = 4,
            name = name,
            title = name,
            type = PlantType.VINE,
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

    private fun die(sides: Int, value: Int): Die = FixedDie(sides, value)

    private class FixedDie(sides: Int, value: Int) : Die(sides) {
        init {
            adjustTo(value)
        }

        override fun roll(): Die = this
    }

    private fun markerMessages(game: Game): List<String> =
        game.chronicle.entries
            .filterIsInstance<GameEntry.Marker>()
            .map { it.message }

    private data class Fixture(
        val game: Game,
        val coordinator: CultivationCleanupCoordinator
    )
}
