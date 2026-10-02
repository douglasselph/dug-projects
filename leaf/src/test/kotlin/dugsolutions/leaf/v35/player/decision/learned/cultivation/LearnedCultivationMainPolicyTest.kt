package dugsolutions.leaf.v35.player.decision.learned.cultivation

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.plant.domain.PlantScoringRule
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.creature.CreatureCard
import dugsolutions.leaf.v35.player.creature.CreatureCardId
import dugsolutions.leaf.v35.player.creature.CreaturePosition
import dugsolutions.leaf.v35.player.creature.CreatureSide
import dugsolutions.leaf.v35.player.decision.DecisionDirector
import dugsolutions.leaf.v35.player.decision.baseline.effect.HumanBaselineEffectStrategy
import dugsolutions.leaf.v35.player.decision.context.CreatureCardView
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.context.SelfPlayerView
import dugsolutions.leaf.v35.player.decision.cultivation.ChooseCultivationMainActionRequest
import dugsolutions.leaf.v35.player.decision.cultivation.CultivationMainAction
import dugsolutions.leaf.v35.player.decision.cultivation.CultivationMainObservation
import dugsolutions.leaf.v35.player.decision.cultivation.HumanCultivationMainPolicy
import dugsolutions.leaf.v35.player.decision.learned.buy.LearnedPolicyCardManifestMismatchException
import dugsolutions.leaf.v35.player.decision.learned.buy.PlantCardManifest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class LearnedCultivationMainPolicyTest {
    @Test
    fun `standard feature ordering is stable enum ordering`() {
        val request = request(listOf(CultivationMainAction.Draw, CultivationMainAction.RoundEffect1))
        val features = CultivationMainFeatureExtractor.extract(request, CultivationMainAction.Draw)

        assertEquals(CultivationMainFeature.entries, features.asMap().keys.toList())
    }

    @Test
    fun `policy chooses only among legal actions and scoring is reproducible`() {
        val request = request(listOf(CultivationMainAction.Draw, CultivationMainAction.RoundEffect1))
        val weights = LearnedCultivationMainWeights.zeros()
            .let { LearnedCultivationMainWeights.fromDoubleArray(it.toDoubleArray().also { a ->
                a[CultivationMainFeature.ACTION_DRAW.ordinal] = 3.0
            }) }
        val policy = LearnedCultivationMainPolicy(weights)

        assertEquals(CultivationMainAction.Draw, policy.chooseMainAction(request))
        assertEquals(CultivationMainAction.Draw, policy.chooseMainAction(request))
    }

    @Test
    fun `effective Round override identity is visible to learner`() {
        val base = LearnedCultivationMainWeights.zeros(
            namedFeatureKeys = listOf(
                LearnedCultivationMainWeights.effectFeature(GameEffect.GAIN_SUNLIGHT_TOKEN),
                LearnedCultivationMainWeights.effectFeature(GameEffect.GAIN_WATER_TOKEN)
            )
        )
        val weights = base.withNamed(
            LearnedCultivationMainWeights.effectFeature(GameEffect.GAIN_SUNLIGHT_TOKEN),
            5.0
        )
        val request = request(
            listOf(CultivationMainAction.Draw, CultivationMainAction.RoundEffect1, CultivationMainAction.RoundEffect2),
            first = GameEffect.GAIN_SUNLIGHT_TOKEN,
            second = GameEffect.GAIN_WATER_TOKEN
        )

        assertEquals(CultivationMainAction.RoundEffect1, LearnedCultivationMainPolicy(weights).chooseMainAction(request))
    }

    @Test
    fun `effective Plant cost and effect overrides in visible context become candidate features`() {
        val canonical = plant("Root_Test", cost = 7, effect = GameEffect.RAISE_ANY_DIE_PLUS_1)
        val creature = CreatureCard(
            id = CreatureCardId(1),
            card = canonical,
            side = CreatureSide.LEFT,
            position = CreaturePosition(-1, -1)
        )
        val visible = CreatureCardView(
            id = creature.id,
            name = canonical.name,
            title = canonical.title,
            type = canonical.type,
            cost = 14,
            effect = GameEffect.RAISE_D8_PLUS_1,
            scoringRule = PlantScoringRule.Fixed(3),
            side = creature.side,
            position = creature.position,
            facing = CreatureCard.Facing.FACE_UP,
            isSnippable = true
        )
        val context = DecisionContext.EMPTY.copy(
            self = SelfPlayerView(
                board = SelfPlayerView.EMPTY.board.copy(creature = listOf(visible)),
                wisps = emptyList()
            )
        )
        val action = CultivationMainAction.ActivatePlant(creature)
        val request = ChooseCultivationMainActionRequest(
            legalActions = listOf(action),
            referenceAction = action,
            observation = CultivationMainObservation(
                mainActionsRemaining = 2,
                roundCardName = "Round",
                firstRoundEffect = GameEffect.UNKNOWN,
                secondRoundEffect = GameEffect.UNKNOWN,
                context = context
            )
        )

        val features = CultivationMainFeatureExtractor.extract(request, action)
        assertEquals(14.0, features[CultivationMainFeature.ACTION_COST])
        assertEquals(1.0, features.namedValue(LearnedCultivationMainWeights.costFeature(14)))
        assertEquals(1.0, features.namedValue(LearnedCultivationMainWeights.effectFeature(GameEffect.RAISE_D8_PLUS_1)))
        assertEquals(0.0, features.namedValue(LearnedCultivationMainWeights.effectFeature(GameEffect.RAISE_ANY_DIE_PLUS_1)))
    }

    @Test
    fun `learned Main director keeps Human lower-level Effect strategy`() {
        val director = LearnedCultivationMain.createDirector(LearnedCultivationMainWeights.zeros())
        assertIs<LearnedCultivationMainPolicy>(director.cultivationMain)
        assertIs<HumanBaselineEffectStrategy>(director.effect)
    }

    @Test
    fun `default Human director still uses Human Cultivation Main policy`() {
        assertIs<HumanCultivationMainPolicy>(DecisionDirector.humanBaseline().cultivationMain)
    }

    @Test
    fun `trained manifest rejects changed canonical Plant definitions`() {
        val oldCard = plant("Root_Test", 7, GameEffect.RAISE_ANY_DIE_PLUS_1)
        val changedCard = plant("Root_Test", 9, GameEffect.RAISE_ANY_DIE_PLUS_1)
        val trained = LearnedCultivationMainWeights.zeros(
            provenance = LearnedCultivationMainProvenance(
                trainingStatus = "trained",
                cardManifest = PlantCardManifest.from(listOf(oldCard))
            )
        )

        assertFailsWith<LearnedPolicyCardManifestMismatchException> {
            LearnedCultivationMainCatalog.prepare(trained, listOf(changedCard))
        }
    }

    @Test
    fun `Sunlight effect has contextual named interaction features`() {
        val request = request(
            listOf(CultivationMainAction.RoundEffect1),
            first = GameEffect.GAIN_SUNLIGHT_TOKEN
        )
        val features = CultivationMainFeatureExtractor.extract(request, CultivationMainAction.RoundEffect1)

        assertTrue(features.namedValues().containsKey(
            LearnedCultivationMainWeights.effectSunlightHeldFeature(GameEffect.GAIN_SUNLIGHT_TOKEN)
        ))
        assertTrue(features.namedValues().containsKey(
            LearnedCultivationMainWeights.effectBattlesRemainingFeature(GameEffect.GAIN_SUNLIGHT_TOKEN)
        ))
    }

    private fun request(
        legal: List<CultivationMainAction>,
        first: GameEffect = GameEffect.GAIN_WATER_TOKEN,
        second: GameEffect = GameEffect.MULCH_DIE_FROM_HAND
    ) = ChooseCultivationMainActionRequest(
        legalActions = legal,
        referenceAction = legal.first(),
        observation = CultivationMainObservation(
            mainActionsRemaining = 2,
            roundCardName = "Round",
            firstRoundEffect = first,
            secondRoundEffect = second,
            context = DecisionContext.EMPTY
        )
    )

    private fun plant(name: String, cost: Int, effect: GameEffect): PlantCard = PlantCard(
        quantity = 1,
        name = name,
        title = name,
        type = PlantType.ROOT,
        cost = cost,
        lineIcon = null,
        vpIcon = "",
        typeIcon = "",
        fgColor = "",
        textColor = "",
        fullImage = "",
        backgroundImage = "",
        cardBackgroundImage = "",
        effect = effect
    )
}
