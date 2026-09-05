package dugsolutions.leaf.v35.player.decision.context

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.game.GameStatus
import dugsolutions.leaf.v35.plant.domain.PlantScoringRule
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.creature.CreatureCard
import dugsolutions.leaf.v35.player.creature.CreatureCardId
import dugsolutions.leaf.v35.player.creature.CreaturePosition
import dugsolutions.leaf.v35.player.creature.CreatureSide
import dugsolutions.leaf.v35.random.die.DieSides
import dugsolutions.leaf.v35.round.domain.RoundCardType
import dugsolutions.leaf.v35.tokens.Butterfly
import dugsolutions.leaf.v35.tokens.Critter

/**
 * Immutable, player-facing observation supplied with every production decision.
 *
 * Strategies never receive Game, Player, Grove, BattleState, Die, or any other
 * mutable gameplay aggregate. The rules engine remains responsible for legal
 * choices and mutation; a strategy observes this snapshot and chooses among the
 * legal choices carried by its request.
 */
data class DecisionContext(
    val status: GameStatus,
    val phase: RoundCardType?,
    val progress: GameProgressView,
    val round: RoundView?,
    val self: SelfPlayerView,
    val opponents: List<OpponentView>,
    val grove: GroveView,
    val battle: BattleView?
) {
    companion object {
        /**
         * Compatibility context for isolated legacy unit tests that construct
         * decision requests directly. Production gameplay always supplies a
         * real snapshot through DecisionContextFactory.
         */
        val EMPTY = DecisionContext(
            status = GameStatus.READY,
            phase = null,
            progress = GameProgressView.EMPTY,
            round = null,
            self = SelfPlayerView.EMPTY,
            opponents = emptyList(),
            grove = GroveView.EMPTY,
            battle = null
        )
    }
}

data class GameProgressView(
    val roundNumber: Int,
    val roundsCompleted: Int,
    val totalRounds: Int,
    val roundsRemainingToReveal: Int,
    val cultivationRoundsCompleted: Int,
    val battleRoundsCompleted: Int,
    val totalCultivationRounds: Int?,
    val totalBattleRounds: Int?,
    val currentCultivationRoundNumber: Int?,
    val currentBattleRoundNumber: Int?,
    val cultivationRoundsRemaining: Int?,
    val battleRoundsRemaining: Int?,
    val isFinalRound: Boolean,
    val isFinalCultivationRound: Boolean,
    val isFinalBattleRound: Boolean
) {
    companion object {
        val EMPTY = GameProgressView(
            roundNumber = 0,
            roundsCompleted = 0,
            totalRounds = 0,
            roundsRemainingToReveal = 0,
            cultivationRoundsCompleted = 0,
            battleRoundsCompleted = 0,
            totalCultivationRounds = null,
            totalBattleRounds = null,
            currentCultivationRoundNumber = null,
            currentBattleRoundNumber = null,
            cultivationRoundsRemaining = null,
            battleRoundsRemaining = null,
            isFinalRound = false,
            isFinalCultivationRound = false,
            isFinalBattleRound = false
        )
    }
}

data class RoundView(
    val number: Int,
    val name: String,
    val type: RoundCardType,
    val firstEffect: GameEffect,
    val secondEffect: GameEffect
)

data class DieView(
    val index: Int,
    val sides: Int,
    val value: Int
) {
    init {
        require(index >= 0) { "Die view index cannot be negative: $index" }
        require(sides > 0) { "Die view sides must be positive: $sides" }
        require(value > 0) { "Die view value must be positive: $value" }
    }
}

data class CreatureCardView(
    val id: CreatureCardId,
    val name: String,
    val title: String,
    val type: PlantType,
    val cost: Int,
    val effect: GameEffect,
    val scoringRule: PlantScoringRule,
    val side: CreatureSide,
    val position: CreaturePosition,
    val facing: CreatureCard.Facing,
    val isSnippable: Boolean
) {
    val isFaceUp: Boolean get() = facing == CreatureCard.Facing.FACE_UP
    val isFaceDown: Boolean get() = facing == CreatureCard.Facing.FACE_DOWN
}

