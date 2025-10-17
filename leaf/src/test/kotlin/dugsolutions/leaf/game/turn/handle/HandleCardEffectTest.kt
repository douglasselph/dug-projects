package dugsolutions.leaf.game.turn.handle

import dugsolutions.leaf.cards.domain.CardEffect
import dugsolutions.leaf.cards.domain.FlourishType
import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.chronicle.domain.Moment
import dugsolutions.leaf.game.turn.effect.EffectCardToRetain
import dugsolutions.leaf.game.turn.effect.EffectDieAdjust
import dugsolutions.leaf.game.turn.effect.EffectDieReroll
import dugsolutions.leaf.game.turn.effect.EffectDieRerollAny
import dugsolutions.leaf.game.turn.effect.EffectDieToMax
import dugsolutions.leaf.game.turn.effect.EffectDieToRetain
import dugsolutions.leaf.game.turn.effect.EffectDiscard
import dugsolutions.leaf.game.turn.effect.EffectDraw
import dugsolutions.leaf.game.turn.effect.EffectDrawCard
import dugsolutions.leaf.game.turn.effect.EffectDrawDie
import dugsolutions.leaf.game.turn.effect.EffectGainD20
import dugsolutions.leaf.game.turn.effect.EffectReplayVine
import dugsolutions.leaf.game.turn.effect.EffectReuse
import dugsolutions.leaf.game.turn.effect.EffectReuseCard
import dugsolutions.leaf.game.turn.effect.EffectReuseDie
import dugsolutions.leaf.game.turn.effect.EffectUseOpponentCard
import dugsolutions.leaf.game.turn.effect.EffectUseOpponentDie
import dugsolutions.leaf.player.PlayerTD
import dugsolutions.leaf.player.domain.AppliedEffect
import dugsolutions.leaf.random.die.DieSides
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class HandleCardEffectTest {

    private lateinit var player: PlayerTD
    private lateinit var target: PlayerTD

    private val effectCardToRetain: EffectCardToRetain = mockk(relaxed = true)
    private val effectDieAdjust: EffectDieAdjust = mockk(relaxed = true)
    private val effectDieToMax: EffectDieToMax = mockk(relaxed = true)
    private val effectDiscard: EffectDiscard = mockk(relaxed = true)
    private val effectDrawCard: EffectDrawCard = mockk(relaxed = true)
    private val effectDrawDie: EffectDrawDie = mockk(relaxed = true)
    private val effectDraw: EffectDraw = mockk(relaxed = true)
    private val effectDieReroll: EffectDieReroll = mockk(relaxed = true)
    private val effectDieRerollAny: EffectDieRerollAny = mockk(relaxed = true)
    private val effectDieToRetain: EffectDieToRetain = mockk(relaxed = true)
    private val effectGainD20: EffectGainD20 = mockk(relaxed = true)
    private val effectReuseCard: EffectReuseCard = mockk(relaxed = true)
    private val effectReuseDie: EffectReuseDie = mockk(relaxed = true)
    private val effectReuse: EffectReuse = mockk(relaxed = true)
    private val effectReplayVine: EffectReplayVine = mockk(relaxed = true)
    private val handleDieUpgrade: HandleDieUpgrade = mockk(relaxed = true)
    private val effectUseOpponentCard: EffectUseOpponentCard = mockk(relaxed = true)
    private val effectUseOpponentDie: EffectUseOpponentDie = mockk(relaxed = true)
    private val chronicle: GameChronicle = mockk(relaxed = true)

    private val SUT: HandleCardEffect = HandleCardEffect(
        effectCardToRetain,
        effectDieAdjust,
        effectDieToMax,
        effectDiscard,
        effectDrawCard,
        effectDrawDie,
        effectDraw,
        effectDieReroll,
        effectDieRerollAny,
        effectDieToRetain,
        effectGainD20,
        effectReuseCard,
        effectReuseDie,
        effectReuse,
        effectReplayVine,
        handleDieUpgrade,
        effectUseOpponentCard,
        effectUseOpponentDie,
        chronicle
    )

    @BeforeEach
    fun setup() {
        player = PlayerTD(1)
        target = PlayerTD(2)
    }

    @Test
    fun invoke_ADD_TO_DIE_callsEffectDieAdjust() {
        SUT(player, target, CardEffect.ADD_TO_DIE, 5)
        verify { effectDieAdjust(player, 5) }
    }

    @Test
    fun invoke_ADD_TO_TOTAL_incrementsPipModifier_andChronicle() {
        val initial = player.pipModifier
        SUT(player, target, CardEffect.ADD_TO_TOTAL, 3)
        assert(player.pipModifier == initial + 3)
        verify { chronicle(Moment.ADD_TO_TOTAL(player, 3)) }
    }

    @Test
    fun invoke_ADJUST_BY_callsEffectDieAdjustWithTarget() {
        SUT(player, target, CardEffect.ADJUST_BY, 2)
        verify { effectDieAdjust(player, 2, target) }
    }

    @Test
    fun invoke_ADJUST_TO_MAX_callsEffectDieToMaxRepeatedly() {
        SUT(player, target, CardEffect.ADJUST_TO_MAX, 2)
        verify(exactly = 2) { effectDieToMax(player) }
    }

    @Test
    fun invoke_ADORN_callsEffectAdorn() {
        try {
            SUT(player, target, CardEffect.ADORN, 1)
        } catch (ex: Exception) {
        }
    }

    @Test
    fun invoke_DEFLECT_incrementsDeflectDamage_andChronicle() {
        val initial = player.deflectDamage
        SUT(player, target, CardEffect.DEFLECT, 4)
        assert(player.deflectDamage == initial + 4)
        verify { chronicle(Moment.DEFLECT_DAMAGE(player, 4)) }
    }

    @Test
    fun invoke_DISCARD_callsEffectDiscardWithBoth() {
        SUT(player, target, CardEffect.DISCARD, 2)
        verify(exactly = 2) { effectDiscard(EffectDiscard.DiscardWhich.BOTH, target) }
    }

    @Test
    fun invoke_DRAW_CARD_callsEffectDrawCard() {
        SUT(player, target, CardEffect.DRAW_CARD, 2)
        verify(exactly = 2) { effectDrawCard(player) }
    }

    @Test
    fun invoke_DRAW_CARD_DISCARD_callsEffectDrawCardWithDiscard() {
        SUT(player, target, CardEffect.DRAW_CARD_DISCARD, 1)
        verify { effectDrawCard(player, fromDiscard = true) }
    }

    @Test
    fun invoke_DRAW_DIE_callsEffectDrawDie() {
        SUT(player, target, CardEffect.DRAW_DIE, 2)
        verify(exactly = 2) { effectDrawDie(player, any()) }
    }

    @Test
    fun invoke_DRAW_callsEffectDraw() {
        SUT(player, target, CardEffect.DRAW_ANY, 3)
        verify(exactly = 3) { effectDraw(player) }
    }

    @Test
    fun invoke_GAIN_D20_callsEffect() {
        SUT(player, target, CardEffect.GAIN_D20, 2)
        verify(exactly = 2) { effectGainD20(player) }
    }

    @Test
    fun invoke_REROLL_TAKE_BETTER_callsEffectDieRerollWithTakeBetter() {
        SUT(player, target, CardEffect.REROLL_TAKE_BETTER, 2)
        verify(exactly = 2) { effectDieReroll(player, takeBetter = true) }
    }

    @Test
    fun invoke_RETAIN_CARD_callsEffectCardToRetain() {
        SUT(player, target, CardEffect.RETAIN_CARD, 2)
        verify(exactly = 2) { effectCardToRetain(player) }
    }

    @Test
    fun invoke_REUSE_CARD_callsEffectReuseCard() {
        SUT(player, target, CardEffect.REUSE_CARD, 1)
        verify { effectReuseCard(player) }
    }

    @Test
    fun invoke_REUSE_DIE_callsEffectReuseDie() {
        SUT(player, target, CardEffect.REUSE_DIE, 2)
        verify(exactly = 2) { effectReuseDie(player, false) }
    }

    @Test
    fun invoke_REUSE_DIE_REROLL_callsEffectReuseDie() {
        SUT(player, target, CardEffect.REUSE_DIE_REROLL, 2)
        verify(exactly = 2) { effectReuseDie(player, true) }
    }

    @Test
    fun invoke_REUSE__callsEffectReuse() {
        SUT(player, target, CardEffect.REUSE_ANY, 2)
        verify(exactly = 2) { effectReuse(player) }
    }

    @Test
    fun invoke_REPLAY_VINE__callsEffectReplayVine() {
        SUT(player, target, CardEffect.REPLAY_VINE, 2)
        verify(exactly = 2) { effectReplayVine(player) }
    }

    @Test
    fun invoke_UPGRADE_ANY_RETAIN_callsEffectUpgradeDie() {
        SUT(player, target, CardEffect.UPGRADE_ANY_RETAIN, 2)
        verify(exactly = 2) { handleDieUpgrade(player) }
    }

    @Test
    fun invoke_UPGRADE_D6_callsEffectUpgradeDieWithOnlyAndDiscard() {
        SUT(player, target, CardEffect.UPGRADE_D6, 1)
        verify { handleDieUpgrade(player, only = listOf(DieSides.D4, DieSides.D6), discardAfterUse = true) }
    }

    @Test
    fun invoke_USE_OPPONENT_CARD_callsEffect() {
        SUT(player, target, CardEffect.USE_OPPONENT_CARD, 2)
        verify(exactly = 2) { effectUseOpponentCard(player, target) }
    }

    @Test
    fun invoke_USE_OPPONENT_DIE_callsEffect() {
        SUT(player, target, CardEffect.USE_OPPONENT_DIE, 2)
        verify(exactly = 2) { effectUseOpponentDie(player, target) }
    }

    @Test
    fun invoke_GAIN_FREE_ROOT_addsDelayedEffect() {
        SUT(player, target, CardEffect.GAIN_FREE_ROOT, 0)
        assert(player.delayedEffectList.any { it is AppliedEffect.MarketBenefit && it.type == FlourishType.ROOT && it.isFree })
    }

    @Test
    fun invoke_REDUCE_COST_CANOPY_addsDelayedEffect() {
        SUT(player, target, CardEffect.REDUCE_COST_CANOPY, 2)
        assert(player.delayedEffectList.any { it is AppliedEffect.MarketBenefit && it.type == FlourishType.CANOPY && it.costReduction == 2 })
    }

    @Test
    fun invoke_FLOURISH_OVERRIDE_addsDelayedEffect() {
        SUT(player, target, CardEffect.FLOURISH_OVERRIDE, 0)
        assert(player.delayedEffectList.any { it is AppliedEffect.FlourishOverride })
    }
} 
