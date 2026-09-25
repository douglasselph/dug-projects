package dugsolutions.leaf.v35.chronicle

import dugsolutions.leaf.v35.chronicle.domain.GameEntry
import dugsolutions.leaf.v35.chronicle.domain.GraftedPlantSnapshot
import dugsolutions.leaf.v35.chronicle.domain.PlayerRoundSummarySnapshot
import dugsolutions.leaf.v35.chronicle.domain.ChronicleRollRewardPolicy
import dugsolutions.leaf.v35.chronicle.domain.RollReason
import dugsolutions.leaf.v35.chronicle.domain.RollRewardKind
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.random.die.DieSides
import dugsolutions.leaf.v35.tokens.Butterfly

/**
 * Simple human-readable rendering of a typed Chronicle.
 *
 * This is intentionally diagnostic rather than polished presentation. The
 * Chronicle remains typed data; this renderer gives smoke runs and experiments
 * a stable way to write something a human can inspect after a game.
 */
object ChronicleTextRenderer {

    /**
     * Renders the full Chronicle. Compact mode is the human-facing default:
     * opening Draw 3 activity becomes one HAND line plus one optional REWARD
     * line, and scored decision reasoning is hidden. [detail] restores the
     * original line-by-line diagnostic rendering.
     */
    fun render(entries: List<GameEntry>, detail: Boolean = false): String =
        buildString {
            var roundNumber = 0
            var sequenceWithinRound = 0
            var separatorAlreadyWritten = false
            var openingRoundActive = false
            val openingDraws = mutableMapOf<PlayerId, OpeningDrawBuffer>()
            val completedOpeningDraws = mutableSetOf<PlayerId>()

            fun appendBody(body: String) {
                separatorAlreadyWritten = false
                sequenceWithinRound += 1
                val roundPrefix = roundNumber.toString().padStart(2, '0')
                val localPrefix = sequenceWithinRound.toString().padStart(3, '0')
                appendLine("$roundPrefix.$localPrefix  $body")
            }

            fun appendEntry(entry: GameEntry) {
                appendBody(renderBody(entry))
            }

            entries.forEach { entry ->
                if (entry is GameEntry.RoundRevealed) {
                    if (isNotEmpty() && !separatorAlreadyWritten) appendLine()
                    separatorAlreadyWritten = false
                    roundNumber = entry.roundNumber
                    sequenceWithinRound = 0
                    openingRoundActive = true
                    openingDraws.clear()
                    completedOpeningDraws.clear()
                }

                if (!detail && entry is GameEntry.DecisionReasoning) {
                    return@forEach
                }

                if (!detail && openingRoundActive) {
                    when (entry) {
                        is GameEntry.DieRolled -> {
                            val buffer = openingDraws.getOrPut(entry.playerId, ::OpeningDrawBuffer)
                            if (
                                entry.playerId !in completedOpeningDraws &&
                                entry.reason == RollReason.DRAW &&
                                buffer.dice.size < OPENING_HAND_SIZE
                            ) {
                                buffer.dice += OpeningDie(entry.sides, entry.value)
                                buffer.awaitingReward =
                                    entry.rewardPolicy == ChronicleRollRewardPolicy.NORMAL &&
                                        entry.value in 1..2
                                return@forEach
                            }
                        }

                        is GameEntry.RollReward -> {
                            val buffer = openingDraws[entry.playerId]
                            if (
                                entry.playerId !in completedOpeningDraws &&
                                buffer?.awaitingReward == true
                            ) {
                                compactReward(entry)?.let(buffer.rewards::add)
                                buffer.awaitingReward = false
                                return@forEach
                            }
                        }

                        is GameEntry.OpeningDrawCompleted -> {
                            val buffer = openingDraws.getOrPut(entry.playerId, ::OpeningDrawBuffer)
                            appendBody(renderOpeningHand(entry.playerId, buffer.dice))
                            if (buffer.rewards.isNotEmpty()) {
                                appendBody(
                                    "${player(entry.playerId)} REWARD " +
                                        buffer.rewards.joinToString(" ")
                                )
                            }
                            completedOpeningDraws += entry.playerId
                            return@forEach
                        }

                        else -> Unit
                    }
                }

                appendEntry(entry)

                if (entry is GameEntry.RoundCompleted && entry.playerSummaries.isNotEmpty()) {
                    openingRoundActive = false
                    appendLine()
                    entry.playerSummaries.forEach { summary ->
                        sequenceWithinRound += 1
                        appendLine(
                            renderSummaryWithRoundPrefix(
                                summary = summary,
                                roundNumber = roundNumber,
                                sequenceWithinRound = sequenceWithinRound
                            )
                        )
                    }
                    appendLine()
                    separatorAlreadyWritten = true
                }
            }
        }

