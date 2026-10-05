package dugsolutions.leaf.v35.player.decision.learned.battle.main

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.battle.*
import dugsolutions.leaf.v35.player.decision.context.BattleView
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.context.GameProgressView
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class LearnedBattleMainPolicyTest {
    private fun context(): DecisionContext = DecisionContext.EMPTY.copy(
        progress = GameProgressView.EMPTY.copy(
            totalRounds = 10,
            roundsCompleted = 5,
            currentBattleRoundNumber = 2,
            battleRoundsRemaining = 2
        ),
        battle = BattleView(playerOrder = emptyList(), donePlayerIds = emptySet(), rows = emptyList())
    )

    private fun request(reference: BattleMainAction = BattleMainAction.Draw) = ChooseBattleMainActionRequest(
        legalActions = listOf(BattleMainAction.Draw, BattleMainAction.RoundEffect1),
        referenceAction = reference,
        observation = BattleMainObservation(
            stage = BattleMainPolicyStage.FIRST,
            roundCardName = "Battle",
            firstRoundEffect = GameEffect.GAIN_WATER_TOKEN,
            secondRoundEffect = GameEffect.GAIN_SUNLIGHT_TOKEN,
            context = context()
        )
    )

    @Test
    fun `Human Battle Main preserves reference action exactly`() {
        val reference = BattleMainAction.RoundEffect1
        assertSame(reference, HumanBattleMainPolicy().chooseMainAction(request(reference)))
    }

    @Test
    fun `learned Battle Main selects only legal action deterministically`() {
        val weights = LearnedBattleMainWeights.zeros().let { base ->
            val values = base.toDoubleArray()
            values[BattleMainFeature.ACTION_ROUND_EFFECT_1.ordinal] = 10.0
            LearnedBattleMainWeights.fromDoubleArray(values, base.provenance, mapOf(
                LearnedBattleMainWeights.effectFeature(GameEffect.GAIN_WATER_TOKEN) to 0.0,
                LearnedBattleMainWeights.effectFeature(GameEffect.GAIN_SUNLIGHT_TOKEN) to 0.0
            ))
        }
        assertEquals(BattleMainAction.RoundEffect1, LearnedBattleMainPolicy(weights).chooseMainAction(request()))
    }

    @Test
    fun `Battle Main weights round trip`() {
        val path = Files.createTempFile("battle-main", ".weights")
        try {
            val weights = LearnedBattleMainWeights.zeros(namedFeatureKeys = listOf(
                LearnedBattleMainWeights.effectFeature(GameEffect.GAIN_WATER_TOKEN)
            ))
            weights.save(path)
            val loaded = LearnedBattleMainWeights.load(path)
            assertEquals(weights.toDoubleArray().toList(), loaded.toDoubleArray().toList())
            assertEquals(weights.namedWeights(), loaded.namedWeights())
        } finally {
            Files.deleteIfExists(path)
        }
    }
}
