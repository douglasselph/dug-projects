package dugsolutions.leaf.v30.game.round

import dugsolutions.leaf.v30.battle.Battle
import dugsolutions.leaf.v30.battle.PlayerGridOrder
import dugsolutions.leaf.v30.battle.domain.BattleItem
import dugsolutions.leaf.v30.battle.domain.BattleStrikeRow
import dugsolutions.leaf.v30.common.Commons
import dugsolutions.leaf.v30.common.Critter
import dugsolutions.leaf.v30.common.Token
import dugsolutions.leaf.v30.game.effect.GameCardEffectExecutorBattle
import dugsolutions.leaf.v30.game.effect.WispCardEffectExecutor
import dugsolutions.leaf.v30.grove.Grove
import dugsolutions.leaf.v30.player.Player
import dugsolutions.leaf.v30.player.decision.domain.CardsToRefresh
import dugsolutions.leaf.v30.player.decision.domain.Decision
import dugsolutions.leaf.v30.player.decision.domain.DecisionDirector
import dugsolutions.leaf.v30.player.decision.domain.ItemsToBuy
import dugsolutions.leaf.v30.player.decision.domain.ActionBattleMain
import dugsolutions.leaf.v30.player.decision.domain.ActionBattleSupport
import dugsolutions.leaf.v30.player.decision.domain.ActionCultivation
import dugsolutions.leaf.v30.player.decision.domain.ActionRound
import dugsolutions.leaf.v30.player.domain.CreatureCard
import dugsolutions.leaf.v30.random.Randomizer
import dugsolutions.leaf.v30.random.die.Die
import dugsolutions.leaf.v30.random.die.DieSides
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
import kotlin.test.assertEquals

class RoundBattleTest {

    @Test
    fun prepare_setsUpBattleGridFromTablePlayers() {
        val battle = Battle(playerGridOrder = PlayerGridOrder(SequentialRandomizer()))
        val players = listOf(
            player(1, FixedDie(20, 4), FixedDie(6, 2), FixedDie(8, 1)),
            player(2, FixedDie(6, 6), FixedDie(4, 1), FixedDie(8, 1)),
            player(3, FixedDie(8, 5), FixedDie(6, 3), FixedDie(20, 1)),
            player(4, FixedDie(4, 2), FixedDie(6, 2), FixedDie(8, 2))
        )
        val table = createTable(battle).apply { players.forEach { add(it) } }
        val round = RoundBattle(table, loadBattleCard(), battle = battle)

        round.prepare()

        assertEquals(listOf(2, 3, 1, 4), battle.grid.playerIdsInGridOrder)
    }

    @Test
    fun performMainActions_callsBattleDecisionInReverseGridOrder() {
        val callOrder = mutableListOf<Int>()
        val battle = Battle(playerGridOrder = PlayerGridOrder(SequentialRandomizer()))
        val players = listOf(
            player(1, FixedDie(20, 4), FixedDie(6, 2), FixedDie(8, 1), decisionDirector = RecordingBattleDecisionDirector(callOrder)),
            player(2, FixedDie(6, 6), FixedDie(4, 1), FixedDie(8, 1), decisionDirector = RecordingBattleDecisionDirector(callOrder)),
            player(3, FixedDie(8, 5), FixedDie(6, 3), FixedDie(20, 1), decisionDirector = RecordingBattleDecisionDirector(callOrder)),
            player(4, FixedDie(4, 2), FixedDie(6, 2), FixedDie(8, 2), decisionDirector = RecordingBattleDecisionDirector(callOrder))
        )
        val table = createTable(battle).apply { players.forEach { add(it) } }
        val round = RoundBattle(table, loadBattleCard(), battle = battle)
        round.prepare()

        round.performMainActions()

        assertEquals(listOf(4, 4, 1, 1, 3, 3, 2, 2), callOrder)
    }

