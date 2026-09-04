package dugsolutions.leaf.v35.effect

import dugsolutions.leaf.v35.game.Game
import dugsolutions.leaf.v35.game.GameEngineTestFixture
import dugsolutions.leaf.v35.player.Player
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.DecisionDirector
import dugsolutions.leaf.v35.player.decision.effect.ChooseEffectDieRequest
import dugsolutions.leaf.v35.player.decision.effect.ChooseOptionalEffectDieRequest
import dugsolutions.leaf.v35.player.decision.effect.EffectDieChoice
import dugsolutions.leaf.v35.player.decision.effect.EffectStrategy
import dugsolutions.leaf.v35.player.dice.PlayerDice
import dugsolutions.leaf.v35.random.die.Die
import dugsolutions.leaf.v35.round.domain.RoundCard
import dugsolutions.leaf.v35.round.domain.RoundCardEffect
import dugsolutions.leaf.v35.round.domain.RoundCardType
import dugsolutions.leaf.v35.wisp.domain.WispCard

internal object EffectTestFixture {

    fun game(vararg players: Player): Game =
        GameEngineTestFixture.game(
            players = if (players.isEmpty()) {
                listOf(player(1), player(2))
            } else {
                players.toList()
            }
        )

    fun player(
        id: Int,
        hand: List<Die> = emptyList(),
        effectStrategy: EffectStrategy = DecisionDirector.baseline().effect
    ): Player =
        Player(
            id = PlayerId(id),
            decisions = DecisionDirector.baseline().copy(
                effect = effectStrategy
            ),
            dice = PlayerDice(hand = hand)
        )

    fun request(
        game: Game,
        actor: Player,
        effect: GameEffect,
        source: GameEffectSource = GameEffectSource.Round(
            roundCard(effect),
            RoundEffectSlot.FIRST
        )
    ): GameEffectRequest =
        GameEffectRequest(
            game = game,
            actor = actor,
            effect = effect,
            source = source,
            phase = GameEffectPhase.CULTIVATION
        )

    fun roundCard(effect: GameEffect): RoundCard =
        RoundCard(
            quantity = 1,
            name = "Test_Round",
            type = RoundCardType.CULTIVATION,
            firstEffect = roundEffect(effect),
            secondEffect = roundEffect(effect),
            backImage = ""
        )

    fun wispquake(): WispCard =
        WispCard(
            quantity = 1,
            name = "Wispquake",
            title = "Wispquake",
            count = 1,
            effect = GameEffect.REROLL_ALL_PLAYERS_DICE_KEEP_ONE_OWN,
            lineIcons = null,
            lineIconsHeight = 0,
            vpIcon = null,
            mainBackdrop = "",
            playImmediately = true
        )

    private fun roundEffect(effect: GameEffect): RoundCardEffect =
        RoundCardEffect(
            title = "Test",
            backgroundColor = "",
            textColor = "",
            image = "",
            icon = null,
            effect = effect
        )
}

internal open class FixedEffectDie(
    sides: Int,
    value: Int
) : Die(sides) {
    init {
        adjustTo(value)
    }

    override fun roll(): Die = this
}

internal class SequenceEffectDie(
    sides: Int,
    initial: Int,
    private val next: Int
) : Die(sides) {
    init {
        adjustTo(initial)
    }

    override fun roll(): Die {
        adjustTo(next)
        return this
    }
}

internal open class FirstEffectChoiceStrategy : EffectStrategy {
    override fun chooseDie(
        request: ChooseEffectDieRequest
    ): EffectDieChoice =
        request.legalChoices.first()

    override fun chooseOptionalDie(
        request: ChooseOptionalEffectDieRequest
    ): EffectDieChoice? =
        request.legalChoices.firstOrNull()
}

internal open class LastEffectChoiceStrategy : EffectStrategy {
    override fun chooseDie(
        request: ChooseEffectDieRequest
    ): EffectDieChoice =
        request.legalChoices.last()

    override fun chooseOptionalDie(
        request: ChooseOptionalEffectDieRequest
    ): EffectDieChoice? =
        request.legalChoices.lastOrNull()
}
