package dugsolutions.leaf.simulation.v35.learning.buy

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
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
import dugsolutions.leaf.v35.round.domain.RoundCardType
import dugsolutions.leaf.v35.round.domain.RoundCard
import dugsolutions.leaf.v35.round.domain.RoundCardEffect
import dugsolutions.leaf.v35.round.RoundCardManager
import dugsolutions.leaf.v35.wisp.WispCardManager
import dugsolutions.leaf.v35.wisp.domain.WispCard
import dugsolutions.leaf.simulation.v35.analysis.*
import dugsolutions.leaf.v35.player.creature.CreatureSide
import dugsolutions.leaf.v35.plant.domain.*
import dugsolutions.leaf.v35.battle.*
import dugsolutions.leaf.v35.battle.domain.*

class EvaluateBuyPolicyTest {
    @Test
    fun `Cultivation Main policy defaults to Human and learned mode requires weights`() {
        assertEquals("human", EvalOptions.parse(emptyList()).cultivationMainPolicy)
        assertEquals("human", EvalOptions.parse(listOf("--cultivation-main-policy", "human")).cultivationMainPolicy)
        assertFailsWith<IllegalArgumentException> {
            EvalOptions.parse(listOf("--cultivation-main-policy", "learned"))
        }
        val learned = EvalOptions.parse(listOf(
            "--cultivation-main-policy", "learned",
            "--cultivation-main-weights", "output/cult.weights"
        ))
        assertEquals("learned", learned.cultivationMainPolicy)
        assertEquals("output/cult.weights", learned.cultivationMainWeights.toString())
    }

    @Test
    fun `Plant Effect policy defaults to Human and learned mode requires weights`() {
        assertEquals("human", EvalOptions.parse(emptyList()).plantEffectPolicy)
        assertFailsWith<IllegalArgumentException> {
            EvalOptions.parse(listOf("--plant-effect-policy", "learned"))
        }
        val learned = EvalOptions.parse(listOf(
            "--plant-effect-policy", "learned",
            "--plant-effect-weights", "output/plant-effect.weights"
        ))
        assertEquals("learned", learned.plantEffectPolicy)
        assertEquals("output/plant-effect.weights", learned.plantEffectWeights.toString())
    }

    @Test
    fun `Buy evaluator accepts coordinated learned surrounding context`() {
        val o = EvalOptions.parse(listOf(
            "--cultivation-main-policy", "learned",
            "--cultivation-main-weights", "output/cult.weights",
            "--plant-effect-policy", "learned",
            "--plant-effect-weights", "output/plant.weights",
            "--battle-support-policy", "learned",
            "--battle-support-weights", "output/battle.weights"
        ))
        assertEquals("learned", o.cultivationMainPolicy)
        assertEquals("learned", o.plantEffectPolicy)
        assertEquals("learned", o.battleSupportPolicy)
    }

    @Test
    fun `Battle Support policy option defaults to Human and rejects unavailable learned mode`() {
        assertEquals("human", EvalOptions.parse(emptyList()).battleSupportPolicy)
        assertEquals("human", EvalOptions.parse(listOf("--battle-support-policy", "human")).battleSupportPolicy)
        assertFailsWith<IllegalArgumentException> {
            EvalOptions.parse(listOf("--battle-support-policy", "learned"))
        }
    }

    @Test fun `evaluation accumulator starts empty and keeps four seat buckets`() {
        val a=EvalAccumulator()
        assertEquals(0.0,a.winShare)
        assertEquals(0L,a.plantPurchases)
        assertEquals(4,a.seatWins.size)
        assertEquals(4,a.seatGames.size)
    }

