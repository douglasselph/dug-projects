package dugsolutions.leaf.v30.chronicle.di

import dugsolutions.leaf.v30.chronicle.Chronicle
import dugsolutions.leaf.v30.chronicle.GameChronicle
import dugsolutions.leaf.v30.chronicle.decision.DecisionCostEvaluator
import dugsolutions.leaf.v30.chronicle.decision.DecisionCostEvaluatorBaseline
import dugsolutions.leaf.v30.chronicle.decision.DecisionCountChronicle
import dugsolutions.leaf.v30.chronicle.decision.DecisionCountLog
import dugsolutions.leaf.v30.chronicle.decision.DecisionDirectorCounting
import dugsolutions.leaf.v30.player.decision.baseline.DecisionDirectorBaseline
import dugsolutions.leaf.v30.player.decision.domain.DecisionDirector
import org.koin.core.module.Module
import org.koin.dsl.module

val chronicleModule: Module = module {

    single { GameChronicle() }
    single<Chronicle> { get<GameChronicle>() }

    single { DecisionCountChronicle() }
    single<DecisionCountLog> { get<DecisionCountChronicle>() }
    single<DecisionCostEvaluator> { DecisionCostEvaluatorBaseline() }
    single { DecisionDirectorBaseline() }
    single<DecisionDirector> {
        DecisionDirectorCounting(
            delegate = get<DecisionDirectorBaseline>(),
            decisionCountLog = get(),
            decisionCostEvaluator = get()
        )
    }

    factory { (delegate: DecisionDirector) ->
        DecisionDirectorCounting(
            delegate = delegate,
            decisionCountLog = get(),
            decisionCostEvaluator = get()
        )
    }
}
