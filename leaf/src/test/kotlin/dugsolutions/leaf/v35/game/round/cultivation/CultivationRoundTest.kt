package dugsolutions.leaf.v35.game.round.cultivation

import dugsolutions.leaf.v35.chronicle.domain.ChroniclePhase
import dugsolutions.leaf.v35.chronicle.domain.GameEntry
import dugsolutions.leaf.v35.effect.GameEffectExecutor
import dugsolutions.leaf.v35.effect.GameEffectRequest
import dugsolutions.leaf.v35.game.Game
import dugsolutions.leaf.v35.game.GameEngineTestFixture
import dugsolutions.leaf.v35.game.round.RoundCoordinator
import dugsolutions.leaf.v35.game.round.RoundExecutor
import dugsolutions.leaf.v35.player.Player
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.DecisionDirector
import dugsolutions.leaf.v35.player.decision.buy.BuyChoice
import dugsolutions.leaf.v35.player.decision.buy.BuyItem
import dugsolutions.leaf.v35.player.decision.buy.BuyPayment
import dugsolutions.leaf.v35.player.decision.buy.BuyStrategy
import dugsolutions.leaf.v35.player.decision.buy.ChoosePaymentRequest
import dugsolutions.leaf.v35.player.decision.buy.ChoosePurchaseRequest
import dugsolutions.leaf.v35.player.decision.cultivation.ChooseCultivationActionRequest
import dugsolutions.leaf.v35.player.decision.cultivation.CultivationAction
import dugsolutions.leaf.v35.player.decision.cultivation.CultivationMainAction
import dugsolutions.leaf.v35.player.decision.cultivation.CultivationStrategy
import dugsolutions.leaf.v35.player.dice.PlayerDice
import dugsolutions.leaf.v35.random.die.Die
import dugsolutions.leaf.v35.round.domain.RoundCard
import dugsolutions.leaf.v35.round.domain.RoundCardType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CultivationRoundTest {

    @Test
    fun executeRound_runsBuildThenBuyThenCleanupAndReturnsAllPhaseResults() {
        val first = player(1, listOf(die(8, 5)), DoneBuyStrategy())
        val second = player(2, listOf(die(6, 3)), DoneBuyStrategy())
        val game = game(first, second)
        val card = checkNotNull(game.roundDeck.next())
        val effects = RecordingEffectExecutor()
        val round = CultivationRound(effects)

        val result = round.executeRound(game, card)

        assertEquals(4, result.build.actions.size)
        assertEquals(listOf(first.id, second.id), result.buy.order)
        assertTrue(result.buy.purchases.isEmpty())
        assertEquals(2, result.cleanup.totalDiscardedDice)
        assertTrue(first.dice.hand.isEmpty())
        assertTrue(second.dice.hand.isEmpty())
        assertEquals(1, first.dice.discardSize)
        assertEquals(1, second.dice.discardSize)

        val entries = game.chronicle.entries
        val actionIndex = entries.indexOfFirst { it is GameEntry.MainAction }
        val buyIndex = entries.indexOfFirst { it is GameEntry.BuyOrder }
        val cleanupIndex = entries.indexOfFirst {
            it is GameEntry.Cleanup && it.phase == ChroniclePhase.CULTIVATION
        }
        assertTrue(actionIndex >= 0)
        assertTrue(actionIndex < buyIndex)
        assertTrue(buyIndex < cleanupIndex)
    }

    @Test
    fun executeRound_effectRequestsCarryTheExactGameContext() {
        val first = player(1, emptyList(), DoneBuyStrategy())
        val second = player(2, emptyList(), DoneBuyStrategy())
        val game = game(first, second)
        val card = checkNotNull(game.roundDeck.next())
        val effects = RecordingEffectExecutor()

        CultivationRound(effects).executeRound(game, card)

        assertEquals(4, effects.requests.size)
        assertTrue(effects.requests.all { it.game === game })
    }

    @Test
    fun executeRound_boughtPlantEntersFaceDownThenRefreshesAtCleanup() {
        val first = player(1, listOf(die(20, 20)), BuyOnePlantStrategy())
        val second = player(2, emptyList(), DoneBuyStrategy())
        val game = game(first, second)
        val card = checkNotNull(game.roundDeck.next())

        val result = CultivationRound(RecordingEffectExecutor())
            .executeRound(game, card)

        assertEquals(1, result.buy.purchases.size)
        assertTrue(result.buy.purchases.single().item is BuyItem.Plant)
        assertEquals(1, first.creature.size)
        assertTrue(first.creature.cards.single().isFaceUp)
        assertEquals(listOf(first.id), result.cleanup.refreshedPlayers)
    }

    @Test
    fun executeRound_boughtDieAndSpentPaymentRemainInDiscardAfterCleanup() {
        val first = player(1, listOf(die(20, 20)), BuyOneD6Strategy())
        val second = player(2, emptyList(), DoneBuyStrategy())
        val game = game(first, second)
        val card = checkNotNull(game.roundDeck.next())

        val result = CultivationRound(RecordingEffectExecutor())
            .executeRound(game, card)

        assertEquals(1, result.buy.purchases.size)
        assertEquals(2, first.dice.discardSize)
        assertTrue(first.dice.hand.isEmpty())
        assertEquals(0, result.cleanup.players.first().discardedDice)
        assertEquals(8, game.grove.graftBed.count(dugsolutions.leaf.v35.random.die.DieSides.D6))
    }

    @Test
    fun execute_asRoundExecutor_plugsDirectlyIntoRoundCoordinator() {
        val first = player(1, emptyList(), DoneBuyStrategy())
        val second = player(2, emptyList(), DoneBuyStrategy())
        val game = game(first, second)
        val round = CultivationRound(RecordingEffectExecutor())
        var battleCalled = false
        val coordinator = RoundCoordinator(
            cultivation = round,
            battle = RoundExecutor { _, _ -> battleCalled = true }
        )

        val execution = checkNotNull(coordinator.executeNext(game))

        assertEquals(RoundCardType.CULTIVATION, execution.card.type)
        assertEquals(1, execution.roundNumber)
        assertTrue(!battleCalled)
        val entries = game.chronicle.entries
        assertTrue(entries.first() is GameEntry.RoundRevealed)
        assertTrue(entries.last() is GameEntry.RoundCompleted)
        assertTrue(entries.any {
            it is GameEntry.Cleanup && it.phase == ChroniclePhase.CULTIVATION
        })
    }

    @Test
    fun executeRound_rejectsBattleCardBeforePlayerMutation() {
        val firstDie = die(8, 7)
        val first = player(1, listOf(firstDie), DoneBuyStrategy())
        val second = player(2, emptyList(), DoneBuyStrategy())
        val game = GameEngineTestFixture.game(
            cultivationRounds = 0,
            battleRounds = 1,
            players = listOf(first, second)
        )
        val battleCard = checkNotNull(game.roundDeck.next())
        assertEquals(RoundCardType.BATTLE, battleCard.type)

        assertFailsWith<IllegalArgumentException> {
            CultivationRound(RecordingEffectExecutor())
                .executeRound(game, battleCard)
        }

        assertEquals(listOf(firstDie), first.dice.hand)
        assertTrue(first.dice.discard.isEmpty())
        assertTrue(game.chronicle.entries.isEmpty())
    }

    private fun game(first: Player, second: Player): Game =
        GameEngineTestFixture.game(
            cultivationRounds = 1,
            battleRounds = 0,
            players = listOf(first, second)
        )

    private fun player(
        id: Int,
        hand: List<Die>,
        buy: BuyStrategy
    ): Player =
        Player(
            id = PlayerId(id),
            decisions = DecisionDirector.baseline().copy(
                cultivation = RoundEffectStrategy(),
                buy = buy
            ),
            dice = PlayerDice(hand = hand)
        )

    private class RoundEffectStrategy : CultivationStrategy {
        override fun chooseAction(
            request: ChooseCultivationActionRequest
        ): CultivationAction =
            if (request.mainActionsRemaining > 0) {
                CultivationAction.Main(CultivationMainAction.RoundEffect1)
            } else {
                CultivationAction.Done
            }
    }

    private class DoneBuyStrategy : BuyStrategy {
        override fun choosePurchase(request: ChoosePurchaseRequest): BuyChoice =
            BuyChoice.Done

        override fun choosePayment(request: ChoosePaymentRequest): BuyPayment =
            BuyPayment()
    }

    private abstract class BuyOnceStrategy : BuyStrategy {
        private var bought = false

        final override fun choosePurchase(request: ChoosePurchaseRequest): BuyChoice {
            if (bought) return BuyChoice.Done
            val item = chooseItem(request.options)
            bought = true
            return BuyChoice.Purchase(item)
        }

        final override fun choosePayment(request: ChoosePaymentRequest): BuyPayment =
            BuyPayment(
                dice = listOf(request.availableDice.first())
            )

        protected abstract fun chooseItem(options: List<BuyItem>): BuyItem
    }

    private class BuyOnePlantStrategy : BuyOnceStrategy() {
        override fun chooseItem(options: List<BuyItem>): BuyItem =
            options.filterIsInstance<BuyItem.Plant>().first()
    }

    private class BuyOneD6Strategy : BuyOnceStrategy() {
        override fun chooseItem(options: List<BuyItem>): BuyItem =
            options.filterIsInstance<BuyItem.Die>()
                .first { it.sides.value == 6 }
    }

    private class RecordingEffectExecutor : GameEffectExecutor {
        val requests = mutableListOf<GameEffectRequest>()

        override fun execute(request: GameEffectRequest) {
            requests.add(request)
        }
    }

    private fun die(sides: Int, value: Int): Die = FixedDie(sides, value)

    private class FixedDie(sides: Int, value: Int) : Die(sides) {
        init {
            adjustTo(value)
        }

        override fun roll(): Die = this
    }

}
