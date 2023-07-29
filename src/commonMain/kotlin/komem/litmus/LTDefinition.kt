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
