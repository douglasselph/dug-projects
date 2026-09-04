package dugsolutions.leaf.v35.game.operation

import dugsolutions.leaf.v35.battle.BattleState
import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.GameEffectExecutor
import dugsolutions.leaf.v35.effect.GameEffectPhase
import dugsolutions.leaf.v35.effect.GameEffectRequest
import dugsolutions.leaf.v35.game.GameEngineTestFixture
import dugsolutions.leaf.v35.player.Player
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.DecisionDirector
import dugsolutions.leaf.v35.player.decision.battle.BattleMainAction
import dugsolutions.leaf.v35.player.decision.battle.BattleStrategy
import dugsolutions.leaf.v35.player.decision.battle.BattleTurnAction
import dugsolutions.leaf.v35.player.decision.battle.ChooseBattleDiePlacementRequest
import dugsolutions.leaf.v35.player.decision.battle.ChooseBattleFirstMainActionRequest
import dugsolutions.leaf.v35.player.decision.battle.ChooseBattleTurnActionRequest
import dugsolutions.leaf.v35.player.decision.support.HandDieChoice
import dugsolutions.leaf.v35.player.decision.support.SupportAction
import dugsolutions.leaf.v35.player.dice.PlayerDice
import dugsolutions.leaf.v35.random.die.Die
import dugsolutions.leaf.v35.random.die.DieSides
import dugsolutions.leaf.v35.tokens.Token
import dugsolutions.leaf.v35.wisp.domain.WispCard
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SupportActionExecutorBattleTest {

    @Test
    fun waterReroll_keepsExactGridLocation() {
        val die = fixedDie(8, 5)
        val player = player(1, die = die)
        val other = player(2)
        player.tokens.add(Token.WATER)
        val fixture = fixture(player, other)
        fixture.state.placeInitialHands()

        val before = fixture.state.grid.locationOf(die)
        fixture.executor.executeBattle(
            game = fixture.game,
            player = player,
            battleState = fixture.state,
            action = SupportAction.UseWaterReroll(
                HandDieChoice(0, 8, 5)
            )
        )

        assertEquals(before, fixture.state.grid.locationOf(die))
        assertEquals(0, player.tokens.waterCount)
    }

    @Test
    fun mulch_rollsIntoHandThenBattleStrategyChoosesStrikeRow() {
        val strategy = RowStrategy(StrikeRow.BOTTOM)
        val player = player(1, battle = strategy)
        val other = player(2)
        player.tokens.add(Token.MULCH(DieSides.D6))
        val fixture = fixture(player, other)

        fixture.executor.executeBattle(
            game = fixture.game,
            player = player,
            battleState = fixture.state,
            action = SupportAction.UseMulch(Token.MULCH(DieSides.D6))
        )

        assertEquals(1, player.dice.handSize)
        val gained = player.dice.hand.single()
        assertEquals(StrikeRow.BOTTOM, fixture.state.grid.locationOf(gained)?.row)
        assertEquals(0, player.tokens.mulchCount)
        assertEquals(1, strategy.placementCalls)
    }

    @Test
    fun battleOnlyWisp_executesInBattleAndReceivesBattleState() {
        val player = player(1)
        val other = player(2)
        val card = wisp("Battle Wisp", battleOnly = true)
        player.wisps.add(card)
        val fixture = fixture(player, other)

        fixture.executor.executeBattle(
            game = fixture.game,
            player = player,
            battleState = fixture.state,
            action = SupportAction.PlayWisp(card)
        )

        val request = fixture.effects.requests.single()
        assertEquals(GameEffectPhase.BATTLE, request.phase)
        assertTrue(request.battleState === fixture.state)
        assertEquals(0, player.wisps.size)
    }

    private data class Fixture(
        val game: dugsolutions.leaf.v35.game.Game,
        val state: BattleState,
        val effects: RecordingEffects,
        val executor: SupportActionExecutor
    )

    private fun fixture(
        vararg players: Player
    ): Fixture {
        val game = GameEngineTestFixture.game(players = players.toList())
        val state = BattleState(players.toList())
        val effects = RecordingEffects()
        val roll = RollResolver(
            grove = game.grove,
            chronicle = game.chronicle,
            immediateWispHandler = { _, _ -> Unit }
        )
        return Fixture(
            game = game,
            state = state,
            effects = effects,
            executor = SupportActionExecutor(
                rollResolver = roll,
                refreshResolver = RefreshResolver(game.chronicle),
                effectExecutor = effects
            )
        )
    }

    private fun player(
        id: Int,
        battle: BattleStrategy = RowStrategy(StrikeRow.TOP),
        die: Die? = null
    ): Player =
        Player(
            id = PlayerId(id),
            decisions = DecisionDirector.baseline().copy(battle = battle),
            dice = PlayerDice(
                hand = listOfNotNull(die)
            )
        )

    private class RowStrategy(
        private val row: StrikeRow
    ) : BattleStrategy {
        var placementCalls = 0

        override fun chooseFirstMainAction(
            request: ChooseBattleFirstMainActionRequest
        ): BattleMainAction = request.legalChoices.first()

        override fun chooseTurnAction(
            request: ChooseBattleTurnActionRequest
        ): BattleTurnAction = request.legalChoices.first()

        override fun chooseDiePlacement(
            request: ChooseBattleDiePlacementRequest
        ): StrikeRow {
            placementCalls++
            return row
        }
    }

    private class RecordingEffects : GameEffectExecutor {
        val requests = mutableListOf<GameEffectRequest>()

        override fun canExecute(request: GameEffectRequest): Boolean = true

        override fun execute(request: GameEffectRequest) {
            requests += request
        }
    }

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

    private fun wisp(
        name: String,
        battleOnly: Boolean = false
    ) = WispCard(
        quantity = 1,
        name = name,
        title = name,
        count = 1,
        effect = GameEffect.GAIN_ONE_VP,
        lineIcons = null,
        lineIconsHeight = 0,
        vpIcon = null,
        mainBackdrop = "",
        battleOnly = battleOnly
    )
}
