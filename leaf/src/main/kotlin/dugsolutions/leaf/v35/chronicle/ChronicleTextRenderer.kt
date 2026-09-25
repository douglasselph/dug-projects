package dugsolutions.leaf.v35.chronicle

import dugsolutions.leaf.v35.chronicle.domain.GameEntry
import dugsolutions.leaf.v35.player.PlayerId

/**
 * Simple human-readable rendering of a typed Chronicle.
 *
 * This is intentionally diagnostic rather than polished presentation. The
 * Chronicle remains typed data; this renderer gives smoke runs and experiments
 * a stable way to write something a human can inspect after a game.
 */
object ChronicleTextRenderer {

    fun render(entries: List<GameEntry>): String =
        buildString {
            var roundNumber = 0
            var sequenceWithinRound = 0

            entries.forEach { entry ->
                if (entry is GameEntry.RoundRevealed) {
                    if (isNotEmpty()) appendLine()
                    roundNumber = entry.roundNumber
                    sequenceWithinRound = 0
                }
                sequenceWithinRound += 1
                appendLine(renderWithRoundPrefix(entry, roundNumber, sequenceWithinRound))
            }
        }

    /**
     * Renders a standalone entry without round context. Full Chronicle reports
     * should call [render] with the complete entry list so they receive the
     * round-local `01.001`, `01.002`, ... numbering scheme.
     */
    fun render(entry: GameEntry): String {
        val prefix = entry.sequence.toString().padStart(4, '0')
        return "$prefix  ${renderBody(entry)}"
    }

    private fun renderWithRoundPrefix(
        entry: GameEntry,
        roundNumber: Int,
        sequenceWithinRound: Int
    ): String {
        val roundPrefix = roundNumber.toString().padStart(2, '0')
        val localPrefix = sequenceWithinRound.toString().padStart(3, '0')
        return "$roundPrefix.$localPrefix  ${renderBody(entry)}"
    }

    private fun renderBody(entry: GameEntry): String =
        when (entry) {
            is GameEntry.Marker ->
                "MARKER ${entry.message}"

            is GameEntry.RoundRevealed ->
                "ROUND ${entry.roundNumber} REVEAL ${entry.cardType}: ${entry.cardName} " +
                    "[${entry.firstEffect} | ${entry.secondEffect}]"

            is GameEntry.RoundCompleted ->
                "ROUND ${entry.roundNumber} COMPLETE ${entry.cardType}: ${entry.cardName}"

            is GameEntry.DieRolled ->
                "${player(entry.playerId)} ROLL D${entry.sides}=${entry.value} " +
                    "reason=${entry.reason} rewards=${entry.rewardPolicy}"

            is GameEntry.RollReward ->
                buildString {
                    append("${player(entry.playerId)} ROLL REWARD ${entry.kind}")
                    entry.critter?.let { append(" critter=$it") }
                    entry.wispName?.let { append(" wisp=$it") }
                }

            is GameEntry.OpeningDrawCompleted ->
                "${player(entry.playerId)} ${entry.phase} OPENING DRAW complete (${entry.count} dice)"

            is GameEntry.MainAction ->
                buildString {
                    append("${player(entry.playerId)} ${entry.phase} MAIN ${entry.action}")
                    entry.actionNumber?.let { append(" #$it") }
                    entry.battleStage?.let { append(" stage=$it") }
                }

            is GameEntry.SupportAction ->
                buildString {
                    append("${player(entry.playerId)} ${entry.phase} SUPPORT ${entry.action}")
                    entry.row?.let { append(" row=$it") }
                }

            is GameEntry.EffectResolved ->
                "${player(entry.playerId)} ${entry.phase} EFFECT ${entry.sourceKind} " +
                    "${entry.sourceName}: ${entry.effect}"

            is GameEntry.DecisionReasoning ->
                buildString {
                    append("${player(entry.playerId)} DECISION ${entry.choiceLabel} ")
                    append("score=${entry.total} (base=${entry.baseScore}")
                    entry.adjustments.forEach { adjustment ->
                        val sign = if (adjustment.amount >= 0) "+" else ""
                        append(", $sign${adjustment.amount} ${adjustment.reason}")
                    }
                    append(')')
                }

            is GameEntry.BuyOrder ->
                "BUY ORDER ${entry.order.joinToString(" -> ") { player(it) }}"

            is GameEntry.Purchase ->
                "${player(entry.playerId)} PURCHASE ${entry.kind} ${entry.itemName} " +
                    "cost=${entry.cost} paid=${entry.paymentTotal} overpay=${entry.overpayment}"

            is GameEntry.Graft ->
                "${player(entry.playerId)} GRAFT ${entry.plantName}"

            is GameEntry.BattleOrder ->
                "BATTLE ORDER ${entry.order.joinToString(" -> ") { player(it) }} " +
                    "openingDice=${entry.initialDiceCount}"

            is GameEntry.StrikeResolved ->
                buildString {
                    append("STRIKE ${entry.row} ")
                    append(
                        entry.totals.joinToString(", ") { total ->
                            "${player(total.playerId)}=${total.total}" +
                                "(dice=${total.diceTotal},critters=${total.critterTotal})"
                        }
                    )
                    append(" winners=")
                    append(entry.winnerIds.joinToString(", ") { player(it) }.ifBlank { "none" })
                    append(" wounded=")
                    append(entry.woundedPlayerIds.joinToString(", ") { player(it) }.ifBlank { "none" })
                    append(" vpPerWinner=${entry.vpPerWinner}")
                }

            is GameEntry.Wound ->
                "${player(entry.playerId)} WOUND ${entry.kind} ${entry.plantName}"

            is GameEntry.Doom ->
                buildString {
                    append("DOOM count=${entry.count}")
                    if (entry.dice.isNotEmpty()) {
                        append(" [")
                        append(
                            entry.dice.joinToString(", ") { die ->
                                "${player(die.playerId)} ${die.row} ${die.sides}=${die.value}" +
                                    if (die.returnedToGraftBed) " -> GraftBed" else " -> out"
                            }
                        )
                        append(']')
                    }
                }

            is GameEntry.Refresh ->
                "${player(entry.playerId)} REFRESH"

            is GameEntry.Cleanup ->
                "${player(entry.playerId)} ${entry.phase} CLEANUP " +
                    "discardedDice=${entry.discardedDice} returnedCritters=${entry.returnedCritters} " +
                    "refreshed=${entry.refreshed}"

            is GameEntry.Upgrade ->
                "${player(entry.playerId)} UPGRADE ${entry.from} -> ${entry.to} destination=${entry.destination}"

            is GameEntry.TrashDie ->
                "${player(entry.playerId)} TRASH ${entry.sides} destination=${entry.destination}"

            is GameEntry.FinalScore ->
                "FINAL SCORE ${player(entry.playerId)} total=${entry.totalVp} " +
                    "existing=${entry.existingVp} plants=${entry.plantVp} " +
                    "wisps=${entry.unplayedWispVp} graftedPlants=${entry.graftedPlantCount}"

            is GameEntry.FinalWinners ->
                "WINNER(S) ${entry.winnerIds.joinToString(", ") { player(it) }.ifBlank { "none" }}"

            is GameEntry.GameCompleted ->
                "GAME COMPLETE rounds=${entry.roundsCompleted}"
        }

    private fun player(playerId: PlayerId): String =
        "P${playerId.value}"
}
