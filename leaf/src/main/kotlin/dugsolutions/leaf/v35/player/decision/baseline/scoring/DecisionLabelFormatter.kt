package dugsolutions.leaf.v35.player.decision.baseline.scoring

import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.creature.CreatureCard
import dugsolutions.leaf.v35.player.creature.GraftPlacement
import dugsolutions.leaf.v35.player.decision.battle.BattleMainAction
import dugsolutions.leaf.v35.player.decision.battle.BattleSupportAction
import dugsolutions.leaf.v35.player.decision.battle.BattleTurnAction
import dugsolutions.leaf.v35.player.decision.buy.BuyChoice
import dugsolutions.leaf.v35.player.decision.buy.BuyItem
import dugsolutions.leaf.v35.player.decision.buy.BuyPayment
import dugsolutions.leaf.v35.player.decision.cultivation.CultivationAction
import dugsolutions.leaf.v35.player.decision.cultivation.CultivationMainAction
import dugsolutions.leaf.v35.player.decision.effect.*
import dugsolutions.leaf.v35.player.decision.support.SupportAction
import dugsolutions.leaf.v35.player.decision.wound.WoundChoice
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.wisp.domain.WispCard

/**
 * Human-readable labels for scored Human Baseline decisions.
 *
 * Decision reasoning is diagnostic output, not object serialization. Raw data-class
 * toString() output can expose image/template fields and other implementation noise,
 * so scored choices use this formatter by default instead.
 */
object DecisionLabelFormatter {
    fun longForm(value: Any?): String =
        when (value) {
            null -> "null"
            is PlayerId -> player(value)
            is CultivationAction -> cultivation(value)
            is CultivationMainAction -> cultivationMain(value)
            is SupportAction -> support(value)
            is BattleMainAction -> battleMain(value)
            is BattleSupportAction -> battleSupport(value)
            is BattleTurnAction -> battleTurn(value)
            is BuyChoice -> buyChoice(value)
            is BuyItem -> buyItem(value)
            is BuyPayment -> buyPayment(value)
            is WoundChoice -> wound(value)
            is CreatureCard -> creatureCard(value)
            is PlantCard -> plantCard(value)
            is WispCard -> wispCard(value)
            is GraftPlacement -> "GraftPlacement(${value.side}@${value.position.x},${value.position.y})"
            is EffectDieChoice -> die(value)
            is EffectBattleDieChoice -> battleDie(value)
            is RootWellBattleChoice.OwnDice ->
                "RootWellOwnDice(${battleDie(value.first)}, ${battleDie(value.second)})"
            is RootWellBattleChoice.OpponentDie ->
                "RootWellOpponentDie(${battleDie(value.die)})"
            is EffectCrossPlayerDieSwapChoice ->
                "Swap(${battleDie(value.ownDie)} <-> ${battleDie(value.opponentDie)})"
            is EffectDiceChoice -> "Dice[${value.selected.joinToString(", ") { die(it) }}]"
            is EffectDiePairChoice -> "DiePair(${die(value.source)} -> ${die(value.target)})"
            is EffectCritterDieChoice -> "${value.critter} -> ${die(value.die)}"
            is PetalToDie4Choice.GainD4 -> "GainD4"
            is PetalToDie4Choice.TrashD4AndRaiseAll ->
                "TrashD4AndRaiseAll(${die(value.die)})"
            is EffectBeeSourceChoice.Grove -> "BeeSource(GROVE)"
            is EffectBeeSourceChoice.Opponent -> "BeeSource(${player(value.playerId)})"
            is EffectButterflyTargetChoice ->
                "Butterfly(${value.butterfly}${value.ownerId?.let { " from ${player(it)}" } ?: " from GROVE"})"
            is EffectPlantChoice ->
                "Plant(${value.cardName}#${value.cardId.value}, ${if (value.isFaceUp) "FACE_UP" else "FACE_DOWN"})"
            is EffectOpponentPlantWoundChoice.Flip ->
                "Flip(${player(value.ownerId)} ${value.cardName}#${value.cardId.value})"
            is EffectOpponentPlantWoundChoice.Snip ->
                "Snip(${player(value.ownerId)} ${value.cardName}#${value.cardId.value})"
            is OEdelweissChoice.Play -> "Play(${longForm(value.card)})"
            is OEdelweissChoice.Flip -> "Flip(${longForm(value.card)})"
            is OEdelweissChoice.Done -> "Done"
            is EffectWispChoice -> "Wisp(${value.name}/${value.title})"
            is EffectWispsChoice -> "Wisps[${value.selected.joinToString(", ") { "${it.name}/${it.title}" }}]"
            else -> value.toString()
        }