    @Test
    fun performMainActions_whenDecisionIsPullDie_addsPulledDieToSelectedBattleRow() {
        val callOrder = mutableListOf<Int>()
        val battle = Battle(playerGridOrder = PlayerGridOrder(SequentialRandomizer()))
        val target = player(
            4,
            FixedDie(4, 2),
            FixedDie(6, 2),
            FixedDie(8, 2),
            decisionDirector = SequenceBattleDecisionDirector(
                playerId = 4,
                callOrder = callOrder,
                actions = listOf(
                    ActionBattleMain.PullDie(BattleStrikeRow.STRIKE_2),
                    ActionBattleMain.DoRoundAction(ActionRound.ACTION_1)
                )
            )
        ).apply {
            addDieToSupply(FixedDie(12, 5))
        }
        val players = listOf(
            player(1, FixedDie(20, 4), FixedDie(6, 2), FixedDie(8, 1), decisionDirector = RecordingBattleDecisionDirector(callOrder)),
            player(2, FixedDie(6, 6), FixedDie(4, 1), FixedDie(8, 1), decisionDirector = RecordingBattleDecisionDirector(callOrder)),
            player(3, FixedDie(8, 5), FixedDie(6, 3), FixedDie(20, 1), decisionDirector = RecordingBattleDecisionDirector(callOrder)),
            target
        )
        val table = createTable(battle).apply { players.forEach { add(it) } }
        val round = RoundBattle(table, loadBattleCard(), battle = battle)
        round.prepare()

        round.performMainActions()

        val items = battle.grid.getSquare(target.id, BattleStrikeRow.STRIKE_2).all
        assertEquals(2, items.size)
        val addedDie = items.last() as BattleItem.DieItem
        assertEquals(12, addedDie.die.sides)
        assertEquals(5, addedDie.die.value)
        assertEquals(listOf(4, 4, 1, 1, 3, 3, 2, 2), callOrder)
    }

    @Test
    fun performMainActions_whenDecisionIsWispCard_doesNotSpendMainAction() {
        val wispCard = loadWispCard()
        val callOrder = mutableListOf<Int>()
        val wispExecutor = TrackingWispCardEffectExecutor()
        val battle = Battle(playerGridOrder = PlayerGridOrder(SequentialRandomizer()))
        val players = listOf(
            player(1, FixedDie(20, 4), FixedDie(6, 2), FixedDie(8, 1), decisionDirector = RecordingBattleDecisionDirector(callOrder)),
            player(2, FixedDie(6, 6), FixedDie(4, 1), FixedDie(8, 1), decisionDirector = RecordingBattleDecisionDirector(callOrder)),
            player(3, FixedDie(8, 5), FixedDie(6, 3), FixedDie(20, 1), decisionDirector = RecordingBattleDecisionDirector(callOrder)),
            player(
                4,
                FixedDie(4, 2),
                FixedDie(6, 2),
                FixedDie(8, 2),
                decisionDirector = SequenceBattleDecisionDirector(
                    playerId = 4,
                    callOrder = callOrder,
                    actions = listOf(
                        ActionBattleMain.PlayWispCard(wispCard),
                        ActionBattleMain.DoRoundAction(ActionRound.ACTION_1),
                        ActionBattleMain.DoRoundAction(ActionRound.ACTION_1)
                    )
                )
            )
        )
        val table = createTable(battle).apply { players.forEach { add(it) } }
        val round = RoundBattle(table, loadBattleCard(), battle = battle, wispCardEffectExecutor = wispExecutor)
        round.prepare()

        round.performMainActions()

        assertEquals(listOf(wispCard), wispExecutor.cards)
        assertEquals(listOf(4, 4, 4, 1, 1, 3, 3, 2, 2), callOrder)
    }

