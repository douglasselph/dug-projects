package dugsolutions.leaf.v30.game.round

import dugsolutions.leaf.v30.chronicle.Chronicle
import dugsolutions.leaf.v30.chronicle.GameChronicle
import dugsolutions.leaf.v30.chronicle.domain.Moment
import dugsolutions.leaf.v30.chronicle.domain.WarningType
import dugsolutions.leaf.v30.common.Critter
import dugsolutions.leaf.v30.player.Player
import dugsolutions.leaf.v30.player.decision.domain.Decision
import dugsolutions.leaf.v30.random.die.Die
import dugsolutions.leaf.v30.round.domain.RoundCard
import dugsolutions.leaf.v30.table.Table

abstract class RoundBase(
    protected val table: Table,
    val card: RoundCard,
    protected val chronicle: Chronicle = GameChronicle(),
    private val checkRefresh: CheckRefresh = CheckRefresh()
) {

    private companion object {
        const val DICE_PER_PLAYER_PER_ROUND = 3
        const val ROLL_GAIN_CRITTER = 1
        const val ROLL_GAIN_WISP = 2
    }

    open fun performMainActions() {
    }

    fun drawDice() {
        table.players.forEach { player ->
            player.discardHandDice()
            repeat(DICE_PER_PLAYER_PER_ROUND) {
                player.drawDiceWithRefresh()
            }
        }
    }

    fun rollDice() {
        table.players.forEach { player ->
            player.rollDice()
            chronicle(Moment.DiceRolled(player))
        }
    }

    fun resolveRewards() {
        table.players.forEach { player ->
            player.diceHand.dice.forEach { die ->
                resolveReward(player, die)
            }
        }
    }

    protected fun resolveReward(
        player: Player,
        die: Die
    ) {
        when (die.value) {
            ROLL_GAIN_CRITTER -> gainCritter(player)?.let { critter ->
                chronicle(Moment.Reward(player = player, die = die, critter = critter))
            }
            ROLL_GAIN_WISP -> table.grove.drawWispCard()?.let { wispCard ->
                player.addWispCard(wispCard)
                chronicle(Moment.Reward(player = player, die = die, wispCard = wispCard))
            }
        }
    }

    fun cleanup() {
        returnBattleCrittersToGrove()
        normalizePlayerCritters()
        checkRefresh()
    }

    fun checkRefresh() {
        table.players.forEach { player ->
            checkRefresh(player)
        }
    }

    fun checkWormRefresh() {
        table.players.forEach { player ->
            val cardsToRefresh = player.decisionDirector.chooseCardsToRefreshWithWorms(
                Decision.ChooseCardsToRefreshWithWorms(player)
            )
            cardsToRefresh.cards.forEach cards@{ card ->
                if (!player.removeCritter(Critter.WORM)) {
                    chronicle(
                        Moment.Warning(
                            player = player,
                            type = WarningType.MISSING_WORM,
                            card = card
                        )
                    )
                    return@cards
                }
                table.grove.add(Critter.WORM)
                player.flipCreatureCardFaceUp(card)
            }
        }
    }

    private fun gainCritter(player: Player): Critter? {
        val critter = player.decisionDirector.chooseCritter(
            Decision.ChooseCritter(
                player = player,
                availableCritters = availableCritters()
            )
        )
        if (table.grove.remove(critter)) {
            player.addCritter(critter)
            return critter
        }
        return null
    }

    private fun availableCritters(): List<Critter> {
        return Critter.entries.filter { table.grove.has(it) }
    }

    private fun normalizePlayerCritters() {
        table.players.forEach { player ->
            player.replaceCritter(Critter.BOOSTED_WORM, Critter.WORM)
            player.replaceCritter(Critter.BOOSTED_BEE, Critter.BEE)
        }
    }

    private fun returnBattleCrittersToGrove() {
        table.battle.drainCritters().forEach { critter ->
            table.grove.add(critter)
        }
    }

}
