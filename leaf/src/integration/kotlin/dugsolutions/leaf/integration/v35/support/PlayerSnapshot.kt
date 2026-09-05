package dugsolutions.leaf.integration.v35.support

import dugsolutions.leaf.v35.plant.domain.PlantScoringRule
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.Player
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.creature.CreatureCard
import dugsolutions.leaf.v35.player.creature.CreatureSide
import dugsolutions.leaf.v35.random.die.Die
import dugsolutions.leaf.v35.random.die.DieSides
import dugsolutions.leaf.v35.tokens.Butterfly
import dugsolutions.leaf.v35.tokens.Critter
import dugsolutions.leaf.v35.wisp.domain.WispCard

data class DieSnapshot(
    val sides: Int,
    val value: Int
) {
    val dieSides: DieSides
        get() = DieSides.from(sides)

    companion object {
        fun capture(die: Die): DieSnapshot =
            DieSnapshot(
                sides = die.sides,
                value = die.value
            )
    }
}

data class PlantSnapshot(
    val id: Int,
    val name: String,
    val title: String,
    val type: PlantType,
    val cost: Int,
    val scoringRule: PlantScoringRule,
    val side: CreatureSide,
    val x: Int,
    val y: Int,
    val faceUp: Boolean
) {
    companion object {
        fun capture(card: CreatureCard): PlantSnapshot =
            PlantSnapshot(
                id = card.id.value,
                name = card.card.name,
                title = card.card.title,
                type = card.card.type,
                cost = card.card.cost,
                scoringRule = card.card.scoringRule,
                side = card.side,
                x = card.position.x,
                y = card.position.y,
                faceUp = card.isFaceUp
            )
    }
}

/** Compatibility name retained for the first harness revision. */
typealias CreatureCardSnapshot = PlantSnapshot

data class ButterflySnapshot(
    val butterfly: Butterfly,
    val faceUp: Boolean
)

data class WispSnapshot(
    val name: String,
    val title: String,
    val endGameVp: Int,
    val playImmediately: Boolean,
    val battleOnly: Boolean
) {
    companion object {
        fun capture(card: WispCard): WispSnapshot =
            WispSnapshot(
                name = card.name,
                title = card.title,
                endGameVp = card.endGameVp,
                playImmediately = card.playImmediately,
                battleOnly = card.battleOnly
            )
    }
}

data class MulchSnapshot(
    /** Null means an empty Mulch token. */
    val storedDieSides: DieSides?
)

data class PlayerSnapshot(
    val id: PlayerId,
    val vp: Int,
    val supply: List<DieSnapshot>,
    val hand: List<DieSnapshot>,
    val discard: List<DieSnapshot>,
    val critters: Map<Critter, Int>,
    val critterValues: Map<Critter, Int>,
    val water: Int,
    val mulchTokens: List<MulchSnapshot>,
    val pendingMulchTokens: List<MulchSnapshot>,
    val butterflies: List<ButterflySnapshot>,
    val wispCards: List<WispSnapshot>,
    val plants: List<PlantSnapshot>
) {
    val idValue: Int
        get() = id.value

    val bees: Int
        get() = critters.getValue(Critter.BEE)

    val worms: Int
        get() = critters.getValue(Critter.WORM)

    val beeValue: Int
        get() = critterValues.getValue(Critter.BEE)

    val wormValue: Int
        get() = critterValues.getValue(Critter.WORM)

    val mulch: Int
        get() = mulchTokens.size

    val pendingMulch: Int
        get() = pendingMulchTokens.size

    /** Stable card names are usually the most convenient Wisp assertion. */
    val wisps: List<String>
        get() = immutableList(wispCards.map { it.name })

    /** Convenience name for grafted Plant cards. */
    val creature: List<PlantSnapshot>
        get() = plants

    companion object {
        fun capture(player: Player): PlayerSnapshot =
            PlayerSnapshot(
                id = player.id,
                vp = player.vp,
                supply = immutableList(player.dice.supply.map(DieSnapshot::capture)),
                hand = immutableList(player.dice.hand.map(DieSnapshot::capture)),
                discard = immutableList(player.dice.discard.map(DieSnapshot::capture)),
                critters = immutableMap(
                    Critter.entries.associateWith(player.critters::count)
                ),
                critterValues = immutableMap(
                    Critter.entries.associateWith(player.critterValues::valueOf)
                ),
                water = player.tokens.waterCount,
                mulchTokens = immutableList(
                    player.tokens.mulchTokens.map { token ->
                        MulchSnapshot(token.sides)
                    }
                ),
                pendingMulchTokens = immutableList(
                    player.tokens.pendingMulchTokens.map { token ->
                        MulchSnapshot(token.sides)
                    }
                ),
                butterflies = immutableList(
                    player.butterflies.all.map { butterfly ->
                        ButterflySnapshot(
                            butterfly = butterfly,
                            faceUp = player.butterflies.isFaceUp(butterfly)
                        )
                    }
                ),
                wispCards = immutableList(
                    player.wisps.cards.cards.map(WispSnapshot::capture)
                ),
                plants = immutableList(
                    player.creature.cards.map(PlantSnapshot::capture)
                )
            )
    }
}