    @Test
    fun performMainActions_whenDecisionIsExecuteCard_flipsCreatureCardFaceDown() {
        val callOrder = mutableListOf<Int>()
        val battle = Battle(playerGridOrder = PlayerGridOrder(SequentialRandomizer()))
        val card = loadGameCard("Root_05_01")
        val gameCardEffectExecutor = TrackingGameCardEffectExecutor()
        val target = player(
            4,
            FixedDie(4, 2),
            FixedDie(6, 2),
            FixedDie(8, 2),
            decisionDirector = SequenceBattleDecisionDirector(
                playerId = 4,
                callOrder = callOrder,
                actions = listOf(
                    ActionBattleMain.ExecuteCard(card),
                    ActionBattleMain.DoRoundAction(ActionRound.ACTION_1)
                )
            )
        ).apply {
            addCardToCreature(CreatureCard(card, CreatureCard.Facing.FACE_UP))
        }
        val players = listOf(
            player(1, FixedDie(20, 4), FixedDie(6, 2), FixedDie(8, 1), decisionDirector = RecordingBattleDecisionDirector(callOrder)),
            player(2, FixedDie(6, 6), FixedDie(4, 1), FixedDie(8, 1), decisionDirector = RecordingBattleDecisionDirector(callOrder)),
            player(3, FixedDie(8, 5), FixedDie(6, 3), FixedDie(20, 1), decisionDirector = RecordingBattleDecisionDirector(callOrder)),
            target
        )
        val table = createTable(battle).apply { players.forEach { add(it) } }
        val round = RoundBattle(
            table = table,
            card = loadBattleCard(),
            battle = battle,
            gameCardEffectExecutor = gameCardEffectExecutor
        )
        round.prepare()

        round.performMainActions()

        assertEquals(listOf(ActionBattleMain.ExecuteCard(card)), gameCardEffectExecutor.actions)
        assertEquals(true, target.creatureCards.single { it.card == card }.isFaceDown)
        assertEquals(listOf(4, 4, 1, 1, 3, 3, 2, 2), callOrder)
    }

    @Test
    fun performMainActions_whenDecisionIsPlayMulchToken_addsDieToBattleGridAndDoesNotSpendMainAction() {
        val callOrder = mutableListOf<Int>()
        val battle = Battle(playerGridOrder = PlayerGridOrder(SequentialRandomizer()))
        val target = player(
            4,
            FixedDie(4, 2),
            FixedDie(6, 2),
            FixedDie(8, 2),
            decisionDirector = SequenceBattleDecisionDirector(
                playerId = 4,
                callOrder = callOrder,
                actions = listOf(
                    ActionBattleMain.DoRoundAction(ActionRound.ACTION_1),
                    ActionBattleMain.DoRoundAction(ActionRound.ACTION_1)
                ),
                supportActions = listOf(
                    ActionBattleSupport.PlayMulchToken(Token.MULCH(DieSides.D8), BattleStrikeRow.STRIKE_1),
                    ActionBattleSupport.None
                )
            )
        )
        target.add(Token.MULCH(DieSides.D8))
        val players = listOf(
            player(1, FixedDie(20, 4), FixedDie(6, 2), FixedDie(8, 1), decisionDirector = RecordingBattleDecisionDirector(callOrder)),
            player(2, FixedDie(6, 6), FixedDie(4, 1), FixedDie(8, 1), decisionDirector = RecordingBattleDecisionDirector(callOrder)),
            player(3, FixedDie(8, 5), FixedDie(6, 3), FixedDie(20, 1), decisionDirector = RecordingBattleDecisionDirector(callOrder)),
            target
        )
        val table = createTable(battle).apply { players.forEach { add(it) } }
        val round = RoundBattle(
            table = table,
            card = loadBattleCard(),
            battle = battle,
            dieFactory = DieFactory(IdentityRandomizer())
        )
        round.prepare()

        round.performSupportActions()
        round.performMainActions()

        assertEquals(emptyList(), target.mulchTokens)
        assertEquals(2, battle.grid.getSquare(target.id, BattleStrikeRow.STRIKE_1).size)
        val dieItem = battle.grid.getSquare(target.id, BattleStrikeRow.STRIKE_1).all.last() as BattleItem.DieItem
        assertEquals(8, dieItem.die.sides)
        assertEquals(listOf(Critter.BEE), target.critters)
        assertEquals(8, table.grove.count(Critter.BEE))
        assertEquals(listOf(4, 4, 1, 3, 2, 4, 4, 1, 1, 3, 3, 2, 2), callOrder)
    }

