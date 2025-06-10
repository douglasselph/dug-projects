package dugsolutions.leaf.main.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import dugsolutions.leaf.components.CardEffect
import dugsolutions.leaf.components.Cost
import dugsolutions.leaf.components.CostElement
import dugsolutions.leaf.components.FlourishType
import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.components.MatchWith
import dugsolutions.leaf.components.die.Dice
import dugsolutions.leaf.components.die.SampleDie
import dugsolutions.leaf.grove.domain.MarketStackID
import dugsolutions.leaf.main.domain.Colors
import dugsolutions.leaf.main.domain.GroveInfo
import dugsolutions.leaf.main.domain.HighlightInfo
import dugsolutions.leaf.main.domain.ItemInfo
import dugsolutions.leaf.main.domain.StackInfo
import dugsolutions.leaf.main.gather.GatherCardInfo
import dugsolutions.leaf.main.gather.GatherDiceInfo


@Composable
fun GroveDisplay(grove: GroveInfo, onSelected: (item: ItemInfo) -> Unit = {}) {
    Surface(
        border = BorderStroke(2.dp, MaterialTheme.colors.primary),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .padding(8.dp)
            .shadow(4.dp, RoundedCornerShape(8.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            GroveTitle(grove)
            if (grove.dice.values.isNotEmpty()) {
                DiceDisplay(
                    dice = grove.dice,
                    elementsPerRow = grove.dice.values.size
                ) { die -> onSelected(ItemInfo.Die(die)) }
            }
            GroveCards(grove, onSelected)
        }
    }
}

@Composable
private fun GroveTitle(grove: GroveInfo) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left side: Grove title and quantities grouped together
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Grove",
                style = MaterialTheme.typography.h5,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            grove.quantities?.let {
                Surface(
                    color = MaterialTheme.colors.primary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colors.primary.copy(alpha = 0.3f)),
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = grove.quantities,
                        style = MaterialTheme.typography.caption,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
        // Right side: instruction (unchanged)
        grove.instruction?.let { text ->
            Surface(
                color = Colors.SelectableColor,
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.subtitle1,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

// region Preview

// Preview window for testing grove display
fun main() = application {
    val gatherCardInfo = GatherCardInfo()
    val gatherDiceInfo = GatherDiceInfo()
    val sampleDie = SampleDie()

    Window(
        onCloseRequest = ::exitApplication,
        title = "Grove Display Preview",
        state = WindowState(
            width = 1200.dp,
            height = 1100.dp
        )
    ) {
        // Sample grove data
        val sampleGrove = GroveInfo(
            instruction = "Select for Player 1",
            quantities = "2D4 3D6 4D8 4D10 4D12 4D20",
            dice = gatherDiceInfo(
                Dice(
                    listOf(
                        sampleDie.d4,
                        sampleDie.d6,
                        sampleDie.d8,
                        sampleDie.d10,
                        sampleDie.d12,
                        sampleDie.d20
                    )
                ), values = false
            ),
            stacks = listOf(
                StackInfo(
                    stack = MarketStackID.ROOT_1,
                    topCard = gatherCardInfo(
                        card = GameCard(
                            id = 2,
                            name = "Nourishing Root",
                            type = FlourishType.ROOT,
                            resilience = 3,
                            cost = Cost(listOf(CostElement.TotalDiceMinimum(2))),
                            primaryEffect = CardEffect.DRAW_DIE,
                            primaryValue = 1,
                            matchWith = MatchWith.None,
                            matchEffect = null,
                            matchValue = 0,
                            trashEffect = null,
                            trashValue = 0,
                            thorn = 0
                        )
                    ),
                    numCards = 28
                ),
                StackInfo(
                    stack = MarketStackID.ROOT_2,
                    topCard = gatherCardInfo(
                        card = GameCard(
                            id = 2,
                            name = "Nourishing Root",
                            type = FlourishType.ROOT,
                            resilience = 3,
                            cost = Cost(listOf(CostElement.TotalDiceMinimum(2))),
                            primaryEffect = CardEffect.DRAW_DIE,
                            primaryValue = 1,
                            matchWith = MatchWith.None,
                            matchEffect = null,
                            matchValue = 0,
                            trashEffect = null,
                            trashValue = 0,
                            thorn = 0
                        )
                    ),
                    numCards = 28
                ),
                StackInfo(
                    stack = MarketStackID.CANOPY_1,
                    topCard = gatherCardInfo(
                        card = GameCard(
                            id = 3,
                            name = "Sheltering Canopy",
                            type = FlourishType.CANOPY,
                            resilience = 4,
                            cost = Cost(listOf(CostElement.FlourishTypePresent(FlourishType.ROOT))),
                            primaryEffect = CardEffect.DEFLECT,
                            primaryValue = 2,
                            matchWith = MatchWith.None,
                            matchEffect = null,
                            matchValue = 0,
                            trashEffect = null,
                            trashValue = 0,
                            thorn = 0
                        )
                    ),
                    numCards = 15
                ),
                StackInfo(
                    stack = MarketStackID.CANOPY_2,
                    topCard = gatherCardInfo(
                        card = GameCard(
                            id = 3,
                            name = "Sheltering Canopy",
                            type = FlourishType.CANOPY,
                            resilience = 4,
                            cost = Cost(listOf(CostElement.FlourishTypePresent(FlourishType.ROOT))),
                            primaryEffect = CardEffect.DEFLECT,
                            primaryValue = 2,
                            matchWith = MatchWith.None,
                            matchEffect = null,
                            matchValue = 0,
                            trashEffect = null,
                            trashValue = 0,
                            thorn = 0
                        )
                    ),
                    numCards = 15
                ),
                StackInfo(
                    stack = MarketStackID.VINE_1,
                    topCard = gatherCardInfo(
                        card = GameCard(
                            id = 1,
                            name = "Long Vine",
                            type = FlourishType.VINE,
                            resilience = 2,
                            cost = Cost(emptyList()),
                            primaryEffect = CardEffect.DRAW_CARD,
                            primaryValue = 1,
                            matchWith = MatchWith.None,
                            matchEffect = null,
                            matchValue = 0,
                            trashEffect = null,
                            trashValue = 0,
                            thorn = 0
                        )
                    ),
                    numCards = 42
                ),
                StackInfo(
                    stack = MarketStackID.VINE_2,
                    topCard = gatherCardInfo(
                        card = GameCard(
                            id = 1,
                            name = "Long Vine",
                            type = FlourishType.VINE,
                            resilience = 2,
                            cost = Cost(emptyList()),
                            primaryEffect = CardEffect.DRAW_CARD,
                            primaryValue = 1,
                            matchWith = MatchWith.None,
                            matchEffect = null,
                            matchValue = 0,
                            trashEffect = null,
                            trashValue = 0,
                            thorn = 0
                        )
                    ),
                    numCards = 42
                ),
                StackInfo(
                    stack = MarketStackID.FLOWER_1,
                    topCard = gatherCardInfo(
                        card = GameCard(
                            id = 4,
                            name = "Blooming Flower",
                            type = FlourishType.FLOWER,
                            resilience = 3,
                            cost = Cost(listOf(CostElement.TotalDiceMinimum(1))),
                            primaryEffect = CardEffect.DRAW_CARD,
                            primaryValue = 1,
                            matchWith = MatchWith.None,
                            matchEffect = null,
                            matchValue = 0,
                            trashEffect = null,
                            trashValue = 0,
                            thorn = 0
                        ),
                        highlight = HighlightInfo.SELECTABLE
                    ),
                    numCards = 20
                ),
                StackInfo(
                    stack = MarketStackID.FLOWER_2,
                    topCard = gatherCardInfo(
                        card = GameCard(
                            id = 4,
                            name = "Blooming Flower",
                            type = FlourishType.FLOWER,
                            resilience = 3,
                            cost = Cost(listOf(CostElement.SingleDieMinimum(1))),
                            primaryEffect = CardEffect.DRAW_CARD,
                            primaryValue = 1,
                            matchWith = MatchWith.None,
                            matchEffect = null,
                            matchValue = 0,
                            trashEffect = null,
                            trashValue = 0,
                            thorn = 0
                        ),
                        highlight = HighlightInfo.SELECTED
                    ),
                    numCards = 20
                ),
                StackInfo(
                    stack = MarketStackID.FLOWER_3,
                    topCard = gatherCardInfo(
                        card = GameCard(
                            id = 4,
                            name = "Blooming Flower",
                            type = FlourishType.FLOWER,
                            resilience = 3,
                            cost = Cost(listOf(CostElement.SingleDieMinimum(1))),
                            primaryEffect = CardEffect.DRAW_CARD,
                            primaryValue = 1,
                            matchWith = MatchWith.None,
                            matchEffect = null,
                            matchValue = 0,
                            trashEffect = null,
                            trashValue = 0,
                            thorn = 0
                        )
                    ),
                    numCards = 20
                ),
                StackInfo(
                    stack = MarketStackID.WILD_1,
                    topCard = gatherCardInfo(
                        card = GameCard(
                            id = 5,
                            name = "Wild Growth",
                            type = FlourishType.ROOT,
                            resilience = 3,
                            cost = Cost(listOf(CostElement.SingleDieMinimum(3))),
                            primaryEffect = CardEffect.DRAW_CARD,
                            primaryValue = 2,
                            matchWith = MatchWith.None,
                            matchEffect = null,
                            matchValue = 0,
                            trashEffect = null,
                            trashValue = 0,
                            thorn = 0
                        )
                    ),
                    numCards = 10
                )
            )
        )
        GroveDisplay(sampleGrove)
    }
}

// endregion Preview 
