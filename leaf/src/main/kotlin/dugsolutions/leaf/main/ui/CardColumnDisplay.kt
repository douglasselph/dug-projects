package dugsolutions.leaf.main.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import dugsolutions.leaf.cards.FakeCards
import dugsolutions.leaf.main.domain.CardInfo
import dugsolutions.leaf.main.gather.GatherCardInfo

@Composable
fun CardsColumnDisplay(
    cards: List<CardInfo>
) {
    Column {
        cards.forEach { cardInfo ->
            CardDisplay(cardInfo)
        }
    }
}


// Preview window for testing card rows
fun main() = application {
    val gatherCardInfo = GatherCardInfo()
    Window(
        onCloseRequest = ::exitApplication,
        title = "CardsColumnDisplay Preview",
        state = WindowState(
            width = 800.dp,
            height = 800.dp
        )
    ) {

        CardsColumnDisplay(
            listOf(
                gatherCardInfo(card = FakeCards.bloomCard),
                gatherCardInfo(card = FakeCards.bloomCard2),
                gatherCardInfo(card = FakeCards.bloomCard3)
            )
        )

    }
}
