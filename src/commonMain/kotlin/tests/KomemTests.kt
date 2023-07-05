package tests

import LitmusTest
import kotlinx.atomicfu.locks.reentrantLock
import kotlinx.atomicfu.locks.withLock
import setupOutcomes
import kotlin.concurrent.Volatile

class AtomTest : LitmusTest("access atomicity") {

    var x = 0

    override fun actor1() {
        x = -1
    }

    override fun actor2() {
        outcome = x
    }

    init {
        setupOutcomes {
            accepted = setOf(0, -1)
        }
    }
}

//data class PairOutcome(val a: Int, val b: Int) {
//    override fun toString() = "($a, $b)"
//}

class SBTest : LitmusTest("store buffering") {

    var x = 0
    var y = 0

    var a = 0
    var b = 0

    override fun actor1() {
        x = 1
        a = y
    }

    override fun actor2() {
        y = 1
        b = x
    }

    override fun arbiter() {
        outcome = a to b
    }

    init {
        setupOutcomes {
            accepted = setOf(0 to 1, 1 to 0, 1 to 1)
            interesting = setOf(0 to 0)
        }
    }
}

@OptIn(ExperimentalStdlibApi::class)
class SBVolatileTest : LitmusTest("store buffering + volatile") {

    @Volatile
    var x = 0

    @Volatile
    var y = 0

    var a = 0
    var b = 0

    override fun actor1() {
        x = 1
        a = y
    }

    override fun actor2() {
        y = 1
        b = x
    }

    override fun arbiter() {
        outcome = a to b
    }

    init {
        setupOutcomes {
            accepted = setOf(0 to 1, 1 to 0, 1 to 1)
            forbidden = setOf(0 to 0)
        }
    }
}

// TODO: atomicfu mutex, sb+mutex
class MutexTest : LitmusTest("atomicfu mutex") {
    val l = reentrantLock()
    var x = 0

    var a = 0
    var b = 0

    override fun actor1() {
        l.withLock {
            a = ++x
        }
    }

    override fun actor2() {
        l.withLock {
            b = ++x
        }
    }

    override fun arbiter() {
        outcome = a to b
    }

    init {
        setupOutcomes {
            accepted = setOf(1 to 2, 2 to 1)
            forbidden = setOf(1 to 1)
        }
    }
}

class SBMutexTest : LitmusTest("SB + atomicfu lock") {
    val l = reentrantLock()
    var x = 0
    var y = 0

    var a = 0
    var b = 0

    override fun actor1() {
        l.withLock {
            x = 1
            a = y
        }
    }

    override fun actor2() {
        l.withLock {
            y = 1
            b = x
        }
    }

    override fun arbiter() {
        outcome = a to b
    }

    init {
        setupOutcomes {
            accepted = setOf(0 to 1, 1 to 0)
            forbidden = setOf(0 to 0, 1 to 1)
        }
    }
}

class MPTest : LitmusTest("message passing") {

    var x = 0
    var y = 0

    override fun actor1() {
        x = 1
        y = 1
    }

    override fun actor2() {
        val a = y
        val b = x
        outcome = a to b
    }

    init {
        setupOutcomes {
            accepted = setOf(0 to 0, 0 to 1, 1 to 1)
            interesting = setOf(1 to 0)
        }
    }
}

//val producer : Builder.() -> (List<() -> LitmusOutcome>)
//
//producer(Builder())
//producer(Builder())
//producer(Builder())
//producer(Builder())
//
//val MP_TEST = litmus {
//    class Holder(var x: Int)
//    var holder1 = Holder(0)
//    thread {
//        holder1 = Holder(1)
//        x = 1
//        y = 1
//    }
//    thread {
//        val a = y
//        val b = x
//        outcome = a to b
//    }
//    outcomes {
//        accepted = setOf(0 to 0, 0 to 1, 1 to 1)
//        interesting = setOf(1 to 0)
//    }
//}

@OptIn(ExperimentalStdlibApi::class)
class MPVolatileTest : LitmusTest("message passing + volatile") {

    var x = 0

    @Volatile
    var y = 0

    var a = 0
    var b = 0

    override fun actor1() {
        x = 1
        y = 1
    }

    override fun actor2() {
        a = y
        b = x
    }

    override fun arbiter() {
        outcome = a to b
    }

    init {
        setupOutcomes {
            accepted = setOf(0 to 0, 0 to 1, 1 to 1)
            forbidden = setOf(1 to 0)
        }
    }
}

class MPMutexTest : LitmusTest("MP + atomicfu lock") {
    val l = reentrantLock()
    var x = 0
    var y = 0

    var a = 0
    var b = 0

    override fun actor1() {
        x = 1
        l.withLock {
            y = 1
        }
    }

    override fun actor2() {
        l.withLock {
            a = y
        }
        b = x
    }

    override fun arbiter() {
        outcome = a to b
    }

    init {
        setupOutcomes {
            accepted = setOf(0 to 0, 0 to 1, 1 to 1)
            forbidden = setOf(1 to 0)
        }
    }
}

@OptIn(ExperimentalStdlibApi::class)
class MP_DRF_Test : LitmusTest("message passing + drf") {
    var x = 0

    @Volatile
    var y = 0

    override fun actor1() {
        x = 1
        y = 1
    }

    override fun actor2() {
        if (y != 0) {
            outcome = x
        }
    }

    init {
        setupOutcomes {
            accepted = setOf(1, null)
        }
    }
}

