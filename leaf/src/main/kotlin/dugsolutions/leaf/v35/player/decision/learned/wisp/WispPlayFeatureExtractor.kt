package dugsolutions.leaf.v35.player.decision.learned.wisp

import dugsolutions.leaf.v35.player.decision.wisp.*
import dugsolutions.leaf.v35.round.domain.RoundCardType

object WispPlayFeatureExtractor {
    fun extract(request: ChooseWispPlayRequest, decision: WispPlayDecision)=WispPlayFeatureVector.build {
        val c=request.observation.context; val b=c.self.board; val p=c.progress
        this[WispPlayFeature.BIAS]=1
        this[WispPlayFeature.PHASE_CULTIVATION]=if(request.observation.phase==RoundCardType.CULTIVATION)1 else 0
        this[WispPlayFeature.PHASE_BATTLE]=if(request.observation.phase==RoundCardType.BATTLE)1 else 0
        this[WispPlayFeature.GAME_PROGRESS]=if(p.totalRounds>0)p.roundsCompleted.toDouble()/p.totalRounds else 0.0
        this[WispPlayFeature.ROUNDS_REMAINING]=p.roundsRemainingToReveal
        this[WispPlayFeature.BATTLES_REMAINING]=p.battleRoundsRemaining ?: p.upcomingRoundTypes.count{it==RoundCardType.BATTLE}
        this[WispPlayFeature.FINAL_BATTLE]=if(p.isFinalBattleRound)1 else 0
        this[WispPlayFeature.SUPPORT_PASS]=request.observation.supportPassNumber ?: 0
        this[WispPlayFeature.MAIN_ACTIONS_REMAINING]=request.observation.mainActionsRemaining ?: 0
        this[WispPlayFeature.SELF_VP]=b.vp
        val oppBest=c.opponents.maxOfOrNull{it.board.vp} ?: b.vp
        this[WispPlayFeature.RELATIVE_VP]=b.vp-oppBest
        this[WispPlayFeature.SELF_WISP_COUNT]=c.self.wispCount
        this[WispPlayFeature.WISP_DECK_REMAINING]=c.grove.wispDeckRemaining
        this[WispPlayFeature.SELF_DICE_COUNT]=b.supply.size+b.hand.size+b.discard.size
        this[WispPlayFeature.SELF_DICE_POWER]=b.dicePower
        this[WispPlayFeature.SELF_HAND_POWER]=b.hand.sumOf{it.value}
        this[WispPlayFeature.SELF_PLANT_COUNT]=b.plantCount
        this[WispPlayFeature.SELF_WATER]=b.water; this[WispPlayFeature.SELF_SUNLIGHT]=b.sunlight; this[WispPlayFeature.SELF_MULCH]=b.mulch.size
        this[WispPlayFeature.SELF_BEES]=b.bees; this[WispPlayFeature.SELF_WORMS]=b.worms
        c.battle?.let { battle ->
            var deficit=0; var risk=0; var winning=0
            battle.rows.filterNot{it.closed}.forEach { row ->
                val self=row.forPlayer(c.self.id)?.total ?: 0
                val opp=row.players.filter{it.playerId!=c.self.id}.maxOfOrNull{it.total} ?: 0
                if(self>=opp) winning++ else deficit += opp-self
                if(opp-self>=5) risk++
            }
            this[WispPlayFeature.BATTLE_TOTAL_DEFICIT]=deficit; this[WispPlayFeature.BATTLE_WOUND_RISK_ROWS]=risk; this[WispPlayFeature.BATTLE_WINNING_ROWS]=winning
        }
        when(decision){
            WispPlayDecision.Hold -> { this[WispPlayFeature.ACTION_HOLD]=1; this[WispPlayFeature.HUMAN_REFERENCE_MATCH]=if(request.referenceCard==null)1 else 0 }
            is WispPlayDecision.Play -> { this[WispPlayFeature.ACTION_PLAY]=1; this[WispPlayFeature.WISP_END_GAME_VP]=decision.card.endGameVp; this[WispPlayFeature.HUMAN_REFERENCE_MATCH]=if(decision.card==request.referenceCard)1 else 0; putNamed(LearnedWispPlayWeights.wispFeature(decision.card.name),1.0); putNamed(LearnedWispPlayWeights.effectFeature(decision.card.effect.name),1.0) }
        }
    }
}
