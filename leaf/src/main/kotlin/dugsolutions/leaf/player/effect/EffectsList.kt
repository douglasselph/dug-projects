package dugsolutions.leaf.player.effect

class EffectsList : Iterable<AppliedEffect> {
    private val effects = mutableListOf<AppliedEffect>()

    fun clear() {
        effects.clear()
    }

    fun add(effect: AppliedEffect) {
        effects.add(effect)
    }

    fun addAll(newEffects: List<AppliedEffect>) {
        effects.addAll(newEffects)
    }

    fun remove(effect: AppliedEffect) {
        effects.remove(effect)
    }

    fun findAdjustDieRoll(amount: Int): AppliedEffect? {
        val adjustDieRolls = effects.filterIsInstance<AppliedEffect.AdjustDieRoll>()
        return adjustDieRolls.firstOrNull { it.adjustment == amount }
    }

    fun findAdjustToMax(): AppliedEffect? {
        val adjustDieRolls = effects.filterIsInstance<AppliedEffect.AdjustDieToMax>()
        return adjustDieRolls.firstOrNull()
    }

    fun removeAll(effectsToRemove: List<AppliedEffect>) {
        effects.removeAll(effectsToRemove)
    }

    fun filterAndRemove(predicate: (AppliedEffect) -> Boolean): List<AppliedEffect> {
        val matchingEffects = effects.filter(predicate)
        effects.removeAll(matchingEffects)
        return matchingEffects
    }

    fun copy(): List<AppliedEffect> = ArrayList(effects)

    fun isEmpty(): Boolean = effects.isEmpty()

    override fun iterator(): Iterator<AppliedEffect> = effects.iterator()
} 
