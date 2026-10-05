package dugsolutions.leaf.v35.player.decision.learned.wisp

class WispPlayFeatureVector private constructor(private val values: DoubleArray, private val named: Map<String,Double>) {
    operator fun get(f: WispPlayFeature)=values[f.ordinal]
    fun namedValues()=named
    class Builder { private val v=DoubleArray(WispPlayFeature.entries.size); private val n=linkedMapOf<String,Double>(); operator fun set(f:WispPlayFeature,x:Number){v[f.ordinal]=x.toDouble()}; fun putNamed(k:String,x:Double){n[k]=x}; fun build()=WispPlayFeatureVector(v,n.toMap()) }
    companion object { fun build(block:Builder.()->Unit)=Builder().apply(block).build() }
}
