package dugsolutions.leaf.v35.player.decision.cultivation

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.support.SupportAction
import dugsolutions.leaf.v35.round.domain.RoundCard
import dugsolutions.leaf.v35.round.domain.RoundCardEffect
import dugsolutions.leaf.v35.round.domain.RoundCardType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class BaselineCultivationStrategyTest {

    private val strategy = BaselineCultivationStrategy()

    @Test
    fun chooseAction_whenMainDrawOffered_choosesDraw() {
        val request = request(
            remaining = 2,
            choices = listOf(
                CultivationAction.Main(CultivationMainAction.RoundEffect2),
                CultivationAction.Main(CultivationMainAction.Draw)
            )
        )

        assertEquals(
            CultivationAction.Main(CultivationMainAction.Draw),
            strategy.chooseAction(request)
        )
    }

    @Test
    fun chooseAction_whenDrawUnavailable_choosesFirstMainBeforeSupport() {
        val support = CultivationAction.Support(
            SupportAction.UseWaterRefresh
        )
        val main = CultivationAction.Main(
            CultivationMainAction.RoundEffect2
        )
        val request = request(
            remaining = 1,
            choices = listOf(support, main)
        )

        assertEquals(main, strategy.chooseAction(request))
    }

    @Test
    fun chooseAction_afterBothMainActions_prefersDoneOverOptionalSupport() {
        val request = request(
            remaining = 0,
            choices = listOf(
                CultivationAction.Support(SupportAction.UseWaterRefresh),
                CultivationAction.Done
            )
        )

        assertEquals(CultivationAction.Done, strategy.chooseAction(request))
    }

    @Test
    fun request_defensivelyCopiesLegalChoices() {
        val incoming = mutableListOf<CultivationAction>(
            CultivationAction.Main(CultivationMainAction.RoundEffect1)
        )
        val request = request(2, incoming)
        incoming.clear()

        assertEquals(
            listOf(CultivationAction.Main(CultivationMainAction.RoundEffect1)),
            request.legalChoices
        )
    }

    private fun request(
        remaining: Int,
        choices: List<CultivationAction>
    ) = ChooseCultivationActionRequest(
        roundCard = roundCard(),
        mainActionsRemaining = remaining,
        legalChoices = choices
    )

    private fun roundCard() = RoundCard(
        quantity = 1,
        name = "Cultivation_Test",
        type = RoundCardType.CULTIVATION,
        firstEffect = effect("First"),
        secondEffect = effect("Second"),
        backImage = ""
    )

    private fun effect(title: String) = RoundCardEffect(
        title = title,
        backgroundColor = "",
        textColor = "",
        image = "",
        icon = null,
        effect = GameEffect.UNKNOWN
    )
}
