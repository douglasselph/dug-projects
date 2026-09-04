package dugsolutions.leaf.v35.game.round.cultivation

import dugsolutions.leaf.v35.chronicle.domain.GameEntry
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.GameEffectExecutor
import dugsolutions.leaf.v35.effect.GameEffectRequest
import dugsolutions.leaf.v35.effect.GameEffectSource
import dugsolutions.leaf.v35.effect.RoundEffectSlot
import dugsolutions.leaf.v35.game.Game
import dugsolutions.leaf.v35.game.GameEngineTestFixture
import dugsolutions.leaf.v35.game.operation.RollResolver
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.Player
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.creature.CreatureCard
import dugsolutions.leaf.v35.player.creature.CreaturePosition
import dugsolutions.leaf.v35.player.creature.CreatureSide
import dugsolutions.leaf.v35.player.creature.GraftPlacement
import dugsolutions.leaf.v35.player.decision.DecisionDirector
import dugsolutions.leaf.v35.player.decision.cultivation.ChooseCultivationMainActionRequest
import dugsolutions.leaf.v35.player.decision.cultivation.CultivationMainAction
import dugsolutions.leaf.v35.player.decision.cultivation.CultivationStrategy
import dugsolutions.leaf.v35.player.dice.PlayerDice
import dugsolutions.leaf.v35.random.die.Die
import dugsolutions.leaf.v35.round.domain.RoundCard
import dugsolutions.leaf.v35.round.domain.RoundCardType
import dugsolutions.leaf.v35.tokens.Critter
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class CultivationBuildCoordinatorTest {

    @Test
    fun execute_openingDrawsExactlyThreeDiceForEachPlayer() {
        val first = player(1, dice(4), RoundEffectStrategy())
        val second = player(2, dice(4), RoundEffectStrategy())
        val fixture = fixture(first, second)

        val result = fixture.coordinator.execute(fixture.game, fixture.card)

        assertEquals(mapOf(PlayerId(1) to 3, PlayerId(2) to 3), result.openingDrawCounts)
        assertEquals(3, first.dice.handSize)
        assertEquals(3, second.dice.handSize)
        assertEquals(1, first.dice.supplySize)
        assertEquals(1, second.dice.supplySize)
    }

    @Test
    fun execute_openingDrawResolvesRollRewardImmediately() {
        val first = player(1, listOf(FixedDie(4, 1)), RoundEffectStrategy())
        val second = player(2, emptyList(), RoundEffectStrategy())
        val fixture = fixture(first, second)

        fixture.coordinator.execute(fixture.game, fixture.card)

        assertEquals(listOf(Critter.BEE), first.critters.all)
        assertEquals(8, fixture.game.grove.critters.count(Critter.BEE))
        assertTrue(markerMessages(fixture.game).any { it.startsWith("ROLL_REWARD player=1") })
    }

    @Test
    fun execute_whenOnlyTwoDiceAvailable_openingDrawsTwoWithoutFailure() {
        val first = player(1, dice(2), RoundEffectStrategy())
        val second = player(2, emptyList(), RoundEffectStrategy())
        val fixture = fixture(first, second)

        val result = fixture.coordinator.execute(fixture.game, fixture.card)

        assertEquals(2, result.openingDrawCounts.getValue(first.id))
        assertEquals(2, first.dice.handSize)
    }

    @Test
    fun execute_asksForTwoActionsNumberedOneThenTwoAndReportsBoth() {
        val strategy = RoundEffectStrategy()
        val first = player(1, emptyList(), strategy)
        val fixture = fixture(first, player(2, emptyList(), RoundEffectStrategy()))

        val result = fixture.coordinator.execute(fixture.game, fixture.card)

        assertEquals(listOf(1, 2), strategy.requests.map { it.actionNumber })
        assertEquals(2, result.actions.count { it.playerId == first.id })
        assertEquals(listOf(1, 2), result.actions.filter { it.playerId == first.id }.map { it.actionNumber })
    }

    @Test
    fun execute_baselineWithAvailableDice_choosesDrawTwice() {
        val first = player(1, dice(5), DecisionDirector.baseline().cultivation)
        val fixture = fixture(first, player(2, emptyList(), RoundEffectStrategy()))

        val result = fixture.coordinator.execute(fixture.game, fixture.card)

        assertEquals(
            listOf(CultivationMainAction.Draw, CultivationMainAction.Draw),
            result.actions.filter { it.playerId == first.id }.map { it.action }
        )
        assertEquals(5, first.dice.handSize)
    }

    @Test
    fun execute_sameRoundEffectMayBeChosenTwice() {
        val first = player(1, emptyList(), RoundEffectStrategy())
        val fixture = fixture(first, player(2, emptyList(), RoundEffectStrategy()))

        fixture.coordinator.execute(fixture.game, fixture.card)

        val firstRequests = fixture.effects.requests.filter { it.actor === first }
        assertEquals(2, firstRequests.size)
        assertTrue(firstRequests.all {
            (it.source as GameEffectSource.Round).slot == RoundEffectSlot.FIRST
        })
    }

    @Test
    fun execute_drawMainActionNormallyRollsDieIntoHand() {
        val strategy = SequenceStrategy(
            CultivationMainAction.Draw,
            CultivationMainAction.RoundEffect1
        )
        val first = player(
            1,
            List(4) { FixedDie(8, 6) },
            strategy
        )
        val fixture = fixture(first, player(2, emptyList(), RoundEffectStrategy()))

        fixture.coordinator.execute(fixture.game, fixture.card)

        assertEquals(4, first.dice.handSize)
        assertTrue(first.dice.hand.all { it.value == 6 })
    }

    @Test
    fun execute_offersOnlyFaceUpPlantsAsActivationChoices() {
        val strategy = RoundEffectStrategy()
        val first = player(1, emptyList(), strategy)
        val faceUp = graft(first, plant("FaceUp"), -1, 0)
        graft(first, plant("FaceDown"), 1, 0)
        first.creature.faceUp(faceUp.id)
        val fixture = fixture(first, player(2, emptyList(), RoundEffectStrategy()))

        fixture.coordinator.execute(fixture.game, fixture.card)

        val plantChoices = strategy.requests.first().legalChoices
            .filterIsInstance<CultivationMainAction.ActivatePlant>()
        assertEquals(listOf(faceUp.id), plantChoices.map { it.card.id })
    }

    @Test
    fun execute_selectedPlantEffectRunsWhileFaceUpThenFlipsPlantFaceDown() {
        val strategy = SelectPlantStrategy()
        val first = player(1, emptyList(), strategy)
        val plant = graft(first, plant("Active", GameEffect.GAIN_ONE_VP), -1, 0)
        first.creature.faceUp(plant.id)
        val fixture = fixture(first, player(2, emptyList(), RoundEffectStrategy()))
        var faceUpDuringExecution = false
        fixture.effects.onExecute = {
            if (it.actor === first && it.source is GameEffectSource.Plant) {
                faceUpDuringExecution = first.creature.get(plant.id)!!.isFaceUp
            }
        }

        fixture.coordinator.execute(fixture.game, fixture.card)

        val request = fixture.effects.requests.first { it.source is GameEffectSource.Plant }
        assertEquals(GameEffect.GAIN_ONE_VP, request.effect)
        assertEquals(plant.id, assertIs<GameEffectSource.Plant>(request.source).card.id)
        assertTrue(faceUpDuringExecution)
        assertTrue(first.creature.get(plant.id)!!.isFaceDown)
    }

    @Test
    fun execute_whenPlantEffectThrows_leavesPlantFaceUp() {
        val first = player(1, emptyList(), SelectPlantStrategy())
        val plant = graft(first, plant("Active"), -1, 0)
        first.creature.faceUp(plant.id)
        val fixture = fixture(first, player(2, emptyList(), RoundEffectStrategy()))
        fixture.effects.failure = IllegalStateException("effect failed")

        assertFailsWith<IllegalStateException> {
            fixture.coordinator.execute(fixture.game, fixture.card)
        }

        assertTrue(first.creature.get(plant.id)!!.isFaceUp)
        assertFalse(markerMessages(fixture.game).any { it.contains("MAIN_ACTION player=1") })
    }

    @Test
    fun execute_offersBothRoundEffectsAndExecutesExactSelectedEffects() {
        val strategy = SequenceStrategy(
            CultivationMainAction.RoundEffect1,
            CultivationMainAction.RoundEffect2
        )
        val first = player(1, emptyList(), strategy)
        val fixture = fixture(first, player(2, emptyList(), RoundEffectStrategy()))

        fixture.coordinator.execute(fixture.game, fixture.card)

        assertTrue(strategy.requests.first().legalChoices.contains(CultivationMainAction.RoundEffect1))
        assertTrue(strategy.requests.first().legalChoices.contains(CultivationMainAction.RoundEffect2))
        val requests = fixture.effects.requests.filter { it.actor === first }
        assertEquals(fixture.card.firstEffect.effect, requests[0].effect)
        assertEquals(fixture.card.secondEffect.effect, requests[1].effect)
        assertEquals(RoundEffectSlot.FIRST, assertIs<GameEffectSource.Round>(requests[0].source).slot)
        assertEquals(RoundEffectSlot.SECOND, assertIs<GameEffectSource.Round>(requests[1].source).slot)
    }

    @Test
    fun execute_whenStrategyReturnsUnofferedAction_rejectsBeforeMutation() {
        val invalid = CultivationMainAction.ActivatePlant(
            CreatureCard(
                id = dugsolutions.leaf.v35.player.creature.CreatureCardId(99),
                card = plant("Foreign"),
                side = CreatureSide.LEFT,
                position = CreaturePosition(-1, 0),
                facing = CreatureCard.Facing.FACE_UP
            )
        )
        val first = player(1, emptyList(), SequenceStrategy(invalid))
        val fixture = fixture(first, player(2, emptyList(), RoundEffectStrategy()))

        assertFailsWith<IllegalStateException> {
            fixture.coordinator.execute(fixture.game, fixture.card)
        }

        assertTrue(fixture.effects.requests.isEmpty())
        assertFalse(markerMessages(fixture.game).any { it.contains("MAIN_ACTION player=1") })
    }

    @Test
    fun execute_processesPlayersInDeterministicListOrder() {
        val order = mutableListOf<Int>()
        val firstStrategy = RoundEffectStrategy { order.add(1) }
        val secondStrategy = RoundEffectStrategy { order.add(2) }
        val fixture = fixture(
            player(1, emptyList(), firstStrategy),
            player(2, emptyList(), secondStrategy)
        )

        fixture.coordinator.execute(fixture.game, fixture.card)

        assertEquals(listOf(1, 1, 2, 2), order)
    }

    @Test
    fun execute_chronicleContainsOneRollEntryPerSuccessfulDraw() {
        val first = player(1, dice(5), DecisionDirector.baseline().cultivation)
        val fixture = fixture(first, player(2, emptyList(), RoundEffectStrategy()))

        fixture.coordinator.execute(fixture.game, fixture.card)

        val rolls = markerMessages(fixture.game).filter { it.startsWith("ROLL player=1") }
        assertEquals(5, rolls.size)
    }

    @Test
    fun execute_whenBattleCardProvided_rejectsBeforeMutation() {
        val first = player(1, dice(3), RoundEffectStrategy())
        val fixture = fixture(first, player(2, emptyList(), RoundEffectStrategy()))
        val battle = fixture.game.roundDeck.cards.cards.first { it.type == RoundCardType.BATTLE }

        assertFailsWith<IllegalArgumentException> {
            fixture.coordinator.execute(fixture.game, battle)
        }

        assertTrue(first.dice.hand.isEmpty())
        assertTrue(fixture.game.chronicle.entries.isEmpty())
    }

    private fun fixture(first: Player, second: Player): Fixture {
        val game = GameEngineTestFixture.game(
            cultivationRounds = 1,
            battleRounds = 1,
            players = listOf(first, second)
        )
        val effects = RecordingEffectExecutor()
        return Fixture(
            game = game,
            card = game.roundDeck.cards.cards.first { it.type == RoundCardType.CULTIVATION },
            effects = effects,
            coordinator = CultivationBuildCoordinator(
                rollResolver = RollResolver(game.grove, game.chronicle),
                effectExecutor = effects
            )
        )
    }

    private fun player(
        id: Int,
        dice: List<Die>,
        strategy: CultivationStrategy
    ): Player = Player(
        id = PlayerId(id),
        decisions = DecisionDirector.baseline().copy(cultivation = strategy),
        dice = PlayerDice(supply = dice)
    )

    private fun dice(count: Int): List<Die> =
        List(count) { FixedDie(6, 4) }

    private fun graft(player: Player, card: PlantCard, x: Int, y: Int): CreatureCard =
        player.creature.graft(
            card,
            GraftPlacement(
                side = if (x < 0) CreatureSide.LEFT else CreatureSide.RIGHT,
                position = CreaturePosition(x, y)
            )
        )

    private fun plant(
        name: String,
        effect: GameEffect = GameEffect.UNKNOWN
    ): PlantCard = PlantCard(
        quantity = 1,
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
        effect = effect
    )

    private fun markerMessages(game: Game): List<String> =
        game.chronicle.entries.filterIsInstance<GameEntry.Marker>().map { it.message }

    private class FixedDie(sides: Int, private val rolledValue: Int) : Die(sides) {
        override fun roll(): Die {
            adjustTo(rolledValue)
            return this
        }
    }

    private open class RoundEffectStrategy(
        private val onChoose: () -> Unit = {}
    ) : CultivationStrategy {
        val requests = mutableListOf<ChooseCultivationMainActionRequest>()

        override fun chooseMainAction(
            request: ChooseCultivationMainActionRequest
        ): CultivationMainAction {
            requests.add(request)
            onChoose()
            return CultivationMainAction.RoundEffect1
        }
    }

    private class SelectPlantStrategy : CultivationStrategy {
        override fun chooseMainAction(
            request: ChooseCultivationMainActionRequest
        ): CultivationMainAction =
            request.legalChoices.filterIsInstance<CultivationMainAction.ActivatePlant>()
                .firstOrNull() ?: CultivationMainAction.RoundEffect1
    }

    private class SequenceStrategy(
        vararg actions: CultivationMainAction
    ) : CultivationStrategy {
        private val actions = ArrayDeque(actions.toList())
        val requests = mutableListOf<ChooseCultivationMainActionRequest>()

        override fun chooseMainAction(
            request: ChooseCultivationMainActionRequest
        ): CultivationMainAction {
            requests.add(request)
            return actions.removeFirst()
        }
    }

    private class RecordingEffectExecutor : GameEffectExecutor {
        val requests = mutableListOf<GameEffectRequest>()
        var failure: RuntimeException? = null
        var onExecute: (GameEffectRequest) -> Unit = {}

        override fun execute(request: GameEffectRequest) {
            requests.add(request)
            onExecute(request)
            failure?.let { throw it }
        }
    }

    private data class Fixture(
        val game: Game,
        val card: RoundCard,
        val effects: RecordingEffectExecutor,
        val coordinator: CultivationBuildCoordinator
    )
}
