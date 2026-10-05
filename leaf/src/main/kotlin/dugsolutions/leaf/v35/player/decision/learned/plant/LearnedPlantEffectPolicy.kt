package dugsolutions.leaf.v35.player.decision.learned.plant

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.effect.*
import dugsolutions.leaf.v35.player.decision.plant.*
import dugsolutions.leaf.v35.random.die.DieSides

/** Linear learned scorer for effect-internal choices of an already-selected Plant. */
class LearnedPlantEffectPolicy(
    private val weights: LearnedPlantEffectWeights
) : PlantEffectPolicy {
    override fun strategyFor(source: PlantEffectSource, fallback: EffectStrategy): EffectStrategy = Bound(source, fallback)

    private inner class Bound(private val source: PlantEffectSource, private val reference: EffectStrategy) : EffectStrategy by reference {
        override fun chooseDie(request: ChooseEffectDieRequest) = choose("DIE", request.context, request.legalChoices, reference.chooseDie(request))
        override fun chooseBattleDie(request: ChooseEffectBattleDieRequest) = choose("BATTLE_DIE", request.context, request.legalChoices, reference.chooseBattleDie(request))
        override fun chooseRootWellBattle(request: ChooseRootWellBattleRequest) = choose("ROOT_WELL", request.context, request.legalChoices, reference.chooseRootWellBattle(request))
        override fun chooseCrossPlayerDieSwap(request: ChooseEffectCrossPlayerDieSwapRequest) = choose("CROSS_SWAP", request.context, request.legalChoices, reference.chooseCrossPlayerDieSwap(request))
        override fun chooseOptionalDie(request: ChooseOptionalEffectDieRequest): EffectDieChoice? = chooseOptional("OPTIONAL_DIE", request.context, request.legalChoices, reference.chooseOptionalDie(request))
        override fun chooseDice(request: ChooseEffectDiceRequest): EffectDiceChoice {
            val ref = reference.chooseDice(request)
            val options = subsetCandidates(request, ref)
            return choose("DICE", request.context, options, ref)
        }
        override fun chooseDiePair(request: ChooseEffectDiePairRequest) = choose("DIE_PAIR", request.context, request.legalChoices, reference.chooseDiePair(request))
        override fun chooseOptionalDiePair(request: ChooseOptionalEffectDiePairRequest): EffectDiePairChoice? = chooseOptional("OPTIONAL_DIE_PAIR", request.context, request.legalChoices, reference.chooseOptionalDiePair(request))
        override fun chooseCritterAndDie(request: ChooseEffectCritterDieRequest) = choose("CRITTER_DIE", request.context, request.legalChoices, reference.chooseCritterAndDie(request))
        override fun choosePetalD4Source(request: ChoosePetalD4SourceRequest) = choose("PETAL_SOURCE", request.context, request.legalChoices, reference.choosePetalD4Source(request))
        override fun choosePetalToDie4(request: ChoosePetalToDie4Request) = choose("PETAL_BRANCH", request.context, request.legalChoices, reference.choosePetalToDie4(request))
        override fun chooseBeeSource(request: ChooseBeeSourceRequest) = choose("BEE_SOURCE", request.context, request.legalChoices, reference.chooseBeeSource(request))
        override fun chooseButterflyTarget(request: ChooseEffectButterflyTargetRequest) = choose("BUTTERFLY", request.context, request.legalChoices, reference.chooseButterflyTarget(request))
        override fun chooseOptionalPlant(request: ChooseOptionalEffectPlantRequest): EffectPlantChoice? = chooseOptional("OPTIONAL_PLANT", request.context, request.legalChoices, reference.chooseOptionalPlant(request))
        override fun chooseOpponentPlantWound(request: ChooseEffectOpponentPlantWoundRequest) = choose("OPPONENT_PLANT_WOUND", request.context, request.legalChoices, reference.chooseOpponentPlantWound(request))
        override fun choosePlantEffect(request: ChooseEffectPlantRequest) = choose("PLANT_EFFECT", request.context, request.legalChoices, reference.choosePlantEffect(request))
        override fun chooseOEdelweiss(request: ChooseOEdelweissRequest) = choose("O_EDELWEISS", request.context, request.legalChoices, reference.chooseOEdelweiss(request))
        override fun chooseWispsToKeep(request: ChooseWispsToKeepRequest): EffectWispsChoice {
            // Plant effects currently do not produce this request, but keep routing total and legal.
            return reference.chooseWispsToKeep(request)
        }
        override fun chooseDieSize(request: ChooseEffectDieSizeRequest) = choose("DIE_SIZE", request.context, request.legalChoices, reference.chooseDieSize(request))
        override fun choosePlayer(request: ChooseEffectPlayerRequest) = choose("PLAYER", request.context, request.legalChoices, reference.choosePlayer(request))
        override fun chooseStrikeRow(request: ChooseEffectStrikeRowRequest) = choose("STRIKE_ROW", request.context, request.legalChoices, reference.chooseStrikeRow(request))

        private fun <T:Any> choose(kind:String, context:DecisionContext, legal:List<T>, ref:T):T {
            require(legal.isNotEmpty())
            val chosen = legal.maxWithOrNull(compareBy<T> { score(kind,context,it,ref) }.thenBy { stableId(it) })!!
            trace(kind, legal.map(::stableId), stableId(chosen), stableId(ref))
            return chosen
        }

        private fun <T:Any> chooseOptional(kind:String, context:DecisionContext, legal:List<T>, ref:T?):T? {
            val candidates = listOf<T?>(null) + legal
            val chosen = candidates.maxWithOrNull(compareBy<T?> { score(kind,context,it,ref) }.thenBy { stableId(it) })
            trace(kind, candidates.map(::stableId), stableId(chosen), stableId(ref))
            return chosen
        }

        private fun trace(kind:String, legal:List<String>, selected:String, referenceId:String?) =
            source.trace.record(PlantEffectDecisionRecord(source.plantId, source.effect, kind, legal, selected, referenceId))

        private fun score(kind:String, context:DecisionContext, candidate:Any?, ref:Any?):Double {
            val f = DoubleArray(PlantEffectFeature.entries.size)
            fun set(k:PlantEffectFeature,v:Double){f[k.ordinal]=v}
            set(PlantEffectFeature.BIAS,1.0)
            set(PlantEffectFeature.PHASE_BATTLE,if(context.phase?.name=="BATTLE")1.0 else 0.0)
            set(PlantEffectFeature.GAME_PROGRESS, if(context.progress.totalRounds<=0)0.0 else context.progress.roundsCompleted.toDouble()/context.progress.totalRounds)
            set(PlantEffectFeature.BATTLES_REMAINING,(context.progress.battleRoundsRemaining?:0).toDouble()/3.0)
            val b=context.self.board
            set(PlantEffectFeature.HAND_DICE_COUNT,b.hand.size/10.0)
            set(PlantEffectFeature.HAND_DICE_POWER,b.hand.sumOf{it.sides}/100.0)
            set(PlantEffectFeature.PLANT_COUNT,b.plantCount/12.0)
            set(PlantEffectFeature.WATER,b.water/5.0);set(PlantEffectFeature.SUNLIGHT,b.sunlight/5.0);set(PlantEffectFeature.BEES,b.bees/8.0);set(PlantEffectFeature.WORMS,b.worms/8.0);set(PlantEffectFeature.WISPS,context.self.wispCount/5.0)
            candidateMetrics(candidate,context,set=::set)
            set(PlantEffectFeature.OPTIONAL_DECLINE,if(candidate==null || candidate is OEdelweissChoice.Done)1.0 else 0.0)
            set(PlantEffectFeature.HUMAN_REFERENCE_MATCH,if(stableId(candidate)==stableId(ref))1.0 else 0.0)
            var total=PlantEffectFeature.entries.sumOf { f[it.ordinal]*weights[it] }
            total += weights.named(LearnedPlantEffectWeights.plantFeature(source.plantId))
            total += weights.named(LearnedPlantEffectWeights.effectFeature(source.effect))
            total += weights.named(LearnedPlantEffectWeights.decisionFeature(kind))
            return total
        }

        private fun candidateMetrics(candidate:Any?, context:DecisionContext, set:(PlantEffectFeature,Double)->Unit) {
            val dice=mutableListOf<EffectDieChoice>()
            var opponent=false; var faceUp=false; var row:StrikeRow?=null
            when(candidate){
                is EffectDieChoice -> dice += candidate
                is EffectBattleDieChoice -> { dice+=candidate.die; opponent=candidate.ownerId!=context.self.id; row=candidate.row }
                is RootWellBattleChoice.OwnDice -> { dice+=candidate.first.die;dice+=candidate.second.die;row=candidate.first.row }
                is RootWellBattleChoice.OpponentDie -> { dice+=candidate.die.die;opponent=true;row=candidate.die.row }
                is EffectCrossPlayerDieSwapChoice -> { dice+=candidate.ownDie.die;dice+=candidate.opponentDie.die;opponent=true;row=candidate.opponentDie.row }
                is EffectDiceChoice -> dice+=candidate.selected
                is EffectDiePairChoice -> { dice+=candidate.source;dice+=candidate.target }
                is EffectCritterDieChoice -> dice+=candidate.die
                is PetalD4SourceChoice.Opponent -> { dice+=candidate.die;opponent=true }
                is PetalToDie4Choice.TrashD4AndRaiseAll -> dice+=candidate.die
                is EffectBeeSourceChoice.Opponent -> opponent=true
                is EffectButterflyTargetChoice -> opponent=candidate.ownerId!=null
                is EffectPlantChoice -> faceUp=candidate.isFaceUp
                is EffectOpponentPlantWoundChoice -> opponent=true
                is OEdelweissChoice.Play -> faceUp=candidate.card.isFaceUp
                is OEdelweissChoice.Flip -> faceUp=candidate.card.isFaceUp
                is StrikeRow -> row=candidate
            }
            if(dice.isNotEmpty()){
                set(PlantEffectFeature.TARGET_DIE_VALUE,dice.map{it.value.toDouble()/it.sides}.average())
                set(PlantEffectFeature.TARGET_DIE_SIDES,dice.map{it.sides/20.0}.average())
                set(PlantEffectFeature.TARGET_DIE_HEADROOM,dice.map{(it.sides-it.value).toDouble()/it.sides}.average())
            }
            set(PlantEffectFeature.TARGET_IS_OPPONENT,if(opponent)1.0 else 0.0)
            set(PlantEffectFeature.TARGET_PLANT_FACE_UP,if(faceUp)1.0 else 0.0)
            row?.let { r ->
                context.battle?.row(r)?.let { rv ->
                    val own=rv.forPlayer(context.self.id)?.total?:0
                    val bestOpp=rv.players.filter{it.playerId!=context.self.id}.maxOfOrNull{it.total}?:0
                    set(PlantEffectFeature.TARGET_ROW_DEFICIT,(bestOpp-own).coerceAtLeast(0)/30.0)
                    set(PlantEffectFeature.TARGET_ROW_LEAD,(own-bestOpp).coerceAtLeast(0)/30.0)
                }
            }
        }

        private fun subsetCandidates(request:ChooseEffectDiceRequest, ref:EffectDiceChoice):List<EffectDiceChoice>{
            val legal=request.legalChoices
            val result=linkedMapOf<String,EffectDiceChoice>()
            fun add(c:EffectDiceChoice){if(c.selected.size in request.minChoices..request.maxChoices)result[stableId(c)]=c}
            add(ref)
            if(request.minChoices==0)add(EffectDiceChoice(emptyList()))
            legal.forEach { add(EffectDiceChoice(listOf(it))) }
            if(legal.size in request.minChoices..request.maxChoices)add(EffectDiceChoice(legal))
            // A bounded set of deterministic prefixes gives the learner count/subset freedom without 2^N explosion.
            val ranked=legal.sortedWith(compareByDescending<EffectDieChoice>{it.value.toDouble()/it.sides}.thenBy{it.index})
            for(n in request.minChoices..request.maxChoices.coerceAtMost(ranked.size)) add(EffectDiceChoice(ranked.take(n)))
            return result.values.toList()
        }

        private fun stableId(v:Any?):String = when(v){
            null -> "NONE"
            is EffectDieChoice -> "DIE:${v.index}:D${v.sides}@${v.value}"
            is EffectBattleDieChoice -> "BDIE:P${v.ownerId.value}:${v.row}:${stableId(v.die)}"
            is RootWellBattleChoice.OwnDice -> "OWN:${stableId(v.first)}:${stableId(v.second)}"
            is RootWellBattleChoice.OpponentDie -> "OPP:${stableId(v.die)}"
            is EffectCrossPlayerDieSwapChoice -> "SWAP:${stableId(v.ownDie)}:${stableId(v.opponentDie)}"
            is EffectDiceChoice -> "DICE:"+v.selected.joinToString("|"){stableId(it)}
            is EffectDiePairChoice -> "PAIR:${stableId(v.source)}:${stableId(v.target)}"
            is EffectCritterDieChoice -> "CRITTER:${v.critter}:${stableId(v.die)}"
            PetalD4SourceChoice.Grove -> "PETAL_SOURCE:GROVE"
            is PetalD4SourceChoice.Opponent -> "PETAL_SOURCE:P${v.playerId.value}:${v.zone}:${stableId(v.die)}"
            PetalToDie4Choice.GainD4 -> "PETAL:GAIN"
            is PetalToDie4Choice.TrashD4AndRaiseAll -> "PETAL:TRASH:${stableId(v.die)}"
            EffectBeeSourceChoice.Grove -> "BEE:GROVE"
            is EffectBeeSourceChoice.Opponent -> "BEE:P${v.playerId.value}"
            is EffectButterflyTargetChoice -> "BUTTERFLY:${v.ownerId?.value?:-1}:${v.butterfly}"
            is EffectPlantChoice -> "PLANT:${v.cardName}:${v.cardId.value}:${v.isFaceUp}"
            is EffectOpponentPlantWoundChoice.Flip -> "WOUND:FLIP:P${v.ownerId.value}:${v.cardName}:${v.cardId.value}"
            is EffectOpponentPlantWoundChoice.Snip -> "WOUND:SNIP:P${v.ownerId.value}:${v.cardName}:${v.cardId.value}"
            is OEdelweissChoice.Play -> "EDEL:PLAY:${stableId(v.card)}"
            is OEdelweissChoice.Flip -> "EDEL:FLIP:${stableId(v.card)}"
            OEdelweissChoice.Done -> "EDEL:DONE"
            is DieSides -> "DIE_SIZE:${v.name}"
            is PlayerId -> "PLAYER:${v.value}"
            is StrikeRow -> "ROW:${v.name}"
            else -> v.toString()
        }
    }
}
