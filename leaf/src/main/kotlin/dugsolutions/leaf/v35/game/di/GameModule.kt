package dugsolutions.leaf.v35.game.di

import dugsolutions.leaf.v35.game.GameRunner
import dugsolutions.leaf.v35.game.round.RoundCoordinator
import dugsolutions.leaf.v35.game.round.battle.BattleRound
import dugsolutions.leaf.v35.game.round.cultivation.CultivationRound
import dugsolutions.leaf.v35.game.scoring.FinalScorer
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Production engine services are application-wide because they retain no
 * mutable per-game/per-round state. GameFactory continues to create the live
 * Game graph; the runner/coordinators execute against the Game passed to them.
 */
val gameModule: Module = module {
    single {
        GameFactory(
            groveFactory = get(),
            roundCardManager = get()
        )
    }

    single { CultivationRound(effectExecutor = get()) }
    single { BattleRound(effectExecutor = get()) }

    single {
        RoundCoordinator(
            cultivation = get<CultivationRound>(),
            battle = get<BattleRound>()
        )
    }

    single { FinalScorer() }

    single {
        GameRunner(
            roundCoordinator = get(),
            finalScorer = get()
        )
    }
}
