package komem.litmus
import kotlin.concurrent.AtomicReference
import kotlin.reflect.KClass

abstract class LitmusTest(val name: String) {

    abstract fun thread1(): Any?
    abstract fun thread2(): Any?
    open fun thread3(): Any? = Placeholder
    open fun thread4(): Any? = Placeholder
    open fun thread5(): Any? = Placeholder
    open fun thread6(): Any? = Placeholder
    open fun thread7(): Any? = Placeholder
    open fun thread8(): Any? = Placeholder

    open fun arbiter() {}

    private val outcomeRef = AtomicReference<Any?>(null)
    var outcome: Any?
        protected set(v) {
            if (!outcomeRef.compareAndSet(null, v))
                throw IllegalStateException("cannot set outcome more than once")
        }
        get() = outcomeRef.value

    // note: modifies receiver object
    fun overriddenThreads(): List<(LitmusTest) -> Any?> {
        val threads = mutableListOf(LitmusTest::thread1, LitmusTest::thread2)
        if (thread3() != Placeholder) threads.add(LitmusTest::thread3)
        if (thread4() != Placeholder) threads.add(LitmusTest::thread4)
        if (thread5() != Placeholder) threads.add(LitmusTest::thread5)
        if (thread6() != Placeholder) threads.add(LitmusTest::thread6)
        if (thread7() != Placeholder) threads.add(LitmusTest::thread7)
        if (thread8() != Placeholder) threads.add(LitmusTest::thread8)
        return threads
    }

    companion object {
        var memShuffler: MemShuffler? = null

        private object Placeholder
    }
}

private val testOutcomesSetup = mutableMapOf<KClass<*>, OutcomeSetupScope>()

fun LitmusTest.setupOutcomes(block: OutcomeSetupScope.() -> Unit) {
    testOutcomesSetup[this::class] = OutcomeSetupScope().apply(block)
}

fun LitmusTest.getOutcomeSetup(): OutcomeSetupScope? = testOutcomesSetup[this::class]
