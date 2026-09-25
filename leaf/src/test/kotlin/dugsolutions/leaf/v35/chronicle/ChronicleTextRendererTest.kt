package dugsolutions.leaf.v35.chronicle

import dugsolutions.leaf.v35.chronicle.domain.GameEntry
import dugsolutions.leaf.v35.chronicle.domain.ChroniclePhase
import dugsolutions.leaf.v35.chronicle.domain.ChronicleRollRewardPolicy
import dugsolutions.leaf.v35.chronicle.domain.DecisionScoreAdjustmentSnapshot
import dugsolutions.leaf.v35.chronicle.domain.GraftedPlantSnapshot
import dugsolutions.leaf.v35.chronicle.domain.PlayerRoundSummarySnapshot
import dugsolutions.leaf.v35.chronicle.domain.RollReason
import dugsolutions.leaf.v35.chronicle.domain.RollRewardKind
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.random.die.DieSides
import dugsolutions.leaf.v35.round.domain.RoundCardType
import dugsolutions.leaf.v35.tokens.Butterfly
import dugsolutions.leaf.v35.tokens.Critter
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ChronicleTextRendererTest {

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
            "01.005  P1 ROLL D6=4 reason=DRAW rewards=NORMAL",
            lines[4]
        )
        assertEquals(-1, lines.indexOfFirst { "DECISION" in it })
        assertEquals(-1, lines.indexOfFirst { "OPENING DRAW" in it })
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
    fun `standalone entry rendering retains global sequence because round context is unavailable`() {
        assertEquals(
            "0042  MARKER standalone",
            ChronicleTextRenderer.render(GameEntry.Marker(sequence = 42, message = "standalone"))
        )
    }
}
