package dugsolutions.leaf.v35.chronicle

import dugsolutions.leaf.v35.chronicle.domain.GameEntry
import dugsolutions.leaf.v35.chronicle.domain.BuyOrderLeadDieSnapshot
import dugsolutions.leaf.v35.chronicle.domain.BattleOrderHighDieSnapshot
import dugsolutions.leaf.v35.chronicle.domain.BattleMainStage
import dugsolutions.leaf.v35.chronicle.domain.EffectSourceKind
import dugsolutions.leaf.v35.chronicle.domain.MainActionKind
import dugsolutions.leaf.v35.chronicle.domain.SupportActionKind
import dugsolutions.leaf.v35.chronicle.domain.UpgradeDestination
import dugsolutions.leaf.v35.chronicle.domain.ChroniclePhase
import dugsolutions.leaf.v35.chronicle.domain.ChronicleRollRewardPolicy
import dugsolutions.leaf.v35.chronicle.domain.DecisionScoreAdjustmentSnapshot
import dugsolutions.leaf.v35.chronicle.domain.GraftedPlantSnapshot
import dugsolutions.leaf.v35.chronicle.domain.PlayerRoundSummarySnapshot
import dugsolutions.leaf.v35.chronicle.domain.RollReason
import dugsolutions.leaf.v35.chronicle.domain.RollRewardKind
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.plant.domain.PlantScoringRule
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.random.die.DieSides
import dugsolutions.leaf.v35.round.domain.RoundCardType
import dugsolutions.leaf.v35.tokens.Butterfly
import dugsolutions.leaf.v35.tokens.Critter
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChronicleTextRendererTest {


    @Test
    fun `compact cleanup omits zero discard and returned critter counts`() {
        val entries = listOf(
            GameEntry.RoundRevealed(
                sequence = 1,
                roundNumber = 1,
                cardName = "first",
                cardType = RoundCardType.CULTIVATION,
                firstEffect = GameEffect.GAIN_ONE_VP,
                secondEffect = GameEffect.GAIN_ONE_VP
            ),
            GameEntry.Cleanup(
                sequence = 2,
                playerId = PlayerId(1),
                phase = ChroniclePhase.CULTIVATION,
                discardedDice = 0,
                returnedCritters = 0,
                refreshed = true
            ),
            GameEntry.Cleanup(
                sequence = 3,
                playerId = PlayerId(2),
                phase = ChroniclePhase.BATTLE,
                discardedDice = 2,
                returnedCritters = 1,
                refreshed = false
            )
        )

        val compact = ChronicleTextRenderer.render(entries).lines()
        assertEquals("01.002  P1 CULTIVATION CLEANUP refreshed=true", compact[1])
        assertEquals(
            "01.003  P2 BATTLE CLEANUP discardedDice=2 returnedCritters=1 refreshed=false",
            compact[2]
        )

        val detail = ChronicleTextRenderer.render(entries, detail = true).lines()
        assertEquals(
            "01.002  P1 CULTIVATION CLEANUP discardedDice=0 returnedCritters=0 refreshed=true",
            detail[1]
        )
    }


    @Test
    fun `Chronicle can list selected Plant cards before round one`() {
        val cards = listOf(
            plant("Flower_14_02", "Bloom Backflip", PlantType.FLOWER, 14),
            plant("Root_05_02", "Root Four More", PlantType.ROOT, 5),
            plant("Vine_11_04", "Vine's the Limit", PlantType.VINE, 11)
        )
        val entries = listOf(
            GameEntry.RoundRevealed(
                sequence = 1,
                roundNumber = 1,
                cardName = "first",
                cardType = RoundCardType.CULTIVATION,
                firstEffect = GameEffect.GAIN_ONE_VP,
                secondEffect = GameEffect.GAIN_ONE_VP
            )
        )

        val lines = ChronicleTextRenderer.render(entries, selectedPlantCards = cards).lines()

        assertEquals("PLANTS (3)", lines[0])
        assertEquals("  R5  Root Four More [Root_05_02]", lines[1])
        assertEquals("  V11  Vine's the Limit [Vine_11_04]", lines[2])
        assertEquals("  F14  Bloom Backflip [Flower_14_02]", lines[3])
        assertEquals("", lines[4])
        assertEquals("01.001  ROUND 1 REVEAL CULTIVATION: first [GAIN_ONE_VP | GAIN_ONE_VP]", lines[5])
    }

    @Test
    fun `full Chronicle rendering resets the visible sequence for each round`() {
        val entries = listOf(
            GameEntry.RoundRevealed(
                sequence = 17,
                roundNumber = 1,
                cardName = "first",
                cardType = RoundCardType.CULTIVATION,
                firstEffect = GameEffect.GAIN_ONE_VP,
                secondEffect = GameEffect.GAIN_ONE_VP
            ),
            GameEntry.Marker(sequence = 18, message = "one"),
            GameEntry.Marker(sequence = 19, message = "two"),
            GameEntry.RoundRevealed(
                sequence = 20,
                roundNumber = 2,
                cardName = "second",
                cardType = RoundCardType.BATTLE,
                firstEffect = GameEffect.GAIN_ONE_VP,
                secondEffect = GameEffect.GAIN_ONE_VP
            ),
            GameEntry.Marker(sequence = 21, message = "three")
        )

        val lines = ChronicleTextRenderer.render(entries).lines()

        assertEquals("01.001  ROUND 1 REVEAL CULTIVATION: first [GAIN_ONE_VP | GAIN_ONE_VP]", lines[0])
        assertEquals("01.002  MARKER one", lines[1])
        assertEquals("01.003  MARKER two", lines[2])
        assertEquals("", lines[3])
        assertEquals("02.001  ROUND 2 REVEAL BATTLE: second [GAIN_ONE_VP | GAIN_ONE_VP]", lines[4])
        assertEquals("02.002  MARKER three", lines[5])
    }


    @Test
    fun `round completion renders compact player summaries between rounds`() {
        val entries = listOf(
            GameEntry.RoundRevealed(
                sequence = 1,
                roundNumber = 1,
                cardName = "first",
                cardType = RoundCardType.CULTIVATION,
                firstEffect = GameEffect.GAIN_ONE_VP,
                secondEffect = GameEffect.GAIN_ONE_VP
            ),
            GameEntry.RoundCompleted(
                sequence = 2,
                roundNumber = 1,
                cardName = "first",
                cardType = RoundCardType.CULTIVATION,
                playerSummaries = listOf(
                    PlayerRoundSummarySnapshot(
                        playerId = PlayerId(1),
                        graftedPlants = listOf(
                            GraftedPlantSnapshot(PlantType.FLOWER, 14),
                            GraftedPlantSnapshot(PlantType.ROOT, 5),
                            GraftedPlantSnapshot(PlantType.VINE, 11),
                            GraftedPlantSnapshot(PlantType.FLOWER, 11),
                            GraftedPlantSnapshot(PlantType.ROOT, 5)
                        ),
                        supplyDice = listOf(DieSides.D4, DieSides.D4, DieSides.D6),
                        discardDice = listOf(DieSides.D8, DieSides.D10),
                        beeCount = 1,
                        wormCount = 0,
                        waterCount = 2,
                        mulchDice = listOf(DieSides.D6, DieSides.D12),
                        wispCount = 3,
                        butterflies = listOf(Butterfly.GREEN, Butterfly.YELLOW)
                    ),
                    PlayerRoundSummarySnapshot(
                        playerId = PlayerId(2),
                        graftedPlants = emptyList(),
                        supplyDice = emptyList(),
                        discardDice = listOf(DieSides.D4),
                        beeCount = 0,
                        wormCount = 1,
                        waterCount = 0,
                        mulchDice = emptyList(),
                        wispCount = 0,
                        butterflies = emptyList()
                    )
                )
            ),
            GameEntry.RoundRevealed(
                sequence = 3,
                roundNumber = 2,
                cardName = "second",
                cardType = RoundCardType.BATTLE,
                firstEffect = GameEffect.GAIN_ONE_VP,
                secondEffect = GameEffect.GAIN_ONE_VP
            )
        )

        val lines = ChronicleTextRenderer.render(entries).lines()

        assertEquals("01.001  ROUND 1 REVEAL CULTIVATION: first [GAIN_ONE_VP | GAIN_ONE_VP]", lines[0])
        assertEquals("01.002  ROUND 1 COMPLETE CULTIVATION: first", lines[1])
        assertEquals("", lines[2])
        assertEquals(
            "01.003  P1 S=2D4,1D6 D=1D8,1D10 B=1 Wa=2 M=2[D6,D12] Wi=3 BF=2[GB,YB] G[2R5 1V11 1F11 1F14]",
            lines[3]
        )
        assertEquals("01.004  P2 S=- D=1D4 W=1 G[]", lines[4])
        assertEquals("", lines[5])
        assertEquals("02.001  ROUND 2 REVEAL BATTLE: second [GAIN_ONE_VP | GAIN_ONE_VP]", lines[6])
    }


    @Test
    fun `compact Chronicle combines opening hand and rewards and hides decisions`() {
        val entries = openingDrawEntries()

        val lines = ChronicleTextRenderer.render(entries).lines()

        assertEquals(
            "01.001  ROUND 1 REVEAL CULTIVATION: first [GAIN_ONE_VP | GAIN_ONE_VP]",
            lines[0]
        )
        assertEquals("01.002  P1 HAND D6=1 D8=5 D10=2", lines[1])
        assertEquals("01.003  P1 REWARD BEE WISP_GAIN_GREEN", lines[2])
        assertEquals("01.004  P2 HAND D4=3 D6=4 D8=6", lines[3])
        assertEquals(
            "01.005  P1 ROLL D6=4 reason=DRAW",
            lines[4]
        )
        assertEquals(-1, lines.indexOfFirst { "DECISION" in it })
        assertEquals(-1, lines.indexOfFirst { "OPENING DRAW" in it })
    }

    @Test
    fun `compact Chronicle omits redundant Cultivation Main Draw after the roll`() {
        val entries = openingDrawEntries() + GameEntry.MainAction(
            sequence = 15,
            playerId = PlayerId(1),
            phase = ChroniclePhase.CULTIVATION,
            action = MainActionKind.DRAW,
            actionNumber = 1,
            battleStage = null
        )

        val lines = ChronicleTextRenderer.render(entries).lines()
        val detailLines = ChronicleTextRenderer.render(entries, detail = true).lines()

        assertEquals(1, lines.count { "P1 ROLL D6=4 reason=DRAW" in it })
        assertEquals(0, lines.count { "CULTIVATION MAIN DRAW" in it })
        assertEquals(1, detailLines.count { "CULTIVATION MAIN DRAW #1" in it })
    }

    @Test
    fun `detail Chronicle preserves line by line opening draw and decisions`() {
        val lines = ChronicleTextRenderer.render(openingDrawEntries(), detail = true).lines()

        assertEquals(
            "01.002  P1 ROLL D6=1 reason=DRAW rewards=NORMAL",
            lines[1]
        )
        assertEquals(
            "01.003  P1 DECISION BEE score=70 (base=50, +20 Ordinary Critter reward preference)",
            lines[2]
        )
        assertEquals("01.004  P1 ROLL REWARD CRITTER_GAINED critter=BEE", lines[3])
        assertEquals(
            "01.008  P1 CULTIVATION OPENING DRAW complete (3 dice)",
            lines[7]
        )
    }


    private fun plant(
        name: String,
        title: String,
        type: PlantType,
        cost: Int
    ): PlantCard =
        PlantCard(
            quantity = 1,
            name = name,
            title = title,
            type = type,
            cost = cost,
            lineIcon = null,
            vpIcon = "",
            typeIcon = "",
            fgColor = "",
            textColor = "",
            fullImage = "",
            backgroundImage = "",
            cardBackgroundImage = "",
            effect = GameEffect.GAIN_ONE_VP,
            scoringRule = PlantScoringRule.Fixed(1)
        )

    private fun openingDrawEntries(): List<GameEntry> =
        listOf(
            GameEntry.RoundRevealed(
                sequence = 1,
                roundNumber = 1,
                cardName = "first",
                cardType = RoundCardType.CULTIVATION,
                firstEffect = GameEffect.GAIN_ONE_VP,
                secondEffect = GameEffect.GAIN_ONE_VP
            ),
            GameEntry.DieRolled(
                sequence = 2,
                playerId = PlayerId(1),
                sides = 6,
                value = 1,
                rewardPolicy = ChronicleRollRewardPolicy.NORMAL,
                reason = RollReason.DRAW
            ),
            GameEntry.DecisionReasoning(
                sequence = 3,
                playerId = PlayerId(1),
                choiceLabel = "BEE",
                baseScore = 50,
                adjustments = listOf(
                    DecisionScoreAdjustmentSnapshot(
                        amount = 20,
                        reason = "Ordinary Critter reward preference"
                    )
                ),
                total = 70
            ),
            GameEntry.RollReward(
                sequence = 4,
                playerId = PlayerId(1),
                kind = RollRewardKind.CRITTER_GAINED,
                critter = Critter.BEE,
                wispName = null
            ),
            GameEntry.DieRolled(
                sequence = 5,
                playerId = PlayerId(1),
                sides = 8,
                value = 5,
                rewardPolicy = ChronicleRollRewardPolicy.NORMAL,
                reason = RollReason.DRAW
            ),
            GameEntry.DieRolled(
                sequence = 6,
                playerId = PlayerId(1),
                sides = 10,
                value = 2,
                rewardPolicy = ChronicleRollRewardPolicy.NORMAL,
                reason = RollReason.DRAW
            ),
            GameEntry.RollReward(
                sequence = 7,
                playerId = PlayerId(1),
                kind = RollRewardKind.WISP_GAINED,
                critter = null,
                wispName = "Wisp_Gain_Green"
            ),
            GameEntry.OpeningDrawCompleted(
                sequence = 8,
                phase = ChroniclePhase.CULTIVATION,
                playerId = PlayerId(1),
                count = 3
            ),
            GameEntry.DieRolled(
                sequence = 9,
                playerId = PlayerId(2),
                sides = 4,
                value = 3,
                rewardPolicy = ChronicleRollRewardPolicy.NORMAL,
                reason = RollReason.DRAW
            ),
            GameEntry.DieRolled(
                sequence = 10,
                playerId = PlayerId(2),
                sides = 6,
                value = 4,
                rewardPolicy = ChronicleRollRewardPolicy.NORMAL,
                reason = RollReason.DRAW
            ),
            GameEntry.DieRolled(
                sequence = 11,
                playerId = PlayerId(2),
                sides = 8,
                value = 6,
                rewardPolicy = ChronicleRollRewardPolicy.NORMAL,
                reason = RollReason.DRAW
            ),
            GameEntry.OpeningDrawCompleted(
                sequence = 12,
                phase = ChroniclePhase.CULTIVATION,
                playerId = PlayerId(2),
                count = 3
            ),
            GameEntry.DecisionReasoning(
                sequence = 13,
                playerId = PlayerId(1),
                choiceLabel = "Draw",
                baseScore = 49,
                adjustments = emptyList(),
                total = 49
            ),
            GameEntry.DieRolled(
                sequence = 14,
                playerId = PlayerId(1),
                sides = 6,
                value = 4,
                rewardPolicy = ChronicleRollRewardPolicy.NORMAL,
                reason = RollReason.DRAW
            )
        )



    @Test
    fun `compact Chronicle combines Round Effect and affected Mulch die`() {
        val entries = listOf(
            GameEntry.RoundRevealed(
                sequence = 1,
                roundNumber = 1,
                cardName = "Resource_Compost_Mulch",
                cardType = RoundCardType.CULTIVATION,
                firstEffect = GameEffect.UPGRADE_DIE_FROM_HAND,
                secondEffect = GameEffect.MULCH_DIE_FROM_HAND
            ),
            GameEntry.MainAction(
                sequence = 2,
                playerId = PlayerId(1),
                phase = ChroniclePhase.CULTIVATION,
                action = MainActionKind.ROUND_EFFECT_2,
                actionNumber = 1,
                battleStage = null,
                decisionProbabilityPercent = 80
            ),
            GameEntry.EffectResolved(
                sequence = 3,
                playerId = PlayerId(1),
                effect = GameEffect.MULCH_DIE_FROM_HAND,
                sourceKind = EffectSourceKind.ROUND,
                sourceName = "Resource_Compost_Mulch:SECOND",
                phase = ChroniclePhase.CULTIVATION,
                hierarchyDepth = 1
            ),
            GameEntry.MulchStored(
                sequence = 4,
                playerId = PlayerId(1),
                sides = DieSides.D4,
                value = 1,
                fromDiscard = false,
                hierarchyDepth = 2
            )
        )

        val lines = ChronicleTextRenderer.render(entries).lines()

        assertEquals(
            "01.002  P1 ROUND EFFECT_2 MULCH_DIE_FROM_HAND (80%) D4=1",
            lines[1]
        )
        assertEquals(2, lines.count { it.isNotEmpty() })
    }


    @Test
    fun `compact Chronicle shows the recorded Compost willingness percentage`() {
        val entries = listOf(
            GameEntry.RoundRevealed(
                sequence = 1,
                roundNumber = 1,
                cardName = "Resource_Compost_Mulch",
                cardType = RoundCardType.CULTIVATION,
                firstEffect = GameEffect.UPGRADE_DIE_FROM_HAND,
                secondEffect = GameEffect.MULCH_DIE_FROM_HAND
            ),
            GameEntry.MainAction(
                sequence = 2,
                playerId = PlayerId(2),
                phase = ChroniclePhase.CULTIVATION,
                action = MainActionKind.ROUND_EFFECT_1,
                actionNumber = 1,
                battleStage = null,
                decisionProbabilityPercent = 75
            ),
            GameEntry.EffectResolved(
                sequence = 3,
                playerId = PlayerId(2),
                effect = GameEffect.UPGRADE_DIE_FROM_HAND,
                sourceKind = EffectSourceKind.ROUND,
                sourceName = "Resource_Compost_Mulch:FIRST",
                phase = ChroniclePhase.CULTIVATION,
                hierarchyDepth = 1
            ),
            GameEntry.Upgrade(
                sequence = 4,
                playerId = PlayerId(2),
                from = DieSides.D4,
                to = DieSides.D6,
                destination = UpgradeDestination.DISCARD,
                fromValue = 1,
                hierarchyDepth = 2
            )
        )

        val lines = ChronicleTextRenderer.render(entries).lines()

        assertEquals(
            "01.002  P2 ROUND EFFECT_1 UPGRADE_DIE_FROM_HAND (75%) D4=1 -> D6",
            lines[1]
        )
        assertEquals(2, lines.count { it.isNotEmpty() })
    }


    @Test
    fun `compact Chronicle combines Overgrowth upgrade roll and willingness percentage`() {
        val entries = listOf(
            GameEntry.RoundRevealed(
                sequence = 1,
                roundNumber = 1,
                cardName = "Resource_Compost_Mulch",
                cardType = RoundCardType.CULTIVATION,
                firstEffect = GameEffect.UPGRADE_DIE_FROM_HAND,
                secondEffect = GameEffect.MULCH_DIE_FROM_HAND
            ),
            GameEntry.SupportAction(
                sequence = 2,
                playerId = PlayerId(1),
                phase = ChroniclePhase.CULTIVATION,
                action = SupportActionKind.WISP,
                row = null,
                wispUsePercentage = 5
            ),
            GameEntry.EffectResolved(
                sequence = 3,
                playerId = PlayerId(1),
                effect = GameEffect.UPGRADE_DIE_TWO_STEPS_SKIP_MISSING_AND_USE_NOW,
                sourceKind = EffectSourceKind.WISP,
                sourceName = "Wisp_Upgrade_Die",
                phase = ChroniclePhase.CULTIVATION,
                hierarchyDepth = 1
            ),
            GameEntry.Upgrade(
                sequence = 4,
                playerId = PlayerId(1),
                from = DieSides.D4,
                to = DieSides.D8,
                destination = UpgradeDestination.HAND,
                fromValue = 2,
                hierarchyDepth = 2
            ),
            GameEntry.DieRolled(
                sequence = 5,
                playerId = PlayerId(1),
                sides = 8,
                value = 7,
                rewardPolicy = ChronicleRollRewardPolicy.NORMAL,
                reason = RollReason.ROLL,
                hierarchyDepth = 2
            )
        )

        val lines = ChronicleTextRenderer.render(entries).lines()

        assertEquals(
            "01.002  P1 Wisp_Upgrade_Die (5%) D4 -> D8=7",
            lines[1]
        )
        assertEquals(2, lines.count { it.isNotEmpty() })
    }

    @Test
    fun `compact Chronicle annotates Buy Order leader with highest Hand die`() {
        val entries = listOf(
            GameEntry.RoundRevealed(
                sequence = 1,
                roundNumber = 1,
                cardName = "first",
                cardType = RoundCardType.CULTIVATION,
                firstEffect = GameEffect.GAIN_ONE_VP,
                secondEffect = GameEffect.GAIN_ONE_VP
            ),
            GameEntry.BuyOrder(
                sequence = 2,
                order = listOf(PlayerId(1), PlayerId(2), PlayerId(3), PlayerId(4)),
                leaderDie = BuyOrderLeadDieSnapshot(DieSides.D8, 7)
            )
        )

        val lines = ChronicleTextRenderer.render(entries).lines()

        assertEquals("01.002  BUY ORDER P1(D8=7) -> P2 -> P3 -> P4", lines[1])
    }


    @Test
    fun `compact Chronicle appends concrete Sunlight die change to Round Effect`() {
        val entries = listOf(
            GameEntry.RoundRevealed(
                sequence = 1,
                roundNumber = 1,
                cardName = "Resource_Sunlight_Water",
                cardType = RoundCardType.CULTIVATION,
                firstEffect = GameEffect.RAISE_DIE_PLUS_3,
                secondEffect = GameEffect.GAIN_WATER_TOKEN
            ),
            GameEntry.MainAction(
                sequence = 2,
                playerId = PlayerId(3),
                phase = ChroniclePhase.CULTIVATION,
                action = MainActionKind.ROUND_EFFECT_1,
                actionNumber = 1,
                battleStage = null,
                decisionProbabilityPercent = 50
            ),
            GameEntry.EffectResolved(
                sequence = 3,
                playerId = PlayerId(3),
                effect = GameEffect.RAISE_DIE_PLUS_3,
                sourceKind = EffectSourceKind.ROUND,
                sourceName = "Resource_Sunlight_Water:FIRST",
                phase = ChroniclePhase.CULTIVATION,
                hierarchyDepth = 1
            ),
            GameEntry.DieValueChanged(
                sequence = 4,
                playerId = PlayerId(3),
                effect = GameEffect.RAISE_DIE_PLUS_3,
                sides = DieSides.D4,
                before = 1,
                after = 4,
                hierarchyDepth = 2
            )
        )

        val lines = ChronicleTextRenderer.render(entries).lines()

        assertEquals(
            "01.002  P3 ROUND EFFECT_1 RAISE_DIE_PLUS_3 (50%) (D4=1->4)",
            lines[1]
        )
        assertEquals(2, lines.count { it.isNotEmpty() })
    }


    @Test
    fun `compact Chronicle combines Pocketed Spark effect and stored discard die`() {
        val entries = listOf(
            GameEntry.RoundRevealed(
                sequence = 1,
                roundNumber = 1,
                cardName = "first",
                cardType = RoundCardType.CULTIVATION,
                firstEffect = GameEffect.GAIN_ONE_VP,
                secondEffect = GameEffect.GAIN_ONE_VP
            ),
            GameEntry.SupportAction(
                sequence = 2,
                playerId = PlayerId(2),
                phase = ChroniclePhase.CULTIVATION,
                action = SupportActionKind.WISP,
                row = null
            ),
            GameEntry.EffectResolved(
                sequence = 3,
                playerId = PlayerId(2),
                effect = GameEffect.GAIN_MULCH_AND_STORE_DIE_FROM_DISCARD,
                sourceKind = EffectSourceKind.WISP,
                sourceName = "Wisp_Mulch_Die",
                phase = ChroniclePhase.CULTIVATION,
                hierarchyDepth = 1
            ),
            GameEntry.MulchStored(
                sequence = 4,
                playerId = PlayerId(2),
                sides = DieSides.D6,
                value = 2,
                fromDiscard = true,
                hierarchyDepth = 2
            )
        )

        val lines = ChronicleTextRenderer.render(entries).lines()

        assertEquals("01.002  P2 Wisp_Mulch_Die D6=2 -> MULCH", lines[1])
        assertEquals(0, lines.count { "SUPPORT WISP" in it })
        assertEquals(2, lines.count { it.isNotEmpty() })
    }

    @Test
    fun `compact Chronicle appends an immediate roll reward to the roll line`() {
        val entries = listOf(
            GameEntry.RoundRevealed(
                sequence = 1,
                roundNumber = 1,
                cardName = "first",
                cardType = RoundCardType.CULTIVATION,
                firstEffect = GameEffect.GAIN_ONE_VP,
                secondEffect = GameEffect.GAIN_ONE_VP
            ),
            GameEntry.OpeningDrawCompleted(
                sequence = 2,
                phase = ChroniclePhase.CULTIVATION,
                playerId = PlayerId(1),
                count = 0
            ),
            GameEntry.MainAction(
                sequence = 3,
                playerId = PlayerId(1),
                phase = ChroniclePhase.CULTIVATION,
                action = MainActionKind.DRAW,
                actionNumber = 1,
                battleStage = null
            ),
            GameEntry.DieRolled(
                sequence = 4,
                playerId = PlayerId(1),
                sides = 8,
                value = 2,
                rewardPolicy = ChronicleRollRewardPolicy.NORMAL,
                reason = RollReason.DRAW,
                hierarchyDepth = 1
            ),
            GameEntry.RollReward(
                sequence = 5,
                playerId = PlayerId(1),
                kind = RollRewardKind.WISP_GAINED,
                critter = null,
                wispName = "Wisp_Gain_Green",
                hierarchyDepth = 1
            )
        )

        val lines = ChronicleTextRenderer.render(entries).lines()

        assertEquals("01.003  P1 ROLL D8=2 reason=DRAW REWARD WISP_GAIN_GREEN", lines[2])
        assertEquals(0, lines.count { "ROLL REWARD" in it })
    }


    @Test
    fun `compact Chronicle suppresses redundant Plant Main Action line`() {
        val entries = listOf(
            GameEntry.RoundRevealed(
                sequence = 1,
                roundNumber = 1,
                cardName = "first",
                cardType = RoundCardType.CULTIVATION,
                firstEffect = GameEffect.GAIN_ONE_VP,
                secondEffect = GameEffect.GAIN_ONE_VP
            ),
            GameEntry.MainAction(
                sequence = 2,
                playerId = PlayerId(3),
                phase = ChroniclePhase.CULTIVATION,
                action = MainActionKind.ACTIVATE_PLANT,
                actionNumber = 1,
                battleStage = null
            ),
            GameEntry.EffectResolved(
                sequence = 3,
                playerId = PlayerId(3),
                effect = GameEffect.SET_LOWEST_VALUE_DIE_TO_MAX,
                sourceKind = EffectSourceKind.PLANT,
                sourceName = "Vine_09_01",
                phase = ChroniclePhase.CULTIVATION,
                hierarchyDepth = 1
            )
        )

        val lines = ChronicleTextRenderer.render(entries).lines()
        val detail = ChronicleTextRenderer.render(entries, detail = true).lines()

        assertEquals(1, lines.count { "EFFECT PLANT Vine_09_01" in it })
        assertEquals(0, lines.count { "MAIN ACTIVATE_PLANT" in it })
        assertEquals(1, detail.count { "MAIN ACTIVATE_PLANT #1" in it })
        assertTrue(detail.any { it.startsWith("01.003    P3 CULTIVATION EFFECT PLANT") })
    }


    @Test
    fun `scoped Battle Draw renders parent before indented roll`() {
        val entries = listOf(
            GameEntry.RoundRevealed(
                sequence = 1,
                roundNumber = 1,
                cardName = "battle",
                cardType = RoundCardType.BATTLE,
                firstEffect = GameEffect.GAIN_ONE_VP,
                secondEffect = GameEffect.GAIN_ONE_VP
            ),
            GameEntry.MainAction(
                sequence = 2,
                playerId = PlayerId(2),
                phase = ChroniclePhase.BATTLE,
                action = MainActionKind.DRAW,
                actionNumber = null,
                battleStage = BattleMainStage.FIRST
            ),
            GameEntry.DieRolled(
                sequence = 3,
                playerId = PlayerId(2),
                sides = 8,
                value = 8,
                rewardPolicy = ChronicleRollRewardPolicy.NORMAL,
                reason = RollReason.DRAW,
                hierarchyDepth = 1
            )
        )

        val lines = ChronicleTextRenderer.render(entries).lines()

        assertEquals("01.002  P2 BATTLE MAIN DRAW stage=FIRST", lines[1])
        assertEquals("01.003    P2 ROLL D8=8 reason=DRAW", lines[2])
    }

    @Test
    fun `Battle Order shows each players highest opening die and omits opening count`() {
        val entries = listOf(
            GameEntry.RoundRevealed(
                sequence = 1,
                roundNumber = 1,
                cardName = "battle",
                cardType = RoundCardType.BATTLE,
                firstEffect = GameEffect.GAIN_ONE_VP,
                secondEffect = GameEffect.GAIN_ONE_VP
            ),
            GameEntry.BattleOrder(
                sequence = 2,
                order = listOf(PlayerId(1), PlayerId(4), PlayerId(2), PlayerId(3)),
                initialDiceCount = 12,
                highestDice = listOf(
                    BattleOrderHighDieSnapshot(PlayerId(1), DieSides.D8, 6),
                    BattleOrderHighDieSnapshot(PlayerId(4), DieSides.D12, 5),
                    BattleOrderHighDieSnapshot(PlayerId(2), DieSides.D6, 5),
                    BattleOrderHighDieSnapshot(PlayerId(3), DieSides.D4, 3)
                )
            )
        )

        val lines = ChronicleTextRenderer.render(entries).lines()

        assertEquals(
            "01.002  BATTLE ORDER P1(D8=6) -> P4(D12=5) -> P2(D6=5) -> P3(D4=3)",
            lines[1]
        )
    }

    @Test
    fun `standalone entry rendering retains global sequence because round context is unavailable`() {
        assertEquals(
            "0042  MARKER standalone",
            ChronicleTextRenderer.render(GameEntry.Marker(sequence = 42, message = "standalone"))
        )
    }
}
