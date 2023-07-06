package komem.litmus
typealias LitmusOutcome = Any?

enum class LitmusOutcomeType { ACCEPTED, INTERESTING, FORBIDDEN }

data class LitmusOutcomeInfo(
    val outcome: LitmusOutcome,
    val count: Long,
    val type: LitmusOutcomeType?,
)

data class OutcomeSetupScope(
    var accepted: Set<LitmusOutcome> = emptySet(),
    var interesting: Set<LitmusOutcome> = emptySet(),
    var forbidden: Set<LitmusOutcome> = emptySet(),
    var default: LitmusOutcomeType = LitmusOutcomeType.FORBIDDEN,
) {
    fun getType(outcome: LitmusOutcome) = when (outcome) {
        in accepted -> LitmusOutcomeType.ACCEPTED
        in interesting -> LitmusOutcomeType.INTERESTING
        in forbidden -> LitmusOutcomeType.FORBIDDEN
        else -> default
    }
}

fun List<LitmusOutcome>.groupToInfo(outcomeSetup: OutcomeSetupScope?): List<LitmusOutcomeInfo> = this
    .groupingBy { it }
    .eachCount()
    .map { (outcome, count) ->
        LitmusOutcomeInfo(outcome, count.toLong(), outcomeSetup?.getType(outcome))
    }