    private data class OpeningDie(
        val sides: Int,
        val value: Int
    )

    private class OpeningDrawBuffer {
        val dice = mutableListOf<OpeningDie>()
        val rewards = mutableListOf<String>()
        var awaitingReward: Boolean = false
    }

    private fun renderOpeningHand(playerId: PlayerId, dice: List<OpeningDie>): String =
        buildString {
            append("${player(playerId)} HAND")
            if (dice.isEmpty()) {
                append(" -")
            } else {
                dice.forEach { die ->
                    append(" D${die.sides}=${die.value}")
                }
            }
        }

    private fun compactReward(entry: GameEntry.RollReward): String? =
        when (entry.kind) {
            RollRewardKind.CRITTER_GAINED -> entry.critter?.name
            RollRewardKind.WISP_GAINED,
            RollRewardKind.WISP_PLAYED_IMMEDIATELY -> entry.wispName?.uppercase()
            RollRewardKind.IGNORED,
            RollRewardKind.CRITTER_UNAVAILABLE,
            RollRewardKind.WISP_UNAVAILABLE -> null
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

    private fun renderSummaryWithRoundPrefix(
        summary: PlayerRoundSummarySnapshot,
        roundNumber: Int,
        sequenceWithinRound: Int
    ): String {
        val roundPrefix = roundNumber.toString().padStart(2, '0')
        val localPrefix = sequenceWithinRound.toString().padStart(3, '0')
        return "$roundPrefix.$localPrefix  ${renderPlayerSummary(summary)}"
    }

    private fun renderPlayerSummary(summary: PlayerRoundSummarySnapshot): String =
        buildList {
            add(player(summary.playerId))
            add("S=${renderDiceCounts(summary.supplyDice)}")
            add("D=${renderDiceCounts(summary.discardDice)}")
            if (summary.beeCount > 0) add("B=${summary.beeCount}")
            if (summary.wormCount > 0) add("W=${summary.wormCount}")
            if (summary.waterCount > 0) add("Wa=${summary.waterCount}")
            if (summary.mulchDice.isNotEmpty()) {
                add(
                    "M=${summary.mulchDice.size}[" +
                        summary.mulchDice.joinToString(",") { it?.toString() ?: "?" } +
                        "]"
                )
            }
            if (summary.wispCount > 0) add("Wi=${summary.wispCount}")
            if (summary.butterflies.isNotEmpty()) {
                add(
                    "BF=${summary.butterflies.size}[" +
                        summary.butterflies
                            .sortedBy { it.ordinal }
                            .joinToString(",", transform = ::butterflyAbbreviation) +
                        "]"
                )
            }
            add(renderGraftedPlants(summary.graftedPlants))
        }.joinToString(" ")

    private fun renderGraftedPlants(plants: List<GraftedPlantSnapshot>): String {
        if (plants.isEmpty()) return "G[]"

        val counts = plants.groupingBy { it }.eachCount()
        val body = counts.entries
            .sortedWith(
                compareBy<Map.Entry<GraftedPlantSnapshot, Int>>(
                    { plantTypeOrder(it.key.type) },
                    { it.key.cost }
                )
            )
            .joinToString(" ") { (plant, count) ->
                "$count${plantTypeAbbreviation(plant.type)}${plant.cost}"
            }
        return "G[$body]"
    }

    private fun plantTypeOrder(type: PlantType): Int =
        when (type) {
            PlantType.ROOT -> 0
            PlantType.VINE -> 1
            PlantType.FLOWER -> 2
        }

    private fun plantTypeAbbreviation(type: PlantType): String =
        when (type) {
            PlantType.ROOT -> "R"
            PlantType.VINE -> "V"
            PlantType.FLOWER -> "F"
        }

    private fun renderDiceCounts(dice: List<DieSides>): String {
        if (dice.isEmpty()) return "-"
        val counts = dice.groupingBy { it }.eachCount()
        return DieSides.entries
            .mapNotNull { sides ->
                counts[sides]?.let { count -> "$count$sides" }
            }
            .joinToString(",")
    }

    private fun butterflyAbbreviation(butterfly: Butterfly): String =
        when (butterfly) {
            Butterfly.GREEN -> "GB"
            Butterfly.YELLOW -> "YB"
            Butterfly.RED -> "RB"
            Butterfly.PURPLE -> "PB"
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

    private const val OPENING_HAND_SIZE = 3
}
