package dugsolutions.leaf.main.ui

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import dugsolutions.leaf.components.*
import dugsolutions.leaf.main.domain.CardInfo
import dugsolutions.leaf.main.gather.GatherCardInfo

@Composable
fun CardDisplay(cardInfo: CardInfo) {
    Card(
        modifier = Modifier
            .width(200.dp)
            .padding(4.dp),
        elevation = 4.dp,
        shape = RoundedCornerShape(8.dp)
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
                    modifier = Modifier.weight(1f)
                )

                // Type indicator
                if (cardInfo.type.isNotEmpty()) {
                    Surface(
                        border = BorderStroke(1.dp, MaterialTheme.colors.primary),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.padding(horizontal = 2.dp)
                    ) {
                        Text(
                            text = cardInfo.type,
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.primary,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                // Resilience value
                if (cardInfo.resilience > 0) {
                    Surface(
                        border = BorderStroke(1.dp, Color.Green),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.padding(horizontal = 2.dp)
                    ) {
                        Text(
                            text = cardInfo.resilience.toString(),
                            style = MaterialTheme.typography.caption,
                            color = Color.Green,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                // Thorn value
                if (cardInfo.thorn > 0) {
                    Surface(
                        border = BorderStroke(1.dp, Color.Red),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.padding(horizontal = 2.dp)
                    ) {
                        Text(
                            text = cardInfo.thorn.toString(),
                            style = MaterialTheme.typography.caption,
                            color = Color.Red,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Effect rows
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
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
            }
        }
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

// Preview helper function
@Composable
private fun PreviewCard(gameCard: GameCard) {
    val gatherCardInfo = GatherCardInfo()
    CardDisplay(gatherCardInfo(gameCard))
}

// Preview window for testing cards
fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Card Display Preview"
    ) {
        // Create a sample card for preview
        val sampleCard = GameCard(
            id = 1,
            name = "Sprouting Seed",
            type = FlourishType.SEEDLING,
            resilience = 2,
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
        
        PreviewCard(sampleCard)
    }
}

// Preview cards for each FlourishType
@Preview
@Composable
fun PreviewSeedlingCard() {
    PreviewCard(
        GameCard(
            id = 1,
            name = "Sprouting Seed",
            type = FlourishType.SEEDLING,
            resilience = 2,
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

@Preview
@Composable
fun PreviewRootCard() {
    PreviewCard(
        GameCard(
            id = 2,
            name = "Nourishing Root",
            type = FlourishType.ROOT,
            resilience = 3,
            cost = Cost(listOf(CostElement.SingleDieMinimum(2))),
            primaryEffect = CardEffect.DRAW_DIE,
            primaryValue = 1,
            matchWith = MatchWith.None,
            matchEffect = null,
            matchValue = 0,
            trashEffect = CardEffect.RETAIN_DIE,
            trashValue = 1,
            thorn = 0
        )
    )
}

@Preview
@Composable
fun PreviewCanopyCard() {
    PreviewCard(
        GameCard(
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
    )
}

@Preview
@Composable
fun PreviewVineCard() {
    PreviewCard(
        GameCard(
            id = 4,
            name = "Thorny Vine",
            type = FlourishType.VINE,
            resilience = 2,
            cost = Cost(emptyList()),
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

@Preview
@Composable
fun PreviewFlowerCard() {
    PreviewCard(
        GameCard(
            id = 5,
            name = "Spring Flower",
            type = FlourishType.FLOWER,
            resilience = 1,
            cost = Cost(emptyList()),
            primaryEffect = CardEffect.ADORN,
            primaryValue = 1,
            matchWith = MatchWith.None,
            matchEffect = null,
            matchValue = 0,
            trashEffect = CardEffect.RESILIENCE_BOOST,
            trashValue = 5,
            thorn = 0
        )
    )
}

@Preview
@Composable
fun PreviewBloomCard() {
    PreviewCard(
        GameCard(
            id = 6,
            name = "Spring Bloom",
            type = FlourishType.BLOOM,
            resilience = 1,
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
@Preview
@Composable
fun PreviewCardWithAllEffects() {
    PreviewCard(
        GameCard(
            id = 7,
            name = "Powerful Card",
            type = FlourishType.VINE,
            resilience = 3,
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

@Preview
@Composable
fun PreviewCardWithNoEffects() {
    PreviewCard(
        GameCard(
            id = 8,
            name = "Basic Card",
            type = FlourishType.ROOT,
            resilience = 2,
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

@Preview
@Composable
fun PreviewCardWithOnlyPrimary() {
    PreviewCard(
        GameCard(
            id = 9,
            name = "Primary Only",
            type = FlourishType.CANOPY,
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
    )
} 
