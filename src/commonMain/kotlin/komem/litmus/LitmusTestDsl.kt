package komem.litmus

class LTDefinition<S>(
    internal val stateProducer: () -> S
) {
    internal val threadFunctions = mutableListOf<S.() -> Any?>()
    internal var outcomeFinalizer: (S.() -> LitmusOutcome)? = null
    internal var outcomeSetup = LitmusOutcomeSetupScope()

    // outcome is the returned value
    // dev note: T just in order to return Unit neatly (i.e. to remove compile error when nothing is returned)
    fun <T> thread(function: S.() -> T) = this.also {
        threadFunctions.add(function)
    }

    // if present, thread outcomes should be ignored (to save memory)
    fun finalizeOutcome(function: S.() -> LitmusOutcome) = this.also {
        outcomeFinalizer = function
    }

    fun outcomeSetup(setup: LitmusOutcomeSetupScope.() -> Unit) = this.also {
        outcomeSetup.setup()
    }

    val threadCount get() = threadFunctions.size
}

fun <S> litmusTest(stateProducer: () -> S) = LTDefinition(stateProducer)
