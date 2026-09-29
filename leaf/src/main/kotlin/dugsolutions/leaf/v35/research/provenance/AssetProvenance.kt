package dugsolutions.leaf.v35.research.provenance

import dugsolutions.leaf.v35.battle.StrikeContributionLedger
import dugsolutions.leaf.v35.battle.StrikeContributionSource
import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.Player
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.random.die.Die
import java.util.IdentityHashMap

data class ForgetMeNotProvenance(
    val assetId: Long,
    val playerId: PlayerId,
    val sourceCard: String,
    val dieSides: Int,
    val estimatedNaturalDrawsUntilAvailable: Int,
    val estimatedDrawsAccelerated: Int,
    var reachedNextBattle: Boolean = false,
    var placedInBattle: Boolean = false,
    var battleRow: StrikeRow? = null,
    var contributedToWinningStrike: Boolean = false,
    var individuallyWinnerDecisive: Boolean = false,
    var individuallyWoundDecisive: Boolean = false,
    var associatedBattleVp: Int = 0
)

data class ImmediateDieEffectProvenance(
    val assetId: Long,
    val playerId: PlayerId,
    val sourceCard: String,
    val effect: GameEffect,
    val dieSides: Int,
    val before: Int,
    val after: Int,
    val magnitude: Int,
    var placedInBattle: Boolean = false,
    var battleRow: StrikeRow? = null,
    var contributedToWinningStrike: Boolean = false,
    var individuallyWinnerDecisive: Boolean = false,
    var individuallyWoundDecisive: Boolean = false,
    var associatedBattleVp: Int = 0
)

/**
 * Small game-local research ledger for deliberately selected asset lineages.
 * It is not a general causal graph and has no effect on game decisions or RNG.
 */
class AssetProvenance {
    private val ids = IdentityHashMap<Die, Long>()
    private var nextId = 1L
    private fun id(die: Die): Long = ids.getOrPut(die) { nextId++ }

    private val _forgetMeNot = mutableListOf<ForgetMeNotProvenance>()
    val forgetMeNot: List<ForgetMeNotProvenance> get() = _forgetMeNot.toList()

    private val _immediateDieEffects = mutableListOf<ImmediateDieEffectProvenance>()
    val immediateDieEffects: List<ImmediateDieEffectProvenance> get() = _immediateDieEffects.toList()

    fun assetId(die: Die): Long? = ids[die]

    fun recordForgetMeNot(player: Player, die: Die, sourceCard: String) {
        // Without intervention, every remaining Supply die is drawn before Discard
        // recycles; within Discard, lower-sided dice (and earlier equal-sided dice)
        // are drawn first. This is intentionally approximate and player-visible.
        val discardIndex = player.dice.discard.indexOfFirst { it === die }
        val aheadInDiscard = player.dice.discard.withIndex().count { (index, candidate) ->
            candidate !== die && (candidate.sides < die.sides ||
                (candidate.sides == die.sides && index < discardIndex))
        }
        val naturalDraws = player.dice.supply.size + aheadInDiscard + 1
        _forgetMeNot += ForgetMeNotProvenance(
            assetId = id(die),
            playerId = player.id,
            sourceCard = sourceCard,
            dieSides = die.sides,
            estimatedNaturalDrawsUntilAvailable = naturalDraws,
            estimatedDrawsAccelerated = naturalDraws
        )
    }

    fun recordImmediateDieEffect(
        player: Player,
        die: Die,
        sourceCard: String,
        effect: GameEffect,
        before: Int,
        after: Int
    ) {
        _immediateDieEffects += ImmediateDieEffectProvenance(
            assetId = id(die), playerId = player.id, sourceCard = sourceCard,
            effect = effect, dieSides = die.sides, before = before, after = after,
            magnitude = after - before
        )
    }

    fun markBattleReached(player: Player) {
        val handIds = player.dice.hand.mapNotNull { ids[it] }.toSet()
        _forgetMeNot.filter { it.playerId == player.id && it.assetId in handIds }
            .forEach { it.reachedNextBattle = true }
    }

    fun markBattlePlacement(playerId: PlayerId, die: Die, row: StrikeRow) {
        val assetId = ids[die] ?: return
        _forgetMeNot.filter { it.playerId == playerId && it.assetId == assetId }.forEach {
            it.reachedNextBattle = true; it.placedInBattle = true; it.battleRow = row
        }
        _immediateDieEffects.filter { it.playerId == playerId && it.assetId == assetId }.forEach {
            it.placedInBattle = true; it.battleRow = row
        }
    }

    fun observeStrike(ledger: StrikeContributionLedger) {
        ledger.contributions.forEach { contribution ->
            val assetId = (contribution.source as? StrikeContributionSource.Die)?.assetId ?: return@forEach
            _forgetMeNot.filter { it.playerId == contribution.playerId && it.assetId == assetId }.forEach {
                it.contributedToWinningStrike = contribution.associatedBattleVp > 0 && contribution.contributesValue
                it.individuallyWinnerDecisive = contribution.individuallyWinnerDecisive
                it.individuallyWoundDecisive = contribution.individuallyWoundDecisive
                it.associatedBattleVp = contribution.associatedBattleVp
            }
            _immediateDieEffects.filter { it.playerId == contribution.playerId && it.assetId == assetId }.forEach {
                it.contributedToWinningStrike = contribution.associatedBattleVp > 0 && contribution.contributesValue
                it.individuallyWinnerDecisive = contribution.individuallyWinnerDecisive
                it.individuallyWoundDecisive = contribution.individuallyWoundDecisive
                it.associatedBattleVp = contribution.associatedBattleVp
            }
        }
    }
}
