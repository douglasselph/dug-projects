package dugsolutions.leaf.chronicle.report

import dugsolutions.leaf.chronicle.GameChronicle

class ReportDamage {

    operator fun invoke(moment: GameChronicle.Moment.DELIVER_DAMAGE): String {
        val lines = mutableListOf<String>()
        if (moment.damageToDefender > 0) {
            lines.add("Player ${moment.defender.id} took ${moment.damageToDefender}")
        }
        if (moment.damageToAttacker > 0) {
            lines.add("Player ${moment.attacker.id} took back ${moment.damageToAttacker}")
        }
        return lines.joinToString(",")
    }
}
