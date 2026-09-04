package dugsolutions.leaf.v35.game.operation

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.GameEffectExecutor
import dugsolutions.leaf.v35.effect.GameEffectRequest
import dugsolutions.leaf.v35.effect.GameEffectSource
import dugsolutions.leaf.v35.game.GameEngineTestFixture
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.Player
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.creature.CreaturePosition
import dugsolutions.leaf.v35.player.creature.CreatureSide
import dugsolutions.leaf.v35.player.creature.GraftPlacement
import dugsolutions.leaf.v35.player.decision.DecisionDirector
import dugsolutions.leaf.v35.player.decision.support.ButterflyRollChoice
import dugsolutions.leaf.v35.player.decision.support.ChooseButterflyRollRequest
import dugsolutions.leaf.v35.player.decision.support.HandDieChoice
import dugsolutions.leaf.v35.player.decision.support.SupportAction
import dugsolutions.leaf.v35.player.decision.support.SupportStrategy
import dugsolutions.leaf.v35.player.dice.PlayerDice
import dugsolutions.leaf.v35.random.die.Die
import dugsolutions.leaf.v35.random.die.DieSides
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

class SupportActionExecutorTest {

    @Test
    fun waterReroll_spendsAndReturnsWater_andResolvesRollReward() {
        val die = FixedRollDie(6, initial = 5, rolled = 1)
        val player = player(hand = listOf(die))
        player.tokens.add(Token.WATER)
        val fixture = fixture(player)
        val groveBefore = fixture.game.grove.tokens.waterCount

        fixture.executor.executeCultivation(
            fixture.game,
            player,
            SupportAction.UseWaterReroll(HandDieChoice(0, 6, 5))
        )

        assertEquals(1, die.value)
        assertEquals(0, player.tokens.waterCount)
        assertEquals(groveBefore + 1, fixture.game.grove.tokens.waterCount)
        assertEquals(listOf(Critter.BEE), player.critters.all)
    }

    @Test
    fun waterRefresh_forcesPlantsAndButterfliesFaceUp() {
        val player = player()
        player.tokens.add(Token.WATER)
        val grafted = player.creature.graft(
            plant("Vine"),
            GraftPlacement(CreatureSide.LEFT, CreaturePosition(-1, 0))
        )
        player.butterflies.add(Butterfly.GREEN)
        player.butterflies.faceDown(Butterfly.GREEN)
        val fixture = fixture(player)

        fixture.executor.executeCultivation(
            fixture.game,
            player,
            SupportAction.UseWaterRefresh
        )

        assertTrue(player.creature.get(grafted.id)!!.isFaceUp)
        assertTrue(player.butterflies.isFaceUp(Butterfly.GREEN))
        assertEquals(0, player.tokens.waterCount)
    }

    @Test
    fun mulch_rollsStoredDieIntoHand_resolvesReward_andReturnsEmptyToken() {
        val player = player()
        val stored = Token.MULCH(DieSides.D6)
        player.tokens.add(stored)
        val fixture = fixture(player)
        val groveBefore = fixture.game.grove.tokens.mulchCount
        fixture.game.dieFactory.config = dugsolutions.leaf.v35.random.die.di.DieFactory.Config.UNIFORM

        fixture.executor.executeCultivation(
            fixture.game,
            player,
            SupportAction.UseMulch(stored)
        )

        assertEquals(0, player.tokens.mulchCount)
        assertEquals(1, player.dice.handSize)
        assertEquals(6, player.dice.hand.single().sides)
        assertEquals(groveBefore + 1, fixture.game.grove.tokens.mulchCount)
    }

    @Test
    fun wormFlip_spendsWorm_flipsCard_andReturnsWormToGrove() {
        val player = player()
        player.critters.add(Critter.WORM)
        val grafted = player.creature.graft(
            plant("Vine"),
            GraftPlacement(CreatureSide.LEFT, CreaturePosition(-1, 0))
        )
        val fixture = fixture(player)
        val groveBefore = fixture.game.grove.critters.count(Critter.WORM)

        fixture.executor.executeCultivation(
            fixture.game,
            player,
            SupportAction.UseWormFlip(grafted.id)
        )

        assertTrue(player.creature.get(grafted.id)!!.isFaceUp)
        assertTrue(player.critters.isEmpty)
        assertEquals(groveBefore + 1, fixture.game.grove.critters.count(Critter.WORM))
    }

