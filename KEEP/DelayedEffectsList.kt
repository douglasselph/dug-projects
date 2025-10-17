package dugsolutions.leaf.player.effect

import dugsolutions.leaf.player.domain.AppliedEffect

class DelayedEffectsList : Iterable<AppliedEffect> {

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

    override fun toString(): String {
        return "$effects"
    }


} 
