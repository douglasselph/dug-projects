package dugsolutions.leaf.v35.research.provenance

import dugsolutions.leaf.v35.battle.StrikeContributionLedger
import dugsolutions.leaf.v35.battle.StrikeContributionSource
import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.GameEffectPhase
import dugsolutions.leaf.v35.effect.GameEffectSource
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
    val sunlightFunded: Boolean = false,
    /** Round-effect lineage that ultimately funded this immediate change, when known. */
    val roundEffectLineageId: Long? = null,
    var placedInBattle: Boolean = false,
    var observedInResolvedStrike: Boolean = false,
    var contributedToResolvedStrike: Boolean = false,
    var battleRow: StrikeRow? = null,
    var contributedToWinningStrike: Boolean = false,
    var individuallyWinnerDecisive: Boolean = false,
    var individuallyWoundDecisive: Boolean = false,
    var associatedBattleVp: Int = 0
)

/**
 * Research-only lineage for the four Cultivation Round resources whose downstream
 * value we want to explain: Water, Mulch, Compost(use-now), and Sunlight.
 *
 * It is deliberately descriptive.  "winnerDecisive" means that the associated
 * die contribution was individually decisive in the existing Strike ledger; it
 * is not a claim that the entire game result was caused by this Round effect.
 */
data class RoundEffectConsequenceProvenance(
    val lineageId: Long,
    val playerId: PlayerId,
    val effect: GameEffect,
    val roundCardName: String,
    val roundSlot: String,
    val acquiredPhase: GameEffectPhase,
    val sourceDieSides: Int? = null,
    val sourceDieValue: Int? = null,
    var resultDieSides: Int? = null,
    var resultDieValue: Int? = null,
    var spentOrUsed: Boolean = false,
    var usePhase: GameEffectPhase? = null,
    var useKind: String? = null,
    var fundedMainAction: String? = null,
    var fundedPlantName: String? = null,
    var assetId: Long? = null,
    var waterRerollBefore: Int? = null,
    var waterRerollAfter: Int? = null,
    var waterRefreshPlants: Int = 0,
    var waterRefreshButterflies: Int = 0,
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
    private var sunlightFundingPlayerId: PlayerId? = null
    private var sunlightFundingRoundLineageId: Long? = null
    private var nextId = 1L
    private var nextRoundLineageId = 1L
    private fun id(die: Die): Long = ids.getOrPut(die) { nextId++ }

    private val _forgetMeNot = mutableListOf<ForgetMeNotProvenance>()
    val forgetMeNot: List<ForgetMeNotProvenance> get() = _forgetMeNot.toList()

    private val _immediateDieEffects = mutableListOf<ImmediateDieEffectProvenance>()
    val immediateDieEffects: List<ImmediateDieEffectProvenance> get() = _immediateDieEffects.toList()

    private val _roundEffectConsequences = mutableListOf<RoundEffectConsequenceProvenance>()
    val roundEffectConsequences: List<RoundEffectConsequenceProvenance> get() = _roundEffectConsequences.toList()

    /* Token identity is physically interchangeable.  These FIFO origin queues
       let research attribute a later spend to the oldest still-held gain without
       changing gameplay objects.  Null means the token was gained from a non-Round source. */
    private val waterOrigins = mutableMapOf<PlayerId, MutableList<Long?>>()
    private val sunlightOrigins = mutableMapOf<PlayerId, MutableList<Long?>>()
    private data class MulchOrigin(val sides: Int, val roundLineageId: Long?)
    private val mulchOrigins = mutableMapOf<PlayerId, MutableList<MulchOrigin>>()

    fun assetId(die: Die): Long? = ids[die]

    fun <T> withSunlightFunding(playerId: PlayerId, block: () -> T): T =
        withSunlightFunding(playerId, null, block)

    fun <T> withSunlightFunding(playerId: PlayerId, roundLineageId: Long?, block: () -> T): T {
        val previousPlayer = sunlightFundingPlayerId
        val previousLineage = sunlightFundingRoundLineageId
        sunlightFundingPlayerId = playerId
        sunlightFundingRoundLineageId = roundLineageId
        return try {
            block()
        } finally {
            sunlightFundingPlayerId = previousPlayer
            sunlightFundingRoundLineageId = previousLineage
        }
    }

    fun activeSunlightRoundLineage(playerId: PlayerId): Long? =
        sunlightFundingRoundLineageId.takeIf { sunlightFundingPlayerId == playerId }

    private fun newRoundLineage(
        player: Player,
        effect: GameEffect,
        source: GameEffectSource,
        phase: GameEffectPhase,
        sourceDieSides: Int? = null,
        sourceDieValue: Int? = null
    ): Long? {
        val round = source as? GameEffectSource.Round ?: return null
        val lineageId = nextRoundLineageId++
        _roundEffectConsequences += RoundEffectConsequenceProvenance(
            lineageId = lineageId,
            playerId = player.id,
            effect = effect,
            roundCardName = round.card.name,
            roundSlot = round.slot.name,
            acquiredPhase = phase,
            sourceDieSides = sourceDieSides,
            sourceDieValue = sourceDieValue
        )
        return lineageId
    }

    private fun roundLineage(lineageId: Long?): RoundEffectConsequenceProvenance? =
        lineageId?.let { id -> _roundEffectConsequences.firstOrNull { it.lineageId == id } }

    fun recordWaterGain(player: Player, source: GameEffectSource, phase: GameEffectPhase) {
        val lineageId = newRoundLineage(player, GameEffect.GAIN_WATER_TOKEN, source, phase)
        waterOrigins.getOrPut(player.id) { mutableListOf() }.add(lineageId)
    }

    fun consumeWater(playerId: PlayerId): Long? {
        val origins = waterOrigins[playerId] ?: return null
        return if (origins.isEmpty()) null else origins.removeAt(0)
    }

    fun markWaterReroll(
        lineageId: Long?, player: Player, die: Die, before: Int, after: Int, phase: GameEffectPhase
    ) {
        val lineage = roundLineage(lineageId) ?: return
        lineage.spentOrUsed = true
        lineage.usePhase = phase
        lineage.useKind = "WATER_REROLL"
        lineage.waterRerollBefore = before
        lineage.waterRerollAfter = after
        lineage.resultDieSides = die.sides
        lineage.resultDieValue = after
        lineage.assetId = id(die)
        _immediateDieEffects += ImmediateDieEffectProvenance(
            assetId = id(die), playerId = player.id, sourceCard = lineage.roundCardName,
            effect = GameEffect.GAIN_WATER_TOKEN, dieSides = die.sides,
            before = before, after = after, magnitude = after - before,
            roundEffectLineageId = lineage.lineageId
        )
    }

    fun markWaterRefresh(
        lineageId: Long?, phase: GameEffectPhase, refreshedPlants: Int, refreshedButterflies: Int
    ) {
        val lineage = roundLineage(lineageId) ?: return
        lineage.spentOrUsed = true
        lineage.usePhase = phase
        lineage.useKind = "WATER_REFRESH"
        lineage.waterRefreshPlants = refreshedPlants
        lineage.waterRefreshButterflies = refreshedButterflies
    }

    fun recordSunlightGain(player: Player, source: GameEffectSource, phase: GameEffectPhase) {
        val lineageId = newRoundLineage(player, GameEffect.GAIN_SUNLIGHT_TOKEN, source, phase)
        sunlightOrigins.getOrPut(player.id) { mutableListOf() }.add(lineageId)
    }

    fun consumeSunlight(playerId: PlayerId): Long? {
        val origins = sunlightOrigins[playerId] ?: return null
        val lineageId = if (origins.isEmpty()) null else origins.removeAt(0)
        roundLineage(lineageId)?.spentOrUsed = true
        roundLineage(lineageId)?.usePhase = GameEffectPhase.BATTLE
        return lineageId
    }

    fun markSunlightMainAction(lineageId: Long?, action: String, plantName: String? = null) {
        val lineage = roundLineage(lineageId) ?: return
        lineage.useKind = "SUNLIGHT_MAIN"
        lineage.fundedMainAction = action
        lineage.fundedPlantName = plantName
    }

    fun markSunlightFundedDraw(playerId: PlayerId, die: Die) {
        val lineage = roundLineage(activeSunlightRoundLineage(playerId)) ?: return
        lineage.assetId = id(die)
        lineage.resultDieSides = die.sides
        lineage.resultDieValue = die.value
    }

    fun recordMulchStored(
        player: Player, die: Die, source: GameEffectSource, phase: GameEffectPhase
    ) {
        val lineageId = newRoundLineage(
            player = player, effect = GameEffect.MULCH_DIE_FROM_HAND, source = source, phase = phase,
            sourceDieSides = die.sides, sourceDieValue = die.value
        )
        mulchOrigins.getOrPut(player.id) { mutableListOf() }
            .add(MulchOrigin(die.sides, lineageId))
    }

    fun consumeMulch(playerId: PlayerId, sides: Int): Long? {
        val origins = mulchOrigins[playerId] ?: return null
        val index = origins.indexOfFirst { it.sides == sides }
        if (index < 0) return null
        return origins.removeAt(index).roundLineageId
    }

    fun markMulchUsed(lineageId: Long?, die: Die, phase: GameEffectPhase) {
        val lineage = roundLineage(lineageId) ?: return
        lineage.spentOrUsed = true
        lineage.usePhase = phase
        lineage.useKind = if (phase == GameEffectPhase.BATTLE) "MULCH_BATTLE" else "MULCH_CULTIVATION"
        lineage.resultDieSides = die.sides
        lineage.resultDieValue = die.value
        lineage.assetId = id(die)
    }

    fun recordRoundCompostUseNow(
        player: Player,
        source: GameEffectSource,
        phase: GameEffectPhase,
        sourceDieSides: Int,
        sourceDieValue: Int,
        replacement: Die
    ) {
        val lineageId = newRoundLineage(
            player = player, effect = GameEffect.UPGRADE_DIE_AND_USE_NOW,
            source = source, phase = phase,
            sourceDieSides = sourceDieSides, sourceDieValue = sourceDieValue
        ) ?: return
        roundLineage(lineageId)?.apply {
            spentOrUsed = true
            usePhase = phase
            useKind = "COMPOST_USE_NOW"
            resultDieSides = replacement.sides
            resultDieValue = replacement.value
            assetId = id(replacement)
        }
    }

    fun recordForgetMeNot(player: Player, die: Die, sourceCard: String) {
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
            magnitude = after - before,
            sunlightFunded = sunlightFundingPlayerId == player.id,
            roundEffectLineageId = if (sunlightFundingPlayerId == player.id) sunlightFundingRoundLineageId else null
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
        _roundEffectConsequences.filter { it.playerId == playerId && it.assetId == assetId }.forEach {
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
                it.observedInResolvedStrike = true
                it.contributedToResolvedStrike = contribution.contributesValue
                it.contributedToWinningStrike = contribution.associatedBattleVp > 0 && contribution.contributesValue
                it.individuallyWinnerDecisive = contribution.individuallyWinnerDecisive
                it.individuallyWoundDecisive = contribution.individuallyWoundDecisive
                it.associatedBattleVp = contribution.associatedBattleVp
            }
            _roundEffectConsequences.filter { it.playerId == contribution.playerId && it.assetId == assetId }.forEach {
                it.contributedToWinningStrike = contribution.associatedBattleVp > 0 && contribution.contributesValue
                it.individuallyWinnerDecisive = contribution.individuallyWinnerDecisive
                it.individuallyWoundDecisive = contribution.individuallyWoundDecisive
                it.associatedBattleVp = contribution.associatedBattleVp
            }
        }
    }
}