    @Test
    fun butterfly_canKeepOriginalAfterReroll_andFlipsFaceDown() {
        val die = FixedRollDie(8, initial = 7, rolled = 1)
        val support = object : SupportStrategy {
            override fun chooseButterflyRoll(
                request: ChooseButterflyRollRequest
            ) = ButterflyRollChoice.ORIGINAL
        }
        val player = player(hand = listOf(die), support = support)
        player.butterflies.add(Butterfly.PURPLE)
        val fixture = fixture(player)

        fixture.executor.executeCultivation(
            fixture.game,
            player,
            SupportAction.UseButterfly(
                Butterfly.PURPLE,
                HandDieChoice(0, 8, 7)
            )
        )

        assertEquals(7, die.value)
        assertTrue(player.butterflies.isFaceDown(Butterfly.PURPLE))
        assertEquals(listOf(Critter.BEE), player.critters.all) // reroll reward still resolves
    }

    @Test
    fun normalWisp_executesEffectThenLeavesHand() {
        val player = player()
        val card = wisp("Normal", GameEffect.GAIN_ONE_VP)
        player.wisps.add(card)
        val fixture = fixture(player)

        fixture.executor.executeCultivation(
            fixture.game,
            player,
            SupportAction.PlayWisp(card)
        )

        assertTrue(player.wisps.isEmpty)
        val request = fixture.effects.requests.single()
        assertEquals(GameEffect.GAIN_ONE_VP, request.effect)
        assertEquals(card, assertIs<GameEffectSource.Wisp>(request.source).card)
    }

    @Test
    fun immediateOrBattleOnlyWisp_isRejectedBeforeEffectExecution() {
        val player = player()
        val immediate = wisp("Immediate", playImmediately = true)
        player.wisps.add(immediate)
        val fixture = fixture(player)

        assertFailsWith<IllegalStateException> {
            fixture.executor.executeCultivation(
                fixture.game,
                player,
                SupportAction.PlayWisp(immediate)
            )
        }

        assertTrue(player.wisps.isNotEmpty)
        assertTrue(fixture.effects.requests.isEmpty())
    }

    @Test
    fun staleHandDieChoice_isRejectedBeforeWaterIsSpent() {
        val die = FixedRollDie(6, initial = 5, rolled = 3)
        val player = player(hand = listOf(die))
        player.tokens.add(Token.WATER)
        val fixture = fixture(player)

        assertFailsWith<IllegalStateException> {
            fixture.executor.executeCultivation(
                fixture.game,
                player,
                SupportAction.UseWaterReroll(HandDieChoice(0, 6, 4))
            )
        }

        assertEquals(1, player.tokens.waterCount)
        assertEquals(5, die.value)
    }

    private fun fixture(player: Player): Fixture {
        val second = player(id = 2)
        val game = GameEngineTestFixture.game(
            cultivationRounds = 1,
            battleRounds = 0,
            players = listOf(player, second)
        )
        val effects = RecordingEffectExecutor()
        val roll = RollResolver(game.grove, game.chronicle)
        return Fixture(
            game = game,
            effects = effects,
            executor = SupportActionExecutor(
                rollResolver = roll,
                refreshResolver = RefreshResolver(game.chronicle),
                effectExecutor = effects
            )
        )
    }

    private fun player(
        id: Int = 1,
        hand: List<Die> = emptyList(),
        support: SupportStrategy = DecisionDirector.baseline().support
    ): Player = Player(
        id = PlayerId(id),
        decisions = DecisionDirector.baseline().copy(support = support),
        dice = PlayerDice(hand = hand)
    )

    private fun plant(name: String) = PlantCard(
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
        effect = GameEffect.UNKNOWN
    )

    private fun wisp(
        name: String,
        effect: GameEffect = GameEffect.UNKNOWN,
        playImmediately: Boolean = false,
        battleOnly: Boolean = false
    ) = WispCard(
        quantity = 1,
        name = name,
        title = name,
        count = 1,
        effect = effect,
        lineIcons = null,
        lineIconsHeight = 0,
        vpIcon = null,
        mainBackdrop = "",
        playImmediately = playImmediately,
        battleOnly = battleOnly
    )

    private class FixedRollDie(
        sides: Int,
        initial: Int,
        private val rolled: Int
    ) : Die(sides) {
        init {
            adjustTo(initial)
        }

        override fun roll(): Die {
            adjustTo(rolled)
            return this
        }
    }

    private class RecordingEffectExecutor : GameEffectExecutor {
        val requests = mutableListOf<GameEffectRequest>()
        override fun execute(request: GameEffectRequest) {
            requests.add(request)
        }
    }

    private data class Fixture(
        val game: dugsolutions.leaf.v35.game.Game,
        val effects: RecordingEffectExecutor,
        val executor: SupportActionExecutor
    )
}
