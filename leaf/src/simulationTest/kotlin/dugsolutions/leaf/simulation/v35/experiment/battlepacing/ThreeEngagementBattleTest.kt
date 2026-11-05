package dugsolutions.leaf.simulation.v35.experiment.battlepacing

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.common.CardDataFiles
import dugsolutions.leaf.v35.di.appModules
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.game.GameConfig
import dugsolutions.leaf.v35.game.GameRoundSetup
import dugsolutions.leaf.v35.game.PlayerDecisionFactory
import dugsolutions.leaf.v35.game.di.GameFactory
import dugsolutions.leaf.v35.game.round.RoundCoordinator
import dugsolutions.leaf.v35.game.round.battle.BattleRound
import dugsolutions.leaf.v35.plant.PlantCardManager
import dugsolutions.leaf.v35.plant.PlantCardRegistry
import dugsolutions.leaf.v35.round.RoundCardManager
import dugsolutions.leaf.v35.round.RoundCardRegistry
import dugsolutions.leaf.v35.wisp.WispCardManager
import dugsolutions.leaf.v35.wisp.WispCardRegistry
import org.koin.dsl.koinApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ThreeEngagementBattleTest {
    @Test
    fun `paired real battle openings execute control and three engagements with shared legal deck`() {
        val app = koinApplication { modules(appModules) }
        try {
            val koin = app.koin
            val root = CardDataFiles.dataDirectory()
            koin.get<PlantCardRegistry>().apply {
                clear()
                loadFromCsv(CardDataFiles.dataPath(CardDataFiles.ROOT_CARD_LIST, root), CardDataFiles.dataPath(CardDataFiles.VF_CARD_LIST, root))
            }.also { koin.get<PlantCardManager>().loadCards(it) }
            koin.get<WispCardRegistry>().apply {
                clear()
                loadFromCsv(CardDataFiles.dataPath(CardDataFiles.WISP_LIST, root))
            }.also { koin.get<WispCardManager>().loadCards(it) }
            koin.get<RoundCardRegistry>().apply {
                clear()
                loadFromCsv(CardDataFiles.dataPath(CardDataFiles.ROUND_CARD_LIST, root))
            }.also { koin.get<RoundCardManager>().loadCards(it) }

            val battle = koin.get<BattleRound>()
            val pairedRunner = PairedBattleOpeningReplay(koin.get<GameFactory>(), koin.get<RoundCoordinator>(), battle)
            for ((players, ordinal) in listOf(2 to 1, 3 to 2, 4 to 3)) {
                val config = GameConfig(
                    playerDecisionFactories = List(players) { PlayerDecisionFactory.mechanicalControl() },
                    roundSetup = GameRoundSetup.standard(),
                    seed = 202610300L + players, strategySeed = 13103L,
                    eventBasedDieRolls = true, sharedPercentileDieRolls = true
                )
                val deck = BattlePacingWispDeck.withoutResolve(koin.get<WispCardManager>(), 9913L)
                assertTrue(deck.none { it.effect == GameEffect.RESOLVE_STRIKE_IMMEDIATELY_AND_CLEAR_ROW })
                val paired = pairedRunner.create(config, ordinal, exactWispCards = deck)
                val aGame = paired.controlGame
                val aState = paired.controlBattle
                val card = aGame.config.roundValues.cardFor(requireNotNull(aGame.currentRound))
                battle.executeActions(aGame, card, aState)
                battle.executeStrikes(aGame, aState)
                battle.executeDoom(aGame, aState)
                battle.executeCleanup(aGame, aState)

                val b = ThreeEngagementBattle(battle).execute(
                    paired.candidateGame, paired.candidateBattle,
                    if (players == 2) SupportSpendingEngagementPolicy else MechanicalEngagementPolicy
                )
                assertEquals(3, b.engagements.size)
                assertEquals(StrikeRow.entries, b.engagements.map { it.order.row })
                assertTrue(StrikeRow.entries.all { paired.candidateBattle.grid.isRowClosed(it) })
                val mains = b.engagements.flatMap { it.actions }
                    .count { it.choice is EngagementChoice.Main }
                assertTrue(mains <= players * 2)
                b.engagements.forEach { result ->
                    result.order.clockwiseActors.forEach { id ->
                        val actions = result.actions.filter { it.playerId == id }
                        assertTrue(actions.count { it.choice is EngagementChoice.Main } <= 1)
                        assertTrue(actions.count { it.choice is EngagementChoice.Support } <= 2)
                    }
                }
                assertTrue(b.trace.any { it.startsWith("ENGAGEMENT_RESOLVE,row=TOP") })
                assertTrue(b.doomCount >= 0)
                println("PAIRED_BATTLE,players=$players,battle=$ordinal,opening=${paired.fingerprint.sha256}," +
                    "controlVP=${aGame.players.map { it.vp }},candidateVP=${paired.candidateGame.players.map { it.vp }}," +
                    "candidateActions=${b.engagements.sumOf { it.actions.size }}")
            }
        } finally {
            app.close()
        }
    }

    @Test
    fun `full Wisp deck allows Resolve during an Engagement without double resolution`() {
        val app = koinApplication { modules(appModules) }
        try {
            val koin = app.koin
            val root = CardDataFiles.dataDirectory()
            koin.get<PlantCardRegistry>().apply {
                clear()
                loadFromCsv(CardDataFiles.dataPath(CardDataFiles.ROOT_CARD_LIST, root),
                    CardDataFiles.dataPath(CardDataFiles.VF_CARD_LIST, root))
            }.also { koin.get<PlantCardManager>().loadCards(it) }
            koin.get<WispCardRegistry>().apply {
                clear()
                loadFromCsv(CardDataFiles.dataPath(CardDataFiles.WISP_LIST, root))
            }.also { koin.get<WispCardManager>().loadCards(it) }
            koin.get<RoundCardRegistry>().apply {
                clear()
                loadFromCsv(CardDataFiles.dataPath(CardDataFiles.ROUND_CARD_LIST, root))
            }.also { koin.get<RoundCardManager>().loadCards(it) }
            val round = koin.get<BattleRound>()
            val pair = PairedBattleOpeningReplay(koin.get<GameFactory>(), koin.get<RoundCoordinator>(), round)
                .create(GameConfig(
                    playerDecisionFactories = List(2) { PlayerDecisionFactory.mechanicalControl() },
                    seed = 20261104L, strategySeed = 20261105L,
                    roundSetup = GameRoundSetup.standard()
                ))
            val game = pair.candidateGame
            val battle = pair.candidateBattle
            val starter = EngagementOrderSelector.select(battle, game.players.map { it.id }, StrikeRow.TOP).starter
            val resolve = koin.get<WispCardManager>().getAllCards().cards.first {
                it.effect == GameEffect.RESOLVE_STRIKE_IMMEDIATELY_AND_CLEAR_ROW
            }
            battle.player(starter).wisps.add(resolve)
            var used = false
            val policy = EngagementPolicy { decision ->
                val support = decision.legalSupports.firstOrNull { option ->
                    val action = (option as? dugsolutions.leaf.v35.player.decision.battle.BattleSupportAction.Shared)?.action
                    (action as? dugsolutions.leaf.v35.player.decision.support.SupportAction.PlayWisp)?.card?.effect ==
                        GameEffect.RESOLVE_STRIKE_IMMEDIATELY_AND_CLEAR_ROW
                }
                if (!used && decision.row == StrikeRow.TOP && decision.playerId == starter && support != null) {
                    used = true
                    EngagementChoice.Support(support)
                } else EngagementChoice.Pass
            }
            val result = ThreeEngagementBattle(round).execute(game, battle, policy)
            assertTrue(used, "Wisp's Resolve must be a legal Support in Three Engagements")
            assertEquals(3, result.engagements.size)
            assertEquals(3, StrikeRow.entries.count { battle.finishedStrike(it) != null })
            assertEquals(StrikeRow.entries.sumOf { battle.finishedStrike(it)!!.doom.count }, result.doomCount)
            assertEquals(StrikeRow.entries.sumOf { battle.finishedStrike(it)!!.cleanup.totalDiscardedDice },
                result.cleanupDiceCount)
            assertTrue(StrikeRow.entries.all { battle.grid.isRowClosed(it) })
            assertTrue(battle.grid.diePlacements.isEmpty())
            assertTrue(battle.grid.critterPlacements.isEmpty())
            assertTrue(game.players.all { it.dice.hand.isEmpty() })
            assertEquals(0, battle.player(starter).wisps.cards.cards.count { it === resolve })
        } finally { app.close() }
    }

    @Test
    fun `Wisp Resolve finishes a future row and scheduler skips its turn sequence`() {
        val app = koinApplication { modules(appModules) }
        try {
            val koin = app.koin
            val root = CardDataFiles.dataDirectory()
            koin.get<PlantCardRegistry>().apply {
                clear()
                loadFromCsv(CardDataFiles.dataPath(CardDataFiles.ROOT_CARD_LIST, root),
                    CardDataFiles.dataPath(CardDataFiles.VF_CARD_LIST, root))
            }.also { koin.get<PlantCardManager>().loadCards(it) }
            koin.get<WispCardRegistry>().apply {
                clear()
                loadFromCsv(CardDataFiles.dataPath(CardDataFiles.WISP_LIST, root))
            }.also { koin.get<WispCardManager>().loadCards(it) }
            koin.get<RoundCardRegistry>().apply {
                clear()
                loadFromCsv(CardDataFiles.dataPath(CardDataFiles.ROUND_CARD_LIST, root))
            }.also { koin.get<RoundCardManager>().loadCards(it) }
            val targetMiddleFactory = PlayerDecisionFactory {
                dugsolutions.leaf.v35.player.decision.DecisionDirector.mechanicalControl().copy(
                    effect = object : dugsolutions.leaf.v35.player.decision.effect.EffectStrategy by
                        dugsolutions.leaf.v35.player.decision.mechanical.effect.MechanicalEffectStrategy() {
                        override fun chooseStrikeRow(
                            request: dugsolutions.leaf.v35.player.decision.effect.ChooseEffectStrikeRowRequest
                        ): StrikeRow = request.legalChoices.firstOrNull { it == StrikeRow.MIDDLE }
                            ?: request.legalChoices.first()
                    }
                )
            }
            val round = koin.get<BattleRound>()
            val pair = PairedBattleOpeningReplay(koin.get<GameFactory>(), koin.get<RoundCoordinator>(), round)
                .create(GameConfig(
                    playerDecisionFactories = List(2) { targetMiddleFactory },
                    seed = 20261106L, strategySeed = 20261107L,
                    roundSetup = GameRoundSetup.standard()
                ))
            val game = pair.candidateGame
            val battle = pair.candidateBattle
            val starter = EngagementOrderSelector.select(battle, game.players.map { it.id }, StrikeRow.TOP).starter
            val resolve = koin.get<WispCardManager>().getAllCards().cards.first {
                it.effect == GameEffect.RESOLVE_STRIKE_IMMEDIATELY_AND_CLEAR_ROW
            }
            battle.player(starter).wisps.add(resolve)
            var played = false
            val policy = EngagementPolicy { decision ->
                val wisp = decision.legalSupports.firstOrNull { option ->
                    val shared = (option as? dugsolutions.leaf.v35.player.decision.battle.BattleSupportAction.Shared)?.action
                    (shared as? dugsolutions.leaf.v35.player.decision.support.SupportAction.PlayWisp)?.card?.effect ==
                        GameEffect.RESOLVE_STRIKE_IMMEDIATELY_AND_CLEAR_ROW
                }
                if (!played && decision.row == StrikeRow.TOP && decision.playerId == starter && wisp != null) {
                    played = true
                    EngagementChoice.Support(wisp)
                } else EngagementChoice.Pass
            }
            val result = ThreeEngagementBattle(round).execute(game, battle, policy)
            assertTrue(played)
            assertTrue(result.engagements[1].preempted)
            assertTrue(result.engagements[1].opportunities.isEmpty())
            assertTrue(result.trace.any { it == "ENGAGEMENT_PREEMPTED,row=MIDDLE,reason=WISP_RESOLVE" })
            assertEquals(3, StrikeRow.entries.count { battle.finishedStrike(it) != null })
            assertEquals(3, result.engagements.map { it.resolution.row }.distinct().size)
        } finally { app.close() }
    }

    @Test
    fun `two Resolve Wisps finish two future Strikes in one capped Support turn`() {
        val app = koinApplication { modules(appModules) }
        try {
            val koin = app.koin
            val root = CardDataFiles.dataDirectory()
            koin.get<PlantCardRegistry>().apply {
                clear()
                loadFromCsv(CardDataFiles.dataPath(CardDataFiles.ROOT_CARD_LIST, root),
                    CardDataFiles.dataPath(CardDataFiles.VF_CARD_LIST, root))
            }.also { koin.get<PlantCardManager>().loadCards(it) }
            koin.get<WispCardRegistry>().apply {
                clear()
                loadFromCsv(CardDataFiles.dataPath(CardDataFiles.WISP_LIST, root))
            }.also { koin.get<WispCardManager>().loadCards(it) }
            koin.get<RoundCardRegistry>().apply {
                clear()
                loadFromCsv(CardDataFiles.dataPath(CardDataFiles.ROUND_CARD_LIST, root))
            }.also { koin.get<RoundCardManager>().loadCards(it) }

            val chooseFutureRow = PlayerDecisionFactory {
                dugsolutions.leaf.v35.player.decision.DecisionDirector.mechanicalControl().copy(
                    effect = object : dugsolutions.leaf.v35.player.decision.effect.EffectStrategy by
                        dugsolutions.leaf.v35.player.decision.mechanical.effect.MechanicalEffectStrategy() {
                        override fun chooseStrikeRow(
                            request: dugsolutions.leaf.v35.player.decision.effect.ChooseEffectStrikeRowRequest
                        ): StrikeRow = request.legalChoices.firstOrNull { it == StrikeRow.MIDDLE }
                            ?: request.legalChoices.firstOrNull { it == StrikeRow.BOTTOM }
                            ?: request.legalChoices.first()
                    }
                )
            }
            val round = koin.get<BattleRound>()
            val pair = PairedBattleOpeningReplay(koin.get<GameFactory>(), koin.get<RoundCoordinator>(), round)
                .create(GameConfig(
                    playerDecisionFactories = List(2) { chooseFutureRow },
                    seed = 20261108L, strategySeed = 20261109L,
                    roundSetup = GameRoundSetup.standard()
                ))
            val game = pair.candidateGame
            val battle = pair.candidateBattle
            val starter = EngagementOrderSelector.select(battle, game.players.map { it.id }, StrikeRow.TOP).starter
            val resolve = koin.get<WispCardManager>().getAllCards().cards.single {
                it.effect == GameEffect.RESOLVE_STRIKE_IMMEDIATELY_AND_CLEAR_ROW
            }
            assertTrue(resolve.quantity >= 2, "Official v38 deck must supply two copies")
            repeat(2) { battle.player(starter).wisps.add(resolve) }
            var played = 0
            val policy = EngagementPolicy { decision ->
                val wisp = decision.legalSupports.firstOrNull { option ->
                    val shared = (option as? dugsolutions.leaf.v35.player.decision.battle.BattleSupportAction.Shared)?.action
                    (shared as? dugsolutions.leaf.v35.player.decision.support.SupportAction.PlayWisp)?.card?.effect ==
                        GameEffect.RESOLVE_STRIKE_IMMEDIATELY_AND_CLEAR_ROW
                }
                if (played < 2 && decision.row == StrikeRow.TOP && decision.playerId == starter && wisp != null) {
                    played++
                    EngagementChoice.Support(wisp)
                } else EngagementChoice.Pass
            }
            val result = ThreeEngagementBattle(round).execute(game, battle, policy)
            assertEquals(2, played)
            assertEquals(2, result.engagements.first().actions.count {
                it.playerId == starter && it.choice is EngagementChoice.Support
            })
            assertTrue(result.engagements[1].preempted)
            assertTrue(result.engagements[2].preempted)
            assertTrue(result.engagements[1].opportunities.isEmpty())
            assertTrue(result.engagements[2].opportunities.isEmpty())
            assertEquals(3, StrikeRow.entries.count { battle.finishedStrike(it) != null })
            assertEquals(3, result.engagements.map { it.resolution.row }.distinct().size)
            assertEquals(3, StrikeRow.entries.count { battle.grid.isRowClosed(it) })
            assertEquals(StrikeRow.entries.sumOf { battle.finishedStrike(it)!!.doom.count }, result.doomCount)
            assertTrue(battle.grid.diePlacements.isEmpty())
            assertTrue(game.players.all { it.dice.hand.isEmpty() })
            assertEquals(0, battle.player(starter).wisps.cards.cards.count { it == resolve })
        } finally { app.close() }
    }

    @Test
    fun `Sunlight funds extra Main inside one Support slot without spending normal Main`() {
        val app = koinApplication { modules(appModules) }
        try {
            val koin = app.koin
            val root = CardDataFiles.dataDirectory()
            koin.get<PlantCardRegistry>().apply {
                clear()
                loadFromCsv(CardDataFiles.dataPath(CardDataFiles.ROOT_CARD_LIST, root),
                    CardDataFiles.dataPath(CardDataFiles.VF_CARD_LIST, root))
            }.also { koin.get<PlantCardManager>().loadCards(it) }
            koin.get<WispCardRegistry>().apply {
                clear()
                loadFromCsv(CardDataFiles.dataPath(CardDataFiles.WISP_LIST, root))
            }.also { koin.get<WispCardManager>().loadCards(it) }
            koin.get<RoundCardRegistry>().apply {
                clear()
                loadFromCsv(CardDataFiles.dataPath(CardDataFiles.ROUND_CARD_LIST, root))
            }.also { koin.get<RoundCardManager>().loadCards(it) }
            val round = koin.get<BattleRound>()
            val pair = PairedBattleOpeningReplay(koin.get<GameFactory>(), koin.get<RoundCoordinator>(), round)
                .create(GameConfig(
                    playerDecisionFactories = List(2) { PlayerDecisionFactory.mechanicalControl() },
                    seed = 20261110L, strategySeed = 20261111L,
                    roundSetup = GameRoundSetup.standard()
                ))
            val game = pair.candidateGame
            val battle = pair.candidateBattle
            val starter = EngagementOrderSelector.select(battle, game.players.map { it.id }, StrikeRow.TOP).starter
            val player = battle.player(starter)
            // Ensure exactly one available token, independently of earlier Cultivation.
            val beforeSunlight = player.tokens.sunlightCount
            assertTrue(dugsolutions.leaf.v35.game.operation.SunlightTokenResolver.gain(game, player))
            val beforeEvents = game.chronicle.entries
                .filterIsInstance<dugsolutions.leaf.v35.chronicle.domain.GameEntry.SunlightMainAction>().size
            var sunlightUsed = false
            var normalUsed = false
            val policy = EngagementPolicy { decision ->
                if (decision.row != StrikeRow.TOP || decision.playerId != starter) {
                    EngagementChoice.Pass
                } else {
                    val sunlight = decision.legalSupports
                        .filterIsInstance<dugsolutions.leaf.v35.player.decision.battle.BattleSupportAction.UseSunlight>()
                        .firstOrNull { it.mainAction == dugsolutions.leaf.v35.player.decision.battle.BattleMainAction.Draw }
                    when {
                        !sunlightUsed && sunlight != null -> {
                            sunlightUsed = true
                            EngagementChoice.Support(sunlight)
                        }
                        !normalUsed && dugsolutions.leaf.v35.player.decision.battle.BattleMainAction.Draw in decision.legalMains -> {
                            normalUsed = true
                            EngagementChoice.Main(dugsolutions.leaf.v35.player.decision.battle.BattleMainAction.Draw)
                        }
                        else -> EngagementChoice.Pass
                    }
                }
            }
            val result = ThreeEngagementBattle(round).execute(game, battle, policy)
            assertTrue(sunlightUsed)
            assertTrue(normalUsed)
            val starterActions = result.engagements.first().actions.filter { it.playerId == starter }
            assertEquals(1, starterActions.count { it.choice is EngagementChoice.Support })
            assertEquals(1, starterActions.count { it.choice is EngagementChoice.Main })
            assertEquals(1, starterActions.last().normalMainsRemainingAfter,
                "Sunlight does not consume either normal Main token")
            assertEquals(beforeSunlight, player.tokens.sunlightCount)
            assertEquals(beforeEvents + 1, game.chronicle.entries
                .filterIsInstance<dugsolutions.leaf.v35.chronicle.domain.GameEntry.SunlightMainAction>().size)
        } finally { app.close() }
    }

    @Test
    fun `closed grid row rejects mutation until post battle reclaim`() {
        val p1 = dugsolutions.leaf.v35.player.Player(
            id = dugsolutions.leaf.v35.player.PlayerId(1),
            decisions = dugsolutions.leaf.v35.player.decision.DecisionDirector.mechanicalControl(),
            dice = dugsolutions.leaf.v35.player.dice.PlayerDice(hand = listOf(FixedDie(6, 5), FixedDie(6, 4), FixedDie(6, 3)))
        )
        val p2 = dugsolutions.leaf.v35.player.Player(
            id = dugsolutions.leaf.v35.player.PlayerId(2),
            decisions = dugsolutions.leaf.v35.player.decision.DecisionDirector.mechanicalControl(),
            dice = dugsolutions.leaf.v35.player.dice.PlayerDice(hand = listOf(FixedDie(6, 3), FixedDie(6, 2), FixedDie(6, 1)))
        )
        val grid = dugsolutions.leaf.v35.battle.BattleState(listOf(p1, p2)).apply { placeInitialHands() }.grid
        val die = grid.square(p1.id, StrikeRow.TOP).dice.single()
        val value = die.value
        grid.closeRow(StrikeRow.TOP)
        assertFailsWith<IllegalStateException> { die.adjustBy(1) }
        assertEquals(value, die.value)
        assertFailsWith<dugsolutions.leaf.v35.error.InvalidGameStateException> { grid.removeDie(die) }
        assertFailsWith<dugsolutions.leaf.v35.error.InvalidGameStateException> { grid.moveDie(die, StrikeRow.MIDDLE) }
        assertFailsWith<dugsolutions.leaf.v35.error.InvalidGameStateException> { grid.swapDieLocations(die, grid.square(p2.id, StrikeRow.TOP).dice.single()) }
        StrikeRow.entries.drop(1).forEach { grid.closeRow(it) }
        grid.beginPostBattleRemoval()
        assertTrue(grid.removeDie(die) != null)
        die.adjustTo(2) // Reclaimed dice are usable in the next round.
        assertEquals(2, die.value)
    }

    @Test
    fun `settled Strike preserves effective Critter totals despite later round boosts`() {
        val p1 = dugsolutions.leaf.v35.player.Player(
            id = dugsolutions.leaf.v35.player.PlayerId(1),
            decisions = dugsolutions.leaf.v35.player.decision.DecisionDirector.mechanicalControl(),
            dice = dugsolutions.leaf.v35.player.dice.PlayerDice(hand = listOf(FixedDie(6, 4), FixedDie(6, 3), FixedDie(6, 2)))
        )
        val p2 = dugsolutions.leaf.v35.player.Player(
            id = dugsolutions.leaf.v35.player.PlayerId(2),
            decisions = dugsolutions.leaf.v35.player.decision.DecisionDirector.mechanicalControl(),
            dice = dugsolutions.leaf.v35.player.dice.PlayerDice(hand = listOf(FixedDie(6, 5), FixedDie(6, 3), FixedDie(6, 1)))
        )
        val state = dugsolutions.leaf.v35.battle.BattleState(listOf(p1, p2))
        state.placeInitialHands()
        p1.critters.add(dugsolutions.leaf.v35.tokens.Critter.WORM)
        state.grid.placeCritter(p1, StrikeRow.TOP, dugsolutions.leaf.v35.tokens.Critter.WORM)
        val original = dugsolutions.leaf.v35.battle.domain.BattleGridSnapshot.row(state, StrikeRow.TOP)
        state.lockResolvedRow(StrikeRow.TOP)
        p1.critterValues.boostForRound(dugsolutions.leaf.v35.tokens.Critter.WORM, 3)
        assertEquals(original, dugsolutions.leaf.v35.battle.domain.BattleGridSnapshot.row(state, StrikeRow.TOP))
        assertEquals(1, state.frozenResolvedRow(StrikeRow.TOP)!!.squares.first().critters.single().value)
    }

    private class FixedDie(sides: Int, value: Int) : dugsolutions.leaf.v35.random.die.Die(sides) {
        init { adjustTo(value) }
        override fun roll(): dugsolutions.leaf.v35.random.die.Die = this
    }
}
