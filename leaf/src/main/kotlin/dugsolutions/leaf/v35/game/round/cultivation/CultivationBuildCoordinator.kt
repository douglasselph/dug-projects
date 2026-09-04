package dugsolutions.leaf.v35.game.round.cultivation

import dugsolutions.leaf.v35.chronicle.domain.Moment
import dugsolutions.leaf.v35.effect.GameEffectExecutor
import dugsolutions.leaf.v35.effect.GameEffectPhase
import dugsolutions.leaf.v35.effect.GameEffectRequest
import dugsolutions.leaf.v35.effect.GameEffectSource
import dugsolutions.leaf.v35.effect.RoundEffectSlot
import dugsolutions.leaf.v35.game.Game
import dugsolutions.leaf.v35.game.operation.RollResolver
import dugsolutions.leaf.v35.game.operation.SupportActionExecutor
import dugsolutions.leaf.v35.player.Player
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.cultivation.ChooseCultivationActionRequest
import dugsolutions.leaf.v35.player.decision.cultivation.CultivationAction
import dugsolutions.leaf.v35.player.decision.cultivation.CultivationMainAction
import dugsolutions.leaf.v35.player.decision.support.HandDieChoice
import dugsolutions.leaf.v35.player.decision.support.SupportAction
import dugsolutions.leaf.v35.round.domain.RoundCard
import dugsolutions.leaf.v35.round.domain.RoundCardType
import dugsolutions.leaf.v35.tokens.Critter

/** One successfully completed Main Action. */
data class CultivationActionResult(
    val playerId: PlayerId,
    val actionNumber: Int,
    val action: CultivationMainAction
)

/** One successfully completed optional Support Action. */
data class CultivationSupportActionResult(
    val playerId: PlayerId,
    val sequence: Int,
    val action: SupportAction
)

data class CultivationBuildResult(
    val openingDrawCounts: Map<PlayerId, Int>,
    val actions: List<CultivationActionResult>,
    val supportActions: List<CultivationSupportActionResult> = emptyList()
)

/**
 * Executes Cultivation's opening Draw-and-Roll and Build action loop.
 *
 * Each player must complete exactly two Main Actions, but may interleave any
 * number of currently legal Support Actions before choosing Done.
 */
