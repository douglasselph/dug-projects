package dugsolutions.leaf.integration.v35.support

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.game.Game
import dugsolutions.leaf.v35.game.GameStatus
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.round.domain.RoundCard
import dugsolutions.leaf.v35.round.domain.RoundCardType

/**
 * Deep, assertion-friendly snapshot of the durable state owned by one v35 [Game].
 *
 * The snapshot lives only in the integration source set. Every collection and
 * mutable engine object is copied into immutable values, so a test can retain
 * a `before` snapshot, execute real production code, and compare it with an
 * `after` snapshot safely.
 */
data class GameSnapshot(
    val status: GameStatus,
    val roundNumber: Int,
    val currentRound: RoundCardSnapshot?,
    val roundCardsRemaining: Int,
    val roundDrawPile: List<RoundCardSnapshot>,
    val players: Map<PlayerId, PlayerSnapshot>,
    val grove: GroveSnapshot
) {
    val currentRoundName: String?
        get() = currentRound?.name

    fun player(id: PlayerId): PlayerSnapshot =
        requireNotNull(players[id]) {
            "No player ${id.value} in snapshot"
        }

    fun player(id: Int): PlayerSnapshot =
        player(PlayerId(id))

    companion object {
        fun capture(game: Game): GameSnapshot =
            GameSnapshot(
                status = game.status,
                roundNumber = game.roundNumber,
                currentRound = game.currentRound?.let(RoundCardSnapshot::capture),
                roundCardsRemaining = game.roundDeck.remaining,
                roundDrawPile = immutableList(
                    game.roundDeck.cards.cards.map(RoundCardSnapshot::capture)
                ),
                players = immutableMap(
                    game.players.associate { player ->
                        player.id to PlayerSnapshot.capture(player)
                    }
                ),
                grove = GroveSnapshot.capture(game)
            )
    }
}

data class RoundCardSnapshot(
    val name: String,
    val type: RoundCardType,
    val firstEffectTitle: String,
    val firstEffect: GameEffect,
    val secondEffectTitle: String,
    val secondEffect: GameEffect
) {
    companion object {
        fun capture(card: RoundCard): RoundCardSnapshot =
            RoundCardSnapshot(
                name = card.name,
                type = card.type,
                firstEffectTitle = card.firstEffect.title,
                firstEffect = card.firstEffect.effect,
                secondEffectTitle = card.secondEffect.title,
                secondEffect = card.secondEffect.effect
            )
    }
}
