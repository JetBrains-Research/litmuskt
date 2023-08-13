package komem.litmus

data class LTDefinition<S>(
    val stateProducer: () -> S,
    val threadFunctions: List<S.() -> Any?>,
    val outcomeFinalizer: (S.() -> LTOutcome),
    val outcomeSpec: LTOutcomeSpec
) {
    val threadCount = threadFunctions.size
}

class LTDefinitionScope<S>(
    private val stateProducer: () -> S
) {
    private val threadFunctions = mutableListOf<S.() -> Any?>()
    private lateinit var outcomeFinalizer: S.() -> LTOutcome
    private var outcomeSpec = LTOutcomeSpecScope()

    // outcome is the returned value
    fun thread(function: S.() -> Unit) {
        threadFunctions.add(function)
    }

    fun outcome(function: S.() -> LTOutcome) {
        outcomeFinalizer = function
    }

    fun spec(setup: LTOutcomeSpecScope.() -> Unit) {
        outcomeSpec.setup()
    }

    fun build(): LTDefinition<S> {
        if (!::outcomeFinalizer.isInitialized) throw IllegalStateException("outcome not set")
        return LTDefinition(stateProducer, threadFunctions, outcomeFinalizer, outcomeSpec.build())
    }

    val threadCount get() = threadFunctions.size
}

fun <S> litmusTest(stateProducer: () -> S, setup: LTDefinitionScope<S>.() -> Unit) =
    LTDefinitionScope(stateProducer).apply(setup).build()
