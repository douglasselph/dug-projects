package dugsolutions.leaf.v35.game.operation

import dugsolutions.leaf.v35.chronicle.GameChronicle
import dugsolutions.leaf.v35.chronicle.domain.GameEntry
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.grove.Grove
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.Player
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.creature.CreatureCard
import dugsolutions.leaf.v35.player.creature.CreaturePosition
import dugsolutions.leaf.v35.player.creature.CreatureSide
import dugsolutions.leaf.v35.player.creature.GraftPlacement
import dugsolutions.leaf.v35.player.decision.DecisionDirector
import dugsolutions.leaf.v35.player.decision.wound.ChooseWoundRequest
import dugsolutions.leaf.v35.player.decision.wound.WoundChoice
import dugsolutions.leaf.v35.player.decision.wound.WoundStrategy
import dugsolutions.leaf.v35.random.Randomizer
import dugsolutions.leaf.v35.wisp.WispCardManager
import dugsolutions.leaf.v35.wisp.WispDeck
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WoundResolverTest {

    private lateinit var cards: List<PlantCard>
    private lateinit var grove: Grove
    private lateinit var chronicle: GameChronicle
    private lateinit var strategy: RecordingWoundStrategy
    private lateinit var player: Player
    private lateinit var resolver: WoundResolver

    @BeforeEach
    fun setup() {
        cards = selectedCards()
        grove = Grove(
            selectedPlantCards = cards,
            wispDeck = WispDeck(
                wispCardManager = WispCardManager(),
                randomizer = IdentityRandomizer()
            )
        )
        chronicle = GameChronicle()
        strategy = RecordingWoundStrategy()
        player = playerWith(strategy)
        resolver = WoundResolver(grove, chronicle)
    }

    @Test
    fun resolve_whenFaceUpCardsExist_offersOnlyFlipChoices() {
        // Arrange
        val faceUp = graft(cards[0], -1)
        val faceDown = graft(cards[1], 1)
        player.creature.faceUp(faceUp.id)

        // Act
        resolver.resolve(player)

        // Assert
        assertEquals(
            listOf(faceUp.id),
            strategy.offered.map { it.card.id }
        )
        assertTrue(strategy.offered.all { it is WoundChoice.Flip })
        assertTrue(player.creature.get(faceDown.id)!!.isFaceDown)
    }

    @Test
    fun resolve_whenFlipChosen_turnsChosenCardFaceDown() {
        // Arrange
        val card = graft(cards[0], -1)
        player.creature.faceUp(card.id)

        // Act
        val result = resolver.resolve(player)

        // Assert
        assertIs<WoundResolution.Flipped>(result)
        assertTrue(player.creature.get(card.id)!!.isFaceDown)
    }

    @Test
    fun resolve_whenMultipleFaceUpCards_strategyCanChooseNonFirstCard() {
        // Arrange
        val first = graft(cards[0], -1)
        val second = graft(cards[1], 1)
        player.creature.faceUp(first.id)
        player.creature.faceUp(second.id)
        strategy.indexToChoose = 1

        // Act
        val result = resolver.resolve(player)

        // Assert
        assertEquals(second.id, assertIs<WoundResolution.Flipped>(result).card.id)
        assertTrue(player.creature.get(first.id)!!.isFaceUp)
        assertTrue(player.creature.get(second.id)!!.isFaceDown)
    }

    @Test
    fun resolve_whenAllCardsFaceDown_offersOnlySnippableCards() {
        // Arrange
        val inner = graft(cards[0], -1)
        val outer = graft(cards[1], -2)

        // Act
        resolver.resolve(player)

        // Assert
        assertTrue(strategy.offered.all { it is WoundChoice.Snip })
        assertEquals(listOf(outer.id), strategy.offered.map { it.card.id })
        assertEquals(inner, player.creature.get(inner.id))
    }

    @Test
    fun resolve_whenSnipChosen_removesCardAndReturnsItToMarket() {
        // Arrange
        val card = graft(cards[0], -1)
        val stack = grove.plantMarket.stackFor(card.card)!!
        val remainingBefore = stack.remaining

        // Act
        val result = resolver.resolve(player)

        // Assert
        assertEquals(card, assertIs<WoundResolution.Snipped>(result).card)
        assertNull(player.creature.get(card.id))
        assertEquals(remainingBefore + 1, stack.remaining)
    }

    @Test
    fun resolve_nonSnippableInnerCardIsNeverOffered() {
        // Arrange
        val inner = graft(cards[0], -1)
        graft(cards[1], -2)
        val outer = graft(cards[2], -3)

        // Act
        resolver.resolve(player)

        // Assert
        assertFalse(strategy.offered.any { it.card.id == inner.id })
        assertEquals(listOf(outer.id), strategy.offered.map { it.card.id })
    }

    @Test
    fun resolve_whenStrategyReturnsWrongChoiceType_rejectsBeforeMutation() {
        // Arrange
        val card = graft(cards[0], -1)
        player.creature.faceUp(card.id)
        strategy.choiceOverride = { WoundChoice.Snip(it.first().card) }

        // Act / Assert
        assertFailsWith<IllegalStateException> { resolver.resolve(player) }
        assertTrue(player.creature.get(card.id)!!.isFaceUp)
        assertTrue(chronicle.entries.isEmpty())
    }

    @Test
    fun resolve_whenStrategyReturnsFlipForSnipRequest_rejectsBeforeMutation() {
        // Arrange
        val card = graft(cards[0], -1)
        val stack = grove.plantMarket.stackFor(card.card)!!
        val remainingBefore = stack.remaining
        strategy.choiceOverride = { WoundChoice.Flip(it.first().card) }

        // Act / Assert
        assertFailsWith<IllegalStateException> { resolver.resolve(player) }
        assertEquals(card, player.creature.get(card.id))
        assertEquals(remainingBefore, stack.remaining)
        assertTrue(chronicle.entries.isEmpty())
    }

    @Test
    fun resolve_whenStrategyReturnsUnofferedCard_rejectsBeforeMutation() {
        // Arrange
        val offered = graft(cards[0], -1)
        val foreign = graft(cards[1], 1)
        player.creature.faceUp(offered.id)
        strategy.choiceOverride = { WoundChoice.Flip(foreign) }

        // Act / Assert
        assertFailsWith<IllegalStateException> { resolver.resolve(player) }
        assertTrue(player.creature.get(offered.id)!!.isFaceUp)
        assertTrue(player.creature.get(foreign.id)!!.isFaceDown)
    }

    @Test
    fun resolve_whenCreatureEmpty_returnsNoLegalTarget() {
        // Act
        val result = resolver.resolve(player)

        // Assert
        assertEquals(WoundResolution.NoLegalTarget, result)
        assertTrue(strategy.offered.isEmpty())
        assertTrue(chronicle.entries.isEmpty())
    }

    @Test
    fun resolve_successfulFlip_recordsActualOutcome() {
        // Arrange
        val card = graft(cards[0], -1)
        player.creature.faceUp(card.id)

        // Act
        resolver.resolve(player)

        // Assert
        assertEquals(
            "WOUND player=1 FLIPPED plant=${card.card.name}",
            markerMessages().single()
        )
    }

    @Test
    fun resolve_successfulSnip_recordsActualOutcome() {
        // Arrange
        val card = graft(cards[0], -1)

        // Act
        resolver.resolve(player)

        // Assert
        assertEquals(
            "WOUND player=1 SNIPPED plant=${card.card.name}",
            markerMessages().single()
        )
    }


    @Test
    fun explicitWoundChoice_resolvesChosenFaceUpCardWithoutAskingWoundStrategy() {
        val card =
            graft(
                cards[0],
                -1
            )

        player.creature.faceUp(
            card.id
        )

        strategy.choiceOverride = {
            error(
                "Explicit wound target should not ask WoundStrategy"
            )
        }

        val result =
            resolver.resolve(
                player,
                WoundChoice.Flip(
                    player.creature
                        .get(card.id)!!
                )
            )

        assertIs<
            WoundResolution.Flipped
        >(result)

        assertTrue(
            player.creature
                .get(card.id)!!
                .isFaceDown
        )

        assertTrue(
            strategy.offered.isEmpty()
        )
    }

    @Test
    fun explicitSnipChoice_isRejectedWhenAnyFaceUpCardStillExists() {
        val inner =
            graft(
                cards[0],
                -1
            )
        val outer =
            graft(
                cards[1],
                -2
            )

        player.creature.faceUp(
            inner.id
        )

        assertFailsWith<
            IllegalStateException
        > {
            resolver.resolve(
                player,
                WoundChoice.Snip(
                    player.creature
                        .get(outer.id)!!
                )
            )
        }

        assertTrue(
            player.creature
                .get(inner.id)!!
                .isFaceUp
        )
        assertTrue(
            player.creature
                .get(outer.id)!!
                .isFaceDown
        )
        assertTrue(
            chronicle.entries.isEmpty()
        )
    }

    private fun graft(card: PlantCard, x: Int): CreatureCard {
        val acquired = requireNotNull(grove.plantMarket.take(card))
        return player.creature.graft(
            acquired,
            GraftPlacement(
                side = if (x < 0) CreatureSide.LEFT else CreatureSide.RIGHT,
                position = CreaturePosition(x, -1)
            )
        )
    }

    private fun playerWith(woundStrategy: WoundStrategy): Player =
        Player(
            id = PlayerId(1),
            decisions = DecisionDirector.baseline().copy(wound = woundStrategy)
        )

    private fun markerMessages(): List<String> =
        chronicle.entries.filterIsInstance<GameEntry.Marker>().map { it.message }

    private class RecordingWoundStrategy : WoundStrategy {
        var offered: List<WoundChoice> = emptyList()
        var indexToChoose: Int = 0
        var choiceOverride: ((List<WoundChoice>) -> WoundChoice)? = null

        override fun choose(request: ChooseWoundRequest): WoundChoice {
            offered = request.legalChoices
            return choiceOverride?.invoke(offered) ?: offered[indexToChoose]
        }
    }

    private class IdentityRandomizer : Randomizer {
        override fun nextBoolean(): Boolean = false
        override fun nextInt(from: Int, until: Int): Int = from
        override fun nextInt(until: Int): Int = 0
        override fun <T> randomOrNull(list: List<T>): T? = list.firstOrNull()
        override fun <T> shuffled(list: List<T>): List<T> = list
    }

    private fun selectedCards(): List<PlantCard> =
        listOf(
            card("Root_1", PlantType.ROOT),
            card("Root_2", PlantType.ROOT),
            card("Root_3", PlantType.ROOT),
            card("Vine_1", PlantType.VINE),
            card("Vine_2", PlantType.VINE),
            card("Vine_3", PlantType.VINE),
            card("Flower_1", PlantType.FLOWER),
            card("Flower_2", PlantType.FLOWER),
            card("Flower_3", PlantType.FLOWER)
        )

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
