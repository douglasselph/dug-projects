package dugsolutions.leaf.v35.player.decision.plant

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.effect.*
import dugsolutions.leaf.v35.random.die.DieSides

/**
 * Strategic policy family for HOW an already-selected Plant effect is executed.
 * Main-action policies own whether/which Plant is activated; the engine owns
 * legality and mutation.
 */
fun interface PlantEffectPolicy {
    fun strategyFor(source: PlantEffectSource, fallback: EffectStrategy): EffectStrategy
}

data class PlantEffectSource(
    /** Stable Plant definition id from PlantCard.name, not per-copy CreatureCardId. */
    val plantId: String,
    val effect: GameEffect,
    val trace: PlantEffectDecisionTrace = PlantEffectDecisionTrace.NONE
)

fun interface PlantEffectDecisionTrace {
    fun record(decision: PlantEffectDecisionRecord)
    companion object { val NONE = PlantEffectDecisionTrace { } }
}

data class PlantEffectDecisionRecord(
    val plantId: String,
    val effect: GameEffect,
    val decisionKind: String,
    val legalChoiceIds: List<String>,
    val selectedChoiceId: String,
    val referenceChoiceId: String?
)

/**
 * Human/mechanical preservation adapter. It delegates the exact existing
 * EffectStrategy decision and records it without changing the result.
 */
class HumanPlantEffectPolicy : PlantEffectPolicy {
    override fun strategyFor(source: PlantEffectSource, fallback: EffectStrategy): EffectStrategy =
        RecordingPlantEffectStrategy(source, fallback)
}

private class RecordingPlantEffectStrategy(
    private val source: PlantEffectSource,
    private val delegate: EffectStrategy
) : EffectStrategy by delegate {
    override fun chooseDie(r:ChooseEffectDieRequest)=record("DIE",r.legalChoices,delegate.chooseDie(r))
    override fun chooseBattleDie(r:ChooseEffectBattleDieRequest)=record("BATTLE_DIE",r.legalChoices,delegate.chooseBattleDie(r))
    override fun chooseRootWellBattle(r:ChooseRootWellBattleRequest)=record("ROOT_WELL",r.legalChoices,delegate.chooseRootWellBattle(r))
    override fun chooseCrossPlayerDieSwap(r:ChooseEffectCrossPlayerDieSwapRequest)=record("CROSS_SWAP",r.legalChoices,delegate.chooseCrossPlayerDieSwap(r))
    override fun chooseOptionalDie(r:ChooseOptionalEffectDieRequest)=recordOptional("OPTIONAL_DIE",r.legalChoices,delegate.chooseOptionalDie(r))
    override fun chooseDice(r:ChooseEffectDiceRequest):EffectDiceChoice{val x=delegate.chooseDice(r);trace("DICE",r.legalChoices.map(::plantEffectChoiceId),plantEffectChoiceId(x),plantEffectChoiceId(x));return x}
    override fun chooseDiePair(r:ChooseEffectDiePairRequest)=record("DIE_PAIR",r.legalChoices,delegate.chooseDiePair(r))
    override fun chooseOptionalDiePair(r:ChooseOptionalEffectDiePairRequest)=recordOptional("OPTIONAL_DIE_PAIR",r.legalChoices,delegate.chooseOptionalDiePair(r))
    override fun chooseCritterAndDie(r:ChooseEffectCritterDieRequest)=record("CRITTER_DIE",r.legalChoices,delegate.chooseCritterAndDie(r))
    override fun choosePetalD4Source(r:ChoosePetalD4SourceRequest)=record("PETAL_SOURCE",r.legalChoices,delegate.choosePetalD4Source(r))
    override fun choosePetalToDie4(r:ChoosePetalToDie4Request)=record("PETAL_BRANCH",r.legalChoices,delegate.choosePetalToDie4(r))
    override fun chooseBeeSource(r:ChooseBeeSourceRequest)=record("BEE_SOURCE",r.legalChoices,delegate.chooseBeeSource(r))
    override fun chooseButterflyTarget(r:ChooseEffectButterflyTargetRequest)=record("BUTTERFLY",r.legalChoices,delegate.chooseButterflyTarget(r))
    override fun chooseOptionalPlant(r:ChooseOptionalEffectPlantRequest)=recordOptional("OPTIONAL_PLANT",r.legalChoices,delegate.chooseOptionalPlant(r))
    override fun chooseOpponentPlantWound(r:ChooseEffectOpponentPlantWoundRequest)=record("OPPONENT_PLANT_WOUND",r.legalChoices,delegate.chooseOpponentPlantWound(r))
    override fun choosePlantEffect(r:ChooseEffectPlantRequest)=record("PLANT_EFFECT",r.legalChoices,delegate.choosePlantEffect(r))
    override fun chooseOEdelweiss(r:ChooseOEdelweissRequest)=record("O_EDELWEISS",r.legalChoices,delegate.chooseOEdelweiss(r))
    override fun chooseWispsToKeep(r:ChooseWispsToKeepRequest):EffectWispsChoice{val x=delegate.chooseWispsToKeep(r);trace("WISPS_KEEP",r.legalChoices.map(::plantEffectChoiceId),plantEffectChoiceId(x),plantEffectChoiceId(x));return x}
    override fun chooseDieSize(r:ChooseEffectDieSizeRequest)=record("DIE_SIZE",r.legalChoices,delegate.chooseDieSize(r))
    override fun choosePlayer(r:ChooseEffectPlayerRequest)=record("PLAYER",r.legalChoices,delegate.choosePlayer(r))
    override fun chooseStrikeRow(r:ChooseEffectStrikeRowRequest)=record("STRIKE_ROW",r.legalChoices,delegate.chooseStrikeRow(r))

    private fun <T:Any> record(kind:String,legal:List<T>,selected:T):T { val id=plantEffectChoiceId(selected);trace(kind,legal.map(::plantEffectChoiceId),id,id);return selected }
    private fun <T:Any> recordOptional(kind:String,legal:List<T>,selected:T?):T? { val id=plantEffectChoiceId(selected);trace(kind,listOf("NONE")+legal.map(::plantEffectChoiceId),id,id);return selected }
    private fun trace(kind:String,legal:List<String>,selected:String,reference:String?)=source.trace.record(PlantEffectDecisionRecord(source.plantId,source.effect,kind,legal,selected,reference))
}

