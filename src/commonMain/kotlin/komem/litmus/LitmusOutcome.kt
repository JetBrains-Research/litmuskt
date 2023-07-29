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
    var accepted = setOf<LTOutcome>()
    var interesting = setOf<LTOutcome>()
    var forbidden = setOf<LTOutcome>()
    var default: LTOutcomeType = LTOutcomeType.FORBIDDEN

    fun build() = LTOutcomeSpec(accepted, interesting, forbidden, default)
}

fun List<LTOutcome>.calcStats(outcomeSpec: LTOutcomeSpec): List<LTOutcomeStats> = this
    .groupingBy { it }
    .eachCount()
    .map { (outcome, count) ->
        LTOutcomeStats(outcome, count.toLong(), outcomeSpec.getType(outcome))
    }

fun List<List<LTOutcomeStats>>.mergeStats(): List<LTOutcomeStats> {
    data class LTOutcomeStatTempData(var count: Long, var type: LTOutcomeType?)

    val statMap = mutableMapOf<LTOutcome, LTOutcomeStatTempData>()
    for (stat in this.flatten()) {
        val tempData = statMap.getOrPut(stat.outcome) { LTOutcomeStatTempData(0L, stat.type) }
        if (tempData.type != stat.type) error("merging conflicting stats: ${stat.outcome} is both ${stat.type} and ${tempData.type}")
        tempData.count += stat.count
    }
    return statMap.map { (outcome, tempData) -> LTOutcomeStats(outcome, tempData.count, tempData.type) }
}