    @Test fun `Plant activation research separates Cultivation and Battle by card`() {
        val player = PlayerId(1)
        val other = PlayerId(2)
        val entries = listOf(
            GameEntry.EffectResolved(1, player, GameEffect.DISCARD_ANY_NUMBER_OF_DICE_AND_REDRAW_OR_REROLL_ONE_IN_BATTLE, EffectSourceKind.PLANT, "Root_07_04", ChroniclePhase.CULTIVATION, 0),
            GameEntry.EffectResolved(2, player, GameEffect.DISCARD_ANY_NUMBER_OF_DICE_AND_REDRAW_OR_REROLL_ONE_IN_BATTLE, EffectSourceKind.PLANT, "Root_07_04", ChroniclePhase.BATTLE, 0),
            GameEntry.EffectResolved(3, player, GameEffect.DISCARD_ANY_NUMBER_OF_DICE_AND_REDRAW_OR_REROLL_ONE_IN_BATTLE, EffectSourceKind.PLANT, "Root_07_04", ChroniclePhase.CULTIVATION, 0),
            GameEntry.EffectResolved(4, other, GameEffect.DISCARD_ANY_NUMBER_OF_DICE_AND_REDRAW_OR_REROLL_ONE_IN_BATTLE, EffectSourceKind.PLANT, "Root_07_04", ChroniclePhase.BATTLE, 0),
            GameEntry.EffectResolved(5, player, GameEffect.GAIN_ONE_VP, EffectSourceKind.WISP, "Wisp_01", ChroniclePhase.CULTIVATION, 0)
        )
        val a = PlantActivationByPhaseAccumulator()
        a.addGame(entries, player)
        assertEquals(2L, a.cultivation["Root_07_04"])
        assertEquals(1L, a.battle["Root_07_04"])
        assertEquals(1, a.cultivation.size)
        assertEquals(1, a.battle.size)
    }

