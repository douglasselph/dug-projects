package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.battle.BattleSupportAction
import dugsolutions.leaf.v35.player.decision.battle.BattleTurnAction
import dugsolutions.leaf.v35.player.decision.context.*
import dugsolutions.leaf.v35.player.decision.support.HandDieChoice
import dugsolutions.leaf.v35.player.decision.support.SupportAction
import dugsolutions.leaf.v35.random.die.DieSides
import dugsolutions.leaf.v35.tokens.Butterfly
import dugsolutions.leaf.v35.tokens.Critter
import dugsolutions.leaf.v35.tokens.Token
import dugsolutions.leaf.v35.wisp.domain.WispCard
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BattleSupportCapacityAssessorTest {
    private val actor = PlayerId(0)
    private val opponentA = PlayerId(1)
    private val opponentB = PlayerId(2)
    private val doneOpponent = PlayerId(3)
    private val assessor = BattleSupportCapacityAssessor()

    @Test
    fun `own capacity counts resource instances rather than legal target multiplicity`() {
        val wispA = wisp("A")
        val wispB = wisp("B")
        val immediate = wisp("Immediate", playImmediately = true)
        val context = context(
            selfBoard = board(
                actor,
                bees = 3,
                worms = 2,
                water = 3,
                mulch = listOf(mulch(0, DieSides.D6), mulch(1, DieSides.D8), mulch(2, null)),
                butterflies = listOf(
                    ButterflyView(Butterfly.GREEN, true),
                    ButterflyView(Butterfly.YELLOW, true),
                    ButterflyView(Butterfly.RED, false)
                )
            ),
            selfWisps = listOf(wispView(0, wispA), wispView(1, wispB), wispView(2, immediate)),
            opponents = emptyList(),
            battle = battle(listOf(actor))
        )
        val dieA = HandDieChoice(0, 6, 2)
        val dieB = HandDieChoice(1, 8, 5)
        val legal = listOf(
            shared(SupportAction.PlayWisp(wispA)),
            shared(SupportAction.PlayWisp(wispB)),
            shared(SupportAction.UseWaterRefresh),
            shared(SupportAction.UseWaterReroll(dieA)),
            shared(SupportAction.UseWaterReroll(dieB)),
            shared(SupportAction.UseMulch(Token.MULCH(DieSides.D6))),
            shared(SupportAction.UseMulch(Token.MULCH(DieSides.D8))),
            shared(SupportAction.UseWormFlip(dugsolutions.leaf.v35.player.creature.CreatureCardId(1))),
            shared(SupportAction.UseWormFlip(dugsolutions.leaf.v35.player.creature.CreatureCardId(2))),
            critter(Critter.WORM, StrikeRow.TOP),
            critter(Critter.WORM, StrikeRow.MIDDLE),
            critter(Critter.BEE, StrikeRow.TOP),
            critter(Critter.BEE, StrikeRow.MIDDLE),
            critter(Critter.BEE, StrikeRow.BOTTOM),
            shared(SupportAction.UseButterfly(Butterfly.GREEN, dieA)),
            shared(SupportAction.UseButterfly(Butterfly.GREEN, dieB)),
            shared(SupportAction.UseButterfly(Butterfly.YELLOW, dieA)),
            shared(SupportAction.UseButterfly(Butterfly.YELLOW, dieB))
        )

        val capacity = assessor(context, legal).own

        assertEquals(2, capacity.wispMoves)
        assertEquals(3, capacity.waterMoves)
        assertEquals(2, capacity.mulchMoves)
        assertEquals(2, capacity.wormMoves)
        assertEquals(3, capacity.beeMoves)
        assertEquals(2, capacity.butterflyMoves)
        assertEquals(14, capacity.totalMoves)
    }

    @Test
    fun `public opponent capacity uses visible resources and hidden Wisp count`() {
        val context = context(
            selfBoard = board(actor),
            opponents = listOf(
                OpponentView(
                    board = board(
                        opponentA,
                        bees = 2,
                        worms = 1,
                        water = 1,
                        mulch = listOf(mulch(0, DieSides.D6), mulch(1, null)),
                        butterflies = listOf(
                            ButterflyView(Butterfly.GREEN, true),
                            ButterflyView(Butterfly.YELLOW, false)
                        )
                    ),
                    wispCount = 3
                )
            ),
            battle = battle(
                listOf(actor, opponentA),
                diceByPlayer = mapOf(opponentA to 1)
            )
        )

        val capacity = assessor(context, emptyList()).opponents.getValue(opponentA)

        assertEquals(3, capacity.wispMoves)
        assertEquals(1, capacity.waterMoves)
        assertEquals(1, capacity.mulchMoves)
        assertEquals(1, capacity.wormMoves)
        assertEquals(2, capacity.beeMoves)
        assertEquals(1, capacity.butterflyMoves)
        assertEquals(9, capacity.totalMoves)
    }

    @Test
    fun `relevant Live Threat capacity filters opponents and takes maximum rather than sum`() {
        val context = context(
            selfBoard = board(actor),
            opponents = listOf(
                OpponentView(board(opponentA, bees = 2, worms = 1, water = 1), wispCount = 3),
                OpponentView(
                    board(opponentB, bees = 1, worms = 2, mulch = listOf(mulch(0, DieSides.D4))),
                    wispCount = 0
                ),
                OpponentView(board(doneOpponent, bees = 9, worms = 9, water = 9), wispCount = 9)
            ),
            battle = battle(
                listOf(actor, opponentA, opponentB, doneOpponent),
                done = setOf(doneOpponent)
            )
        )

        val assessment = assessor(
            context = context,
            legalChoices = emptyList(),
            relevantLiveThreatOpponentIds = setOf(opponentA, opponentB)
        )

        assertEquals(7, assessment.relevantLiveThreatOpponents.getValue(opponentA).totalMoves)
        assertEquals(4, assessment.relevantLiveThreatOpponents.getValue(opponentB).totalMoves)
        assertEquals(7, assessment.strongestRelevantOpponentCapacity)
        assertEquals(setOf(opponentA), assessment.strongestRelevantOpponentIds)
        assertEquals(BattleSupportCapacity.ZERO, assessment.opponents.getValue(doneOpponent))
    }

    @Test
    fun `targetless public resources do not become Support moves`() {
        val context = context(
            selfBoard = board(actor),
            opponents = listOf(
                OpponentView(
                    board = board(
                        opponentA,
                        bees = 2,
                        worms = 2,
                        water = 1,
                        mulch = listOf(mulch(0, DieSides.D6)),
                        butterflies = listOf(ButterflyView(Butterfly.PURPLE, true))
                    ),
                    wispCount = 2
                )
            ),
            battle = battle(
                playerIds = listOf(actor, opponentA),
                closed = StrikeRow.entries.toSet()
            )
        )

        val capacity = assessor(context, emptyList()).opponents.getValue(opponentA)

        assertEquals(2, capacity.wispMoves)
        assertEquals(1, capacity.waterMoves)
        assertEquals(0, capacity.mulchMoves)
        assertEquals(0, capacity.wormMoves)
        assertEquals(0, capacity.beeMoves)
        assertEquals(0, capacity.butterflyMoves)
        assertEquals(3, capacity.totalMoves)
    }

    @Test
    fun `Done or unknown players cannot be supplied as relevant Live Threats`() {
        val context = context(
            selfBoard = board(actor),
            opponents = listOf(OpponentView(board(doneOpponent), wispCount = 1)),
            battle = battle(listOf(actor, doneOpponent), done = setOf(doneOpponent))
        )

        assertFailsWith<IllegalArgumentException> {
            assessor(context, emptyList(), setOf(doneOpponent))
        }
        assertFailsWith<IllegalArgumentException> {
            assessor(context, emptyList(), setOf(PlayerId(99)))
        }
    }

    private fun context(
        selfBoard: PlayerBoardView,
        selfWisps: List<WispView> = emptyList(),
        opponents: List<OpponentView>,
        battle: BattleView
    ): DecisionContext =
        DecisionContext.EMPTY.copy(
            self = SelfPlayerView(selfBoard, selfWisps),
            opponents = opponents,
            battle = battle
        )

    private fun board(
        id: PlayerId,
        bees: Int = 0,
        worms: Int = 0,
        water: Int = 0,
        mulch: List<MulchView> = emptyList(),
        butterflies: List<ButterflyView> = emptyList()
    ): PlayerBoardView =
        DecisionContext.EMPTY.self.board.copy(
            id = id,
            bees = bees,
            worms = worms,
            water = water,
            mulch = mulch,
            butterflies = butterflies
        )

    private fun mulch(index: Int, sides: DieSides?): MulchView =
        MulchView(index, sides, pending = false)

    private fun battle(
        playerIds: List<PlayerId>,
        done: Set<PlayerId> = emptySet(),
        diceByPlayer: Map<PlayerId, Int> = emptyMap(),
        closed: Set<StrikeRow> = emptySet()
    ): BattleView =
        BattleView(
            playerOrder = playerIds,
            donePlayerIds = done,
            rows = StrikeRow.entries.map { row ->
                BattleRowView(
                    row = row,
                    closed = row in closed,
                    players = playerIds.map { playerId ->
                        val dieCount = if (row == StrikeRow.TOP) diceByPlayer[playerId] ?: 0 else 0
                        BattlePlayerRowView(
                            playerId = playerId,
                            row = row,
                            dice = List(dieCount) { index -> BattleDieView(index, 6, 1) },
                            critters = emptyList(),
                            dieTotal = dieCount,
                            critterTotal = 0,
                            total = dieCount,
                            withdrawn = false
                        )
                    }
                )
            }
        )

    private fun shared(action: SupportAction): BattleTurnAction =
        BattleTurnAction.Support(BattleSupportAction.Shared(action))

    private fun critter(critter: Critter, row: StrikeRow): BattleTurnAction =
        BattleTurnAction.Support(BattleSupportAction.PlaceCritter(critter, row))

    private fun wisp(name: String, playImmediately: Boolean = false): WispCard =
        WispCard(
            quantity = 1,
            name = name,
            title = name,
            count = 1,
            effect = GameEffect.GAIN_ONE_VP,
            lineIcons = null,
            lineIconsHeight = 0,
            vpIcon = null,
            mainBackdrop = "",
            playImmediately = playImmediately
        )

    private fun wispView(index: Int, card: WispCard): WispView =
        WispView(
            index = index,
            name = card.name,
            title = card.title,
            effect = card.effect,
            playImmediately = card.playImmediately,
            battleOnly = card.battleOnly,
            endGameVp = card.endGameVp
        )
}
