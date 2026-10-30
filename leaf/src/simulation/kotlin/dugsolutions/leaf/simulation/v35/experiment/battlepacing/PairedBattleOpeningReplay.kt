package dugsolutions.leaf.simulation.v35.experiment.battlepacing

import dugsolutions.leaf.v35.battle.BattleState
import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.game.Game
import dugsolutions.leaf.v35.game.GameConfig
import dugsolutions.leaf.v35.game.di.GameFactory
import dugsolutions.leaf.v35.game.round.RoundCoordinator
import dugsolutions.leaf.v35.game.round.battle.BattleRound
import dugsolutions.leaf.v35.round.domain.RoundCardType
import dugsolutions.leaf.v35.tokens.Critter
import java.security.MessageDigest

/** Immutable human-readable snapshot. No live Game/Die references are retained. */
data class BattleOpeningFingerprint(val lines: List<String>) {
    val sha256: String = MessageDigest.getInstance("SHA-256")
        .digest(lines.joinToString("\n").toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

    companion object {
        /** Covers material pre-engagement state, including draws, setup, resources, deck order and grid. */
        fun capture(game: Game, battle: BattleState): BattleOpeningFingerprint {
            val lines = buildList {
                add("round=${game.roundNumber}")
                val card = requireNotNull(game.currentRound) { "Battle card must have been revealed" }
                require(card.type == RoundCardType.BATTLE)
                add("roundCard=${card.name}|${game.config.roundValues.cardFor(card).firstEffect.effect}|${game.config.roundValues.cardFor(card).secondEffect.effect}")
                add("futureRounds=${game.roundDeck.cards.cards.joinToString("|") { it.name }}")
                add("physicalSeats=${game.players.joinToString(",") { it.id.value.toString() }}")
                add("gridColumns=${battle.playerIdsInBattleOrder.joinToString(",") { it.value.toString() }}")
                add("remainingWisps=${game.grove.wispDeck.cards.cards.joinToString("|") { it.name }}")
                add("market=${game.grove.plantMarket.stacks.joinToString("|") { "${it.card.name}:${it.remaining}" }}")
                add("graftBed=${game.grove.graftBed.counts.entries.joinToString("|") { "${it.key}:${it.value}" }}")
                add("groveCritters=${Critter.entries.joinToString("|") { "${it.name}:${game.grove.critters.count(it)}" }}")
                add("groveTokens=water:${game.grove.tokens.waterCount},sunlight:${game.grove.tokens.sunlightCount},mulch:${game.grove.tokens.mulchCount}")
                add("groveButterflies=${game.grove.butterflies.all.joinToString("|") { "${it.name}:${game.grove.butterflies.isFaceUp(it)}" }}")
                game.players.forEach { player ->
                    val p = "player=${player.id.value}"
                    add("$p;vp=${player.vp};critters=${Critter.entries.joinToString("|") { "${it.name}:${player.critters.count(it)}:${player.critterValues.valueOf(it)}" }}")
                    add("$p;tokens=${player.tokens.waterCount}:${player.tokens.sunlightCount}:${player.tokens.mulchTokens.joinToString(",") { it.sides.toString() }}:${player.tokens.pendingMulchTokens.joinToString(",") { it.sides.toString() }}")
                    add("$p;butterflies=${player.butterflies.all.joinToString("|") { "${it.name}:${player.butterflies.isFaceUp(it)}" }}")
                    add("$p;wisps=${player.wisps.cards.cards.joinToString("|") { it.name }}")
                    add("$p;plants=${player.creature.cards.joinToString("|") { "${it.id.value}:${it.card.name}:${it.side}:${it.position}:${it.facing}" }}")
                    add("$p;supply=${player.dice.supply.joinToString("|") { "${it.sides}:${it.value}" }}")
                    add("$p;hand=${player.dice.hand.joinToString("|") { "${it.sides}:${it.value}" }}")
                    add("$p;discard=${player.dice.discard.joinToString("|") { "${it.sides}:${it.value}" }}")
                    StrikeRow.entries.forEach { row ->
                        val square = battle.grid.square(player.id, row)
                        require(square.dice.all { die -> player.dice.hand.any { it === die } }) {
                            "Placed dice must be exact live Hand dice"
                        }
                        add("$p;row=$row;dice=${square.dice.joinToString("|") { "${it.sides}:${it.value}" }};critters=${square.critters.joinToString("|") { it.name }};withdrawn=${battle.grid.isPlayerWithdrawn(player.id, row)};closed=${battle.grid.isRowClosed(row)}")
                    }
                }
            }
            return BattleOpeningFingerprint(lines)
        }
    }
}

/** Two genuinely independent legal pre-Battle graphs, retained for Checkpoint 2. */
data class PairedBattleOpening(
    val controlGame: Game,
    val controlBattle: BattleState,
    val candidateGame: Game,
    val candidateBattle: BattleState,
    val fingerprint: BattleOpeningFingerprint,
    val evidence: List<String>
)

/**
 * Replays the same seed, policy and 3/2/2 (or supplied) Round Deck from setup
 * through real canonical prefix rounds and Battle Draw+Rank+Place.
 *
 * Until Checkpoint 2 BOTH branches run the identical canonical prefix. No
 * variant Battle action scheduler is installed, and NO A/B result is asserted.
 * Differing hidden RNG cursor state is not proven by this state fingerprint;
 * replay divergence after actions must be diagnosed separately.
 */
class PairedBattleOpeningReplay(
    private val gameFactory: GameFactory,
    private val roundCoordinator: RoundCoordinator,
    private val battleRound: BattleRound
) {
    fun create(config: GameConfig, battleOrdinal: Int = 1): PairedBattleOpening {
        require(config.seed != null) { "Paired replay requires an explicit mechanical seed" }
        require(config.strategySeed != null) { "Paired replay requires an explicit strategy seed" }
        require(battleOrdinal in 1..config.roundSetup.battleRounds) {
            "Requested Battle ordinal $battleOrdinal not in game"
        }
        fun one(): Pair<Game, BattleState> {
            val game = gameFactory(config)
            var battleCount = 0
            while (true) {
                val reveal = checkNotNull(roundCoordinator.revealNext(game)) {
                    "Round Deck ended before Battle $battleOrdinal"
                }
                if (reveal.card.type == RoundCardType.BATTLE) {
                    battleCount++
                    if (battleCount == battleOrdinal) {
                        battleRound.executeOpeningDraw(game)
                        val state = battleRound.executeRankAndPlace(game).battleState
                        return game to state
                    }
                }
                roundCoordinator.executeRevealed(game, reveal)
            }
        }
        val (aGame, aBattle) = one()
        val (bGame, bBattle) = one()
        val a = BattleOpeningFingerprint.capture(aGame, aBattle)
        val b = BattleOpeningFingerprint.capture(bGame, bBattle)
        check(a == b) {
            val mismatch = a.lines.zip(b.lines).indexOfFirst { it.first != it.second }
            "Paired pre-Battle state differs: firstLine=$mismatch, " +
                "control=${a.lines.getOrNull(mismatch)}, candidate=${b.lines.getOrNull(mismatch)}"
        }
        val openingEvent = "PAIRED_OPENING,seed=${config.seed},battle=$battleOrdinal,round=${aGame.roundNumber}," +
            "players=${aGame.players.size},sha256=${a.sha256},matched=true"
        val evidence = listOf(openingEvent) + StrikeRow.entries.flatMap { row ->
            EngagementOrderSelector.select(bBattle, bGame.players.map { it.id }, row).traceLines()
        }
        return PairedBattleOpening(aGame, aBattle, bGame, bBattle, a, evidence)
    }
}
