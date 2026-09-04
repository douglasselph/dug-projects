package dugsolutions.leaf.v35.battle

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.game.GameEngineTestFixture
import dugsolutions.leaf.v35.game.operation.WoundResolution
import dugsolutions.leaf.v35.game.operation.WoundResolver
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.Player
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.creature.CreatureCard
import dugsolutions.leaf.v35.player.decision.DecisionDirector
import dugsolutions.leaf.v35.player.dice.PlayerDice
import dugsolutions.leaf.v35.random.die.Die
import dugsolutions.leaf.v35.tokens.Critter
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class StrikeResolverTest {

    @Test
    fun singleWinnerGetsTwoVpPlusOneForEveryLoserWounded() {
        val p1 = player(1, die(20, 10))
        val p2 = player(2, die(20, 8))
        val p3 = player(3, die(20, 3))
        val game = GameEngineTestFixture.game(players = listOf(p1, p2, p3))
        val state = BattleState(listOf(p1, p2, p3))
        state.grid.placeDie(p1, StrikeRow.TOP, p1.dice.hand.single())
        state.grid.placeDie(p2, StrikeRow.TOP, p2.dice.hand.single())
        state.grid.placeDie(p3, StrikeRow.TOP, p3.dice.hand.single())

        // Bee-loved style current value is used in the Strike total.
        game.grove.critters.remove(Critter.BEE)
        p1.critters.add(Critter.BEE)
        p1.critterValues.setForRound(Critter.BEE, 4)
        state.grid.placeCritter(p1, StrikeRow.TOP, Critter.BEE)

        // Give one wounded player a real legal Plant wound target.
        val woundedPlant = graftFaceUpRoot(game, p3)

        val result = resolver(game).resolveRow(game, state, StrikeRow.TOP)

        assertEquals(listOf(PlayerId(1)), result.winnerIds)
        assertEquals(listOf(PlayerId(2), PlayerId(3)), result.woundedPlayerIds)
        assertEquals(4, result.vpPerWinner)
        assertEquals(4, p1.vp)
        assertEquals(0, p2.vp)
        assertEquals(0, p3.vp)
        assertEquals(
            listOf(14, 8, 3),
            result.totals.map { it.total }
        )
        assertEquals(4, result.totals.first().critterTotal)
        assertIs<WoundResolution.NoLegalTarget>(result.wounds[0].resolution)
        assertIs<WoundResolution.Flipped>(result.wounds[1].resolution)
        assertTrue(p3.creature.get(woundedPlant.id)!!.isFaceDown)
    }

    @Test
    fun tiedHighPlayersAllWinAndEachGetsSameWoundBonus() {
        val p1 = player(1, die(12, 10))
        val p2 = player(2, die(12, 10))
        val p3 = player(3, die(8, 4))
        val game = GameEngineTestFixture.game(players = listOf(p1, p2, p3))
        val state = placedTop(p1, p2, p3)

        val result = resolver(game).resolveRow(game, state, StrikeRow.TOP)

        assertEquals(listOf(PlayerId(1), PlayerId(2)), result.winnerIds)
        assertEquals(listOf(PlayerId(3)), result.woundedPlayerIds)
        assertEquals(3, result.vpPerWinner)
        assertEquals(3, p1.vp)
        assertEquals(3, p2.vp)
        assertEquals(0, p3.vp)
    }

    @Test
    fun everyoneTiedMeansNoWinnerNoVpAndNoWounds() {
        val p1 = player(1, die(6, 5))
        val p2 = player(2, die(8, 5))
        val p3 = player(3, die(10, 5))
        val game = GameEngineTestFixture.game(players = listOf(p1, p2, p3))
        val state = placedTop(p1, p2, p3)

        val result = resolver(game).resolveRow(game, state, StrikeRow.TOP)

        assertTrue(result.winnerIds.isEmpty())
        assertTrue(result.wounds.isEmpty())
        assertEquals(0, result.vpPerWinner)
        assertEquals(listOf(0, 0, 0), listOf(p1.vp, p2.vp, p3.vp))
    }

    @Test
    fun loserLessThanFiveBehindIsNotWounded() {
        val p1 = player(1, die(10, 9))
        val p2 = player(2, die(10, 5))
        val game = GameEngineTestFixture.game(players = listOf(p1, p2))
        val state = placedTop(p1, p2)

        val result = resolver(game).resolveRow(game, state, StrikeRow.TOP)

        assertEquals(listOf(PlayerId(1)), result.winnerIds)
        assertTrue(result.wounds.isEmpty())
        assertEquals(2, p1.vp)
    }

    @Test
    fun withdrawnPlayerIsExcludedFromTotalsWoundsAndWinnerCalculation() {
        val p1 = player(1, die(20, 12))
        val p2 = player(2, die(12, 6))
        val p3 = player(3, die(6, 1))
        val game = GameEngineTestFixture.game(players = listOf(p1, p2, p3))
        val state = placedTop(p1, p2, p3)

        // Withdrawal state is authoritative even if stale Grid contents remain.
        state.grid.withdrawPlayer(p1.id, StrikeRow.TOP)

        val result = resolver(game).resolveRow(game, state, StrikeRow.TOP)

        assertEquals(
            listOf(PlayerId(2), PlayerId(3)),
            result.totals.map { it.playerId }
        )
        assertEquals(listOf(PlayerId(2)), result.winnerIds)
        assertEquals(listOf(PlayerId(3)), result.woundedPlayerIds)
        assertEquals(3, result.vpPerWinner)
        assertEquals(0, p1.vp)
        assertEquals(3, p2.vp)
    }

    @Test
    fun oneRemainingParticipantWinsInsteadOfBeingTreatedAsAnAllPlayerTie() {
        val p1 = player(1, die(8, 8))
        val p2 = player(2, die(6, 2))
        val game = GameEngineTestFixture.game(players = listOf(p1, p2))
        val state = placedTop(p1, p2)
        state.grid.withdrawPlayer(p1.id, StrikeRow.TOP)

        val result = resolver(game).resolveRow(game, state, StrikeRow.TOP)

        assertEquals(listOf(PlayerId(2)), result.winnerIds)
        assertEquals(2, result.vpPerWinner)
        assertEquals(2, p2.vp)
    }

    @Test
    fun noRemainingParticipantsProducesNoContestWithoutClosingTheRow() {
        val p1 = player(1, die(8, 8))
        val p2 = player(2, die(6, 2))
        val game = GameEngineTestFixture.game(players = listOf(p1, p2))
        val state = placedTop(p1, p2)
        state.grid.withdrawPlayer(p1.id, StrikeRow.TOP)
        state.grid.withdrawPlayer(p2.id, StrikeRow.TOP)

        val result = resolver(game).resolveRow(game, state, StrikeRow.TOP)

        assertTrue(result.totals.isEmpty())
        assertTrue(result.winnerIds.isEmpty())
        assertTrue(result.wounds.isEmpty())
        assertEquals(0, result.vpPerWinner)
        assertTrue(!state.grid.isRowClosed(StrikeRow.TOP))
    }

    @Test
    fun resolveAllUsesTopToBottomAndSkipsClosedRows() {
        val p1 = player(1, die(6, 6), die(6, 5), die(6, 4))
        val p2 = player(2, die(6, 3), die(6, 2), die(6, 1))
        val game = GameEngineTestFixture.game(players = listOf(p1, p2))
        val state = BattleState(listOf(p1, p2))
        state.placeInitialHands()
        state.grid.closeRow(StrikeRow.MIDDLE)

        val result = resolver(game).resolveAll(game, state)

        assertEquals(
            listOf(StrikeRow.TOP, StrikeRow.BOTTOM),
            result.strikes.map { it.row }
        )
        // Strike resolution itself never removes dice from the Grid.
        assertEquals(6, state.grid.diePlacements.size)
    }

    private fun resolver(game: dugsolutions.leaf.v35.game.Game) =
        StrikeResolver(
            WoundResolver(
                grove = game.grove,
                chronicle = game.chronicle
            )
        )

    private fun placedTop(vararg players: Player): BattleState =
        BattleState(players.toList()).also { state ->
            players.forEach { player ->
                state.grid.placeDie(
                    player,
                    StrikeRow.TOP,
                    player.dice.hand.single()
                )
            }
        }

    private fun graftFaceUpRoot(
        game: dugsolutions.leaf.v35.game.Game,
        player: Player
    ): CreatureCard {
        val stack = game.grove.plantMarket.stacks.first {
            it.card.type == PlantType.ROOT
        }
        val card = requireNotNull(game.grove.plantMarket.take(stack.card))
        val grafted = player.creature.graft(
            card,
            player.creature.legalPlacements(card).first()
        )
        player.creature.faceUp(grafted.id)
        return player.creature.get(grafted.id)!!
    }

    private fun player(
        id: Int,
        vararg dice: Die
    ): Player =
        Player(
            id = PlayerId(id),
            decisions = DecisionDirector.baseline(),
            dice = PlayerDice(hand = dice.toList())
        )

    private fun die(sides: Int, value: Int): Die =
        object : Die(sides) {
            init { adjustTo(value) }
            override fun roll(): Die = this
        }
}
