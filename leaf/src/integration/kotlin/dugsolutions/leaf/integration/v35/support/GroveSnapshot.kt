package dugsolutions.leaf.integration.v35.support

import dugsolutions.leaf.v35.game.Game
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.random.die.DieSides
import dugsolutions.leaf.v35.tokens.Critter

data class PlantStackSnapshot(
    val name: String,
    val title: String,
    val type: PlantType,
    val cost: Int,
    val remaining: Int
)

data class GroveSnapshot(
    val plantStacks: List<PlantStackSnapshot>,
    val graftBed: Map<DieSides, Int>,
    val critters: Map<Critter, Int>,
    val water: Int,
    val mulchTokens: List<MulchSnapshot>,
    val butterflies: List<ButterflySnapshot>,
    val wispCardsRemaining: Int,
    val wispDrawPile: List<WispSnapshot>
) {
    val bees: Int
        get() = critters.getValue(Critter.BEE)

    val worms: Int
        get() = critters.getValue(Critter.WORM)

    val mulch: Int
        get() = mulchTokens.size

    companion object {
        fun capture(game: Game): GroveSnapshot =
            GroveSnapshot(
                plantStacks = immutableList(
                    game.grove.plantMarket.stacks.map { stack ->
                        PlantStackSnapshot(
                            name = stack.card.name,
                            title = stack.card.title,
                            type = stack.card.type,
                            cost = stack.card.cost,
                            remaining = stack.remaining
                        )
                    }
                ),
                graftBed = immutableMap(game.grove.graftBed.counts),
                critters = immutableMap(
                    Critter.entries.associateWith(game.grove.critters::count)
                ),
                water = game.grove.tokens.waterCount,
                mulchTokens = immutableList(
                    game.grove.tokens.mulchTokens.map { token ->
                        MulchSnapshot(token.sides)
                    }
                ),
                butterflies = immutableList(
                    game.grove.butterflies.all.map { butterfly ->
                        ButterflySnapshot(
                            butterfly = butterfly,
                            faceUp = game.grove.butterflies.isFaceUp(butterfly)
                        )
                    }
                ),
                wispCardsRemaining = game.grove.wispDeck.remaining,
                wispDrawPile = immutableList(
                    game.grove.wispDeck.cards.cards.map(WispSnapshot::capture)
                )
            )
    }
}
