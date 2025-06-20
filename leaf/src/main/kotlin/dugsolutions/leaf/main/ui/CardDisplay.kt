package dugsolutions.leaf.main.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import dugsolutions.leaf.cards.domain.CardEffect
import dugsolutions.leaf.cards.cost.Cost
import dugsolutions.leaf.cards.cost.CostElement
import dugsolutions.leaf.cards.domain.FlourishType
import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.cards.domain.MatchWith
import dugsolutions.leaf.game.battle.MatchingBloomCard
import dugsolutions.leaf.main.domain.CardInfo
import dugsolutions.leaf.main.domain.Colors
import dugsolutions.leaf.main.domain.HighlightInfo
import dugsolutions.leaf.main.gather.GatherCardInfo

@Composable
fun CardDisplay(
    cardInfo: CardInfo,
    onSelected: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .width(200.dp)
            .padding(4.dp)
            .then(
                if (cardInfo.highlight != HighlightInfo.NONE) {
                    Modifier.clickable(onClick = onSelected)
                } else Modifier
            ),
        elevation = 4.dp,
        shape = RoundedCornerShape(8.dp),
        backgroundColor = when (cardInfo.highlight) {
            HighlightInfo.SELECTABLE ->  Colors.SelectableColor
            HighlightInfo.SELECTED -> Colors.SelectedColor
            else -> MaterialTheme.colors.surface
        }
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth()
        ) {
            // Top row with name, type, resilience, and thorn
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Card name
                Text(
                    text = cardInfo.name,
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    softWrap = true,
                    overflow = TextOverflow.Visible
                )

                // Type indicator
                if (cardInfo.type.isNotEmpty()) {
                    Indicator(cardInfo.type, MaterialTheme.colors.primary)
                }

                // Resilience value
                if (cardInfo.resilience > 0) {
                    Indicator(cardInfo.resilience.toString(), Color.Red)
                }

                // Nutrient value
                if (cardInfo.nutrient > 0) {
                    Indicator(cardInfo.nutrient.toString(), Color.Blue)
                }

                // Thorn value
                if (cardInfo.thorn > 0) {
                    Indicator(cardInfo.thorn.toString(), Color.Black)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Primary effect
            if (cardInfo.primary != null) {
                EffectBox(
                    label = "P",
                    text = cardInfo.primary,
                    color = MaterialTheme.colors.primary
                )
            }

            // Match effect
            if (cardInfo.match != null) {
                EffectBox(
                    label = "M",
                    text = cardInfo.match,
                    color = Color.Blue
                )
            }

            // Trash effect
            if (cardInfo.trash != null) {
                EffectBox(
                    label = "T",
                    text = cardInfo.trash,
                    color = Color.Red
                )
            }

            // Matching Bloom
            if (cardInfo.bloom != null) {
                EffectBox(
                    label = "B",
                    text = cardInfo.bloom,
                    color = Color.Yellow
                )
            }

            // Cost
            CostBox(cardInfo.cost)
        }
    }
}

@Composable
private fun Indicator(text: String, color: Color) {
    Surface(
        border = BorderStroke(1.dp, color),
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.padding(horizontal = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.caption,
            color = color,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun EffectBox(
    label: String,
    text: String,
    color: Color
) {
    Surface(
        border = BorderStroke(1.dp, color),
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.caption,
                color = color,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(end = 4.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.caption,
                color = color
            )
        }
    }
}

@Composable
private fun CostBox(cost: String) {
    if (cost.isEmpty()) return

    Surface(
        border = BorderStroke(1.dp, Color.Green),
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Text(
            text = cost,
            style = MaterialTheme.typography.caption,
            color = Color.Black,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}

// region Preview

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Card Display Preview",
        state = WindowState(
            width = 1200.dp,
            height = 400.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // First row with 5 cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PreviewSeedlingCard()
                PreviewRootCard()
                PreviewCanopyCard()
                PreviewVineCard()
                PreviewFlowerCard()
            }

            // Second row with 4 cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PreviewBloomCard()
                PreviewCardWithAllEffects()
                PreviewCardWithNoEffects()
                PreviewCardWithOnlyPrimary()
            }
        }
    }
}

@Composable
private fun PreviewCard(
    gameCard: GameCard,
    highlight: HighlightInfo = HighlightInfo.NONE
) {
    val gatherCardInfo = GatherCardInfo.previewVariation()
    CardDisplay(
        gatherCardInfo(
            card = gameCard,
            highlight = highlight
        )
    )
}

@Composable
private fun PreviewSeedlingCard() {
    PreviewCard(
        GameCard(
            id = 1,
            name = "Sprouting Seed",
            type = FlourishType.SEEDLING,
            resilience = 0,
            nutrient = 0,
            cost = Cost(emptyList()),
            primaryEffect = CardEffect.DRAW_CARD,
            primaryValue = 1,
            matchWith = MatchWith.None,
            matchEffect = CardEffect.REDUCE_COST_ROOT,
            matchValue = 2,
            trashEffect = CardEffect.GAIN_FREE_ROOT,
            trashValue = 1,
            thorn = 0
        )
    )
}