    @Test
    fun performMainActions_whenDecisionIsPlayWaterTokenWithoutDie_refreshesCreatureAndDoesNotSpendMainAction() {
        val callOrder = mutableListOf<Int>()
        val battle = Battle(playerGridOrder = PlayerGridOrder(SequentialRandomizer()))
        val target = player(
            4,
            FixedDie(4, 2),
            FixedDie(6, 2),
            FixedDie(8, 2),
            decisionDirector = SequenceBattleDecisionDirector(
                playerId = 4,
                callOrder = callOrder,
                actions = listOf(
                    ActionBattleMain.DoRoundAction(ActionRound.ACTION_1),
                    ActionBattleMain.DoRoundAction(ActionRound.ACTION_1)
                ),
                supportActions = listOf(
                    ActionBattleSupport.PlayWaterToken(row = null),
                    ActionBattleSupport.None
                )
            )
        )
        target.add(Token.WATER)
        target.addCardToCreature(loadGameCard("Root_05_01"))
        val players = listOf(
            player(1, FixedDie(20, 4), FixedDie(6, 2), FixedDie(8, 1), decisionDirector = RecordingBattleDecisionDirector(callOrder)),
            player(2, FixedDie(6, 6), FixedDie(4, 1), FixedDie(8, 1), decisionDirector = RecordingBattleDecisionDirector(callOrder)),
            player(3, FixedDie(8, 5), FixedDie(6, 3), FixedDie(20, 1), decisionDirector = RecordingBattleDecisionDirector(callOrder)),
            target
        )
        val table = createTable(battle).apply { players.forEach { add(it) } }
        val round = RoundBattle(table, loadBattleCard(), battle = battle)
        round.prepare()

        round.performSupportActions()
        round.performMainActions()

        assertEquals(0, target.waterTokenCount)
        assertEquals(true, target.creatureCards.all { it.isFaceUp })
        assertEquals(listOf(4, 4, 1, 3, 2, 4, 4, 1, 1, 3, 3, 2, 2), callOrder)
    }

    @Test
    fun performMainActions_whenDecisionIsPlayWaterTokenWithDie_rerollsBattleGridDieAndDoesNotSpendMainAction() {
        val callOrder = mutableListOf<Int>()
        val battle = Battle(playerGridOrder = PlayerGridOrder(SequentialRandomizer()))
        val targetDie = TrackingDie(8, 6)
        val target = player(
            4,
            targetDie,
            FixedDie(6, 2),
            FixedDie(4, 1),
            decisionDirector = SequenceBattleDecisionDirector(
                playerId = 4,
                callOrder = callOrder,
                actions = listOf(
                    ActionBattleMain.DoRoundAction(ActionRound.ACTION_1),
                    ActionBattleMain.DoRoundAction(ActionRound.ACTION_1)
                ),
                supportActions = listOf(
                    ActionBattleSupport.PlayWaterToken(onDie = FixedDie(8, 6), row = BattleStrikeRow.STRIKE_1),
                    ActionBattleSupport.None
                )
            )
        )
        target.add(Token.WATER)
        val players = listOf(
            player(1, FixedDie(20, 4), FixedDie(6, 2), FixedDie(8, 1), decisionDirector = RecordingBattleDecisionDirector(callOrder)),
            player(2, FixedDie(6, 6), FixedDie(4, 1), FixedDie(8, 1), decisionDirector = RecordingBattleDecisionDirector(callOrder)),
            player(3, FixedDie(8, 5), FixedDie(6, 3), FixedDie(20, 1), decisionDirector = RecordingBattleDecisionDirector(callOrder)),
            target
        )
        val table = createTable(battle).apply { players.forEach { add(it) } }
        val round = RoundBattle(table, loadBattleCard(), battle = battle)
        round.prepare()

        round.performSupportActions()
        round.performMainActions()

        assertEquals(0, target.waterTokenCount)
        assertEquals(1, targetDie.rollCount)
        assertEquals(4, callOrder.count { it == 4 })
    }

    private fun player(
        id: Int,
        vararg dice: Die,
        decisionDirector: DecisionDirector = RecordingBattleDecisionDirector(mutableListOf())
    ): Player {
        return Player(decisionDirector = decisionDirector, id = id).apply {
            dice.forEach { addDieToSupply(it) }
            repeat(dice.size) { drawDie() }
        }
    }

    private fun createTable(battle: Battle): Table {
        return Table(
            grove = Grove(createWispDeck()),
            roundDeck = createRoundDeck(),
            battle = battle
        )
    }

    private fun loadBattleCard(): RoundCard {
        val registry = RoundCardRegistry()
        registry.loadFromCsv(Commons.ROUND_CARD_LIST)
        return registry.getAllCards().first { it.cardType.name == "BATTLE" }
    }

    private fun loadWispCard(): WispCard {
        val registry = WispCardRegistry()
        registry.loadFromCsv(Commons.WISP_LIST)
        return registry.getAllCards().first()
    }

    private fun loadGameCard(name: String) =
        dugsolutions.leaf.v30.cards.GameCardRegistry()
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

