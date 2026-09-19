package dugsolutions.leaf.v35.common

import java.nio.file.Path

object CardDataFiles {

    private const val DATA_DIRECTORY = "data"

    const val ROOT_CARD_LIST = "Cultivation_Cards - Root.csv"
    const val VF_CARD_LIST = "Cultivation_Cards - VF.csv"
    const val WISP_LIST = "Wisp_Cards.csv"
    const val ROUND_CARD_LIST = "Turn_Cards.csv"

    fun dataPath(
        fileName: String,
        directory: Path = dataDirectory()
    ): String =
        directory.resolve(fileName).toString()

    fun dataDirectory(): Path =
        Path.of(DATA_DIRECTORY)
}
