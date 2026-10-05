package dugsolutions.leaf.v35.player.decision.learned.wisp
import dugsolutions.leaf.v35.wisp.domain.WispCard
object LearnedWispPlayCatalog { fun prepare(weights:LearnedWispPlayWeights,cards:List<WispCard>):LearnedWispPlayWeights { var w=weights; cards.forEach{c->listOf(LearnedWispPlayWeights.wispFeature(c.name),LearnedWispPlayWeights.effectFeature(c.effect.name)).forEach{k->if(k !in w.namedWeights()) w=w.withNamed(k,0.0)}};return w } }
