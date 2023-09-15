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
    private lateinit var outcomeSpec: LTOutcomeSpecScope

    fun thread(function: S.() -> Unit) {
        threadFunctions.add(function)
    }

    fun outcome(function: S.() -> LTOutcome) {
        if (::outcomeFinalizer.isInitialized) error("cannot set outcome more than once")
        outcomeFinalizer = function
    }

    fun spec(setup: LTOutcomeSpecScope.() -> Unit) {
        if (::outcomeSpec.isInitialized) error("cannot set spec more than once")
        outcomeSpec = LTOutcomeSpecScope().apply(setup)
    }

    fun build(): LTDefinition<S> {
        if (threadFunctions.size < 2) error("tests require at least two threads")
        if (!::outcomeSpec.isInitialized) error("spec not specified")
        val outcomeFinalizer: S.() -> LTOutcome = when {
            ::outcomeFinalizer.isInitialized -> outcomeFinalizer
            stateProducer() is AutoOutcome -> {
                { (this as AutoOutcome).getOutcome() }
            }

            else -> error("outcome not specified")
        }
        return LTDefinition(stateProducer, threadFunctions, outcomeFinalizer, outcomeSpec.build())
    }
}

fun <S> litmusTest(stateProducer: () -> S, setup: LTDefinitionScope<S>.() -> Unit) =
    LTDefinitionScope(stateProducer).apply(setup).build()
