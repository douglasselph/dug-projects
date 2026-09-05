package dugsolutions.leaf.v35.player.decision.context

import dugsolutions.leaf.v35.battle.BattleState
import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.chronicle.domain.GameEntry
import dugsolutions.leaf.v35.game.Game
import dugsolutions.leaf.v35.game.GameRoundSetup
import dugsolutions.leaf.v35.player.Player
import dugsolutions.leaf.v35.random.die.Die
import dugsolutions.leaf.v35.round.domain.RoundCardType
import dugsolutions.leaf.v35.tokens.Butterfly
import dugsolutions.leaf.v35.tokens.Critter

/**
 * Converts the live mutable game graph into a deep immutable observation.
 *
 * Internal visibility is deliberate: strategy implementations consume
 * DecisionContext but cannot use this factory as a back door to Game.
 */
internal object DecisionContextFactory {

    fun create(
        game: Game,
        actor: Player,
        battleState: BattleState? = null,
        phaseOverride: RoundCardType? = null
    ): DecisionContext {
        require(game.players.any { it === actor }) {
            "Decision actor must belong to the supplied Game: ${actor.id.value}"
        }

        val round = game.currentRound?.let { card ->
            RoundView(
                number = game.roundNumber,
                name = card.name,
                type = card.type,
                firstEffect = card.firstEffect.effect,
                secondEffect = card.secondEffect.effect
            )
        }
        val phase = phaseOverride ?: round?.type

        return DecisionContext(
            status = game.status,
            phase = phase,
            progress = progress(game, phase),
            round = round,
            self = SelfPlayerView(
                board = boardView(actor),
                wisps = actor.wisps.cards.cards.mapIndexed { index, card ->
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
            ),
            opponents = game.players
                .filter { it !== actor }
                .map { player ->
                    OpponentView(
                        board = boardView(player),
                        wispCount = player.wisps.size
                    )
                },
            grove = groveView(game),
            battle = battleState?.let(::battleView)
        )
    }

    private fun progress(
        game: Game,
        phase: RoundCardType?
    ): GameProgressView {
        val completed = game.chronicle.entries
            .filterIsInstance<GameEntry.RoundCompleted>()
        val cultivationCompleted = completed.count {
            it.cardType == RoundCardType.CULTIVATION
        }
        val battleCompleted = completed.count {
            it.cardType == RoundCardType.BATTLE
        }
        val ordered = game.config.roundSetup as? GameRoundSetup.Ordered
        val totalCultivation = ordered?.cultivationRounds
        val totalBattle = ordered?.battleRounds
        val currentCultivation = if (phase == RoundCardType.CULTIVATION) {
            cultivationCompleted + 1
        } else {
            null
        }
        val currentBattle = if (phase == RoundCardType.BATTLE) {
            battleCompleted + 1
        } else {
            null
        }

        return GameProgressView(
            roundNumber = game.roundNumber,
            roundsCompleted = completed.size,
            totalRounds = game.config.roundSetup.totalRounds,
            roundsRemainingToReveal = game.roundDeck.remaining,
            cultivationRoundsCompleted = cultivationCompleted,
            battleRoundsCompleted = battleCompleted,
            totalCultivationRounds = totalCultivation,
            totalBattleRounds = totalBattle,
            currentCultivationRoundNumber = currentCultivation,
            currentBattleRoundNumber = currentBattle,
            cultivationRoundsRemaining = totalCultivation?.minus(cultivationCompleted),
            battleRoundsRemaining = totalBattle?.minus(battleCompleted),
            isFinalRound = game.hasRevealedFinalRound,
            isFinalCultivationRound = currentCultivation != null &&
                totalCultivation != null && currentCultivation == totalCultivation,
            isFinalBattleRound = currentBattle != null &&
                totalBattle != null && currentBattle == totalBattle
        )
    }

    private fun boardView(player: Player): PlayerBoardView =
        PlayerBoardView(
            id = player.id,
            vp = player.vp,
            supply = diceViews(player.dice.supply),
            hand = diceViews(player.dice.hand),
            discard = diceViews(player.dice.discard),
            bees = player.critters.count(Critter.BEE),
            worms = player.critters.count(Critter.WORM),
            beeValue = player.critterValues.valueOf(Critter.BEE),
            wormValue = player.critterValues.valueOf(Critter.WORM),
            water = player.tokens.waterCount,
            mulch = player.tokens.mulchTokens.mapIndexed { index, token ->
                MulchView(index, token.sides, pending = false)
            },
            pendingMulch = player.tokens.pendingMulchTokens.mapIndexed { index, token ->
                MulchView(index, token.sides, pending = true)
            },
            butterflies = Butterfly.entries
                .filter { it in player.butterflies.all }
                .map { butterfly ->
                    ButterflyView(
                        butterfly = butterfly,
                        isFaceUp = player.butterflies.isFaceUp(butterfly)
                    )
                },
            creature = player.creature.cards.map { creatureCard ->
                CreatureCardView(
                    id = creatureCard.id,
                    name = creatureCard.card.name,
                    title = creatureCard.card.title,
                    type = creatureCard.card.type,
                    cost = creatureCard.card.cost,
                    effect = creatureCard.card.effect,
                    scoringRule = creatureCard.card.scoringRule,
                    side = creatureCard.side,
                    position = creatureCard.position,
                    facing = creatureCard.facing,
                    isSnippable = player.creature.canSnip(creatureCard.id)
                )
            }
        )

    private fun diceViews(dice: List<Die>): List<DieView> =
        dice.mapIndexed { index, die ->
            DieView(index, die.sides, die.value)
        }

    private fun groveView(game: Game): GroveView =
        GroveView(
            plantStacks = game.grove.plantMarket.stacks.map { stack ->
                PlantStackView(
                    name = stack.card.name,
                    title = stack.card.title,
                    type = stack.card.type,
                    cost = stack.card.cost,
                    effect = stack.card.effect,
                    scoringRule = stack.card.scoringRule,
                    remaining = stack.remaining
                )
            },
            graftBed = game.grove.graftBed.counts,
            bees = game.grove.critters.count(Critter.BEE),
            worms = game.grove.critters.count(Critter.WORM),
            water = game.grove.tokens.waterCount,
            mulch = game.grove.tokens.mulchCount,
            butterflies = game.grove.butterflies.all,
            wispDeckRemaining = game.grove.wispDeck.remaining
        )

    private fun battleView(state: BattleState): BattleView =
        BattleView(
            playerOrder = state.playerIdsInBattleOrder,
            rows = StrikeRow.entries.map { row ->
                BattleRowView(
                    row = row,
                    closed = state.grid.isRowClosed(row),
                    players = state.playersInBattleOrder.map { player ->
                        val square = state.grid.square(player.id, row)
                        val dice = square.dice.map { die ->
                            val handIndex = player.dice.hand.indexOfFirst { it === die }
                            check(handIndex >= 0) {
                                "Battle Grid die is not in player ${player.id.value}'s Hand"
                            }
                            BattleDieView(
                                handIndex = handIndex,
                                sides = die.sides,
                                value = die.value
                            )
                        }
                        val dieTotal = dice.sumOf { it.value }
                        val critterTotal = square.critters.sumOf {
                            player.critterValues.valueOf(it)
                        }
                        BattlePlayerRowView(
                            playerId = player.id,
                            row = row,
                            dice = dice,
                            critters = square.critters,
                            dieTotal = dieTotal,
                            critterTotal = critterTotal,
                            total = dieTotal + critterTotal,
                            withdrawn = state.grid.isPlayerWithdrawn(player.id, row)
                        )
                    }
                )
            }
        )
}