    private fun cultivation(value: CultivationAction): String =
        when (value) {
            is CultivationAction.Main -> buildString {
                append("Main(${cultivationMain(value.action)}")
                value.decisionProbabilityPercent?.let { append(", chance=$it%") }
                append(')')
            }
            is CultivationAction.Support -> "Support(${support(value.action)})"
            CultivationAction.Done -> "Done"
        }

    private fun cultivationMain(value: CultivationMainAction): String =
        when (value) {
            CultivationMainAction.Draw -> "Draw"
            is CultivationMainAction.ActivatePlant -> "ActivatePlant(${creatureCard(value.card)})"
            CultivationMainAction.RoundEffect1 -> "RoundEffect1"
            CultivationMainAction.RoundEffect2 -> "RoundEffect2"
        }

    private fun support(value: SupportAction): String =
        when (value) {
            is SupportAction.PlayWisp -> buildString {
                append("PlayWisp(${wispCard(value.card)}")
                value.decisionProbabilityPercent?.let { append(", chance=$it%") }
                append(')')
            }
            is SupportAction.UseWaterReroll ->
                "UseWaterReroll(D${value.die.sides}=${value.die.value}@${value.die.index})"
            SupportAction.UseWaterRefresh -> "UseWaterRefresh"
            is SupportAction.UseMulch -> "UseMulch(${value.token.sides?.let { "D${it.value}" } ?: "empty"})"
            is SupportAction.UseWormFlip -> "UseWormFlip(card#${value.cardId.value})"
            is SupportAction.UseButterfly ->
                "UseButterfly(${value.butterfly}, D${value.die.sides}=${value.die.value}@${value.die.index})"
        }

    private fun battleMain(value: BattleMainAction): String =
        when (value) {
            BattleMainAction.Draw -> "Draw"
            is BattleMainAction.ActivatePlant -> "ActivatePlant(${creatureCard(value.card)})"
            BattleMainAction.RoundEffect1 -> "RoundEffect1"
            BattleMainAction.RoundEffect2 -> "RoundEffect2"
        }

    private fun battleSupport(value: BattleSupportAction): String =
        when (value) {
            is BattleSupportAction.Shared -> support(value.action)
            is BattleSupportAction.PlaceCritter -> "PlaceCritter(${value.critter} -> ${value.row})"
        }

    private fun battleTurn(value: BattleTurnAction): String =
        when (value) {
            is BattleTurnAction.Support -> "Support(${battleSupport(value.action)})"
            is BattleTurnAction.FinalMain -> "FinalMain(${battleMain(value.action)})"
        }

    private fun buyChoice(value: BuyChoice): String =
        when (value) {
            BuyChoice.Done -> "Done"
            is BuyChoice.Purchase -> "Purchase(${buyItem(value.item)})"
        }

    private fun buyItem(value: BuyItem): String =
        when (value) {
            is BuyItem.Plant -> "Plant(${plantCard(value.card)})"
            is BuyItem.Die -> "Die(${value.sides})"
        }

    private fun buyPayment(value: BuyPayment): String = buildString {
        append("Payment(")
        val parts = mutableListOf<String>()
        parts += value.dice.map { "D${it.sides}=${it.value}" }
        parts += value.critters.map { "${it.critter}+${it.value}" }
        append(parts.joinToString(" ").ifEmpty { "-" })
        append(" -> ${value.total})")
    }

    private fun wound(value: WoundChoice): String =
        when (value) {
            is WoundChoice.Flip -> "Flip(${creatureCard(value.card)})"
            is WoundChoice.Snip -> "Snip(${creatureCard(value.card)})"
        }

    private fun creatureCard(value: CreatureCard): String =
        "${value.card.name}#${value.id.value}/${value.facing}"

    private fun plantCard(value: PlantCard): String =
        "${value.name}/${value.title} ${value.type} cost=${value.cost} effect=${value.effect}"

    private fun wispCard(value: WispCard): String =
        "${value.name}/${value.title} effect=${value.effect} VP=${value.endGameVp}" 

    private fun die(value: EffectDieChoice): String =
        "D${value.sides}=${value.value}@${value.index}"

    private fun battleDie(value: EffectBattleDieChoice): String =
        "${player(value.ownerId)} ${value.row} ${die(value.die)}"

    private fun player(id: PlayerId): String = "P${id.value}"
}
