package dugsolutions.leaf.v30.game.round

import dugsolutions.leaf.v30.cards.GameCardRegistry
import dugsolutions.leaf.v30.cards.domain.GameCards
import dugsolutions.leaf.v30.common.Commons
import dugsolutions.leaf.v30.common.Critter
import dugsolutions.leaf.v30.common.Critters
import dugsolutions.leaf.v30.common.Token
import dugsolutions.leaf.v30.game.domain.MainActionException
import dugsolutions.leaf.v30.game.effect.GameCardEffectExecutorCultivation
import dugsolutions.leaf.v30.game.effect.RoundActionExecutor
import dugsolutions.leaf.v30.game.effect.WispCardEffectExecutor
import dugsolutions.leaf.v30.grove.Grove
import dugsolutions.leaf.v30.grove.domain.GroveCardStackID
import dugsolutions.leaf.v30.player.Player
import dugsolutions.leaf.v30.player.decision.domain.CardsToRefresh
import dugsolutions.leaf.v30.player.decision.domain.Decision
import dugsolutions.leaf.v30.player.decision.domain.DecisionDirector
import dugsolutions.leaf.v30.player.decision.domain.ItemsToBuy
import dugsolutions.leaf.v30.player.decision.domain.MainActionBattle
import dugsolutions.leaf.v30.player.decision.domain.MainActionCultivation
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
import dugsolutions.leaf.v30.wisp.domain.WispCard
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RoundCultivationTest {

    @Test
    fun performMainActions_whenDecisionIsPullDie_drawsTwoDiceForEachPlayer() {
        val dice = SampleDie(Randomizer.create(seed = 2L))
        val player1 = Player(StaticMainActionDirector(MainActionCultivation.PullDie))
        val player2 = Player(StaticMainActionDirector(MainActionCultivation.PullDie))
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
        val player = Player(StaticMainActionDirector(MainActionCultivation.DoRoundAction(RoundAction.ACTION_2)))
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
        val player = Player(StaticMainActionDirector(MainActionCultivation.ExecuteCard(card)))
        player.addCardToCreature(CreatureCard(card, CreatureCard.Facing.FACE_UP))
        val table = createTable().add(player)
        table.roundDeck.next()
        val gameCardEffectExecutor = TrackingGameCardEffectExecutor()
        val round = RoundCultivation(
            table = table,
            card = loadCultivationCard(),
            gameCardEffectExecutor = gameCardEffectExecutor
        )

        round.performMainActions()

        assertEquals(listOf(card, card), gameCardEffectExecutor.actions.map { it.card })
        assertEquals(true, player.getCreatureLeftCard(0)!!.isFaceDown)
    }

    @Test
    fun performMainActions_whenDecisionIsWispCard_doesNotSpendMainAction() {
        val wispCard = loadWispCard()
        val player = Player(
            SequenceMainActionDirector(
                listOf(
                    MainActionCultivation.PlayWispCard(wispCard),
                    MainActionCultivation.PullDie,
                    MainActionCultivation.PullDie
                )
            )
        )
        val dice = SampleDie(Randomizer.create(seed = 3L))
        player.addDiceToSupply(listOf(dice.d4, dice.d6))
        val table = createTable().add(player)
        val wispCardEffectExecutor = TrackingWispCardEffectExecutor()
        val round = RoundCultivation(
            table = table,
            card = loadCultivationCard(),
            wispCardEffectExecutor = wispCardEffectExecutor
        )

        round.performMainActions()

        assertEquals(listOf(wispCard), wispCardEffectExecutor.cards)
        assertEquals(2, player.diceHand.size)
    }

    @Test
    fun performMainActions_whenDecisionIsPlayMulchToken_addsRolledDieToHandAndDoesNotSpendMainAction() {
        val dice = SampleDie(Randomizer.create(seed = 4L))
        val player = Player(
            SequenceMainActionDirector(
                listOf(
                    MainActionCultivation.PlayMulchToken(Token.MULCH(DieSides.D8)),
                    MainActionCultivation.PullDie,
                    MainActionCultivation.PullDie
                )
            )
        )
        player.add(Token.MULCH(DieSides.D8))
        player.addDiceToSupply(listOf(dice.d4, dice.d6))
        val table = createTable().add(player)
        val round = RoundCultivation(
            table = table,
            card = loadCultivationCard(),
            dieFactory = DieFactory(IdentityRandomizer())
        )

        round.performMainActions()

        assertEquals(emptyList(), player.mulchTokens)
        assertEquals(3, player.diceHand.size)
        assertTrue(player.diceHand.dice.any { it.sides == 8 })
        assertTrue(player.critters.contains(Critter.BEE))
        assertTrue(table.grove.count(Critter.BEE) < 9)
    }

    @Test
    fun performMainActions_whenDecisionIsPlayWaterTokenWithoutDie_refreshesCreatureAndDoesNotSpendMainAction() {
        val card = loadGameCard("Root_05_01")
        val dice = SampleDie(Randomizer.create(seed = 5L))
        val player = Player(
            SequenceMainActionDirector(
                listOf(
                    MainActionCultivation.PlayWaterToken(),
                    MainActionCultivation.PullDie,
                    MainActionCultivation.PullDie
                )
            )
        )
        player.add(Token.WATER)
        player.addCardToCreature(card)
        player.addDiceToSupply(listOf(dice.d4, dice.d6))
        val table = createTable().add(player)
        val round = RoundCultivation(table, loadCultivationCard())

        round.performMainActions()

        assertEquals(0, player.waterTokenCount)
        assertTrue(player.creatureCards.all { it.isFaceUp })
        assertEquals(2, player.diceHand.size)
    }

    @Test
    fun performMainActions_whenDecisionIsPlayWaterTokenWithDie_rerollsHandDieAndDoesNotSpendMainAction() {
        val handDie = TrackingDie(6)
        val dice = SampleDie(Randomizer.create(seed = 6L))
        val player = Player(
            SequenceMainActionDirector(
                listOf(
                    MainActionCultivation.PlayWaterToken(onDie = FixedDie(6, 1)),
                    MainActionCultivation.PullDie,
                    MainActionCultivation.PullDie
                )
            )
        )
        player.add(Token.WATER)
        player.addDieToHand(handDie)
        player.addDiceToSupply(listOf(dice.d4, dice.d8))
        val table = createTable().add(player)
        val round = RoundCultivation(table, loadCultivationCard())

        round.performMainActions()

        assertEquals(0, player.waterTokenCount)
        assertEquals(1, handDie.rollCount)
        assertEquals(3, player.diceHand.size)
    }

    @Test
    fun performMainActions_whenDecisionAlwaysReturnsWispCard_throwsAfterSafeguardLimit() {
        val wispCard = loadWispCard()
        val player = Player(StaticMainActionDirector(MainActionCultivation.PlayWispCard(wispCard)))
        val table = createTable().add(player)
        val round = RoundCultivation(
            table = table,
            card = loadCultivationCard(),
            wispCardEffectExecutor = TrackingWispCardEffectExecutor()
        )

        assertThrows<MainActionException> {
            round.performMainActions()
        }
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

    private fun loadWispCard(): WispCard =
        WispCardRegistry()
            .apply { loadFromCsv(Commons.WISP_LIST) }
            .getAllCards()
            .first()

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
        private val action: MainActionCultivation
    ) : DecisionDirector {
        override fun chooseCritter(input: Decision.ChooseCritter): Critter = Critter.BEE
        override fun chooseMainActionCultivation(input: Decision.ChooseMainActionCultivation): MainActionCultivation = action
        override fun chooseMainActionBattle(input: Decision.ChooseMainActionBattle): MainActionBattle {
            return MainActionBattle.PullDie(dugsolutions.leaf.v30.battle.domain.BattleStrikeRow.STRIKE_1)
        }
        override fun chooseItemsToBuy(input: Decision.ChooseItemsToBuy): ItemsToBuy = ItemsToBuy()
        override fun chooseCardsToRefreshWithWorms(input: Decision.ChooseCardsToRefreshWithWorms): CardsToRefresh = CardsToRefresh()
        override fun chooseFlipOrSnipCard(input: Decision.ChooseFlipOrSnipCard): CreatureCard = input.creatureCards.first()
    }

    private class SequenceMainActionDirector(
        private val actions: List<MainActionCultivation>
    ) : DecisionDirector {
        private var index = 0

        override fun chooseCritter(input: Decision.ChooseCritter): Critter = Critter.BEE
        override fun chooseMainActionCultivation(input: Decision.ChooseMainActionCultivation): MainActionCultivation {
            return actions.getOrElse(index++) { actions.last() }
        }
        override fun chooseMainActionBattle(input: Decision.ChooseMainActionBattle): MainActionBattle {
            return MainActionBattle.PullDie(dugsolutions.leaf.v30.battle.domain.BattleStrikeRow.STRIKE_1)
        }
        override fun chooseItemsToBuy(input: Decision.ChooseItemsToBuy): ItemsToBuy = ItemsToBuy()
        override fun chooseCardsToRefreshWithWorms(input: Decision.ChooseCardsToRefreshWithWorms): CardsToRefresh = CardsToRefresh()
        override fun chooseFlipOrSnipCard(input: Decision.ChooseFlipOrSnipCard): CreatureCard = input.creatureCards.first()
    }

    private class BuyingDecisionDirector(
        private val itemsToBuy: ItemsToBuy
    ) : DecisionDirector {
        override fun chooseCritter(input: Decision.ChooseCritter): Critter = Critter.BEE
        override fun chooseMainActionCultivation(input: Decision.ChooseMainActionCultivation): MainActionCultivation = MainActionCultivation.PullDie
        override fun chooseMainActionBattle(input: Decision.ChooseMainActionBattle): MainActionBattle {
            return MainActionBattle.PullDie(dugsolutions.leaf.v30.battle.domain.BattleStrikeRow.STRIKE_1)
        }
        override fun chooseItemsToBuy(input: Decision.ChooseItemsToBuy): ItemsToBuy = itemsToBuy
        override fun chooseCardsToRefreshWithWorms(input: Decision.ChooseCardsToRefreshWithWorms): CardsToRefresh = CardsToRefresh()
        override fun chooseFlipOrSnipCard(input: Decision.ChooseFlipOrSnipCard): CreatureCard = input.creatureCards.first()
    }

    private class TrackingRoundActionExecutor : RoundActionExecutor() {
        val actions = mutableListOf<RoundAction>()

        override fun invoke(
            table: Table,
            player: Player,
            card: RoundCard,
            action: RoundAction
        ) {
            actions.add(action)
        }
    }

    private class TrackingGameCardEffectExecutor : GameCardEffectExecutorCultivation() {
        val actions = mutableListOf<MainActionCultivation.ExecuteCard>()

        override fun invoke(
            table: Table,
            player: Player,
            action: MainActionCultivation.ExecuteCard
        ) {
            actions.add(action)
        }
    }

    private class TrackingWispCardEffectExecutor : WispCardEffectExecutor() {
        val cards = mutableListOf<WispCard>()

        override fun invoke(
            table: Table,
            player: Player,
            card: WispCard
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

    private class TrackingDie(sides: Int) : dugsolutions.leaf.v30.random.die.Die(sides) {
        var rollCount = 0

        override fun roll(): dugsolutions.leaf.v30.random.die.Die {
            rollCount++
            return this
        }
    }
}
