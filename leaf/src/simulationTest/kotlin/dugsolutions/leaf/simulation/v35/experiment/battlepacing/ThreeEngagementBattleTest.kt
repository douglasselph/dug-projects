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
