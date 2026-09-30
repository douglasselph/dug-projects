package dugsolutions.leaf.simulation.v35.learning.buy

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertIs
import dugsolutions.leaf.v35.game.GameRoundSetup
import dugsolutions.leaf.v35.chronicle.domain.GameEntry
import dugsolutions.leaf.v35.chronicle.domain.PurchaseKind
import dugsolutions.leaf.v35.chronicle.domain.BuyOrderResourceSnapshot
import dugsolutions.leaf.v35.chronicle.domain.BuyOrderDieSnapshot
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.random.die.DieSides
import dugsolutions.leaf.v35.chronicle.domain.*
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.simulation.v35.analysis.*
import dugsolutions.leaf.v35.player.creature.CreatureSide
import dugsolutions.leaf.v35.plant.domain.*

class EvaluateBuyPolicyTest {
    @Test fun `evaluation accumulator starts empty and keeps four seat buckets`() {
        val a=EvalAccumulator()
        assertEquals(0.0,a.winShare)
        assertEquals(0L,a.plantPurchases)
        assertEquals(4,a.seatWins.size)
        assertEquals(4,a.seatGames.size)
    }
    @Test fun `buy shape groups purchases within each Buy phase and records zero purchase phases`() {
        val p = PlayerId(1)
        val entries = listOf(
            GameEntry.BuyOrder(1, listOf(p), listOf(BuyOrderResourceSnapshot(p, listOf(BuyOrderDieSnapshot(DieSides.D20, 17)), emptyList()))),
            GameEntry.Purchase(2, p, PurchaseKind.PLANT, "Vine_07_01", 7, 7),
            GameEntry.Purchase(3, p, PurchaseKind.PLANT, "Vine_07_01", 7, 10),
            GameEntry.BuyOrder(4, listOf(p), listOf(BuyOrderResourceSnapshot(p, emptyList(), emptyList())))
        )
        val shape = BuyShapeAccumulator()
        shape.addGame(entries, p)
        assertEquals(2L, shape.phases)
        assertEquals(2L, shape.purchases)
        assertEquals(2, shape.maxPurchases)
        assertEquals(1L, shape.purchaseCount[0])
        assertEquals(1L, shape.purchaseCount[2])
        assertEquals(1L, shape.kindSequences["Plant -> Plant"])
        assertEquals(1L, shape.itemSequences["P7 -> P7"])
        assertEquals(1L, shape.plantCostSequences["7 -> 7"])
        assertEquals(17L, shape.startingPower)
        assertEquals(17L, shape.spentPower)
        assertEquals(3L, shape.overpayment)
    }


    @Test fun `effect utilization records round effects support upgrades and exact final wisp scoring`() {
        val p = PlayerId(1)
        val entries = listOf(
            GameEntry.EffectResolved(1,p,GameEffect.UPGRADE_DIE_FROM_HAND,EffectSourceKind.ROUND,"Resource_Water_Compost",ChroniclePhase.CULTIVATION,0),
            GameEntry.Upgrade(2,p,DieSides.D12,DieSides.D20,UpgradeDestination.DISCARD,12,1),
            GameEntry.SupportAction(3,p,ChroniclePhase.BATTLE,SupportActionKind.WATER_REROLL,null,null,0),
            GameEntry.RollReward(4,p,RollRewardKind.WISP_GAINED,null,"Wisp_01",0),
            GameEntry.EffectResolved(5,p,GameEffect.GAIN_ONE_VP,EffectSourceKind.WISP,"Wisp_01",ChroniclePhase.BATTLE,0),
            GameEntry.FinalScore(6,p,10,5,2,17,3,0)
        )
        val a=EffectResourceAccumulator(); a.addGame(entries,p)
        assertEquals(1L,a.upgrades["D12->D20"]); assertEquals(1L,a.upgradeSources["ROUND:Resource_Water_Compost"])
        assertEquals(1L,a.supportActions["WATER_REROLL"]); assertEquals(1L,a.rollWispsGained)
        assertEquals(1L,a.wispEffects["Wisp_01"]); assertEquals(2L,a.finalWispVp)
    }


    @Test fun `VP ledger reconciles battle effects plants wisps and remainder`() {
        val p = PlayerId(1)
        val summary = PlayerGameSummary(
            seat=0, playerId=p, won=true, winShare=1.0,
            existingVp=8, plantVp=3, unplayedWispVp=2, totalVp=13,
            battleStrikeVp=4, woundsTaken=0, rollRewardWispsGained=0, wispsPlayed=0, finalWispCount=1,
            finalPlantCount=1, finalPlantPrintedCost=7,
            plantCreatureSignature=PlantCreatureSignature(listOf(PlantCreatureCardSignature("Vine_07_01",CreatureSide.LEFT,0,0))),
            finalDiceCount=0, finalDicePower=0, ownedDiceSignature=OwnedDiceSignature(0,0,0,0,0,0)
        )
        val entries = listOf(
            GameEntry.EffectResolved(1,p,GameEffect.GAIN_ONE_VP,EffectSourceKind.WISP,"Wisp_Award_VP",ChroniclePhase.BATTLE,0)
        )
        val card = PlantCard(1,"Vine_07_01","test",PlantType.VINE,7,null,"","","","","","","",GameEffect.GAIN_ONE_VP,PlantScoringRule.Fixed(3))
        val a=VpLedgerAccumulator(); a.addGame(summary,entries,mapOf(card.name to card))
        assertEquals(4L,a.battleStrikeVp)
        assertEquals(1L,a.directEffectVp)
        assertEquals(3L,a.otherExistingVp)
        assertEquals(3L,a.plantVpByCard["Vine_07_01"])
        assertEquals(2L,a.wispVp)
        assertEquals(13L,a.finalVp)
    }

    @Test fun `default evaluation keeps FirstGameDefault`() {
        val o = EvalOptions.parse(emptyList())
        assertNull(o.grovePattern)
        assertEquals(181000L, o.groveSeed)
    }

    @Test fun `grove option preserves partial Grove pattern and dedicated seed`() {
        val o = EvalOptions.parse(listOf("--grove", "000100000", "--grove-seed", "281000"))
        assertEquals("000100000", o.grovePattern)
        assertEquals(281000L, o.groveSeed)
    }

    @Test fun `random Grove option is all zero pattern`() {
        val o = EvalOptions.parse(listOf("--random-grove"))
        assertEquals("000000000", o.grovePattern)
    }

    @Test fun `weights alias exclusions and standard round blocks parse`() {
        val o = EvalOptions.parse(listOf(
            "--weights", "data/ai/frozen-buy-first-game-default-v1.weights",
            "--random-grove",
            "--exclude-card", "Vine_07_04,Vine_07_01",
            "--rounds", "3/2/2"
        ))
        assertEquals("data/ai/frozen-buy-first-game-default-v1.weights", o.input.toString())
        assertEquals(setOf("Vine_07_04", "Vine_07_01"), o.excludedCards)
        val setup = assertIs<GameRoundSetup.Patterned>(o.roundSetup)
        assertEquals(listOf(3, 2, 2), setup.cultivationBlocks)
        assertEquals("3/2/2", o.roundLabel)
    }

    @Test fun `compact round pattern is cultivation followed by battle`() {
        val o = EvalOptions.parse(listOf("--rounds", "95"))
        val setup = assertIs<GameRoundSetup.Ordered>(o.roundSetup)
        assertEquals(9, setup.cultivationRounds)
        assertEquals(5, setup.battleRounds)
        assertEquals("95", o.roundLabel)
    }

}
