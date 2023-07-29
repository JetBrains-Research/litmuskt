package komem.litmus

typealias LTOutcome = Any?

enum class LTOutcomeType { ACCEPTED, INTERESTING, FORBIDDEN }

data class LTOutcomeStats(
    val outcome: LTOutcome,
    val count: Long,
    val type: LTOutcomeType?,
)

data class LTOutcomeSpec(
    val accepted: Set<LTOutcome>,
    val interesting: Set<LTOutcome>,
    val forbidden: Set<LTOutcome>,
    val default: LTOutcomeType,
) {
    fun getType(outcome: LTOutcome) = when (outcome) {
        in accepted -> LTOutcomeType.ACCEPTED
        in interesting -> LTOutcomeType.INTERESTING
        in forbidden -> LTOutcomeType.FORBIDDEN
        else -> default
    }
}

class LTOutcomeSpecScope {
    private val accepted = mutableSetOf<LTOutcome>()
    private val interesting = mutableSetOf<LTOutcome>()
    private val forbidden = mutableSetOf<LTOutcome>()
    var defaultTo: LTOutcomeType = LTOutcomeType.FORBIDDEN

    fun accept(outcome: LTOutcome) {
        accepted.add(outcome)
    }

    fun accept(vararg outcome: LTOutcome) {
        accepted.add(outcome.toList())
    }

    fun interesting(outcome: LTOutcome) {
        interesting.add(outcome)
    }

    fun interesting(vararg outcome: LTOutcome) {
        interesting.add(outcome.toList())
    }

    fun forbid(outcome: LTOutcome) {
        forbidden.add(outcome)
    }

    fun forbid(vararg outcome: LTOutcome) {
        forbidden.add(outcome.toList())
    }

    fun build() = LTOutcomeSpec(accepted, interesting, forbidden, defaultTo)
}

typealias LTResult = List<LTOutcomeStats>

fun LTResult.prettyPrint() {
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

fun List<LTResult>.mergeResults(): LTResult {
    data class LTOutcomeStatTempData(var count: Long, var type: LTOutcomeType?)

    val statMap = mutableMapOf<LTOutcome, LTOutcomeStatTempData>()
    for (stat in this.flatten()) {
        val tempData = statMap.getOrPut(stat.outcome) { LTOutcomeStatTempData(0L, stat.type) }
        if (tempData.type != stat.type) error("merging conflicting stats: ${stat.outcome} is both ${stat.type} and ${tempData.type}")
        tempData.count += stat.count
    }
    return statMap.map { (outcome, tempData) -> LTOutcomeStats(outcome, tempData.count, tempData.type) }
}