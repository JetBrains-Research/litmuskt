
typealias LitmusOutcome = Any?

// TODO: rename to LitmusOutcomeType (?)
enum class OutcomeType { ACCEPTED, INTERESTING, FORBIDDEN }

data class OutcomeInfo(
    val outcome: LitmusOutcome,
    val count: Long,
    val type: OutcomeType?,
)

data class OutcomeSetupScope(
    var accepted: Set<LitmusOutcome> = emptySet(),
    var interesting: Set<LitmusOutcome> = emptySet(),
    var forbidden: Set<LitmusOutcome> = emptySet(),
    var default: OutcomeType = OutcomeType.FORBIDDEN,
) {
    fun getType(outcome: LitmusOutcome) = when {
        outcome in accepted -> OutcomeType.ACCEPTED
        outcome in interesting -> OutcomeType.INTERESTING
        outcome in forbidden -> OutcomeType.FORBIDDEN
        else -> default
    }
}

fun List<LitmusOutcome>.group(outcomeSetup: OutcomeSetupScope?): List<OutcomeInfo> = this
    .groupingBy { it }
    .eachCount()
    .map { (outcome, count) ->
        OutcomeInfo(outcome, count.toLong(), outcomeSetup?.getType(outcome))
    }
