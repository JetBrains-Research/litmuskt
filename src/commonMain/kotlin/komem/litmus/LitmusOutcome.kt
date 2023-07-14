package komem.litmus

typealias LitmusOutcome = Any?

enum class LitmusOutcomeType { ACCEPTED, INTERESTING, FORBIDDEN }

data class LitmusOutcomeInfo(
    val outcome: LitmusOutcome,
    val count: Long,
    val type: LitmusOutcomeType?,
)

data class LitmusOutcomeSetupScope(
    var accepted: Set<LitmusOutcome> = mutableSetOf(),
    var interesting: Set<LitmusOutcome> = mutableSetOf(),
    var forbidden: Set<LitmusOutcome> = mutableSetOf(),
    var default: LitmusOutcomeType = LitmusOutcomeType.FORBIDDEN,
) {
    fun getType(outcome: LitmusOutcome) = when (outcome) {
        in accepted -> LitmusOutcomeType.ACCEPTED
        in interesting -> LitmusOutcomeType.INTERESTING
        in forbidden -> LitmusOutcomeType.FORBIDDEN
        else -> default
    }
}

fun List<LitmusOutcome>.groupIntoInfo(outcomeSetup: LitmusOutcomeSetupScope?): List<LitmusOutcomeInfo> = this
    .groupingBy { it }
    .eachCount()
    .map { (outcome, count) ->
        LitmusOutcomeInfo(outcome, count.toLong(), outcomeSetup?.getType(outcome))
    }
