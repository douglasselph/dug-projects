package dugsolutions.leaf.chronicle.di

import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.chronicle.domain.PlayerUnderTest
import dugsolutions.leaf.chronicle.local.TestOutputFile
import dugsolutions.leaf.chronicle.local.TransformMomentToEntry
import dugsolutions.leaf.chronicle.report.GenerateGameSummaries
import dugsolutions.leaf.chronicle.report.GenerateGameSummary
import dugsolutions.leaf.chronicle.report.ReportDamage
import dugsolutions.leaf.chronicle.report.ReportGameAnalysis
import dugsolutions.leaf.chronicle.report.ReportGameBrief
import dugsolutions.leaf.chronicle.report.ReportGameSummaries
import dugsolutions.leaf.chronicle.report.ReportGameSummary
import dugsolutions.leaf.chronicle.report.ReportPlayer
import dugsolutions.leaf.chronicle.report.WriteChronicleResults
import dugsolutions.leaf.chronicle.report.WriteGameResults
import dugsolutions.leaf.chronicle.report.WriteGameSummaries
import dugsolutions.leaf.chronicle.report.WriteToFile
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
