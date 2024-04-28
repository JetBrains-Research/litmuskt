package org.jetbrains.litmuskt

data class LitmusTest<S : Any>(
    val stateProducer: () -> S,
    val threadFunctions: List<S.() -> Unit>,
    val outcomeFinalizer: (S.() -> LitmusOutcome),
    val outcomeSpec: LitmusOutcomeSpec
) {
    val threadCount = threadFunctions.size
}

class LitmusTestScope<S : Any>(
    private val stateProducer: () -> S
) {
    private val threadFunctions = mutableListOf<S.() -> Unit>()
    private lateinit var outcomeFinalizer: S.() -> LitmusOutcome
    private lateinit var outcomeSpec: LitmusOutcomeSpecScope<S>

    fun thread(function: S.() -> Unit) {
        threadFunctions.add(function)
    }

    fun outcome(function: S.() -> LitmusOutcome) {
        if (::outcomeFinalizer.isInitialized) error("cannot set outcome more than once")
        outcomeFinalizer = function
    }

    fun spec(setup: LitmusOutcomeSpecScope<S>.() -> Unit) {
        if (::outcomeSpec.isInitialized) error("cannot set spec more than once")
        outcomeSpec = LitmusOutcomeSpecScope<S>().apply(setup)
    }

    fun build(): LitmusTest<*> {
        if (threadFunctions.size < 2) error("tests require at least two threads")
        if (!::outcomeSpec.isInitialized) error("spec not specified")
        val outcomeFinalizer: S.() -> LitmusOutcome = when {
            ::outcomeFinalizer.isInitialized -> outcomeFinalizer
            stateProducer() is LitmusAutoOutcome -> {
                { this }
            }

            else -> error("outcome not specified")
        }
        return LitmusTest(stateProducer, threadFunctions, outcomeFinalizer, outcomeSpec.build())
    }

    inner class InfixStage1 {
        infix fun thread(function: S.() -> Unit): InfixStage1 = this.also {
            this@LitmusTestScope.thread(function)
        }

        infix fun outcome(setup: S.() -> LitmusOutcome) = InfixStage2().also {
            this@LitmusTestScope.outcome(setup)
        }
    }

    inner class InfixStage2 {
        infix fun spec(setup: LitmusOutcomeSpecScope<S>.() -> Unit): LitmusTest<*> {
            this@LitmusTestScope.spec(setup)
            return build()
        }
    }
}

infix fun <S : LitmusAutoOutcome> (LitmusTestScope<S>.InfixStage1).spec(
    setup: LitmusOutcomeSpecScope<S>.() -> Unit
) = outcome { this@outcome }.spec(setup)

fun <S : Any> litmusTest(stateProducer: () -> S, setup: LitmusTestScope<S>.() -> Unit) =
    LitmusTestScope(stateProducer).apply(setup).build()

fun <S : Any> litmusTest(stateProducer: () -> S) = LitmusTestScope(stateProducer).InfixStage1()
