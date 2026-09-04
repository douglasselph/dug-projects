package dugsolutions.leaf.v35.game.round.cultivation

import dugsolutions.leaf.v35.chronicle.domain.GameEntry
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.GameEffectExecutor
import dugsolutions.leaf.v35.effect.GameEffectRequest
import dugsolutions.leaf.v35.effect.GameEffectSource
import dugsolutions.leaf.v35.effect.RoundEffectSlot
import dugsolutions.leaf.v35.game.Game
import dugsolutions.leaf.v35.game.GameEngineTestFixture
import dugsolutions.leaf.v35.game.operation.RefreshResolver
import dugsolutions.leaf.v35.game.operation.RollResolver
import dugsolutions.leaf.v35.game.operation.SupportActionExecutor
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.Player
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.creature.CreatureCard
import dugsolutions.leaf.v35.player.creature.CreatureCardId
import dugsolutions.leaf.v35.player.creature.CreaturePosition
import dugsolutions.leaf.v35.player.creature.CreatureSide
import dugsolutions.leaf.v35.player.creature.GraftPlacement
import dugsolutions.leaf.v35.player.decision.DecisionDirector
import dugsolutions.leaf.v35.player.decision.cultivation.ChooseCultivationActionRequest
import dugsolutions.leaf.v35.player.decision.cultivation.CultivationAction
import dugsolutions.leaf.v35.player.decision.cultivation.CultivationMainAction
import dugsolutions.leaf.v35.player.decision.cultivation.CultivationStrategy
import dugsolutions.leaf.v35.player.decision.support.HandDieChoice
import dugsolutions.leaf.v35.player.decision.support.SupportAction
import dugsolutions.leaf.v35.player.dice.PlayerDice
import dugsolutions.leaf.v35.random.die.Die
import dugsolutions.leaf.v35.random.die.DieSides
import dugsolutions.leaf.v35.round.domain.RoundCard
import dugsolutions.leaf.v35.round.domain.RoundCardType
import dugsolutions.leaf.v35.tokens.Butterfly
import dugsolutions.leaf.v35.tokens.Critter
import dugsolutions.leaf.v35.tokens.Token
import dugsolutions.leaf.v35.wisp.domain.WispCard
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class CultivationBuildCoordinatorTest {

    @Test
    fun execute_openingDrawsThree_thenRequiresExactlyTwoMainActionsAndDone() {
        val strategy = RoundEffectStrategy()
        val first = player(1, dice(4), strategy)
        val fixture = fixture(first, player(2, emptyList(), RoundEffectStrategy()))

        val result = fixture.coordinator.execute(fixture.game, fixture.card)

        assertEquals(3, result.openingDrawCounts.getValue(first.id))
        assertEquals(listOf(2, 1, 0), strategy.requests.map { it.mainActionsRemaining })
        assertEquals(2, result.actions.count { it.playerId == first.id })
        assertTrue(result.supportActions.none { it.playerId == first.id })
    }

    @Test
    fun execute_supportMayOccurBeforeBetweenAndAfterMainActions() {
        val first = player(1, emptyList(), SequenceStrategy())
        first.tokens.add(Token.WATER)
        first.tokens.add(Token.MULCH(DieSides.D6))
        val grafted = graft(first, plant("FlipMe"), -1, 0)
        first.critters.add(Critter.WORM)

        val strategy = first.decisions.cultivation as SequenceStrategy
        strategy.actions.addAll(
            listOf(
                CultivationAction.Support(SupportAction.UseWaterRefresh),
                CultivationAction.Main(CultivationMainAction.RoundEffect1),
                CultivationAction.Support(SupportAction.UseWormFlip(grafted.id)),
                CultivationAction.Main(CultivationMainAction.RoundEffect2),
                CultivationAction.Support(SupportAction.UseMulch(Token.MULCH(DieSides.D6))),
                CultivationAction.Done
            )
        )

        val fixture = fixture(first, player(2, emptyList(), RoundEffectStrategy()))
        val result = fixture.coordinator.execute(fixture.game, fixture.card)

        assertEquals(2, result.actions.count { it.playerId == first.id })
        assertEquals(3, result.supportActions.count { it.playerId == first.id })
        assertEquals(listOf(2, 2, 1, 1, 0, 0), strategy.requests.map { it.mainActionsRemaining })
        assertTrue(first.creature.get(grafted.id)!!.isFaceDown)
        assertEquals(1, first.dice.handSize) // Mulch die
    }

    @Test
    fun execute_doneIsNotOfferedUntilBothMainActionsAreUsed() {
        val strategy = RecordingStrategy()
        val first = player(1, emptyList(), strategy)
        val fixture = fixture(first, player(2, emptyList(), RoundEffectStrategy()))

        fixture.coordinator.execute(fixture.game, fixture.card)

        assertFalse(CultivationAction.Done in strategy.requests[0].legalChoices)
        assertFalse(CultivationAction.Done in strategy.requests[1].legalChoices)
        assertTrue(CultivationAction.Done in strategy.requests[2].legalChoices)
    }

    @Test
    fun execute_supportDoesNotConsumeMainAction() {
        val strategy = SequenceStrategy()
        val first = player(1, emptyList(), strategy)
        first.tokens.add(Token.WATER)
        strategy.actions.addAll(
            listOf(
                CultivationAction.Support(SupportAction.UseWaterRefresh),
                CultivationAction.Main(CultivationMainAction.RoundEffect1),
                CultivationAction.Main(CultivationMainAction.RoundEffect2),
                CultivationAction.Done
            )
        )
        val fixture = fixture(first, player(2, emptyList(), RoundEffectStrategy()))

        fixture.coordinator.execute(fixture.game, fixture.card)

        assertEquals(listOf(2, 2, 1, 0), strategy.requests.map { it.mainActionsRemaining })
    }

    @Test
    fun execute_offersOwnedNormalWispButNotImmediateOrBattleOnlyWisp() {
        val strategy = RecordingStrategy()
        val first = player(1, emptyList(), strategy)
        val normal = wisp("Normal")
        val immediate = wisp("Quake", playImmediately = true)
        val battleOnly = wisp("Battle", battleOnly = true)
        first.wisps.addAll(listOf(normal, immediate, battleOnly))
        val fixture = fixture(first, player(2, emptyList(), RoundEffectStrategy()))

        fixture.coordinator.execute(fixture.game, fixture.card)

        val offered = strategy.requests.first().legalChoices
            .filterIsInstance<CultivationAction.Support>()
            .map { it.action }
            .filterIsInstance<SupportAction.PlayWisp>()
            .map { it.card }
        assertEquals(listOf(normal), offered)
    }

    @Test
    fun execute_offersWaterMulchWormButterflySupportOnlyWhenAvailable() {
        val strategy = RecordingStrategy()
        val die = FixedDie(8, 5)
        val first = player(1, emptyList(), strategy, hand = listOf(die))
        first.tokens.add(Token.WATER)
        first.tokens.add(Token.MULCH(DieSides.D8))
        first.critters.add(Critter.WORM)
        first.butterflies.add(Butterfly.GREEN)
        val grafted = graft(first, plant("Vine"), -1, 0)
        val fixture = fixture(first, player(2, emptyList(), RoundEffectStrategy()))

        fixture.coordinator.execute(fixture.game, fixture.card)

        val supports = strategy.requests.first().legalChoices
            .filterIsInstance<CultivationAction.Support>()
            .map { it.action }
        assertTrue(SupportAction.UseWaterRefresh in supports)
        assertTrue(supports.any { it is SupportAction.UseWaterReroll })
        assertTrue(SupportAction.UseMulch(Token.MULCH(DieSides.D8)) in supports)
        assertTrue(SupportAction.UseWormFlip(grafted.id) in supports)
        assertTrue(supports.any { it is SupportAction.UseButterfly })
    }

    @Test
    fun execute_supportActionIsRegeneratedAfterResourceIsConsumed() {
        val strategy = SequenceStrategy()
        val first = player(1, emptyList(), strategy)
        first.tokens.add(Token.WATER)
        strategy.actions.addAll(
            listOf(
                CultivationAction.Support(SupportAction.UseWaterRefresh),
                CultivationAction.Main(CultivationMainAction.RoundEffect1),
                CultivationAction.Main(CultivationMainAction.RoundEffect2),
                CultivationAction.Done
            )
        )
        val fixture = fixture(first, player(2, emptyList(), RoundEffectStrategy()))

        fixture.coordinator.execute(fixture.game, fixture.card)

        assertFalse(
            strategy.requests[1].legalChoices.any {
                it == CultivationAction.Support(SupportAction.UseWaterRefresh)
            }
        )
    }

    @Test
    fun execute_baselineStillUsesTwoMainActionsThenFinishes() {
        val first = player(1, dice(5), DecisionDirector.baseline().cultivation)
        first.tokens.add(Token.WATER)
        val fixture = fixture(first, player(2, emptyList(), RoundEffectStrategy()))

        val result = fixture.coordinator.execute(fixture.game, fixture.card)

        assertEquals(
            listOf(CultivationMainAction.Draw, CultivationMainAction.Draw),
            result.actions.filter { it.playerId == first.id }.map { it.action }
        )
        assertTrue(result.supportActions.none { it.playerId == first.id })
        assertEquals(1, first.tokens.waterCount)
    }

    @Test
    fun execute_selectedPlantEffectRunsThenPlantFlipsFaceDown() {
        val strategy = SelectPlantStrategy()
        val first = player(1, emptyList(), strategy)
        val active = graft(first, plant("Active", GameEffect.GAIN_ONE_VP), -1, 0)
        first.creature.faceUp(active.id)
        val fixture = fixture(first, player(2, emptyList(), RoundEffectStrategy()))

        fixture.coordinator.execute(fixture.game, fixture.card)

        val request = fixture.effects.requests.first { it.source is GameEffectSource.Plant }
        assertEquals(GameEffect.GAIN_ONE_VP, request.effect)
        assertEquals(active.id, assertIs<GameEffectSource.Plant>(request.source).card.id)
        assertTrue(first.creature.get(active.id)!!.isFaceDown)
    }

    @Test
    fun execute_roundEffectsMayStillBeUsedRepeatedly() {
        val first = player(1, emptyList(), RoundEffectStrategy())
        val fixture = fixture(first, player(2, emptyList(), RoundEffectStrategy()))

        fixture.coordinator.execute(fixture.game, fixture.card)

        val requests = fixture.effects.requests.filter { it.actor === first }
        assertEquals(2, requests.size)
        assertTrue(requests.all {
            assertIs<GameEffectSource.Round>(it.source).slot == RoundEffectSlot.FIRST
        })
    }


    @Test
    fun execute_offersOnlyMainAndWispEffectsReportedExecutable() {
        val strategy = RecordingStrategy()
        val first = player(1, emptyList(), strategy)
        val supportedWisp = wisp("Supported")
        val unsupportedWisp = wisp("Unsupported").copy(effect = GameEffect.SET_DIE_TO_MATCH_ANOTHER)
        first.wisps.addAll(listOf(supportedWisp, unsupportedWisp))

        val effects = RecordingEffectExecutor { request ->
            when (val source = request.source) {
                is GameEffectSource.Round -> source.slot == RoundEffectSlot.FIRST
                is GameEffectSource.Wisp -> source.card == supportedWisp
                is GameEffectSource.Plant -> false
            }
        }
        val fixture = fixture(
            first,
            player(2, emptyList(), RoundEffectStrategy()),
            effects
        )

        fixture.coordinator.execute(fixture.game, fixture.card)

        val firstChoices = strategy.requests.first().legalChoices
        assertTrue(
            CultivationAction.Main(CultivationMainAction.RoundEffect1) in firstChoices
        )
        assertFalse(
            CultivationAction.Main(CultivationMainAction.RoundEffect2) in firstChoices
        )
        val wisps = firstChoices
            .filterIsInstance<CultivationAction.Support>()
            .map { it.action }
            .filterIsInstance<SupportAction.PlayWisp>()
            .map { it.card }
        assertEquals(listOf(supportedWisp), wisps)
    }

    @Test
    fun execute_invalidSupportChoiceIsRejectedBeforeMutation() {
        val invalid = CultivationAction.Support(
            SupportAction.UseWormFlip(CreatureCardId(999))
        )
        val strategy = SequenceStrategy(invalid)
        val first = player(1, emptyList(), strategy)
        val fixture = fixture(first, player(2, emptyList(), RoundEffectStrategy()))

        assertFailsWith<IllegalStateException> {
            fixture.coordinator.execute(fixture.game, fixture.card)
        }

        assertTrue(fixture.effects.requests.isEmpty())
        assertTrue(fixture.game.chronicle.entries.none {
            it is GameEntry.Marker && it.message.startsWith("SUPPORT_ACTION player=1")
        })
    }

    @Test
    fun execute_processesPlayersInDeterministicPlayerOrder() {
        val order = mutableListOf<Int>()
        val first = player(1, emptyList(), RoundEffectStrategy { order += 1 })
        val second = player(2, emptyList(), RoundEffectStrategy { order += 2 })
        val fixture = fixture(first, second)

        fixture.coordinator.execute(fixture.game, fixture.card)

        // Each strategy is asked twice for Main Actions and once to choose Done.
        assertEquals(listOf(1, 1, 1, 2, 2, 2), order)
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

    private fun fixture(
        first: Player,
        second: Player,
        effects: RecordingEffectExecutor = RecordingEffectExecutor()
    ): Fixture {
        val game = GameEngineTestFixture.game(
            cultivationRounds = 1,
            battleRounds = 1,
            players = listOf(first, second)
        )
        val roll = RollResolver(game.grove, game.chronicle)
        return Fixture(
            game = game,
            card = game.roundDeck.cards.cards.first { it.type == RoundCardType.CULTIVATION },
            effects = effects,
            coordinator = CultivationBuildCoordinator(
                rollResolver = roll,
                effectExecutor = effects,
                supportActionExecutor = SupportActionExecutor(
                    rollResolver = roll,
                    refreshResolver = RefreshResolver(game.chronicle),
                    effectExecutor = effects
                )
            )
        )
    }

    private fun player(
        id: Int,
        supply: List<Die>,
        strategy: CultivationStrategy,
        hand: List<Die> = emptyList()
    ): Player = Player(
        id = PlayerId(id),
        decisions = DecisionDirector.baseline().copy(cultivation = strategy),
        dice = PlayerDice(supply = supply, hand = hand)
    )

    private fun dice(count: Int): List<Die> =
        List(count) { FixedDie(6, 4) }

    private fun graft(
        player: Player,
        card: PlantCard,
        x: Int,
        y: Int
    ): CreatureCard = player.creature.graft(
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

    private fun wisp(
        name: String,
        playImmediately: Boolean = false,
        battleOnly: Boolean = false
    ): WispCard = WispCard(
        quantity = 1,
        name = name,
        title = name,
        count = 1,
        effect = GameEffect.GAIN_ONE_VP,
        lineIcons = null,
        lineIconsHeight = 0,
        vpIcon = null,
        mainBackdrop = "",
        playImmediately = playImmediately,
        battleOnly = battleOnly
    )

    private class FixedDie(sides: Int, value: Int) : Die(sides) {
        init {
            adjustTo(value)
        }
        override fun roll(): Die = this
    }

    private open class RoundEffectStrategy(
        private val onChoose: () -> Unit = {}
    ) : CultivationStrategy {
        val requests = mutableListOf<ChooseCultivationActionRequest>()

        override fun chooseAction(
            request: ChooseCultivationActionRequest
        ): CultivationAction {
            requests += request
            onChoose()
            return if (request.mainActionsRemaining > 0) {
                CultivationAction.Main(CultivationMainAction.RoundEffect1)
            } else {
                CultivationAction.Done
            }
        }
    }

    private class RecordingStrategy : RoundEffectStrategy()

    private class SelectPlantStrategy : CultivationStrategy {
        override fun chooseAction(
            request: ChooseCultivationActionRequest
        ): CultivationAction {
            if (request.mainActionsRemaining == 0) return CultivationAction.Done
            val plant = request.legalChoices
                .filterIsInstance<CultivationAction.Main>()
                .map { it.action }
                .filterIsInstance<CultivationMainAction.ActivatePlant>()
                .firstOrNull()
            return CultivationAction.Main(
                plant ?: CultivationMainAction.RoundEffect1
            )
        }
    }

    private class SequenceStrategy(
        vararg initial: CultivationAction
    ) : CultivationStrategy {
        val actions = ArrayDeque(initial.toList())
        val requests = mutableListOf<ChooseCultivationActionRequest>()

        override fun chooseAction(
            request: ChooseCultivationActionRequest
        ): CultivationAction {
            requests += request
            return actions.removeFirst()
        }
    }

    private class RecordingEffectExecutor(
        private val executable: (GameEffectRequest) -> Boolean = { true }
    ) : GameEffectExecutor {
        val requests = mutableListOf<GameEffectRequest>()

        override fun canExecute(request: GameEffectRequest): Boolean =
            executable(request)

        override fun execute(request: GameEffectRequest) {
            requests += request
        }
    }

    private data class Fixture(
        val game: Game,
        val card: RoundCard,
        val effects: RecordingEffectExecutor,
        val coordinator: CultivationBuildCoordinator
    )
}