    @Test fun `evaluation player count defaults to four and accepts two or three`() {
        assertEquals(4, EvalOptions.parse(emptyList()).players)
        assertEquals(2, EvalOptions.parse(listOf("--players", "2")).players)
        assertEquals(3, EvalOptions.parse(listOf("--players=3")).players)
        assertEquals(2, EvalAccumulator(2).seatWins.size)
        assertEquals(3, EvalAccumulator(3).seatGames.size)
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
            GameEntry.FinalScore(6,p,10,5,2,17,3,hierarchyDepth=0)
        )
        val a=EffectResourceAccumulator(); a.addGame(entries,p)
        assertEquals(1L,a.upgrades["D12->D20"]); assertEquals(1L,a.upgradeSources["ROUND:Resource_Water_Compost"])
        assertEquals(1L,a.supportActions["WATER_REROLL"]); assertEquals(1L,a.rollWispsGained)
        assertEquals(1L,a.wispEffects["Wisp_01"]); assertEquals(2L,a.finalWispVp)
    }


    @Test fun `wisp acquisition use and retention remain distinct by card`() {
        val p = PlayerId(1)
        val entries = listOf(
            GameEntry.RollReward(1,p,RollRewardKind.WISP_GAINED,null,"Wisp_Keep",0),
            GameEntry.WispAcquired(2,p,"Wisp_Play",WispAcquisitionSourceKind.EFFECT_DRAW,"Flower_Test",false,0),
            GameEntry.EffectResolved(3,p,GameEffect.GAIN_ONE_VP,EffectSourceKind.WISP,"Wisp_Play",ChroniclePhase.CULTIVATION,0),
            GameEntry.FinalScore(4,p,0,0,2,2,0,listOf("Wisp_Keep"),0)
        )
        val a = EffectResourceAccumulator()
        a.addGame(entries,p)
        assertEquals(1L,a.wispAcquiredByCard["Wisp_Keep"])
        assertEquals(1L,a.wispAcquiredByCard["Wisp_Play"])
        assertEquals(null,a.wispEffects["Wisp_Keep"])
        assertEquals(1L,a.wispEffects["Wisp_Play"])
        assertEquals(1L,a.wispRetainedByCard["Wisp_Keep"])
        assertEquals(null,a.wispRetainedByCard["Wisp_Play"])
        assertEquals(1L,a.wispAcquisitionSources["EFFECT_DRAW:Flower_Test:Wisp_Play"])
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
            "--weights", "data/ai/4p/frozen-buy-first-game-default-v1.weights",
            "--random-grove",
            "--exclude-card", "Vine_07_04,Vine_07_01",
            "--rounds", "3/2/2"
        ))
        assertEquals("data/ai/4p/frozen-buy-first-game-default-v1.weights", o.input.toString())
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

    @Test fun `round effect choice records declines scarcity and effective Sunlight context`() {
        val p = PlayerId(1)
        val entries = listOf(
            GameEntry.RoundRevealed(1, 1, "Resource_Sunlight_Test", RoundCardType.CULTIVATION, GameEffect.GAIN_SUNLIGHT_TOKEN, GameEffect.GAIN_ONE_WISP, 0),
            GameEntry.RoundEffectOpportunity(
                sequence = 2, playerId = p, phase = ChroniclePhase.CULTIVATION,
                roundCardName = "Resource_Sunlight_Test", firstEffect = GameEffect.GAIN_SUNLIGHT_TOKEN,
                secondEffect = GameEffect.GAIN_ONE_WISP, firstExecutable = true, secondExecutable = false,
                hierarchyDepth = 0, secondBlockedBySharedResource = true
            ),
            GameEntry.RoundEffectChoice(
                sequence = 3, playerId = p, phase = ChroniclePhase.CULTIVATION,
                roundCardName = "Resource_Sunlight_Test", firstEffect = GameEffect.GAIN_SUNLIGHT_TOKEN,
                secondEffect = GameEffect.GAIN_ONE_WISP, firstExecutable = true, secondExecutable = false,
                legalMainActions = listOf(MainActionKind.DRAW, MainActionKind.ROUND_EFFECT_1),
                selectedMainAction = MainActionKind.DRAW, sunlightHeld = 1, battlesRemaining = 2, battleNext = true,
                hierarchyDepth = 0
            ),
            GameEntry.FinalScore(4, p, 12, 4, 0, 16, 3, hierarchyDepth = 0),
            GameEntry.FinalWinners(5, listOf(p), 0)
        )
        val a = EffectResourceAccumulator()

        a.addGame(entries, p)

        val sunlight = "Resource_Sunlight_Test / Effect 1 / GAIN_SUNLIGHT_TOKEN"
        val wisp = "Resource_Sunlight_Test / Effect 2 / GAIN_ONE_WISP"
        assertEquals(1L, a.roundEffectExposures[sunlight])
        assertEquals(1L, a.roundEffectOpportunities[sunlight])
        assertEquals(1L, a.roundEffectDeclines[sunlight])
        assertEquals(1L, a.roundEffectIllegal[wisp])
        assertEquals(1L, a.roundEffectResourceBlocked[wisp])
        assertEquals(1L, a.sunlightGainChoiceOpportunities)
        assertEquals(1L, a.sunlightGainChoiceDeclines)
        assertEquals(1L, a.sunlightChoicesByHeld[1])
        assertEquals(1L, a.sunlightChoicesByBattlesRemaining[2])
        assertEquals(1L, a.sunlightChoicesByBattleNext[true])
        assertEquals(1L, a.sunlightDeclineSelections[MainActionKind.DRAW.name])
        assertEquals(16L, a.sunlightDeclineFinalVp)
        assertEquals(1.0, a.sunlightDeclineWinShare)
    }

    @Test fun `round effect selected use remains distinct from legal declines`() {
        val p = PlayerId(1)
        val entries = listOf(
            GameEntry.RoundRevealed(1, 1, "Resource_Sunlight_Test", RoundCardType.CULTIVATION, GameEffect.GAIN_SUNLIGHT_TOKEN, GameEffect.GAIN_ONE_WISP, 0),
            GameEntry.RoundEffectOpportunity(2, p, ChroniclePhase.CULTIVATION, "Resource_Sunlight_Test", GameEffect.GAIN_SUNLIGHT_TOKEN, GameEffect.GAIN_ONE_WISP, true, true, 0),
            GameEntry.RoundEffectChoice(
                3, p, ChroniclePhase.CULTIVATION, "Resource_Sunlight_Test",
                GameEffect.GAIN_SUNLIGHT_TOKEN, GameEffect.GAIN_ONE_WISP, true, true,
                listOf(MainActionKind.ROUND_EFFECT_1, MainActionKind.ROUND_EFFECT_2),
                MainActionKind.ROUND_EFFECT_1, 0, 3, false, 0
            ),
            GameEntry.MainAction(4, p, ChroniclePhase.CULTIVATION, MainActionKind.ROUND_EFFECT_1, 1, null, null, 0),
            GameEntry.FinalScore(5, p, 10, 4, 0, 14, 2, hierarchyDepth = 0),
            GameEntry.FinalWinners(6, listOf(p), 0)
        )
        val a = EffectResourceAccumulator()

        a.addGame(entries, p)

        val sunlight = "Resource_Sunlight_Test / Effect 1 / GAIN_SUNLIGHT_TOKEN"
        val other = "Resource_Sunlight_Test / Effect 2 / GAIN_ONE_WISP"
        assertEquals(1L, a.roundEffectUses[sunlight])
        assertEquals(null, a.roundEffectDeclines[sunlight])
        assertEquals(1L, a.roundEffectDeclines[other])
        assertEquals(1L, a.sunlightGainChoiceUses)
        assertEquals(0L, a.sunlightGainChoiceDeclines)
    }

    @Test fun `round exposure opportunities and use remain distinct`() {
        val p = PlayerId(1)
        val entries = listOf(
            GameEntry.RoundRevealed(1,1,"Cultivation_Test",RoundCardType.CULTIVATION,GameEffect.UPGRADE_DIE_FROM_HAND,GameEffect.GAIN_ONE_WISP,0),
            GameEntry.RoundEffectOpportunity(2,p,ChroniclePhase.CULTIVATION,"Cultivation_Test",GameEffect.UPGRADE_DIE_FROM_HAND,GameEffect.GAIN_ONE_WISP,true,true,0),
            GameEntry.MainAction(3,p,ChroniclePhase.CULTIVATION,MainActionKind.ROUND_EFFECT_1,1,null,null,0),
            GameEntry.RoundEffectOpportunity(4,p,ChroniclePhase.CULTIVATION,"Cultivation_Test",GameEffect.UPGRADE_DIE_FROM_HAND,GameEffect.GAIN_ONE_WISP,false,true,0)
        )
        val a = EffectResourceAccumulator()
        a.addGame(entries,p)
        assertEquals(1L,a.roundCardReveals["Cultivation_Test"])
        assertEquals(1L,a.roundEffectOpportunities["Cultivation_Test / Effect 1 / UPGRADE_DIE_FROM_HAND"])
        assertEquals(2L,a.roundEffectOpportunities["Cultivation_Test / Effect 2 / GAIN_ONE_WISP"])
        assertEquals(1L,a.roundEffectUses["Cultivation_Test / Effect 1 / UPGRADE_DIE_FROM_HAND"])
        assertEquals(null,a.roundEffectUses["Cultivation_Test / Effect 2 / GAIN_ONE_WISP"])
        assertEquals(1L,a.roundCategoryUses["Die upgrade"])
        assertEquals(2L,a.roundCategoryOpportunities["Wisp gain"])
    }

    @Test fun `die development provenance separates buys direct gains and upgrades without double counting replacement die`() {
        val p = PlayerId(1)
        val entries = listOf(
            GameEntry.Purchase(1,p,PurchaseKind.DIE,"D8",8,8,0),
            GameEntry.EffectResolved(2,p,GameEffect.GAIN_D20_TO_DISCARD,EffectSourceKind.ROUND,"Battle_Swell",ChroniclePhase.BATTLE,0),
            GameEntry.DieGained(3,p,DieSides.D20,1),
            GameEntry.EffectResolved(4,p,GameEffect.UPGRADE_DIE_FROM_HAND,EffectSourceKind.WISP,"Wisp_Upgrade_Die",ChroniclePhase.CULTIVATION,0),
            GameEntry.Upgrade(5,p,DieSides.D12,DieSides.D20,UpgradeDestination.DISCARD,7,1),
            GameEntry.EffectResolved(6,p,GameEffect.UPGRADE_DIE_FROM_HAND,EffectSourceKind.ROUND,"Resource_Water_Compost",ChroniclePhase.CULTIVATION,0),
            GameEntry.Upgrade(7,p,DieSides.D8,DieSides.D10,UpgradeDestination.DISCARD,4,1)
        )
        val a = EffectResourceAccumulator()
        a.addGame(entries,p)

        assertEquals(1L,a.dieGainsBySourceAndSize["BUY:D8"])
        assertEquals(1L,a.dieGainsBySourceAndSize["ROUND:Battle_Swell:D20"])
        assertEquals(1L,a.upgradesBySourceAndTransition["WISP:Wisp_Upgrade_Die:D12->D20"])
        assertEquals(1L,a.upgradesBySourceAndTransition["COMPOST:Resource_Water_Compost:D8->D10"])
        assertEquals(8L,a.dieDevelopmentPowerBySource["BUY"])
        assertEquals(20L,a.dieDevelopmentPowerBySource["ROUND"])
        assertEquals(8L,a.dieDevelopmentPowerBySource["WISP"])
        assertEquals(2L,a.dieDevelopmentPowerBySource["COMPOST"])
        // Upgrade replacement D20 is represented only by +8 incremental sides,
        // not as an additional direct D20 gain.
        assertEquals(null,a.dieGainsBySourceAndSize["WISP:Wisp_Upgrade_Die:D20"])
    }


    @Test fun `strike research distinguishes inferior side wins favorable rolls critters and decisive dice`() {
        val p = PlayerId(1)
        val q = PlayerId(2)
        fun square(id:PlayerId, sides:Int, value:Int, critter:Int=0) = BattleGridSquareSnapshot(
            id,
            listOf(BattleGridDieSnapshot(DieSides.from(sides), value)),
            if (critter == 0) emptyList() else listOf(BattleGridCritterSnapshot(dugsolutions.leaf.v35.tokens.Critter.BEE, critter))
        )
        fun strike(seq:Long, winner:BattleGridSquareSnapshot, loser:BattleGridSquareSnapshot) : GameEntry.StrikeResolved {
            val row = BattleGridRowSnapshot(StrikeRow.TOP, listOf(winner, loser))
            val totals = listOf(winner,loser).map { StrikeTotalSnapshot(it.playerId,it.dice.sumOf { d->d.value },it.critters.sumOf { c->c.value },it.total) }
            val winners = listOf(winner.playerId)
            val wounded = if (winner.total-loser.total>=5) listOf(loser.playerId) else emptyList()
            val ledger = StrikeContributionAnalyzer.analyze(row,winners,wounded,2)
            return GameEntry.StrikeResolved(seq,StrikeRow.TOP,totals,row,winners,wounded,2,ledger,0)
        }
        val entries = listOf(
            GameEntry.RoundCompleted(1,1,"C",RoundCardType.CULTIVATION,listOf(PlayerRoundSummarySnapshot(p,0,emptyList(),listOf(DieSides.D8),listOf(DieSides.D6),0,0,0,emptyList(),0,emptyList())),0),
            GameEntry.RoundRevealed(2,2,"B",RoundCardType.BATTLE,GameEffect.GAIN_ONE_VP,GameEffect.GAIN_ONE_VP,0),
            // P1 has inferior committed sides (D8 vs D12) but rolls higher: 8 vs 5.
            strike(3,square(p,8,8),square(q,12,5)),
            // P1 again has inferior sides. Bee is individually decisive, while the die is not:
            // 1 + Bee 3 beats 2; without Bee P1 loses 1-2, while without the die Bee 3 still wins 3-2.
            strike(4,square(p,8,1,3),square(q,12,2)),
            GameEntry.RoundCompleted(5,2,"B",RoundCardType.BATTLE,emptyList(),0)
        )
        val a=StrikeRowResearchAccumulator(); a.addGame(entries,p,1.0)
        assertEquals(2L,a.inferiorSideWins)
        assertEquals(1L,a.inferiorWonWithHigherRollTotal)
        assertEquals(1L,a.inferiorWithDecisiveCritter)
        assertEquals(1L,a.inferiorWithDecisiveDie) // first row's D8 is individually decisive
        assertEquals(2L,a.inferiorWithNoHighDie)
        assertEquals(1L,a.entryBuckets["MAX_D8_OR_LOWER"]?.battles)
        assertEquals(2L,a.entryBuckets["MAX_D8_OR_LOWER"]?.rowsWon)
    }

    @Test fun `strike research records high die placement winning association and individual decisiveness separately`() {
        val p=PlayerId(1); val q=PlayerId(2)
        val winner=BattleGridSquareSnapshot(p,listOf(
            BattleGridDieSnapshot(DieSides.D20,1),
            BattleGridDieSnapshot(DieSides.D6,6)
        ),emptyList())
        val loser=BattleGridSquareSnapshot(q,listOf(BattleGridDieSnapshot(DieSides.D6,5)),emptyList())
        val row=BattleGridRowSnapshot(StrikeRow.TOP,listOf(winner,loser))
        val ledger=StrikeContributionAnalyzer.analyze(row,listOf(p),emptyList(),2)
        val strike=GameEntry.StrikeResolved(3,StrikeRow.TOP,listOf(
            StrikeTotalSnapshot(p,7,0,7),StrikeTotalSnapshot(q,5,0,5)
        ),row,listOf(p),emptyList(),2,ledger,0)
        val entries=listOf(
            GameEntry.RoundCompleted(1,1,"C",RoundCardType.CULTIVATION,listOf(PlayerRoundSummarySnapshot(p,0,emptyList(),listOf(DieSides.D20,DieSides.D6),emptyList(),0,0,0,emptyList(),0,emptyList())),0),
            GameEntry.RoundRevealed(2,2,"B",RoundCardType.BATTLE,GameEffect.GAIN_ONE_VP,GameEffect.GAIN_ONE_VP,0),
            GameEntry.BattleResolvePreview(2,listOf(row),0),
            strike,
            GameEntry.RoundCompleted(4,2,"B",RoundCardType.BATTLE,emptyList(),0)
        )
        val a=StrikeRowResearchAccumulator(); a.addGame(entries,p,0.5)
        assertEquals(1L,a.highDieAvailable["D20"])
        assertEquals(1L,a.highDiePlaced["D20"])
        assertEquals(1L,a.highDieOnWinningRows["D20"])
        assertEquals(1L,a.superiorSideWins)
        assertEquals(null,a.highDieDecisive["D20"]) // removing the D20's showing 1 still leaves 6 > 5
        assertEquals(1L,a.decisiveWinnerDice["D8_OR_LOWER"])
    }

    @Test fun `research environment options parse reusable include exclude controls`() {
        val o=EvalOptions.parse(listOf(
            "--research-environment","upgrade-poor","--environment-seed","291000",
            "--round-exclude-card","B1,B2","--wisp-include-card","W1,W2"
        ))
        assertEquals("upgrade-poor",o.researchEnvironment)
        assertEquals(291000L,o.environmentSeed)
        assertEquals(setOf("B1","B2"),o.roundExcludes)
        assertEquals(setOf("W1","W2"),o.wispIncludes)
    }

    @Test fun `default research environment leaves normal Round and Wisp setup untouched`() {
        val o=EvalOptions.parse(emptyList())
        val resolved=resolveResearchEnvironmentForSample(o,0,RoundCardManager(),WispCardManager())
        assertNull(resolved.roundCards)
        assertNull(resolved.wispCards)
    }

    @Test fun `controlled environment honors constraints and is reproducible for matched sample`() {
        fun effect(e:GameEffect)=RoundCardEffect("t","b","f","i",null,e)
        val rounds=RoundCardManager().apply { loadCards(listOf(
            RoundCard(3,"C_UP",RoundCardType.CULTIVATION,effect(GameEffect.UPGRADE_DIE_FROM_HAND),effect(GameEffect.GAIN_WATER_TOKEN),""),
            RoundCard(3,"C_PLAIN",RoundCardType.CULTIVATION,effect(GameEffect.GAIN_WATER_TOKEN),effect(GameEffect.RAISE_DIE_PLUS_3),""),
            RoundCard(2,"B_HIGH",RoundCardType.BATTLE,effect(GameEffect.GAIN_D20_TO_DISCARD),effect(GameEffect.GAIN_ONE_VP),""),
            RoundCard(2,"B_PLAIN",RoundCardType.BATTLE,effect(GameEffect.GAIN_ONE_VP),effect(GameEffect.GAIN_TWO_WORMS),"")
        )) }
        fun wisp(name:String,e:GameEffect)=WispCard(2,name,name,2,e,null,40,null,"")
        val wisps=WispCardManager().apply { loadCards(listOf(wisp("W_UP",GameEffect.UPGRADE_DIE_TWO_STEPS_SKIP_MISSING_AND_USE_NOW),wisp("W_PLAIN",GameEffect.GAIN_ANY_TWO_CRITTERS))) }
        val poor=EvalOptions.parse(listOf("--rounds","32","--research-environment","upgrade-poor","--environment-seed","7"))
        val a=resolveResearchEnvironmentForSample(poor,4,rounds,wisps)
        val b=resolveResearchEnvironmentForSample(poor,4,rounds,wisps)
        assertEquals(a,b)
        assertEquals(listOf("C_PLAIN","C_PLAIN","C_PLAIN","B_PLAIN","B_PLAIN"),a.roundCards!!.map { it.name })
        assertEquals(setOf("W_PLAIN"),a.wispCards!!.map { it.name }.toSet())

        val constrained=EvalOptions.parse(listOf("--rounds","32","--round-include-card","C_UP,B_HIGH","--wisp-include-card","W_UP","--environment-seed","7"))
        val c=resolveResearchEnvironmentForSample(constrained,4,rounds,wisps)
        assertEquals(setOf("C_UP","B_HIGH"),c.roundCards!!.map { it.name }.toSet())
        assertEquals(setOf("W_UP"),c.wispCards!!.map { it.name }.toSet())

        val excluded=EvalOptions.parse(listOf("--rounds","32","--round-exclude-card","C_PLAIN,B_PLAIN","--wisp-exclude-card","W_PLAIN","--environment-seed","7"))
        val d=resolveResearchEnvironmentForSample(excluded,4,rounds,wisps)
        assertEquals(setOf("C_UP","B_HIGH"),d.roundCards!!.map { it.name }.toSet())
        assertEquals(setOf("W_UP"),d.wispCards!!.map { it.name }.toSet())
    }

    @Test fun `upgrade rich environment prioritizes die development cards`() {
        fun effect(e:GameEffect)=RoundCardEffect("t","b","f","i",null,e)
        val rounds=RoundCardManager().apply { loadCards(listOf(
            RoundCard(3,"C_UP",RoundCardType.CULTIVATION,effect(GameEffect.UPGRADE_DIE_FROM_HAND),effect(GameEffect.GAIN_WATER_TOKEN),""),
            RoundCard(3,"C_PLAIN",RoundCardType.CULTIVATION,effect(GameEffect.GAIN_WATER_TOKEN),effect(GameEffect.RAISE_DIE_PLUS_3),""),
            RoundCard(2,"B_HIGH",RoundCardType.BATTLE,effect(GameEffect.GAIN_D20_TO_DISCARD),effect(GameEffect.GAIN_ONE_VP),""),
            RoundCard(2,"B_PLAIN",RoundCardType.BATTLE,effect(GameEffect.GAIN_ONE_VP),effect(GameEffect.GAIN_TWO_WORMS),"")
        )) }
        fun wisp(name:String,e:GameEffect)=WispCard(2,name,name,2,e,null,40,null,"")
        val wisps=WispCardManager().apply { loadCards(listOf(wisp("W_UP",GameEffect.UPGRADE_DIE_TWO_STEPS_SKIP_MISSING_AND_USE_NOW),wisp("W_PLAIN",GameEffect.GAIN_ANY_TWO_CRITTERS))) }
        val rich=EvalOptions.parse(listOf("--rounds","32","--research-environment","upgrade-rich","--environment-seed","9"))
        val r=resolveResearchEnvironmentForSample(rich,0,rounds,wisps)
        assertEquals(listOf("C_UP","C_UP","C_UP","B_HIGH","B_HIGH"),r.roundCards!!.map { it.name })
        assertEquals(setOf("W_UP"),r.wispCards!!.map { it.name }.toSet())
    }


    @Test fun `evaluation accepts learned Battle Support context`() {
        val o = EvalOptions.parse(listOf(
            "--battle-support-policy", "learned",
            "--battle-support-weights", "output/battle.weights"
        ))
        assertEquals("learned", o.battleSupportPolicy)
        assertEquals("output/battle.weights", o.battleSupportWeights?.toString())
    }

}
