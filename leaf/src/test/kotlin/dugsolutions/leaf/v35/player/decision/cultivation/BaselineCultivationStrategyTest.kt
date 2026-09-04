package dugsolutions.leaf.v35.player.decision.cultivation

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.round.domain.RoundCard
import dugsolutions.leaf.v35.round.domain.RoundCardEffect
import dugsolutions.leaf.v35.round.domain.RoundCardType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class BaselineCultivationStrategyTest {

    private val strategy = BaselineCultivationStrategy()

    @Test
    fun chooseMainAction_whenDrawOffered_choosesDraw() {
        val request = request(listOf(CultivationMainAction.Draw))

        assertEquals(CultivationMainAction.Draw, strategy.chooseMainAction(request))
    }

    @Test
    fun chooseMainAction_whenDrawIsNotFirst_stillChoosesDraw() {
        val request = request(
            listOf(CultivationMainAction.RoundEffect2, CultivationMainAction.Draw)
        )

        assertEquals(CultivationMainAction.Draw, strategy.chooseMainAction(request))
    }

    @Test
    fun chooseMainAction_whenDrawUnavailable_choosesFirstLegalChoiceDeterministically() {
        val choices = listOf(
            CultivationMainAction.RoundEffect2,
            CultivationMainAction.RoundEffect1
        )
        val request = request(choices)

        assertEquals(choices.first(), strategy.chooseMainAction(request))
        assertEquals(choices.first(), strategy.chooseMainAction(request))
    }

    @Test
    fun request_defensivelyCopiesLegalChoices() {
        val incoming = mutableListOf<CultivationMainAction>(
            CultivationMainAction.RoundEffect1
        )

        val request = request(incoming)
        incoming.clear()

        assertEquals(listOf(CultivationMainAction.RoundEffect1), request.legalChoices)
    }

    private fun request(choices: List<CultivationMainAction>) =
        ChooseCultivationMainActionRequest(
            roundCard = roundCard(),
            actionNumber = 1,
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
