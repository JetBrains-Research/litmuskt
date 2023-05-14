import barriers.BarrierProducer
import kotlin.native.concurrent.AtomicReference
import kotlin.reflect.KClass
import kotlin.system.getTimeMillis
import kotlin.time.Duration

abstract class BasicLitmusTest(val name: String) {
    abstract fun actor1(): Any?
    abstract fun actor2(): Any?
    open fun actor3(): Any? = Placeholder
    open fun actor4(): Any? = Placeholder
    open fun actor5(): Any? = Placeholder
    open fun actor6(): Any? = Placeholder
    open fun actor7(): Any? = Placeholder
    open fun actor8(): Any? = Placeholder
    open fun arbiter() {}

    private val outcomeRef = AtomicReference<Any?>(null)
    var outcome: Any?
        protected set(v) {
            if (!outcomeRef.compareAndSet(null, v))
                throw IllegalStateException("cannot set outcome more than once")
        }
        get() = outcomeRef.value

    // note: modifies receiver object
    fun overriddenActors(): List<(BasicLitmusTest) -> Any?> {
        val actors = mutableListOf(BasicLitmusTest::actor1, BasicLitmusTest::actor2)
        if (actor3() != Placeholder) actors.add(BasicLitmusTest::actor3)
        if (actor4() != Placeholder) actors.add(BasicLitmusTest::actor4)
        if (actor5() != Placeholder) actors.add(BasicLitmusTest::actor5)
        if (actor6() != Placeholder) actors.add(BasicLitmusTest::actor6)
        if (actor7() != Placeholder) actors.add(BasicLitmusTest::actor7)
        if (actor8() != Placeholder) actors.add(BasicLitmusTest::actor8)
        return actors
    }

    companion object {
        var memShuffler: MemShuffler? = null

        private object Placeholder
    }
}

data class OutcomeSetupScope(
    var accepted: Set<Any?> = emptySet(),
    var interesting: Set<Any?> = emptySet(),
    var forbidden: Set<Any?> = emptySet(),
    var default: OutcomeType = OutcomeType.FORBIDDEN,
) {
    fun getType(outcome: Any?) = when {
        outcome in accepted -> OutcomeType.ACCEPTED
        outcome in interesting -> OutcomeType.INTERESTING
        outcome in forbidden -> OutcomeType.FORBIDDEN
        else -> default
    }
}

private val testOutcomesSetup = mutableMapOf<KClass<*>, OutcomeSetupScope>()

fun BasicLitmusTest.setupOutcomes(block: OutcomeSetupScope.() -> Unit) {
    testOutcomesSetup[this::class] = OutcomeSetupScope().apply(block)
}

fun BasicLitmusTest.getOutcomeSetup(): OutcomeSetupScope? = testOutcomesSetup[this::class]

typealias AffinityMap = List<Set<Int>>

data class LitmusTestParameters(
    val affinityMap: AffinityMap?,
    val syncPeriod: Int,
    val memShufflerProducer: (() -> MemShuffler)?,
    val barrierProducer: BarrierProducer,
)

enum class OutcomeType { ACCEPTED, INTERESTING, FORBIDDEN }

data class OutcomeInfo(
    val outcome: Any?,
    val count: Int,
    val type: OutcomeType?,
)

typealias LitmusResult = List<OutcomeInfo>

interface LitmusTestRunner {
    fun runTest(
        batchSize: Int,
        parameters: LitmusTestParameters,
        testProducer: () -> BasicLitmusTest,
    ): LitmusResult

    fun runTest(
        timeLimit: Duration,
        parameters: LitmusTestParameters,
        testProducer: () -> BasicLitmusTest,
    ): LitmusResult {
        val results = mutableListOf<OutcomeInfo>()
        val start = getTimeMillis()
        while (getTimeMillis() - start < timeLimit.inWholeMilliseconds) {
            // TODO: fix magic number
            results.addAll(runTest(10_000, parameters, testProducer))
        }
        return results.merge()
    }

    val cpuCoreCount: Int
}
