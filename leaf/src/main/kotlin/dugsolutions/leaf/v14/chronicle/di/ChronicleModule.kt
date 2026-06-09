package dugsolutions.leaf.v14.chronicle.di

import dugsolutions.leaf.v14.chronicle.GameChronicle
import dugsolutions.leaf.v14.chronicle.domain.PlayerUnderTest
import dugsolutions.leaf.v14.chronicle.local.TestOutputFile
import dugsolutions.leaf.v14.chronicle.local.TransformMomentToEntry
import dugsolutions.leaf.v14.chronicle.report.GenerateGameSummaries
import dugsolutions.leaf.v14.chronicle.report.GenerateGameSummary
import dugsolutions.leaf.v14.chronicle.report.ReportDamage
import dugsolutions.leaf.v14.chronicle.report.ReportGameAnalysis
import dugsolutions.leaf.v14.chronicle.report.ReportGameBrief
import dugsolutions.leaf.v14.chronicle.report.ReportGameSummaries
import dugsolutions.leaf.v14.chronicle.report.ReportGameSummary
import dugsolutions.leaf.v14.chronicle.report.ReportPlayer
import dugsolutions.leaf.v14.chronicle.report.WriteChronicleResults
import dugsolutions.leaf.v14.chronicle.report.WriteGameResults
import dugsolutions.leaf.v14.chronicle.report.WriteGameSummaries
import dugsolutions.leaf.v14.chronicle.report.WriteToFile
import org.koin.core.module.Module
import org.koin.dsl.module

val chronicleModule: Module = module {

    single { PlayerUnderTest(get()) }

    single { GenerateGameSummary(get(), get()) }
    single { GenerateGameSummaries() }
    single { ReportPlayer() }
    single { ReportDamage() }
    single { ReportGameBrief(get()) }
    single { ReportGameSummary() }
    single { ReportGameSummaries() }
    single { ReportGameAnalysis(get()) }
    single { WriteGameResults(get(), get(), get(), get(), get()) }
    single { WriteChronicleResults(get(), get()) }
    single { WriteGameSummaries(get(), get(), get(), get()) }

    single { GameChronicle(get(), get()) }
    single { WriteToFile() }
    single { TestOutputFile() }

    single { TransformMomentToEntry(get(), get(), get(), get()) }

}
