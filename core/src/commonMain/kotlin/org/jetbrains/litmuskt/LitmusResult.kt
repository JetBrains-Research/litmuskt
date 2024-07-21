package org.jetbrains.litmuskt

typealias LitmusResult = List<LitmusOutcomeStats>

fun LitmusResult.generateTable(): String {
    val totalCount = sumOf { it.count }
    val table = this.sortedByDescending { it.count }.map {
        val freq = it.count.toDouble() / totalCount
        listOf(
            it.outcome.toString(),
            it.type.toString(),
            it.count.toString(),
            if (freq < 1e-5) "<0.001%" else "${(freq * 100).toString().take(6)}%"
        )
    }
    val tableHeader = listOf("outcome", "type", "count", "frequency")
    return (listOf(tableHeader) + table).tableFormat(true)
}

fun List<LitmusResult>.mergeResults(): LitmusResult {
    data class LTOutcomeStatTempData(var count: Long, var type: LitmusOutcomeType?)

    val statMap = mutableMapOf<LitmusOutcome, LTOutcomeStatTempData>()
    for (stat in this.flatten()) {
        val tempData = statMap.getOrPut(stat.outcome) { LTOutcomeStatTempData(0L, stat.type) }
        if (tempData.type != stat.type) error("merging conflicting stats: ${stat.outcome} is both ${stat.type} and ${tempData.type}")
        tempData.count += stat.count
    }
    return statMap.map { (outcome, tempData) -> LitmusOutcomeStats(outcome, tempData.count, tempData.type) }
}
