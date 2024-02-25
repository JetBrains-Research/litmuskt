package komem.litmus

typealias LitmusOutcome = Any?

enum class LitmusOutcomeType { ACCEPTED, INTERESTING, FORBIDDEN }

data class LitmusOutcomeStats(
    val outcome: LitmusOutcome,
    val count: Long,
    val type: LitmusOutcomeType?,
)

data class LitmusOutcomeSpec(
    val accepted: Set<LitmusOutcome>,
    val interesting: Set<LitmusOutcome>,
    val forbidden: Set<LitmusOutcome>,
    val default: LitmusOutcomeType,
) {
    fun getType(outcome: LitmusOutcome) = when (outcome) {
        in accepted -> LitmusOutcomeType.ACCEPTED
        in interesting -> LitmusOutcomeType.INTERESTING
        in forbidden -> LitmusOutcomeType.FORBIDDEN
        else -> default
    }
}

/**
 * For convenience, it is possible to use `accept(vararg values)` if outcome is a [LitmusAutoState].
 *
 * Use `accept(value)` otherwise. Notice that `accept(a, b)` is NOT the same as `accept(a); accept(b)`.
 * Dev note: this is the reason why 'single values are handled differently' in some other places.
 *
 * The same applies to `interesting()` and `forbid()`.
 */
class LitmusOutcomeSpecScope<S : Any> {
    private val accepted = mutableSetOf<LitmusOutcome>()
    private val interesting = mutableSetOf<LitmusOutcome>()
    private val forbidden = mutableSetOf<LitmusOutcome>()
    private var default: LitmusOutcomeType? = null

    fun accept(outcome: LitmusOutcome) {
        accepted.add(outcome)
    }

    fun interesting(outcome: LitmusOutcome) {
        interesting.add(outcome)
    }

    fun forbid(outcome: LitmusOutcome) {
        forbidden.add(outcome)
    }

    fun default(outcomeType: LitmusOutcomeType) {
        if (default != null)
            error("cannot set default outcome type more than once")
        default = outcomeType
    }

    fun build() = LitmusOutcomeSpec(accepted, interesting, forbidden, default ?: LitmusOutcomeType.FORBIDDEN)
}

// if S is LitmusAutoState, even single values should only be interpreted as r1

fun <S : LitmusAutoState> LitmusOutcomeSpecScope<S>.accept(vararg values: LitmusOutcome) =
    accept(LitmusAutoState(values.toList()))

fun <S : LitmusAutoState> LitmusOutcomeSpecScope<S>.interesting(vararg values: LitmusOutcome) =
    interesting(LitmusAutoState(values.toList()))

fun <S : LitmusAutoState> LitmusOutcomeSpecScope<S>.forbid(vararg values: LitmusOutcome) =
    forbid(LitmusAutoState(values.toList()))

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
