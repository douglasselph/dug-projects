package dugsolutions.leaf.v30.game.round

import dugsolutions.leaf.v30.cards.GameCardRegistry
import dugsolutions.leaf.v30.cards.domain.GameCard
import dugsolutions.leaf.v30.cards.domain.GameCards
import dugsolutions.leaf.v30.common.Commons
import dugsolutions.leaf.v30.common.Critter
import dugsolutions.leaf.v30.common.Critters
import dugsolutions.leaf.v30.game.effect.GameCardEffectExecutor
import dugsolutions.leaf.v30.game.effect.RoundActionExecutor
import dugsolutions.leaf.v30.grove.Grove
import dugsolutions.leaf.v30.grove.domain.GroveCardStackID
import dugsolutions.leaf.v30.player.Player
import dugsolutions.leaf.v30.player.decision.domain.Decision
import dugsolutions.leaf.v30.player.decision.domain.DecisionDirector
import dugsolutions.leaf.v30.player.decision.domain.ItemsToBuy
import dugsolutions.leaf.v30.player.decision.domain.MainAction
import dugsolutions.leaf.v30.player.decision.domain.RoundAction
import dugsolutions.leaf.v30.player.domain.CreatureCard
import dugsolutions.leaf.v30.random.Randomizer
import dugsolutions.leaf.v30.random.die.DieSides
import dugsolutions.leaf.v30.random.die.SampleDie
import dugsolutions.leaf.v30.random.die.di.DieFactory
import dugsolutions.leaf.v30.round.RoundCardManager
import dugsolutions.leaf.v30.round.RoundCardRegistry
import dugsolutions.leaf.v30.round.RoundDeck
import dugsolutions.leaf.v30.round.di.RoundCardsFactory
import dugsolutions.leaf.v30.round.domain.RoundCard
import dugsolutions.leaf.v30.table.Table
import dugsolutions.leaf.v30.wisp.WispCardManager
import dugsolutions.leaf.v30.wisp.WispCardRegistry
import dugsolutions.leaf.v30.wisp.WispDeck
import dugsolutions.leaf.v30.wisp.di.WispCardsFactory
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class RoundCultivationTest {

    @Test
    fun performMainActions_whenDecisionIsPullDie_drawsTwoDiceForEachPlayer() {
        val dice = SampleDie(Randomizer.create(seed = 2L))
        val player1 = Player(StaticMainActionDirector(MainAction.PullDie))
        val player2 = Player(StaticMainActionDirector(MainAction.PullDie))
        player1.addDiceToSupply(listOf(dice.d4, dice.d6))
        player2.addDiceToSupply(listOf(dice.d8, dice.d10))
        val table = createTable().add(player1).add(player2)
        val round = RoundCultivation(table, loadCultivationCard())

        round.performMainActions()

        assertEquals(2, player1.diceHand.size)
        assertEquals(2, player2.diceHand.size)
    }

    @Test
    fun performMainActions_whenDecisionIsRoundAction_dispatchesRoundActionExecutorTwice() {
        val player = Player(StaticMainActionDirector(MainAction.DoRoundAction(RoundAction.ACTION_2)))
        val table = createTable().add(player)
        val roundActionExecutor = TrackingRoundActionExecutor()
        val round = RoundCultivation(
            table = table,
            card = loadCultivationCard(),
            roundActionExecutor = roundActionExecutor
        )

        round.performMainActions()

        assertEquals(listOf(RoundAction.ACTION_2, RoundAction.ACTION_2), roundActionExecutor.actions)
    }

    @Test
    fun performMainActions_whenDecisionIsExecuteCard_dispatchesGameCardEffectExecutorTwice() {
        val card = loadGameCard("Root_05_01")
        val player = Player(StaticMainActionDirector(MainAction.ExecuteCard(card)))
        player.addCardToCreature(CreatureCard(card, CreatureCard.Facing.FACE_UP))
        val table = createTable().add(player)
        val gameCardEffectExecutor = TrackingGameCardEffectExecutor()
        val round = RoundCultivation(
            table = table,
            card = loadCultivationCard(),
            gameCardEffectExecutor = gameCardEffectExecutor
        )

        round.performMainActions()

        assertEquals(listOf(card, card), gameCardEffectExecutor.cards)
        assertEquals(true, player.getCreatureLeftCard(0)!!.isFaceDown)
    }

    @Test
    fun performBuy_buysDiceCardsRemovesCrittersAndDiscardsHandDiceInPlayerOrder() {
        val card = loadGameCard("Root_05_01")
        val player = Player(
            BuyingDecisionDirector(
                ItemsToBuy(
                    dice = listOf(DieSides.D6),
                    cards = GameCards(listOf(card)),
                    crittersUsed = Critters(listOf(Critter.BEE))
                )
            )
        )
        player.addCritter(Critter.BEE)
        player.addDieToSupply(FixedDie(6, 5))
        player.drawDie()
        val table = createTable().add(player)
        table.grove.resetDice(numPlayers = 2)
        table.grove.setCard(card)
        val round = RoundCultivation(
            table = table,
            card = loadCultivationCard(),
            dieFactory = DieFactory(IdentityRandomizer())
        )

        round.performBuy()

        assertEquals(6, table.grove.diceStacks.getCount(DieSides.D6))
        assertEquals(7, table.grove.cardStacks.getCount(GroveCardStackID.ROOT_5))
        assertEquals(0, player.critters.count { it == Critter.BEE })
        assertEquals(listOf(card), player.creatureLeftCards.map { it.card })
        assertEquals(2, player.diceDiscard.size)
        assertEquals(6, player.diceDiscard.dice.maxOf { it.sides })
        assertEquals(0, player.diceHand.size)
    }

    private fun createTable(): Table {
        return Table(
            grove = Grove(createWispDeck()),
            roundDeck = createRoundDeck()
        )
    }

    private fun loadCultivationCard() =
        RoundCardRegistry()
            .apply { loadFromCsv(Commons.ROUND_CARD_LIST) }
            .getCard("Resource_Compost_Mulch")
            ?: error("Missing test round card")

    private fun loadGameCard(name: String) =
        GameCardRegistry()
            .apply { loadFromCsv(Commons.CARD_LIST) }
            .getCard(name)
            ?: error("Missing test game card: $name")

    private fun createRoundDeck(): RoundDeck {
        val registry = RoundCardRegistry()
        registry.loadFromCsv(Commons.ROUND_CARD_LIST)
        val manager = RoundCardManager(RoundCardsFactory())
        manager.loadCards(registry)
        return RoundDeck(manager, IdentityRandomizer())
    }

    private fun createWispDeck(): WispDeck {
        val registry = WispCardRegistry()
        registry.loadFromCsv(Commons.WISP_LIST)
        val manager = WispCardManager(WispCardsFactory())
        manager.loadCards(registry)
        return WispDeck(manager, IdentityRandomizer())
    }

    private class StaticMainActionDirector(
        private val action: MainAction
    ) : DecisionDirector {
        override fun chooseCritter(input: Decision.ChooseCritter): Critter = Critter.BEE
        override fun chooseMainAction(input: Decision.ChooseMainAction): MainAction = action
        override fun chooseItemsToBuy(input: Decision.ChooseItemsToBuy): ItemsToBuy = ItemsToBuy()
    }

    private class BuyingDecisionDirector(
        private val itemsToBuy: ItemsToBuy
    ) : DecisionDirector {
        override fun chooseCritter(input: Decision.ChooseCritter): Critter = Critter.BEE
        override fun chooseMainAction(input: Decision.ChooseMainAction): MainAction = MainAction.PullDie
        override fun chooseItemsToBuy(input: Decision.ChooseItemsToBuy): ItemsToBuy = itemsToBuy
    }

    private class TrackingRoundActionExecutor : RoundActionExecutor() {
        val actions = mutableListOf<RoundAction>()

        override fun execute(
            table: Table,
            player: Player,
            card: RoundCard,
            action: RoundAction
        ) {
            actions.add(action)
        }
    }

    private class TrackingGameCardEffectExecutor : GameCardEffectExecutor() {
        val cards = mutableListOf<GameCard>()

        override fun execute(
            table: Table,
            player: Player,
            card: GameCard
        ) {
            cards.add(card)
        }
    }

    private class IdentityRandomizer : Randomizer {
        override fun nextBoolean(): Boolean = throw UnsupportedOperationException()
        override fun nextInt(from: Int, until: Int): Int = from
        override fun nextInt(until: Int): Int = throw UnsupportedOperationException()
        override fun <T> randomOrNull(list: List<T>): T? = throw UnsupportedOperationException()
        override fun <T> shuffled(list: List<T>): List<T> = list
    }

    private class FixedDie(
        sides: Int,
        value: Int
    ) : dugsolutions.leaf.v30.random.die.Die(sides) {
        init {
            adjustTo(value)
        }

        override fun roll() = this
    }
}
