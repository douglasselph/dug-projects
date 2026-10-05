package dugsolutions.leaf.v35.player.decision.learned.wisp

import dugsolutions.leaf.v35.player.decision.wisp.*

class LearnedWispPlayPolicy(private val weights: LearnedWispPlayWeights):WispPlayPolicy{
    override fun chooseWisp(request:ChooseWispPlayRequest):WispPlayDecision{
        val candidates=buildList<WispPlayDecision>{if(request.allowHold)add(WispPlayDecision.Hold);request.legalCards.forEach{add(WispPlayDecision.Play(it))}}
        return candidates.maxWithOrNull(compareBy<WispPlayDecision>{score(WispPlayFeatureExtractor.extract(request,it))}.thenByDescending{when(it){WispPlayDecision.Hold->"HOLD";is WispPlayDecision.Play->"PLAY:${it.card.name}"}}) ?: WispPlayDecision.Hold
    }
    private fun score(v:WispPlayFeatureVector)=WispPlayFeature.entries.sumOf{v[it]*weights[it]}+v.namedValues().entries.sumOf{(k,x)->x*weights.named(k)}
}
