package dugsolutions.leaf.game.acquire.evaluator

import dugsolutions.leaf.player.Player
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach

class PossibleCardsTest {

    private val canPurchaseCards = mockk<CanPurchaseCards>(relaxed = true)
    private val mockPlayer = mockk<Player>(relaxed = true)

    private val SUT = PossibleCards(canPurchaseCards)

    @BeforeEach
    fun setup() {
    }

}
