package org.jetbrains.litmuskt

import org.jetbrains.litmuskt.autooutcomes.LitmusAutoOutcome

data class LitmusTest<S : Any>(
    val stateProducer: () -> S,
    val threadFunctions: List<S.() -> Unit>,
    val outcomeFinalizer: (S.() -> LitmusOutcome),
    val outcomeSpec: LitmusOutcomeSpec,
    val resetFunction: S.() -> Unit,
) {
    val threadCount = threadFunctions.size
}

class LitmusTestScope<S : Any>(
    private val stateProducer: () -> S
) {
    private val threadFunctions = mutableListOf<S.() -> Unit>()
    private lateinit var outcomeFinalizer: S.() -> LitmusOutcome
    private lateinit var outcomeSpec: LitmusOutcomeSpecScope<S>

    // WARNING: we cannot use `(S as LitmusAutoOutcome).reset()` here. DO NOT FORGET to call it in the runner!
    private lateinit var stateReset: S.() -> Unit

    fun thread(function: S.() -> Unit) {
        threadFunctions.add(function)
    }

    fun outcome(function: S.() -> LitmusOutcome) {
        if (::outcomeFinalizer.isInitialized) error("cannot declare outcome more than once")
        outcomeFinalizer = function
    }

    fun spec(setup: LitmusOutcomeSpecScope<S>.() -> Unit) {
        if (::outcomeSpec.isInitialized) error("cannot declare spec more than once")
        outcomeSpec = LitmusOutcomeSpecScope<S>().apply(setup)
    }

    fun reset(function: S.() -> Unit) {
        if (::stateReset.isInitialized) error("cannot declare reset() more than once")
        stateReset = function
    }

    fun build(): LitmusTest<S> {
        if (threadFunctions.size < 2) error("tests require at least two threads")
        if (!::outcomeSpec.isInitialized) error("spec not specified")
        val outcomeFinalizer: S.() -> LitmusOutcome = when {
            ::outcomeFinalizer.isInitialized -> outcomeFinalizer
            stateProducer() is LitmusAutoOutcome -> {
                { this }
            }
            else -> error("outcome not specified")
        }
        if (!::stateReset.isInitialized) error("reset() not specified")
        return LitmusTest(stateProducer, threadFunctions, outcomeFinalizer, outcomeSpec.build(), stateReset)
    }
}

fun <S : Any> litmusTest(stateProducer: () -> S, setup: LitmusTestScope<S>.() -> Unit): LitmusTest<*> =
    LitmusTestScope(stateProducer).apply(setup).build()