@Composable
private fun PreviewRootCard() {
    PreviewCard(
        GameCard(
            id = 2,
            name = "Nourishing Root",
            type = FlourishType.ROOT,
            resilience = 9,
            nutrient = 1,
            cost = Cost.from(listOf(CostElement.SingleDieMinimum(2))),
            primaryEffect = CardEffect.DRAW_DIE,
            primaryValue = 1,
            matchWith = MatchWith.None,
            matchEffect = null,
            matchValue = 0,
            trashEffect = CardEffect.RETAIN_DIE,
            trashValue = 1,
            thorn = 0
        ),
        highlight = HighlightInfo.SELECTED
    )
}

@Composable
private fun PreviewCanopyCard() {
    PreviewCard(
        GameCard(
            id = 3,
            name = "Sheltering Canopy",
            type = FlourishType.CANOPY,
            resilience = 14,
            nutrient = 3,
            cost = Cost.from(listOf(CostElement.FlourishTypePresent(FlourishType.ROOT))),
            primaryEffect = CardEffect.DEFLECT,
            primaryValue = 2,
            matchWith = MatchWith.None,
            matchEffect = null,
            matchValue = 0,
            trashEffect = null,
            trashValue = 0,
            thorn = 0
        )
    )
}

@Composable
private fun PreviewVineCard() {
    PreviewCard(
        GameCard(
            id = 4,
            name = "Thorny Vine",
            type = FlourishType.VINE,
            resilience = 8,
            nutrient = 1,
            cost = Cost.from(listOf(CostElement.FlourishTypePresent(FlourishType.CANOPY), CostElement.TotalDiceMinimum(14))),
            primaryEffect = CardEffect.DRAW_CARD,
            primaryValue = 1,
            matchWith = MatchWith.None,
            matchEffect = null,
            matchValue = 0,
            trashEffect = CardEffect.REUSE_DIE,
            trashValue = 1,
            thorn = 2
        )
    )
}

@Composable
private fun PreviewFlowerCard() {
    PreviewCard(
        GameCard(
            id = 5,
            name = "Spring Flower",
            type = FlourishType.FLOWER,
            resilience = 10,
            nutrient = 1,
            cost = Cost.from(listOf(CostElement.FlourishTypePresent(FlourishType.CANOPY), CostElement.TotalDiceMinimum(5))),
            primaryEffect = null,
            primaryValue = 0,
            matchWith = MatchWith.None,
            matchEffect = null,
            matchValue = 0,
            trashEffect = null,
            trashValue = 5,
            thorn = 0
        ), highlight = HighlightInfo.SELECTABLE
    )
}

@Composable
private fun PreviewBloomCard() {
    PreviewCard(
        GameCard(
            id = 6,
            name = "Spring Bloom",
            type = FlourishType.BLOOM,
            resilience = 1,
            nutrient = 10,
            cost = Cost(emptyList()),
            primaryEffect = CardEffect.ADD_TO_TOTAL,
            primaryValue = 3,
            matchWith = MatchWith.Flower(5), // References the Flower card above
            matchEffect = CardEffect.ADD_TO_TOTAL,
            matchValue = 1,
            trashEffect = CardEffect.DEFLECT,
            trashValue = 2,
            thorn = 0
        )
    )
}

// Preview cards for different effect combinations
@Composable
private fun PreviewCardWithAllEffects() {
    PreviewCard(
        GameCard(
            id = 7,
            name = "Powerful Card",
            type = FlourishType.VINE,
            resilience = 8,
            nutrient = 1,
            cost = Cost(emptyList()),
            primaryEffect = CardEffect.DRAW_CARD,
            primaryValue = 2,
            matchWith = MatchWith.OnRoll(6),
            matchEffect = CardEffect.DRAW_DIE,
            matchValue = 1,
            trashEffect = CardEffect.REUSE_DIE,
            trashValue = 2,
            thorn = 1
        )
    )
}

@Composable
private fun PreviewCardWithNoEffects() {
    PreviewCard(
        GameCard(
            id = 8,
            name = "Basic Card",
            type = FlourishType.ROOT,
            resilience = 8,
            nutrient = 1,
            cost = Cost(emptyList()),
            primaryEffect = null,
            primaryValue = 0,
            matchWith = MatchWith.None,
            matchEffect = null,
            matchValue = 0,
            trashEffect = null,
            trashValue = 0,
            thorn = 0
        )
    )
}

@Composable
private fun PreviewCardWithOnlyPrimary() {
    PreviewCard(
        GameCard(
            id = 9,
            name = "Primary Only",
            type = FlourishType.CANOPY,
            resilience = 18,
            nutrient = 3,
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
    )
}


// endregion Preview
