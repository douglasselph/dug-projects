package dugsolutions.leaf.simulation.v35.experiment.plant

import dugsolutions.leaf.v35.chronicle.GameChronicle
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.game.Game
import dugsolutions.leaf.v35.game.GameConfig
import dugsolutions.leaf.v35.game.PlayerDecisionFactory
import dugsolutions.leaf.v35.game.scoring.FinalScorer
import dugsolutions.leaf.v35.grove.Grove
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.plant.domain.PlantScoringRule
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.Player
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.DecisionDirector
import dugsolutions.leaf.v35.random.Randomizer
import dugsolutions.leaf.v35.round.RoundCardManager
import dugsolutions.leaf.v35.round.RoundDeck
import dugsolutions.leaf.v35.tokens.Butterfly
import dugsolutions.leaf.v35.wisp.WispCardManager
import dugsolutions.leaf.v35.wisp.WispDeck
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class PlantExperimentScoringIntegrationTest {

    @Test
    fun perGraftedFlowerOverride_scoresEachVineYieldCopyFromFlowerCount() {
        val vineYield = plant("Vine_07_04", PlantType.VINE, PlantScoringRule.PerGraftedVine)
        val flower = plant("Flower_Test", PlantType.FLOWER, PlantScoringRule.Fixed(0))
        val player = player(1)
        val opponent = player(2)

        repeat(3) { graft(player, vineYield) }
        repeat(2) { graft(player, flower) }

        val overrides = PlantExperimentConfig.of(
            vineYield.name to PlantExperimentOverride(
                scoringRule = PlantScoringRule.PerGraftedFlower
            )
        )
        val result = FinalScorer().score(game(listOf(player, opponent), overrides))

        assertEquals(6, result.scores.first { it.playerId == player.id }.plantVp)
        assertEquals(PlantScoringRule.PerGraftedVine, vineYield.scoringRule)
    }

    @Test
    fun fixedScoringOverride_changesOnlyEffectiveScoringRule() {
        val berry = plant("Vine_07_01", PlantType.VINE, PlantScoringRule.Fixed(3))
        val player = player(1)
        val opponent = player(2)
        repeat(2) { graft(player, berry) }

        val overrides = PlantExperimentConfig.of(
            berry.name to PlantExperimentOverride(scoringRule = PlantScoringRule.Fixed(1))
        )
        val result = FinalScorer().score(game(listOf(player, opponent), overrides))

        assertEquals(2, result.scores.first { it.playerId == player.id }.plantVp)
        assertEquals(PlantScoringRule.Fixed(3), berry.scoringRule)
    }

    @Test
    fun existingConditionalScoringRule_canBeAppliedThroughOverlayWithoutIntegerFlattening() {
        val card = plant("Vine_09_03", PlantType.VINE, PlantScoringRule.Fixed(2))
        val player = player(1)
        val opponent = player(2)
        graft(player, card)
        player.butterflies.add(Butterfly.GREEN)
        player.butterflies.add(Butterfly.PURPLE)

        val overrides = PlantExperimentConfig.of(
            card.name to PlantExperimentOverride(scoringRule = PlantScoringRule.PerButterfly)
        )
        val result = FinalScorer().score(game(listOf(player, opponent), overrides))

        assertEquals(2, result.scores.first { it.playerId == player.id }.plantVp)
        assertEquals(PlantScoringRule.PerButterfly, overrides.scoringRuleFor(card))
    }

    private fun game(players: List<Player>, plantValues: PlantExperimentConfig): Game {
        val randomizer = Randomizer.create(123L)
        val marketCards = marketCards()
        return Game(
            config = GameConfig(
                selectedPlantCards = marketCards,
                playerDecisionFactories = List(players.size) { PlayerDecisionFactory.humanBaseline() },
                seed = 123L,
                plantValues = plantValues
            ),
            grove = Grove(
                selectedPlantCards = marketCards,
                wispDeck = WispDeck(WispCardManager(), randomizer)
            ),
            players = players,
            chronicle = GameChronicle(),
            roundDeck = RoundDeck(RoundCardManager(), randomizer),
            randomizer = randomizer
        )
    }

    private fun marketCards(): List<PlantCard> =
        listOf(
            plant("Root_A", PlantType.ROOT, PlantScoringRule.Fixed(0)),
            plant("Root_B", PlantType.ROOT, PlantScoringRule.Fixed(0)),
            plant("Root_C", PlantType.ROOT, PlantScoringRule.Fixed(0)),
            plant("Vine_A", PlantType.VINE, PlantScoringRule.Fixed(0)),
            plant("Vine_B", PlantType.VINE, PlantScoringRule.Fixed(0)),
            plant("Vine_C", PlantType.VINE, PlantScoringRule.Fixed(0)),
            plant("Flower_A", PlantType.FLOWER, PlantScoringRule.Fixed(0)),
            plant("Flower_B", PlantType.FLOWER, PlantScoringRule.Fixed(0)),
            plant("Flower_C", PlantType.FLOWER, PlantScoringRule.Fixed(0))
        )

    private fun player(id: Int): Player =
        Player(PlayerId(id), DecisionDirector.baseline())

    private fun graft(player: Player, card: PlantCard) {
        player.creature.graft(card, player.creature.legalPlacements(card).first())
    }

    private fun plant(
        name: String,
        type: PlantType,
        scoringRule: PlantScoringRule
    ): PlantCard =
        PlantCard(
            quantity = 10,
            name = name,
            title = name,
            type = type,
            cost = 7,
            lineIcon = null,
            vpIcon = "",
            typeIcon = "",
            fgColor = "",
            textColor = "",
            fullImage = "",
            backgroundImage = "",
            cardBackgroundImage = "",
            effect = GameEffect.UNKNOWN,
            scoringRule = scoringRule
        )
}
