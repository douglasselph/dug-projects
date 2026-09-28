package dugsolutions.leaf.v35.player.decision.baseline.buy

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.plant.domain.PlantScoringRule
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.creature.CreatureCard
import dugsolutions.leaf.v35.player.creature.CreatureCardId
import dugsolutions.leaf.v35.player.creature.CreaturePosition
import dugsolutions.leaf.v35.player.creature.CreatureSide
import dugsolutions.leaf.v35.player.decision.buy.BuyItem
import dugsolutions.leaf.v35.player.decision.context.CreatureCardView
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.round.domain.RoundCardType
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class PurchasePriorityTest {
    @Test
    fun `card acquire value breaks a same-cost Plant tie`() {
        val awakening = plant("Root_09_01", GameEffect.UPGRADE_DIE_AND_USE_NOW)
        val cause = plant("Root_09_02", GameEffect.FLIP_OWN_DIE_TO_OPPOSITE_FACE)

        val strong = PurchasePriority.score(DecisionContext.EMPTY, BuyItem.Plant(awakening)).total
        val ordinary = PurchasePriority.score(DecisionContext.EMPTY, BuyItem.Plant(cause)).total

        assertTrue(strong > ordinary)
    }

    @Test
    fun `ordinary Buy favors Root Recall effect over Berry Important VP`() {
        val context = context(battleRoundsRemaining = 2)

        val berry = PurchasePriority.score(context, BuyItem.Plant(berryImportant())).total
        val recall = PurchasePriority.score(context, BuyItem.Plant(rootRecall())).total

        assertTrue(recall > berry, "Expected reusable Root Recall effect to beat Berry Important before the final Buy")
    }

    @Test
    fun `ordinary Buy diversification can favor first Berry over second Root Recall`() {
        val context = context(
            owned = listOf(rootRecall()),
            battleRoundsRemaining = 2
        )

        val berry = PurchasePriority.score(context, BuyItem.Plant(berryImportant())).total
        val secondRecall = PurchasePriority.score(context, BuyItem.Plant(rootRecall())).total

        assertTrue(berry > secondRecall, "A first different card should be attractive against a merely-good duplicate")
    }

    @Test
    fun `ordinary Buy strongly prefers Root Recall over another weak Berry Important`() {
        val context = context(
            owned = listOf(berryImportant()),
            battleRoundsRemaining = 2
        )

        val secondBerry = PurchasePriority.score(context, BuyItem.Plant(berryImportant())).total
        val recall = PurchasePriority.score(context, BuyItem.Plant(rootRecall())).total

        assertTrue(recall > secondBerry)
    }

    @Test
    fun `end game Plant buying window lets high VP Berry Important overtake Root Recall`() {
        val context = context(
            battleRoundsRemaining = 1,
            isFinalCultivationRound = false
        )

        val berry = PurchasePriority.score(context, BuyItem.Plant(berryImportant())).total
        val recall = PurchasePriority.score(context, BuyItem.Plant(rootRecall())).total

        assertTrue(
            berry > recall,
            "One visible Battle remaining should trigger end-game VP pressure even before the final Cultivation round"
        )
    }

    @Test
    fun `high end game purchase value can overcome diversification for repeated Berry Important`() {
        val context = context(
            owned = listOf(berryImportant(), berryImportant()),
            battleRoundsRemaining = 1
        )

        val thirdBerry = PurchasePriority.score(context, BuyItem.Plant(berryImportant())).total
        val firstRecall = PurchasePriority.score(context, BuyItem.Plant(rootRecall())).total

        assertTrue(thirdBerry > firstRecall, "Strong end-game VP value should make repeat Berry buys plausible")
    }

    private fun berryImportant() = plant(
        name = "Vine_07_01",
        effect = GameEffect.RAISE_ANY_DIE_PLUS_1,
        type = PlantType.VINE,
        cost = 7,
        vp = 3
    )

    private fun rootRecall() = plant(
        name = "Root_07_04",
        effect = GameEffect.DISCARD_ANY_NUMBER_OF_DICE_AND_REDRAW_OR_REROLL_ONE_IN_BATTLE,
        type = PlantType.ROOT,
        cost = 7,
        vp = 1
    )

    private fun context(
        owned: List<PlantCard> = emptyList(),
        battleRoundsRemaining: Int?,
        isFinalCultivationRound: Boolean = false
    ): DecisionContext = DecisionContext.EMPTY.copy(
        phase = RoundCardType.CULTIVATION,
        progress = DecisionContext.EMPTY.progress.copy(
            battleRoundsRemaining = battleRoundsRemaining,
            isFinalCultivationRound = isFinalCultivationRound
        ),
        self = DecisionContext.EMPTY.self.copy(
            board = DecisionContext.EMPTY.self.board.copy(
                id = PlayerId(1),
                creature = owned.mapIndexed(::view)
            )
        )
    )

    private fun view(index: Int, card: PlantCard) = CreatureCardView(
        id = CreatureCardId(index + 1),
        name = card.name,
        title = card.title,
        type = card.type,
        cost = card.cost,
        effect = card.effect,
        scoringRule = card.scoringRule,
        side = CreatureSide.LEFT,
        position = CreaturePosition(-(index + 1), 0),
        facing = CreatureCard.Facing.FACE_UP,
        isSnippable = true
    )

    private fun plant(
        name: String,
        effect: GameEffect,
        type: PlantType = PlantType.ROOT,
        cost: Int = 9,
        vp: Int = 1
    ) = PlantCard(
        quantity = 6,
        name = name,
        title = name,
        type = type,
        cost = cost,
        lineIcon = null,
        vpIcon = "",
        typeIcon = "",
        fgColor = "",
        textColor = "",
        fullImage = "",
        backgroundImage = "",
        cardBackgroundImage = "",
        effect = effect,
        scoringRule = PlantScoringRule.Fixed(vp)
    )
}