class CultivationBuildCoordinator(
    private val rollResolver: RollResolver,
    private val effectExecutor: GameEffectExecutor,
    private val supportActionExecutor: SupportActionExecutor
) {

    fun execute(
        game: Game,
        roundCard: RoundCard
    ): CultivationBuildResult {
        require(roundCard.type == RoundCardType.CULTIVATION) {
            "Cultivation Build requires a Cultivation Round card: ${roundCard.type}"
        }

        val openingDrawCounts = linkedMapOf<PlayerId, Int>()
        game.players.forEach { player ->
            var count = 0
            repeat(3) {
                if (rollResolver.draw(player) != null) count++
            }
            openingDrawCounts[player.id] = count
            game.chronicle.record(
                Moment.Marker(
                    "CULTIVATION_OPENING_DRAW_COMPLETE player=${player.id.value} count=$count"
                )
            )
        }

        val mainActionResults = mutableListOf<CultivationActionResult>()
        val supportActionResults = mutableListOf<CultivationSupportActionResult>()

        game.players.forEach { player ->
            var mainActionsUsed = 0
            var supportSequence = 0

            while (true) {
                val legalChoices = legalChoices(
                    player = player,
                    mainActionsRemaining = 2 - mainActionsUsed
                )
                check(legalChoices.isNotEmpty()) {
                    "Player ${player.id.value} has no legal Cultivation action"
                }

                val chosen = player.decisions.cultivation.chooseAction(
                    ChooseCultivationActionRequest(
                        roundCard = roundCard,
                        mainActionsRemaining = 2 - mainActionsUsed,
                        legalChoices = legalChoices
                    )
                )
                check(chosen in legalChoices) {
                    "CultivationStrategy returned an action that was not offered: $chosen"
                }

                when (chosen) {
                    is CultivationAction.Main -> {
                        check(mainActionsUsed < 2) {
                            "Player ${player.id.value} has already used both Main Actions"
                        }
                        executeMainAction(
                            game = game,
                            player = player,
                            roundCard = roundCard,
                            action = chosen.action
                        )
                        mainActionsUsed++
                        mainActionResults += CultivationActionResult(
                            playerId = player.id,
                            actionNumber = mainActionsUsed,
                            action = chosen.action
                        )
                        game.chronicle.record(
                            Moment.Marker(
                                "CULTIVATION_MAIN_ACTION player=${player.id.value} " +
                                    "action=$mainActionsUsed type=${mainActionName(chosen.action)}"
                            )
                        )
                    }

                    is CultivationAction.Support -> {
                        supportActionExecutor.executeCultivation(
                            game = game,
                            player = player,
                            action = chosen.action
                        )
                        supportSequence++
                        supportActionResults += CultivationSupportActionResult(
                            playerId = player.id,
                            sequence = supportSequence,
                            action = chosen.action
                        )
                    }

                    CultivationAction.Done -> {
                        check(mainActionsUsed == 2) {
                            "Player ${player.id.value} cannot finish Build before using both Main Actions"
                        }
                        break
                    }
                }
            }
        }

        return CultivationBuildResult(
            openingDrawCounts = openingDrawCounts.toMap(),
            actions = mainActionResults.toList(),
            supportActions = supportActionResults.toList()
        )
    }

    private fun legalChoices(
        player: Player,
        mainActionsRemaining: Int
    ): List<CultivationAction> =
        buildList {
            if (mainActionsRemaining > 0) {
                mainActions(player).mapTo(this) {
                    CultivationAction.Main(it)
                }
            }

            supportActions(player).mapTo(this) {
                CultivationAction.Support(it)
            }

            if (mainActionsRemaining == 0) {
                add(CultivationAction.Done)
            }
        }

    private fun mainActions(
        player: Player
    ): List<CultivationMainAction> =
        buildList {
            if (!player.dice.isSupplyEmpty || !player.dice.isDiscardEmpty) {
                add(CultivationMainAction.Draw)
            }
            player.creature.cards
                .filter { it.isFaceUp }
                .mapTo(this, CultivationMainAction::ActivatePlant)
            add(CultivationMainAction.RoundEffect1)
            add(CultivationMainAction.RoundEffect2)
        }

    private fun supportActions(
        player: Player
    ): List<SupportAction> =
        buildList {
            player.wisps.cards.cards
                .filterNot { it.playImmediately || it.battleOnly }
                .mapTo(this, SupportAction::PlayWisp)

            val handDice = player.dice.hand.mapIndexed { index, die ->
                HandDieChoice(
                    index = index,
                    sides = die.sides,
                    value = die.value
                )
            }

            if (player.tokens.hasWater) {
                add(SupportAction.UseWaterRefresh)
                handDice.mapTo(this, SupportAction::UseWaterReroll)
            }

            player.tokens.mulchTokens
                .filter { it.sides != null }
                .mapTo(this, SupportAction::UseMulch)

            val hasWorm =
                player.critters.count(Critter.WORM) > 0 ||
                    player.critters.count(Critter.BOOSTED_WORM) > 0
            if (hasWorm) {
                player.creature.cards.forEach {
                    add(SupportAction.UseWormFlip(it.id))
                }
            }

            player.butterflies.all
                .filter { player.butterflies.isFaceUp(it) }
                .forEach { butterfly ->
                    handDice.forEach { die ->
                        add(
                            SupportAction.UseButterfly(
                                butterfly = butterfly,
                                die = die
                            )
                        )
                    }
                }
        }

    private fun executeMainAction(
        game: Game,
        player: Player,
        roundCard: RoundCard,
        action: CultivationMainAction
    ) {
        when (action) {
            CultivationMainAction.Draw ->
                checkNotNull(rollResolver.draw(player)) {
                    "Draw became unavailable for player ${player.id.value}"
                }

            is CultivationMainAction.ActivatePlant -> {
                val current = player.creature.get(action.card.id)
                check(current != null && current.isFaceUp && current == action.card) {
                    "Plant activation target is no longer legal: ${action.card.id}"
                }
                effectExecutor.execute(
                    GameEffectRequest(
                        game = game,
                        actor = player,
                        effect = current.card.effect,
                        source = GameEffectSource.Plant(current),
                        phase = GameEffectPhase.CULTIVATION
                    )
                )
                check(player.creature.faceDown(current.id)) {
                    "Activated Plant could not be flipped face down: ${current.id}"
                }
            }

            CultivationMainAction.RoundEffect1 ->
                executeRoundEffect(game, player, roundCard, RoundEffectSlot.FIRST)

            CultivationMainAction.RoundEffect2 ->
                executeRoundEffect(game, player, roundCard, RoundEffectSlot.SECOND)
        }
    }

    private fun executeRoundEffect(
        game: Game,
        player: Player,
        roundCard: RoundCard,
        slot: RoundEffectSlot
    ) {
        val effect = when (slot) {
            RoundEffectSlot.FIRST -> roundCard.firstEffect.effect
            RoundEffectSlot.SECOND -> roundCard.secondEffect.effect
        }
        effectExecutor.execute(
            GameEffectRequest(
                game = game,
                actor = player,
                effect = effect,
                source = GameEffectSource.Round(roundCard, slot),
                phase = GameEffectPhase.CULTIVATION
            )
        )
    }

    private fun mainActionName(action: CultivationMainAction): String =
        when (action) {
            CultivationMainAction.Draw -> "DRAW"
            is CultivationMainAction.ActivatePlant -> "ACTIVATE_PLANT"
            CultivationMainAction.RoundEffect1 -> "ROUND_EFFECT_1"
            CultivationMainAction.RoundEffect2 -> "ROUND_EFFECT_2"
        }
}
