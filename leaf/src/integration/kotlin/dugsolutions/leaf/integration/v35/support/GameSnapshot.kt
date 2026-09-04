package dugsolutions.leaf.integration.v35.support

import dugsolutions.leaf.v35.game.Game
import dugsolutions.leaf.v35.game.GameStatus
import dugsolutions.leaf.v35.player.Player
import dugsolutions.leaf.v35.player.creature.CreatureCard
import dugsolutions.leaf.v35.random.die.Die
import dugsolutions.leaf.v35.random.die.DieSides
import dugsolutions.leaf.v35.tokens.Butterfly
import dugsolutions.leaf.v35.tokens.Critter

/** Immutable, assertion-friendly view of a complete v35 Game. */
data class GameSnapshot(
    val status: GameStatus,
    val roundNumber: Int,
    val currentRoundName: String?,
    val roundCardsRemaining: Int,
    val roundDrawPile: List<String>,
    val players: List<PlayerSnapshot>,
    val grove: GroveSnapshot
) {
    fun player(id: Int): PlayerSnapshot =
        requireNotNull(players.firstOrNull { it.id == id }) {
            "No player $id in snapshot"
        }

    companion object {
        fun capture(game: Game): GameSnapshot =
            GameSnapshot(
                status = game.status,
                roundNumber = game.roundNumber,
                currentRoundName = game.currentRound?.name,
                roundCardsRemaining = game.roundDeck.remaining,
                roundDrawPile = game.roundDeck.cards.cards.map { it.name },
                players = game.players.map(PlayerSnapshot::capture),
                grove = GroveSnapshot.capture(game)
            )
    }
}

data class DieSnapshot(
    val sides: Int,
    val value: Int
) {
    companion object {
        fun capture(die: Die): DieSnapshot =
            DieSnapshot(die.sides, die.value)
    }
}

data class CreatureCardSnapshot(
    val id: Int,
    val name: String,
    val title: String,
    val type: String,
    val side: String,
    val x: Int,
    val y: Int,
    val faceUp: Boolean
) {
    companion object {
        fun capture(card: CreatureCard): CreatureCardSnapshot =
            CreatureCardSnapshot(
                id = card.id.value,
                name = card.card.name,
                title = card.card.title,
                type = card.card.type.name,
                side = card.side.name,
                x = card.position.x,
                y = card.position.y,
                faceUp = card.isFaceUp
            )
    }
}

data class ButterflySnapshot(
    val butterfly: Butterfly,
    val faceUp: Boolean
)

data class PlayerSnapshot(
    val id: Int,
    val vp: Int,
    val supply: List<DieSnapshot>,
    val hand: List<DieSnapshot>,
    val discard: List<DieSnapshot>,
    val critters: Map<Critter, Int>,
    val critterValues: Map<Critter, Int>,
    val water: Int,
    val mulch: Int,
    val pendingMulch: Int,
    val butterflies: List<ButterflySnapshot>,
    val wisps: List<String>,
    val creature: List<CreatureCardSnapshot>
) {
    companion object {
        fun capture(player: Player): PlayerSnapshot =
            PlayerSnapshot(
                id = player.id.value,
                vp = player.vp,
                supply = player.dice.supply.map(DieSnapshot::capture),
                hand = player.dice.hand.map(DieSnapshot::capture),
                discard = player.dice.discard.map(DieSnapshot::capture),
                critters = Critter.entries.associateWith(player.critters::count),
                critterValues = Critter.entries.associateWith(player.critterValues::valueOf),
                water = player.tokens.waterCount,
                mulch = player.tokens.mulchCount,
                pendingMulch = player.tokens.pendingMulchCount,
                butterflies = player.butterflies.all.map { butterfly ->
                    ButterflySnapshot(
                        butterfly = butterfly,
                        faceUp = player.butterflies.isFaceUp(butterfly)
                    )
                },
                wisps = player.wisps.cards.cards.map { it.name },
                creature = player.creature.cards.map(CreatureCardSnapshot::capture)
            )
    }
}

data class PlantStackSnapshot(
    val name: String,
    val title: String,
    val remaining: Int
)

data class GroveSnapshot(
    val plantStacks: List<PlantStackSnapshot>,
    val graftBed: Map<DieSides, Int>,
    val critters: Map<Critter, Int>,
    val water: Int,
    val mulch: Int,
    val butterflies: List<Butterfly>,
    val wispCardsRemaining: Int,
    val wispDrawPile: List<String>
) {
    companion object {
        fun capture(game: Game): GroveSnapshot =
            GroveSnapshot(
                plantStacks = game.grove.plantMarket.stacks.map { stack ->
                    PlantStackSnapshot(
                        name = stack.card.name,
                        title = stack.card.title,
                        remaining = stack.remaining
                    )
                },
                graftBed = game.grove.graftBed.counts,
                critters = Critter.entries.associateWith(game.grove.critters::count),
                water = game.grove.tokens.waterCount,
                mulch = game.grove.tokens.mulchCount,
                butterflies = game.grove.butterflies.all,
                wispCardsRemaining = game.grove.wispDeck.remaining,
                wispDrawPile = game.grove.wispDeck.cards.cards.map { it.name }
            )
    }
}
