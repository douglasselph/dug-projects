package dugsolutions.leaf.v35.game.scoring

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.game.GameEngineTestFixture
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.plant.domain.PlantScoringRule
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.Player
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.DecisionDirector
import dugsolutions.leaf.v35.tokens.Butterfly
import dugsolutions.leaf.v35.wisp.domain.WispCard
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class FinalScorerTest {

    @Test
    fun score_addsExistingPlantAndUnplayedWispVp_includingVariablePlantRules() {
        val player = player(1)
        val opponent = player(2)
        player.addVp(5)

        graft(player, plant("Berry Important", PlantType.VINE, PlantScoringRule.Fixed(3)))
        graft(player, plant("Vine Yield", PlantType.VINE, PlantScoringRule.PerGraftedVine))
        graft(player, plant("Alluring Nectar", PlantType.FLOWER, PlantScoringRule.PerButterfly))

        player.butterflies.add(Butterfly.GREEN)
        player.butterflies.add(Butterfly.PURPLE)
        player.wisps.add(wisp("Wisp of Honor", endGameVp = 2))

        val result = FinalScorer().score(
            GameEngineTestFixture.game(players = listOf(player, opponent))
        )

        val score = result.scores.first { it.playerId == player.id }
        assertEquals(5, score.existingVp)
        assertEquals(7, score.plantVp) // 3 + 2 Vines + 2 Butterflies
        assertEquals(2, score.unplayedWispVp)
        assertEquals(14, score.totalVp)
        assertEquals(3, score.graftedPlantCount)
        assertEquals(listOf(player.id), result.winnerIds)
    }

    @Test
    fun score_plantCountBreaksTieAmongHighestVpPlayers() {
        val first = player(1).apply { addVp(10) }
        val second = player(2).apply { addVp(10) }

        graft(first, plant("First Root", PlantType.ROOT, PlantScoringRule.Fixed(0)))
        graft(second, plant("Second Root A", PlantType.ROOT, PlantScoringRule.Fixed(0)))
        graft(second, plant("Second Root B", PlantType.ROOT, PlantScoringRule.Fixed(0)))

        val result = FinalScorer().score(
            GameEngineTestFixture.game(players = listOf(first, second))
        )

        assertEquals(listOf(second.id), result.winnerIds)
    }

    @Test
    fun score_whenVpAndPlantCountStillTie_sharesTitle() {
        val first = player(1).apply { addVp(7) }
        val second = player(2).apply { addVp(7) }

        val result = FinalScorer().score(
            GameEngineTestFixture.game(players = listOf(first, second))
        )

        assertEquals(listOf(first.id, second.id), result.winnerIds)
    }

    private fun player(id: Int): Player =
        Player(PlayerId(id), DecisionDirector.baseline())

    private fun graft(player: Player, card: PlantCard) {
        player.creature.graft(
            card,
            player.creature.legalPlacements(card).first()
        )
    }

    private fun plant(
        title: String,
        type: PlantType,
        scoringRule: PlantScoringRule
    ): PlantCard =
        PlantCard(
            quantity = 1,
            name = title.replace(" ", "_"),
            title = title,
            type = type,
            cost = 1,
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

    private fun wisp(
        title: String,
        endGameVp: Int
    ): WispCard =
        WispCard(
            quantity = 1,
            name = title.replace(" ", "_"),
            title = title,
            count = 1,
            effect = GameEffect.UNKNOWN,
            lineIcons = null,
            lineIconsHeight = 0,
            vpIcon = null,
            mainBackdrop = "",
            endGameVp = endGameVp
        )
}
