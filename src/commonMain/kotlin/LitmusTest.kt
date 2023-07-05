
import kotlin.concurrent.AtomicReference
import kotlin.reflect.KClass

//class LitmusContext {
//    private val outcomeRef = AtomicReference<Any?>(null)
//    var outcome: Any?
//        protected set(v) {
//            if (!outcomeRef.compareAndSet(null, v))
//                throw IllegalStateException("cannot set outcome more than once")
//        }
//        get() = outcomeRef.value
//
//    inline var x: Int
//        get() = { memory[shuffler(1)] }
//        set(value: Int) {  }
//
//    private val memory: IntArray = IntArray(100)
//
//    private val shuffler ...
//}


abstract class LitmusTest(val name: String) {
    // BasicLitmusTest consists of
    // - list of actors
    // - state (shared variables)
    // - outcome holder
    // - outcome setup


    // TODO: rename actors to threads
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
    fun overriddenActors(): List<(LitmusTest) -> Any?> {
        val actors = mutableListOf(LitmusTest::actor1, LitmusTest::actor2)
        if (actor3() != Placeholder) actors.add(LitmusTest::actor3)
        if (actor4() != Placeholder) actors.add(LitmusTest::actor4)
        if (actor5() != Placeholder) actors.add(LitmusTest::actor5)
        if (actor6() != Placeholder) actors.add(LitmusTest::actor6)
        if (actor7() != Placeholder) actors.add(LitmusTest::actor7)
        if (actor8() != Placeholder) actors.add(LitmusTest::actor8)
        return actors
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