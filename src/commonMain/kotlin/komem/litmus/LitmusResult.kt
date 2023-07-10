package komem.litmus

typealias LitmusResult = List<LitmusOutcomeInfo>

fun LitmusResult.prettyPrint() {
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
    println((listOf(tableHeader) + table).tableFormat(true))
}

fun List<LitmusOutcomeInfo>.mergeOutcomes(): LitmusResult {
    return groupBy { it.outcome }.map { (outcome, infos) ->
        LitmusOutcomeInfo(outcome, infos.sumOf { it.count }, infos.first().type)
    }
}