/** Stable, human-readable identities for Chronicle research traces and deterministic tie breaks. */
fun plantEffectChoiceId(v:Any?):String = when(v){
    null -> "NONE"
    is EffectDieChoice -> "DIE:${v.index}:D${v.sides}@${v.value}"
    is EffectBattleDieChoice -> "BDIE:P${v.ownerId.value}:${v.row}:${plantEffectChoiceId(v.die)}"
    is RootWellBattleChoice.OwnDice -> "OWN:${plantEffectChoiceId(v.first)}:${plantEffectChoiceId(v.second)}"
    is RootWellBattleChoice.OpponentDie -> "OPP:${plantEffectChoiceId(v.die)}"
    is EffectCrossPlayerDieSwapChoice -> "SWAP:${plantEffectChoiceId(v.ownDie)}:${plantEffectChoiceId(v.opponentDie)}"
    is EffectDiceChoice -> "DICE:"+v.selected.joinToString("|"){plantEffectChoiceId(it)}
    is EffectDiePairChoice -> "PAIR:${plantEffectChoiceId(v.source)}:${plantEffectChoiceId(v.target)}"
    is EffectCritterDieChoice -> "CRITTER:${v.critter}:${plantEffectChoiceId(v.die)}"
    PetalD4SourceChoice.Grove -> "PETAL_SOURCE:GROVE"
    is PetalD4SourceChoice.Opponent -> "PETAL_SOURCE:P${v.playerId.value}:${v.zone}:${plantEffectChoiceId(v.die)}"
    PetalToDie4Choice.GainD4 -> "PETAL:GAIN"
    is PetalToDie4Choice.TrashD4AndRaiseAll -> "PETAL:TRASH:${plantEffectChoiceId(v.die)}"
    EffectBeeSourceChoice.Grove -> "BEE:GROVE"
    is EffectBeeSourceChoice.Opponent -> "BEE:P${v.playerId.value}"
    is EffectButterflyTargetChoice -> "BUTTERFLY:${v.ownerId?.value?:-1}:${v.butterfly}"
    is EffectPlantChoice -> "PLANT:${v.cardName}:${v.cardId.value}:${v.isFaceUp}"
    is EffectOpponentPlantWoundChoice.Flip -> "WOUND:FLIP:P${v.ownerId.value}:${v.cardName}:${v.cardId.value}"
    is EffectOpponentPlantWoundChoice.Snip -> "WOUND:SNIP:P${v.ownerId.value}:${v.cardName}:${v.cardId.value}"
    is OEdelweissChoice.Play -> "EDEL:PLAY:${plantEffectChoiceId(v.card)}"
    is OEdelweissChoice.Flip -> "EDEL:FLIP:${plantEffectChoiceId(v.card)}"
    OEdelweissChoice.Done -> "EDEL:DONE"
    is EffectWispsChoice -> "WISPS:"+v.selected.joinToString("|"){"${it.index}:${it.name}"}
    is DieSides -> "DIE_SIZE:${v.name}"
    is dugsolutions.leaf.v35.player.PlayerId -> "PLAYER:${v.value}"
    is dugsolutions.leaf.v35.battle.domain.StrikeRow -> "ROW:${v.name}"
    else -> v.toString()
}
