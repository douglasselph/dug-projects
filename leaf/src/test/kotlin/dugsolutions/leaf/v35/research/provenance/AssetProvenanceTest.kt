package dugsolutions.leaf.v35.research.provenance

import dugsolutions.leaf.v35.battle.StrikeContribution
import dugsolutions.leaf.v35.battle.StrikeContributionLedger
import dugsolutions.leaf.v35.battle.StrikeContributionSource
import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.Player
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.DecisionDirector
import dugsolutions.leaf.v35.player.dice.PlayerDice
import dugsolutions.leaf.v35.random.die.Die
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AssetProvenanceTest {
    @Test
    fun forgetMeNotRecordsNaturalRecycleDistanceAndAcceleration() {
        val target = die(20, 7)
        val player = Player(
            PlayerId(1), DecisionDirector.baseline(),
            dice = PlayerDice(
                supply = listOf(die(4, 2), die(6, 3)),
                discard = listOf(die(8, 4), die(12, 6), target)
            )
        )
        val provenance = AssetProvenance()

        provenance.recordForgetMeNot(player, target, "Flower_17_02")

        val record = provenance.forgetMeNot.single()
        assertEquals("Flower_17_02", record.sourceCard)
        assertEquals(20, record.dieSides)
        assertEquals(5, record.estimatedNaturalDrawsUntilAvailable)
        assertEquals(5, record.estimatedDrawsAccelerated)
    }

    @Test
    fun sunlightFundedImmediateEffectCanContributeWithoutBeingDecisive() {
        val die = die(12, 7)
        val player = Player(PlayerId(1), DecisionDirector.baseline(), dice = PlayerDice(hand = listOf(die)))
        val provenance = AssetProvenance()
        provenance.withSunlightFunding(player.id) {
            provenance.recordImmediateDieEffect(player, die, "SunPlant", GameEffect.RAISE_DIE_PLUS_4, 3, 7)
        }
        provenance.observeStrike(ledger(player.id, provenance.assetId(die)!!, winnerDecisive = false, woundDecisive = false, battleVp = 2))

        val record = provenance.immediateDieEffects.single()
        assertTrue(record.sunlightFunded)
        assertTrue(record.contributedToResolvedStrike)
        assertTrue(record.contributedToWinningStrike)
        assertFalse(record.individuallyWinnerDecisive)
        assertEquals(2, record.associatedBattleVp)
    }

    @Test
    fun sunlightFundedImmediateEffectCanBeWinnerAndWoundDecisive() {
        val die = die(12, 8)
        val player = Player(PlayerId(1), DecisionDirector.baseline(), dice = PlayerDice(hand = listOf(die)))
        val provenance = AssetProvenance()
        provenance.withSunlightFunding(player.id) {
            provenance.recordImmediateDieEffect(player, die, "SunPlant", GameEffect.RAISE_DIE_PLUS_4, 4, 8)
        }
        provenance.observeStrike(ledger(player.id, provenance.assetId(die)!!, winnerDecisive = true, woundDecisive = true, battleVp = 2))

        val record = provenance.immediateDieEffects.single()
        assertTrue(record.sunlightFunded)
        assertTrue(record.individuallyWinnerDecisive)
        assertTrue(record.individuallyWoundDecisive)
    }

    @Test
    fun unrelatedImmediateEffectIsNotAttributedToSunlight() {
        val die = die(12, 7)
        val player = Player(PlayerId(1), DecisionDirector.baseline(), dice = PlayerDice(hand = listOf(die)))
        val provenance = AssetProvenance()
        provenance.recordImmediateDieEffect(player, die, "NormalPlant", GameEffect.RAISE_DIE_PLUS_4, 3, 7)
        provenance.observeStrike(ledger(player.id, provenance.assetId(die)!!, winnerDecisive = true, woundDecisive = false, battleVp = 2))

        val record = provenance.immediateDieEffects.single()
        assertFalse(record.sunlightFunded)
        assertTrue(record.individuallyWinnerDecisive)
    }

    private fun ledger(
        playerId: PlayerId,
        assetId: Long,
        winnerDecisive: Boolean,
        woundDecisive: Boolean,
        battleVp: Int
    ): StrikeContributionLedger = StrikeContributionLedger(
        row = StrikeRow.TOP,
        winnerIds = if (battleVp > 0) listOf(playerId) else emptyList(),
        woundedPlayerIds = emptyList(),
        vpPerWinner = 2,
        contributions = listOf(
            StrikeContribution(
                playerId = playerId,
                source = StrikeContributionSource.Die(index = 0, sides = 12, assetId = assetId),
                magnitude = 7,
                used = true,
                contributesValue = true,
                individuallyWinnerDecisive = winnerDecisive,
                individuallyWoundDecisive = woundDecisive,
                associatedBattleVp = battleVp
            )
        )
    )

    private fun die(sides: Int, value: Int): Die = object : Die(sides) {
        init { adjustTo(value) }
        override fun roll(): Die = this
    }
}
