package dugsolutions.leaf.v35.game.round.battle

import dugsolutions.leaf.v35.battle.BattleState
import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.GameEffectExecutor
import dugsolutions.leaf.v35.effect.GameEffectRequest
import dugsolutions.leaf.v35.effect.GameEffectSource
import dugsolutions.leaf.v35.error.InvalidDecisionException
import dugsolutions.leaf.v35.game.Game
import dugsolutions.leaf.v35.game.GameEngineTestFixture
import dugsolutions.leaf.v35.game.operation.RefreshResolver
import dugsolutions.leaf.v35.game.operation.RollResolver
import dugsolutions.leaf.v35.game.operation.SupportActionExecutor
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.Player
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.DecisionDirector
import dugsolutions.leaf.v35.player.decision.battle.BattleMainAction
import dugsolutions.leaf.v35.player.decision.battle.BattleStrategy
import dugsolutions.leaf.v35.player.decision.battle.BattleSupportAction
import dugsolutions.leaf.v35.player.decision.battle.BattleTurnAction
import dugsolutions.leaf.v35.player.decision.battle.ChooseBattleDiePlacementRequest
import dugsolutions.leaf.v35.player.decision.battle.ChooseBattleFirstMainActionRequest
import dugsolutions.leaf.v35.player.decision.battle.ChooseBattleTurnActionRequest
import dugsolutions.leaf.v35.player.creature.CreaturePosition
import dugsolutions.leaf.v35.player.creature.CreatureSide
import dugsolutions.leaf.v35.player.creature.GraftPlacement
import dugsolutions.leaf.v35.player.decision.support.SupportAction
import dugsolutions.leaf.v35.player.dice.PlayerDice
import dugsolutions.leaf.v35.random.die.Die
import dugsolutions.leaf.v35.round.domain.RoundCard
import dugsolutions.leaf.v35.round.domain.RoundCardEffect
import dugsolutions.leaf.v35.round.domain.RoundCardType
import dugsolutions.leaf.v35.tokens.Critter
import dugsolutions.leaf.v35.tokens.Token
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class BattleActionCoordinatorTest {

    @Test
    fun execute_allFirstMainsOccurBeforeSupportThenRepeatedPassesSkipFinishedPlayers() {
        val log = mutableListOf<String>()
        val p1Strategy = ScriptedStrategy("p1", log)
        val p2Strategy = ScriptedStrategy("p2", log)

        val p1 = player(1, p1Strategy)
        val p2 = player(2, p2Strategy)
        p1.critters.add(Critter.BEE)

        p1Strategy.first = BattleMainAction.RoundEffect1
        p2Strategy.first = BattleMainAction.RoundEffect1
        p1Strategy.turns += listOf(
            BattleTurnAction.Support(
                BattleSupportAction.PlaceCritter(Critter.BEE, StrikeRow.TOP)
            ),
            BattleTurnAction.FinalMain(BattleMainAction.RoundEffect2)
        )
        p2Strategy.turns +=
            BattleTurnAction.FinalMain(BattleMainAction.RoundEffect2)

        val fixture = fixture(p1, p2)
        val result = fixture.coordinator.execute(
            fixture.game,
            fixture.roundCard,
            fixture.battleState
        )

        assertEquals(
            listOf(
                "p1:first",
                "p2:first",
                "p1:turn:1",
                "p2:turn:1",
                "p1:turn:2"
            ),
            log
        )
        assertEquals(listOf(p1.id, p2.id), result.firstMainActions.map { it.playerId })
        assertEquals(listOf(p2.id, p1.id), result.finalMainActions.map { it.playerId })
        assertEquals(1, result.supportActions.size)
        assertEquals(
            listOf(Critter.BEE),
            fixture.battleState.grid.square(p1.id, StrikeRow.TOP).critters
        )
        assertEquals(0, p1.critters.count(Critter.BEE))
    }

    @Test
    fun execute_drawMainAddsRolledDieToHandAndAsksWhereToPlaceIt() {
        val p1Strategy = ScriptedStrategy("p1", mutableListOf()).apply {
            first = BattleMainAction.Draw
            turns += BattleTurnAction.FinalMain(BattleMainAction.RoundEffect1)
            placementRow = StrikeRow.BOTTOM
        }
        val p2Strategy = finishStrategy("p2")

        val initial = fixedDie(6, 4)
        val drawn = fixedDie(8, 7)
        val p1 = player(1, p1Strategy, hand = listOf(initial), supply = listOf(drawn))
        val p2 = player(2, p2Strategy)

        val fixture = fixture(p1, p2)
        fixture.battleState.placeInitialHands()

        fixture.coordinator.execute(fixture.game, fixture.roundCard, fixture.battleState)

        assertTrue(p1.dice.hand.any { it === drawn })
        assertEquals(StrikeRow.BOTTOM, fixture.battleState.grid.locationOf(drawn)?.row)
        assertEquals(7, p1Strategy.placementRequests.single().die.value)
    }

    @Test
    fun execute_plantActivationUsesBattleEffectContextThenFlipsPlantDown() {
        val strategy = ScriptedStrategy("p1", mutableListOf())
        val p1 = player(1, strategy)
        val active = p1.creature.graft(
            plant("Battle Plant"),
            GraftPlacement(CreatureSide.LEFT, CreaturePosition(-1, -1))
        )
        p1.creature.faceUp(active.id)

        strategy.first = BattleMainAction.ActivatePlant(p1.creature.get(active.id)!!)
        strategy.turns += BattleTurnAction.FinalMain(BattleMainAction.RoundEffect1)
        val p2 = player(2, finishStrategy("p2"))

        val fixture = fixture(p1, p2)
        fixture.coordinator.execute(fixture.game, fixture.roundCard, fixture.battleState)

        val plantRequest = fixture.effects.requests.first {
            it.source is GameEffectSource.Plant
        }
        assertTrue(plantRequest.battleState === fixture.battleState)
        assertEquals(GameEffect.GAIN_ONE_VP, plantRequest.effect)
        assertTrue(p1.creature.get(active.id)!!.isFaceDown)
    }

    @Test
    fun execute_battleSupportChoicesIncludeSharedResourcesAndExactCritterRows() {
        val strategy = RecordingFinishStrategy()
        val die = fixedDie(8, 5)
        val p1 = player(1, strategy, hand = listOf(die))
        val p2 = player(2, finishStrategy("p2"))
        p1.tokens.add(Token.WATER)
        p1.critters.add(Critter.WORM)

        strategy.first = BattleMainAction.RoundEffect1
        val fixture = fixture(p1, p2)
        fixture.battleState.placeInitialHands()

        fixture.coordinator.execute(fixture.game, fixture.roundCard, fixture.battleState)

        val offered = strategy.turnRequests.single().legalChoices
        assertTrue(
            offered.any {
                it == BattleTurnAction.Support(
                    BattleSupportAction.Shared(SupportAction.UseWaterRefresh)
                )
            }
        )
        assertTrue(
            offered.any {
                it == BattleTurnAction.Support(
                    BattleSupportAction.PlaceCritter(Critter.WORM, StrikeRow.BOTTOM)
                )
            }
        )
    }

    @Test
    fun execute_invalidStep5ChoiceIsRejectedBeforeMutation() {
        val illegal = object : BattleStrategy {
            override fun chooseFirstMainAction(
                request: ChooseBattleFirstMainActionRequest
            ) = BattleMainAction.RoundEffect1

            override fun chooseTurnAction(
                request: ChooseBattleTurnActionRequest
            ): BattleTurnAction =
                BattleTurnAction.Support(
                    BattleSupportAction.PlaceCritter(Critter.BEE, StrikeRow.TOP)
                )

            override fun chooseDiePlacement(
                request: ChooseBattleDiePlacementRequest
            ) = request.legalRows.first()
        }
        val p1 = player(1, illegal)
        val p2 = player(2, finishStrategy("p2"))
        val fixture = fixture(p1, p2)

        assertFailsWith<InvalidDecisionException> {
            fixture.coordinator.execute(fixture.game, fixture.roundCard, fixture.battleState)
        }

        assertTrue(fixture.battleState.grid.critterPlacements.isEmpty())
    }

    @Test
    fun execute_roundEffectRequestCarriesCurrentBattleState() {
        val p1 = player(1, finishStrategy("p1"))
        val p2 = player(2, finishStrategy("p2"))
        val fixture = fixture(p1, p2)

        fixture.coordinator.execute(fixture.game, fixture.roundCard, fixture.battleState)

        val roundRequests = fixture.effects.requests.filter {
            it.source is GameEffectSource.Round
        }
        assertTrue(roundRequests.isNotEmpty())
        assertTrue(roundRequests.all { it.battleState === fixture.battleState })
    }

    private data class Fixture(
        val game: Game,
        val roundCard: RoundCard,
        val battleState: BattleState,
        val effects: RecordingEffects,
        val coordinator: BattleActionCoordinator
    )

    private fun fixture(
        vararg players: Player
    ): Fixture {
        val game = GameEngineTestFixture.game(players = players.toList())
        val battleState = BattleState(players.toList())
        val effects = RecordingEffects()
        val rollResolver = RollResolver(
            grove = game.grove,
            chronicle = game.chronicle,
            immediateWispHandler = { _, _ -> Unit }
        )
        val support = SupportActionExecutor(
            rollResolver = rollResolver,
            refreshResolver = RefreshResolver(game.chronicle),
            effectExecutor = effects
        )
        return Fixture(
            game = game,
            roundCard = roundCard(),
            battleState = battleState,
            effects = effects,
            coordinator = BattleActionCoordinator(
                rollResolver = rollResolver,
                effectExecutor = effects,
                supportActionExecutor = support
            )
        )
    }

    private fun player(
        id: Int,
        strategy: BattleStrategy,
        hand: List<Die> = emptyList(),
        supply: List<Die> = emptyList()
    ): Player =
        Player(
            id = PlayerId(id),
            decisions = DecisionDirector.baseline().copy(battle = strategy),
            dice = PlayerDice(
                supply = supply,
                hand = hand
            )
        )

    private fun finishStrategy(name: String): ScriptedStrategy =
        ScriptedStrategy(name, mutableListOf()).apply {
            first = BattleMainAction.RoundEffect1
            turns += BattleTurnAction.FinalMain(BattleMainAction.RoundEffect1)
        }

    private class ScriptedStrategy(
        private val name: String,
        private val log: MutableList<String>
    ) : BattleStrategy {
        var first: BattleMainAction = BattleMainAction.RoundEffect1
        val turns = ArrayDeque<BattleTurnAction>()
        var placementRow: StrikeRow = StrikeRow.TOP
        val placementRequests = mutableListOf<ChooseBattleDiePlacementRequest>()

        override fun chooseFirstMainAction(
            request: ChooseBattleFirstMainActionRequest
        ): BattleMainAction {
            log += "$name:first"
            return first
        }

        override fun chooseTurnAction(
            request: ChooseBattleTurnActionRequest
        ): BattleTurnAction {
            log += "$name:turn:${request.passNumber}"
            return turns.removeFirst()
        }

        override fun chooseDiePlacement(
            request: ChooseBattleDiePlacementRequest
        ): StrikeRow {
            placementRequests += request
            return placementRow
        }
    }

    private class RecordingFinishStrategy : BattleStrategy {
        var first: BattleMainAction = BattleMainAction.RoundEffect1
        val turnRequests = mutableListOf<ChooseBattleTurnActionRequest>()

        override fun chooseFirstMainAction(
            request: ChooseBattleFirstMainActionRequest
        ) = first

        override fun chooseTurnAction(
            request: ChooseBattleTurnActionRequest
        ): BattleTurnAction {
            turnRequests += request
            return request.legalChoices
                .filterIsInstance<BattleTurnAction.FinalMain>()
                .first()
        }

        override fun chooseDiePlacement(
            request: ChooseBattleDiePlacementRequest
        ) = request.legalRows.first()
    }

    private class RecordingEffects : GameEffectExecutor {
        val requests = mutableListOf<GameEffectRequest>()

        override fun canExecute(request: GameEffectRequest): Boolean = true

        override fun execute(request: GameEffectRequest) {
            requests += request
        }
    }

    private fun roundCard() =
        RoundCard(
            quantity = 1,
            name = "Battle Test",
            type = RoundCardType.BATTLE,
            firstEffect = roundEffect("One", GameEffect.GAIN_ONE_VP),
            secondEffect = roundEffect("Two", GameEffect.GAIN_TWO_WORMS),
            backImage = ""
        )

    private fun roundEffect(
        title: String,
        effect: GameEffect
    ) = RoundCardEffect(
        title = title,
        backgroundColor = "",
        textColor = "",
        image = "",
        icon = null,
        effect = effect
    )

    private fun plant(name: String) =
        PlantCard(
            quantity = 4,
            name = name,
            title = name,
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
            effect = GameEffect.GAIN_ONE_VP
        )

    private fun fixedDie(
        sides: Int,
        value: Int
    ): Die =
        object : Die(sides) {
            init {
                adjustTo(value)
            }

            override fun roll(): Die = this
        }
}