data class ButterflyView(
    val butterfly: Butterfly,
    val isFaceUp: Boolean
)

data class MulchView(
    val index: Int,
    val storedDieSides: DieSides?,
    val pending: Boolean
)

data class WispView(
    val index: Int,
    val name: String,
    val title: String,
    val effect: GameEffect,
    val playImmediately: Boolean,
    val battleOnly: Boolean,
    val endGameVp: Int
)

/** Public board information common to the acting player and opponents. */
data class PlayerBoardView(
    val id: PlayerId,
    val vp: Int,
    val supply: List<DieView>,
    val hand: List<DieView>,
    val discard: List<DieView>,
    val bees: Int,
    val worms: Int,
    val beeValue: Int,
    val wormValue: Int,
    val water: Int,
    val mulch: List<MulchView>,
    val pendingMulch: List<MulchView>,
    val butterflies: List<ButterflyView>,
    val creature: List<CreatureCardView>
) {
    val dicePower: Int
        get() = (supply + hand + discard).sumOf { it.sides } +
            mulch.mapNotNull { it.storedDieSides }.sumOf { it.value } +
            pendingMulch.mapNotNull { it.storedDieSides }.sumOf { it.value }

    val plantCount: Int get() = creature.size
}

data class SelfPlayerView(
    val board: PlayerBoardView,
    val wisps: List<WispView>
) {
    val id: PlayerId get() = board.id
    val wispCount: Int get() = wisps.size

    companion object {
        val EMPTY = SelfPlayerView(
            board = PlayerBoardView(
                id = PlayerId(0),
                vp = 0,
                supply = emptyList(),
                hand = emptyList(),
                discard = emptyList(),
                bees = 0,
                worms = 0,
                beeValue = Critter.BEE.baseValue,
                wormValue = Critter.WORM.baseValue,
                water = 0,
                mulch = emptyList(),
                pendingMulch = emptyList(),
                butterflies = emptyList(),
                creature = emptyList()
            ),
            wisps = emptyList()
        )
    }
}

/** Opponent Wisp identities stay hidden; only the public hand count is exposed. */
data class OpponentView(
    val board: PlayerBoardView,
    val wispCount: Int
) {
    val id: PlayerId get() = board.id
}

data class PlantStackView(
    val name: String,
    val title: String,
    val type: PlantType,
    val cost: Int,
    val effect: GameEffect,
    val scoringRule: PlantScoringRule,
    val remaining: Int
)

data class GroveView(
    val plantStacks: List<PlantStackView>,
    val graftBed: Map<DieSides, Int>,
    val bees: Int,
    val worms: Int,
    val water: Int,
    val mulch: Int,
    val butterflies: List<Butterfly>,
    val wispDeckRemaining: Int
) {
    companion object {
        val EMPTY = GroveView(
            plantStacks = emptyList(),
            graftBed = emptyMap(),
            bees = 0,
            worms = 0,
            water = 0,
            mulch = 0,
            butterflies = emptyList(),
            wispDeckRemaining = 0
        )
    }
}

data class BattleDieView(
    val handIndex: Int,
    val sides: Int,
    val value: Int
)

data class BattlePlayerRowView(
    val playerId: PlayerId,
    val row: StrikeRow,
    val dice: List<BattleDieView>,
    val critters: List<Critter>,
    val dieTotal: Int,
    val critterTotal: Int,
    val total: Int,
    val withdrawn: Boolean
)

data class BattleRowView(
    val row: StrikeRow,
    val closed: Boolean,
    val players: List<BattlePlayerRowView>
) {
    fun forPlayer(playerId: PlayerId): BattlePlayerRowView? =
        players.firstOrNull { it.playerId == playerId }
}

data class BattleView(
    val playerOrder: List<PlayerId>,
    val rows: List<BattleRowView>
) {
    fun row(row: StrikeRow): BattleRowView =
        requireNotNull(rows.firstOrNull { it.row == row }) {
            "Battle view does not contain row $row"
        }
}
