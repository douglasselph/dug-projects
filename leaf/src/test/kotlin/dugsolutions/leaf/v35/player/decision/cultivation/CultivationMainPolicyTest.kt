package dugsolutions.leaf.v35.player.decision.cultivation

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.creature.CreatureCard
import dugsolutions.leaf.v35.player.creature.CreatureCardId
import dugsolutions.leaf.v35.player.creature.CreaturePosition
import dugsolutions.leaf.v35.player.creature.CreatureSide
import dugsolutions.leaf.v35.player.decision.DecisionDirector
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class CultivationMainPolicyTest {
    @Test
    fun `stable identities do not depend on display text`() {
        val creature = CreatureCard(
            id = CreatureCardId(7),
            card = plant("Root_09_03", "Root Kindred"),
            side = CreatureSide.LEFT,
            position = CreaturePosition(-1, 0)
        )

        assertEquals("DRAW", CultivationMainAction.Draw.stableIdentity().stableId)
        assertEquals("PLANT:Root_09_03", CultivationMainAction.ActivatePlant(creature).stableIdentity().stableId)
        assertEquals("ROUND_EFFECT:1", CultivationMainAction.RoundEffect1.stableIdentity().stableId)
        assertEquals("ROUND_EFFECT:2", CultivationMainAction.RoundEffect2.stableIdentity().stableId)
        assertEquals("DONE", CultivationMainActionId.Done.stableId)
    }

    @Test
    fun `Human Main policy preserves the reference Human choice exactly`() {
        val request = ChooseCultivationMainActionRequest(
            legalActions = listOf(CultivationMainAction.Draw, CultivationMainAction.RoundEffect1),
            referenceAction = CultivationMainAction.RoundEffect1,
            observation = CultivationMainObservation(
                mainActionsRemaining = 2,
                roundCardName = "Round",
                firstRoundEffect = GameEffect.GAIN_SUNLIGHT_TOKEN,
                secondRoundEffect = GameEffect.GAIN_WATER_TOKEN,
                context = DecisionContext.EMPTY
            )
        )

        assertSame(CultivationMainAction.RoundEffect1, HumanCultivationMainPolicy().chooseMainAction(request))
        assertEquals(
            listOf("DRAW", "ROUND_EFFECT:1"),
            request.legalActionIds.map { it.stableId }
        )
    }

    @Test
    fun `replacing only Cultivation Main policy leaves every other decision area untouched`() {
        val baseline = DecisionDirector.humanBaseline()
        val replacement = CultivationMainPolicy { request -> request.legalActions.first() }
        val changed = baseline.copy(cultivationMain = replacement)

        assertSame(baseline.reward, changed.reward)
        assertSame(baseline.wound, changed.wound)
        assertSame(baseline.placement, changed.placement)
        assertSame(baseline.cultivation, changed.cultivation)
        assertSame(baseline.battle, changed.battle)
        assertSame(baseline.buy, changed.buy)
        assertSame(baseline.support, changed.support)
        assertSame(baseline.effect, changed.effect)
        assertSame(replacement, changed.cultivationMain)
    }

    private fun plant(name: String, title: String): PlantCard = PlantCard(
        quantity = 1,
        name = name,
        title = title,
        type = PlantType.ROOT,
        cost = 9,
        lineIcon = null,
        vpIcon = "",
        typeIcon = "",
        fgColor = "",
        textColor = "",
        fullImage = "",
        backgroundImage = "",
        cardBackgroundImage = "",
        effect = GameEffect.SET_DIE_TO_MATCH_ANOTHER
    )
}
