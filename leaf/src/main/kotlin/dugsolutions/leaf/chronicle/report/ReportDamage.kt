package dugsolutions.leaf.chronicle.report

import dugsolutions.leaf.chronicle.GameChronicle

class ReportDamage {

    operator fun invoke(moment: GameChronicle.Moment.DELIVER_DAMAGE): String {
        val lines = mutableListOf<String>()
        if (moment.damageToDefender > 0) {
            for (defender in moment.defenders) {
                lines.add("Player ${defender.id} took ${moment.damageToDefender}")
            }
        }
        if (moment.damageToAttacker > 0) {
            for (attacker in moment.attackers) {
                lines.add("Player ${attacker.id} took back ${moment.damageToAttacker}")
            }
        }
        return lines.joinToString(",")
    }
}