// TODO: CAS and co. tests

class CoRRTest : LitmusTest("coherence read-read") {
    var x = 0

    override fun actor1() {
        x = 1
    }

    override fun actor2() {
        val a = x
        val b = x
        outcome = a to b
    }

    init {
        setupOutcomes {
            accepted = setOf(0 to 0, 0 to 1, 1 to 1)
            interesting = setOf(1 to 0)
        }
    }
}

class CoRR_CSE_Test : LitmusTest("coherence cse") {
    class Holder(var x: Int)

    val holder1 = Holder(0)
    val holder2 = holder1

    override fun actor1() {
        holder1.x = 1
    }

    override fun actor2() {
        val h1 = holder1
        val h2 = holder2

        val a = h1.x
        val b = h2.x
        val c = h1.x

        outcome = listOf(a, b, c)
    }

    init {
        setupOutcomes {
            accepted = setOf(
                listOf(0, 0, 0),
                listOf(0, 0, 1),
                listOf(0, 1, 0),
                listOf(0, 1, 1),
                listOf(1, 1, 1),
                listOf(1, 0, 1)
            )
            interesting = setOf(
                listOf(1, 0, 0),
                listOf(1, 1, 0)
            )
        }
    }
}

class IRIWTest : LitmusTest("independent reads of independent writes") {
    var x = 0
    var y = 0

    var a = 0
    var b = 0
    var c = 0
    var d = 0

    override fun actor1() {
        x = 1
    }

    override fun actor2() {
        a = x
        b = y
    }

    override fun actor3() {
        c = y
        d = x
    }

    override fun actor4() {
        y = 1
    }

    override fun arbiter() {
        outcome = listOf(a, b, c, d)
    }

    init {
        setupOutcomes {
            interesting = setOf(
                listOf(1, 0, 1, 0)
            )
            default = OutcomeType.ACCEPTED
        }
    }
}

@OptIn(ExperimentalStdlibApi::class)
class IRIWVolatileTest : LitmusTest("iriw + volatile") {
    @Volatile
    var x = 0

    @Volatile
    var y = 0

    var a = 0
    var b = 0
    var c = 0
    var d = 0

    override fun actor1() {
        x = 1
    }

    override fun actor2() {
        a = x
        b = y
    }

    override fun actor3() {
        c = y
        d = x
    }

    override fun actor4() {
        y = 1
    }

    override fun arbiter() {
        outcome = listOf(a, b, c, d)
    }

    init {
        setupOutcomes {
            forbidden = setOf(
                listOf(1, 0, 1, 0)
            )
            default = OutcomeType.ACCEPTED
        }
    }
}

class UPUBTest : LitmusTest("publication") {
    class Holder(val x: Int)

    var h: Holder? = null

    override fun actor1() {
        h = Holder(0)
    }

    override fun actor2() {
        val t = h
        if (t != null) {
            outcome = t.x
        }
    }

    init {
        setupOutcomes {
            accepted = setOf(0, null)
        }
    }
}

class UPUBCtorTest : LitmusTest("publication + ctor") {
    class Holder {
        val x: Int = 1
    }

    var h: Holder? = null

    override fun actor1() {
        h = Holder()
    }

    override fun actor2() {
        val t = h
        if (t != null) {
            outcome = t.x
        }
    }

    init {
        setupOutcomes {
            accepted = setOf(0, 1, null)
        }
    }
}

// TODO: checking if test hangs is not supported yet

class LB_DEPS_Test : LitmusTest("out of thin air") {
    var x = 0
    var y = 0

    var a = 0
    var b = 0

    override fun actor1() {
        a = x
        y = a
    }

    override fun actor2() {
        b = y
        x = b
    }

    override fun arbiter() {
        outcome = a to b
    }

    init {
        setupOutcomes {
            accepted = setOf(0 to 0)
        }
    }
}

class LBTest : LitmusTest("load buffering") {
    var x = 0
    var y = 0

    var a = 0
    var b = 0

    override fun actor1() {
        a = x
        y = 1
    }

    override fun actor2() {
        b = y
        x = 1
    }

    override fun arbiter() {
        outcome = a to b
    }

    init {
        setupOutcomes {
            accepted = setOf(0 to 0, 1 to 0, 0 to 1)
            interesting = setOf(1 to 1)
        }
    }
}

class LBFakeDEPSTest : LitmusTest("LB + fake dependency") {
    var x = 0
    var y = 0

    var a = 0
    var b = 0

    override fun actor1() {
        a = x
        y = 1 + a * 0
    }

    override fun actor2() {
        b = y
        x = b
    }

    override fun arbiter() {
        outcome = a to b
    }

    init {
        setupOutcomes {
            accepted = setOf(0 to 0, 0 to 1)
            forbidden = setOf(1 to 1, 1 to 0)
        }
    }
}

@OptIn(ExperimentalStdlibApi::class)
class LBVolatileTest : LitmusTest("load buffering + volatile") {
    @Volatile
    var x = 0

    @Volatile
    var y = 0

    var a = 0
    var b = 0

    override fun actor1() {
        a = x
        y = 1
    }

    override fun actor2() {
        b = y
        x = 1
    }

    override fun arbiter() {
        outcome = a to b
    }

    init {
        setupOutcomes {
            accepted = setOf(0 to 0, 1 to 0, 0 to 1)
            forbidden = setOf(1 to 1)
        }
    }
}
