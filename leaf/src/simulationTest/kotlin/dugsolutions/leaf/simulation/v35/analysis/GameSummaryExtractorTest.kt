package dugsolutions.leaf.simulation.v35.analysis

import dugsolutions.leaf.v35.chronicle.domain.GameEntry
import dugsolutions.leaf.v35.chronicle.domain.RollRewardKind
import dugsolutions.leaf.v35.chronicle.domain.SupportActionKind
import dugsolutions.leaf.v35.common.CardDataFiles
import dugsolutions.leaf.v35.di.appModules
import dugsolutions.leaf.v35.game.GameConfig
import dugsolutions.leaf.v35.game.GameRunner
import dugsolutions.leaf.v35.game.di.GameFactory
import dugsolutions.leaf.v35.random.die.DieSides
import dugsolutions.leaf.v35.plant.PlantCardManager
import dugsolutions.leaf.v35.plant.PlantCardRegistry
import dugsolutions.leaf.v35.round.RoundCardManager
import dugsolutions.leaf.v35.round.RoundCardRegistry
import dugsolutions.leaf.v35.wisp.WispCardManager
import dugsolutions.leaf.v35.wisp.WispCardRegistry
import org.koin.dsl.koinApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GameSummaryExtractorTest {

    companion object {
        private val FIRST_GAME_PLANT_NAMES = listOf(
            "Root_05_02",
            "Root_07_04",
            "Root_09_03",
            "Vine_07_01",
            "Vine_09_01",
            "Vine_11_04",
            "Flower_11_03",
            "Flower_14_02",
            "Flower_17_04"
        )
    }

    @Test
    fun completedGame_collapsesIntoCompactValueOnlySummary() {
        val application = koinApplication { modules(appModules) }
        try {
            val koin = application.koin
            loadCatalogs(
                plantRegistry = koin.get(),
                plantManager = koin.get(),
                wispRegistry = koin.get(),
                wispManager = koin.get(),
                roundRegistry = koin.get(),
                roundManager = koin.get()
            )

            val plantManager = koin.get<PlantCardManager>()
            val plants = FIRST_GAME_PLANT_NAMES.map { name ->
                requireNotNull(plantManager.getCard(name)) { "Missing test Plant: $name" }
            }
            val game = koin.get<GameFactory>()(
                GameConfig.humanBaseline(
                    selectedPlantCards = plants,
                    numPlayers = 4,
                    seed = 41L,
                    strategySeed = 73L
                )
            )
            val runResult = koin.get<GameRunner>().run(game)

            val summary = GameSummaryExtractor.extract(game, runResult)

            assertEquals(41L, summary.mechanicalSeed)
            assertEquals(73L, summary.strategySeed)
            assertEquals(runResult.roundsCompleted, summary.roundsCompleted)
            assertEquals(runResult.finalScoring.winnerIds, summary.winnerIds)
            assertEquals(4, summary.players.size)

            val entries = game.chronicle.entries
            summary.players.forEach { playerSummary ->
                val player = game.players[playerSummary.seat]
                val score = runResult.finalScoring.scores.first { it.playerId == player.id }

                assertEquals(player.id, playerSummary.playerId)
                assertEquals(score.totalVp, playerSummary.totalVp)
                assertEquals(score.plantVp, playerSummary.plantVp)
                assertEquals(score.unplayedWispVp, playerSummary.unplayedWispVp)
                assertEquals(player.creature.cards.sumOf { it.card.cost }, playerSummary.finalPlantPrintedCost)
                assertEquals(player.creature.size, playerSummary.finalPlantCount)
                assertEquals(
                    player.creature.cards
                        .map { PlantCreatureCardSignature(it.card.name, it.side, it.position.x, it.position.y) }
                        .sortedWith(compareBy({ it.y }, { it.x }, { it.side.ordinal }, { it.plantName })),
                    playerSummary.plantCreatureSignature.cards
                )
                assertEquals(player.wisps.size, playerSummary.finalWispCount)
                assertEquals(playerSummary.finalDiceCount, playerSummary.ownedDiceSignature.totalDice)
                assertEquals(playerSummary.finalDicePower, playerSummary.ownedDiceSignature.totalPower)
                val allOwnedDieSides =
                    (player.dice.supply + player.dice.hand + player.dice.discard).map { it.sides } +
                        player.tokens.mulchTokens.mapNotNull { it.sides?.value } +
                        player.tokens.pendingMulchTokens.mapNotNull { it.sides?.value }
                assertEquals(allOwnedDieSides.count { it == DieSides.D4.value }, playerSummary.ownedDiceSignature.d4)
                assertEquals(allOwnedDieSides.count { it == DieSides.D6.value }, playerSummary.ownedDiceSignature.d6)
                assertEquals(allOwnedDieSides.count { it == DieSides.D8.value }, playerSummary.ownedDiceSignature.d8)
                assertEquals(allOwnedDieSides.count { it == DieSides.D10.value }, playerSummary.ownedDiceSignature.d10)
                assertEquals(allOwnedDieSides.count { it == DieSides.D12.value }, playerSummary.ownedDiceSignature.d12)
                assertEquals(allOwnedDieSides.count { it == DieSides.D20.value }, playerSummary.ownedDiceSignature.d20)
                assertEquals(
                    entries.filterIsInstance<GameEntry.StrikeResolved>()
                        .sumOf { if (player.id in it.winnerIds) it.vpPerWinner else 0 },
                    playerSummary.battleStrikeVp
                )
                assertEquals(
                    entries.count { it is GameEntry.Wound && it.playerId == player.id },
                    playerSummary.woundsTaken
                )
                assertEquals(
                    entries.count {
                        it is GameEntry.RollReward &&
                            it.playerId == player.id &&
                            it.kind in setOf(
                                RollRewardKind.WISP_GAINED,
                                RollRewardKind.WISP_PLAYED_IMMEDIATELY
                            )
                    },
                    playerSummary.rollRewardWispsGained
                )
                assertEquals(
                    entries.count {
                        it is GameEntry.SupportAction &&
                            it.playerId == player.id &&
                            it.action == SupportActionKind.WISP
                    } + entries.count {
                        it is GameEntry.RollReward &&
                            it.playerId == player.id &&
                            it.kind == RollRewardKind.WISP_PLAYED_IMMEDIATELY
                    },
                    playerSummary.wispsPlayed
                )
            }

            assertEquals(1.0, summary.players.sumOf { it.winShare })
            assertTrue(summary.players.filter { it.won }.all { it.winShare > 0.0 })
            assertTrue(summary.players.filterNot { it.won }.all { it.winShare == 0.0 })

            val retainedTypes = GameSummary::class.java.declaredFields.map { it.type.name } +
                PlayerGameSummary::class.java.declaredFields.map { it.type.name } +
                PlantCreatureSignature::class.java.declaredFields.map { it.type.name } +
                PlantCreatureCardSignature::class.java.declaredFields.map { it.type.name } +
                OwnedDiceSignature::class.java.declaredFields.map { it.type.name }
            assertFalse(retainedTypes.any { it.contains("GameChronicle") || it.endsWith(".Game") })
        } finally {
            application.close()
        }
    }

    private fun loadCatalogs(
        plantRegistry: PlantCardRegistry,
        plantManager: PlantCardManager,
        wispRegistry: WispCardRegistry,
        wispManager: WispCardManager,
        roundRegistry: RoundCardRegistry,
        roundManager: RoundCardManager
    ) {
        val root = CardDataFiles.dataDirectory()

        plantRegistry.clear()
        plantRegistry.loadFromCsv(
            CardDataFiles.dataPath(CardDataFiles.ROOT_CARD_LIST, root),
            CardDataFiles.dataPath(CardDataFiles.VF_CARD_LIST, root)
        )
        plantManager.loadCards(plantRegistry)

        wispRegistry.clear()
        wispRegistry.loadFromCsv(CardDataFiles.dataPath(CardDataFiles.WISP_LIST, root))
        wispManager.loadCards(wispRegistry)

        roundRegistry.clear()
        roundRegistry.loadFromCsv(CardDataFiles.dataPath(CardDataFiles.ROUND_CARD_LIST, root))
        roundManager.loadCards(roundRegistry)
    }
}
