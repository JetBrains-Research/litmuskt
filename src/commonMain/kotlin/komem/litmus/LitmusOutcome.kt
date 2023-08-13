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
