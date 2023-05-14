package tests

import BasicLitmusTest
import setupOutcomes

class KindaMP : BasicLitmusTest("kinda mp") {

    var x = 0
    var y = 0

    override  fun actor1() {
        y = 5
        x = 1
    }

    override  fun actor2() {
        if (x == 1) {
            val xx = x
            val yy = y
            outcome = xx to yy
        }
    }

    init {
        setupOutcomes {
             interesting = setOf(1 to 0)
        }
    }
}

// FIXME: throws "Worker is already terminated"
//class LockTest : BasicLitmusTest("mutex / withLock{}") {
//    val l = Mutex()
//    var x = 0
//    var a = 0
//    var b = 0
//
//    override  fun actor1() {
//        l.withLock {
//            a = ++x
//        }
//    }
//
//    override  fun actor2() {
//        l.withLock {
//            b = ++x
//        }
//    }
//
//    override fun arbiter() {
//        outcome = a to b
//    }
//
//    init {
//        setupOutcomes {
//            accepted = setOf(1 to 2, 2 to 1)
//            forbidden = setOf(1 to 1)
//        }
//    }
//}

class IRIW : BasicLitmusTest("multi-copy atomicity") {
    var x = 0
    var y = 0
    data class Outcome4(var a: Int, var b: Int, var c: Int, var d: Int) {
        override fun toString() = "($a, $b, $c, $d)"
    }
    val o = Outcome4(0, 0, 0, 0)

    override  fun actor1() {
        x = 1
    }

    override  fun actor2() {
        o.a = x
        o.b = y
    }

    override  fun actor3() {
        o.c = y
        o.d = x
    }

    override  fun actor4() {
        y = 1
    }

    override fun arbiter() {
        outcome = o
    }

    init {
        setupOutcomes {
            interesting = setOf(1, 0, 1, 0)
        }
    }
}
