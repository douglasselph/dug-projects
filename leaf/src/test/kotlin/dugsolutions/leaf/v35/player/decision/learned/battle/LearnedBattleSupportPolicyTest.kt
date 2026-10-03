package dugsolutions.leaf.v35.player.decision.learned.battle

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.plant.domain.PlantScoringRule
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.creature.CreatureCard
import dugsolutions.leaf.v35.player.creature.CreatureCardId
import dugsolutions.leaf.v35.player.creature.CreaturePosition
import dugsolutions.leaf.v35.player.creature.CreatureSide
import dugsolutions.leaf.v35.player.decision.battle.*
import dugsolutions.leaf.v35.player.decision.context.*
import dugsolutions.leaf.v35.round.domain.RoundCardType
import org.junit.jupiter.api.Test
import java.nio.file.Files
import kotlin.test.*

class LearnedBattleSupportPolicyTest {
    private fun context(effect: GameEffect = GameEffect.RAISE_ANY_DIE_PLUS_1): DecisionContext {
        val selfId = PlayerId(0); val oppId = PlayerId(1); val cardId = CreatureCardId(7)
        val board = DecisionContext.EMPTY.self.board.copy(
            id = selfId, sunlight = 2, bees = 1, worms = 1,
            creature = listOf(CreatureCardView(cardId,"Vine_07_01","Berry",PlantType.VINE,14,effect,PlantScoringRule.Fixed(3),CreatureSide.LEFT,CreaturePosition(0,0),CreatureCard.Facing.FACE_UP,true))
        )
        val rows = StrikeRow.entries.map { row -> BattleRowView(row,false,listOf(
            BattlePlayerRowView(selfId,row,emptyList(),emptyList(),4,0,4,false),
            BattlePlayerRowView(oppId,row,emptyList(),emptyList(),6,0,6,false)
        )) }
        return DecisionContext.EMPTY.copy(
            phase = RoundCardType.BATTLE,
            progress = DecisionContext.EMPTY.progress.copy(currentBattleRoundNumber=2,battleRoundsRemaining=2,totalBattleRounds=3),
            self = SelfPlayerView(board, emptyList()),
            battle = BattleView(listOf(selfId,oppId), emptySet(), rows)
        )
    }

    @Test fun `feature ordering is stable and scoring selects only a legal candidate`() {
        val legal = listOf(
            BattleTurnAction.Support(BattleSupportAction.PlaceCritter(dugsolutions.leaf.v35.tokens.Critter.BEE, StrikeRow.TOP)),
            BattleTurnAction.FinalMain(BattleMainAction.Draw)
        )
        val request = ChooseBattleSupportActionRequest(legal, legal.last(), BattleSupportObservation(1,"B",GameEffect.GAIN_WATER_TOKEN,GameEffect.GAIN_MULCH_AND_STORE_DIE_FROM_DISCARD,context()))
        val values = DoubleArray(BattleSupportFeature.entries.size)
        values[BattleSupportFeature.ACTION_BEE.ordinal] = 10.0
        val policy = LearnedBattleSupportPolicy(LearnedBattleSupportWeights.fromDoubleArray(values))
        assertSame(legal.first(), policy.chooseSupport(request))
        assertEquals(BattleSupportFeature.entries.size, BattleSupportFeatureExtractor.extract(request, legal.first()).toDoubleArray().size)
    }

    @Test fun `effective Round and Plant effects are visible as named features`() {
        val card = CreatureCard(
            id = CreatureCardId(7),
            card = PlantCard(
                quantity = 1,
                name = "Vine_07_01",
                title = "Berry",
                type = PlantType.VINE,
                cost = 7,
                lineIcon = null,
                vpIcon = "",
                typeIcon = "",
                fgColor = "",
                textColor = "",
                fullImage = "",
                backgroundImage = "",
                cardBackgroundImage = "",
                effect = GameEffect.RAISE_ANY_DIE_PLUS_1,
                scoringRule = PlantScoringRule.Fixed(3)
            ),
            side = CreatureSide.LEFT,
            position = CreaturePosition(0, 0),
            facing = CreatureCard.Facing.FACE_UP
        )
        val action = BattleTurnAction.Support(BattleSupportAction.UseSunlight(BattleMainAction.ActivatePlant(card)))
        val request = ChooseBattleSupportActionRequest(listOf(action), action, BattleSupportObservation(1,"B",GameEffect.GAIN_SUNLIGHT_TOKEN,GameEffect.GAIN_MULCH_AND_STORE_DIE_FROM_DISCARD,context(GameEffect.RAISE_D8_PLUS_1)))
        val fv = BattleSupportFeatureExtractor.extract(request, action)
        assertEquals(1.0, fv.namedValues()[LearnedBattleSupportWeights.plantFeature("Vine_07_01")])
        assertEquals(1.0, fv.namedValues()[LearnedBattleSupportWeights.effectFeature(GameEffect.RAISE_D8_PLUS_1)])

        val round = BattleTurnAction.Support(BattleSupportAction.UseSunlight(BattleMainAction.RoundEffect1))
        val rr = request.copyFor(round)
        assertEquals(1.0, BattleSupportFeatureExtractor.extract(rr, round).namedValues()[LearnedBattleSupportWeights.effectFeature(GameEffect.GAIN_SUNLIGHT_TOKEN)])
    }

    @Test fun `weight persistence is reproducible and manifest compatible`() {
        val prepared = LearnedBattleSupportCatalog.prepare(LearnedBattleSupportWeights.zeros(), emptyList())
        val path = Files.createTempFile("battle-support", ".weights")
        prepared.save(path)
        val loaded = LearnedBattleSupportWeights.load(path)
        LearnedBattleSupportCatalog.validateCurrentSchema(loaded, emptyList())
        assertContentEquals(prepared.toDoubleArray(), loaded.toDoubleArray())
    }

    private fun ChooseBattleSupportActionRequest.copyFor(action: BattleTurnAction) =
        ChooseBattleSupportActionRequest(listOf(action), action, observation)
}