    private class RecordingBattleDecisionDirector(
        private val callOrder: MutableList<Int>
    ) : DecisionDirector {
        override fun chooseCritter(input: Decision.ChooseCritter): Critter = Critter.BEE
        override fun chooseMainCultivationAction(input: Decision.ChooseMainActionCultivation): ActionCultivation = ActionCultivation.PullDie
        override fun chooseMainBattleAction(input: Decision.ChooseMainActionBattle): ActionBattleMain {
            callOrder.add(input.player.id)
            return ActionBattleMain.DoRoundAction(ActionRound.ACTION_1)
        }
        override fun chooseSupportBattleAction(input: Decision.ChooseMainActionBattle): ActionBattleSupport {
            callOrder.add(input.player.id)
            return ActionBattleSupport.None
        }
        override fun chooseItemsToBuy(input: Decision.ChooseItemsToBuy): ItemsToBuy = ItemsToBuy()
        override fun chooseCardsToRefreshWithWorms(input: Decision.ChooseCardsToRefreshWithWorms): CardsToRefresh = CardsToRefresh()
        override fun chooseFlipOrSnipCard(input: Decision.ChooseFlipOrSnipCard): CreatureCard = input.creatureCards.first()
    }

    private class SequenceBattleDecisionDirector(
        private val playerId: Int,
        private val callOrder: MutableList<Int>,
        private val actions: List<ActionBattleMain>,
        private val supportActions: List<ActionBattleSupport> = listOf(ActionBattleSupport.None)
    ) : DecisionDirector {
        private var index = 0
        private var supportIndex = 0

        override fun chooseCritter(input: Decision.ChooseCritter): Critter = Critter.BEE
        override fun chooseMainCultivationAction(input: Decision.ChooseMainActionCultivation): ActionCultivation = ActionCultivation.PullDie
        override fun chooseMainBattleAction(input: Decision.ChooseMainActionBattle): ActionBattleMain {
            callOrder.add(playerId)
            return actions.getOrElse(index++) { actions.last() }
        }
        override fun chooseSupportBattleAction(input: Decision.ChooseMainActionBattle): ActionBattleSupport {
            callOrder.add(playerId)
            return supportActions.getOrElse(supportIndex++) { supportActions.last() }
        }
        override fun chooseItemsToBuy(input: Decision.ChooseItemsToBuy): ItemsToBuy = ItemsToBuy()
        override fun chooseCardsToRefreshWithWorms(input: Decision.ChooseCardsToRefreshWithWorms): CardsToRefresh = CardsToRefresh()
        override fun chooseFlipOrSnipCard(input: Decision.ChooseFlipOrSnipCard): CreatureCard = input.creatureCards.first()
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

    private class TrackingGameCardEffectExecutor : GameCardEffectExecutorBattle() {
        val actions = mutableListOf<ActionBattleMain.ExecuteCard>()

        override fun invoke(
            table: Table,
            player: Player,
            action: ActionBattleMain.ExecuteCard
        ) {
            actions.add(action)
        }
    }

    private class FixedDie(
        sides: Int,
        value: Int
    ) : Die(sides) {
        init {
            adjustTo(value)
        }

        override fun roll(): Die = this
    }

    private class TrackingDie(
        sides: Int,
        value: Int
    ) : Die(sides) {
        var rollCount = 0

        init {
            adjustTo(value)
        }

        override fun roll(): Die {
            rollCount++
            return this
        }
    }

    private class SequentialRandomizer : Randomizer {
        private var next = 1
        override fun nextBoolean(): Boolean = true
        override fun nextInt(from: Int, until: Int): Int {
            val result = next.coerceIn(from, until - 1)
            next++
            return result
        }
        override fun nextInt(until: Int): Int = nextInt(0, until)
        override fun <T> randomOrNull(list: List<T>): T? = list.firstOrNull()
        override fun <T> shuffled(list: List<T>): List<T> = list
    }

    private class IdentityRandomizer : Randomizer {
        override fun nextBoolean(): Boolean = true
        override fun nextInt(from: Int, until: Int): Int = from
        override fun nextInt(until: Int): Int = 0
        override fun <T> randomOrNull(list: List<T>): T? = list.firstOrNull()
        override fun <T> shuffled(list: List<T>): List<T> = list
    }
}
