package tests

import BasicLitmusTest
import setupOutcomes
import kotlin.concurrent.Volatile

class MP_NoDRF_Test : BasicLitmusTest("MP + broken DRF") {

    var x = 0
    var y = 0

    override  fun actor1() {
        y = 1
        x = 1
    }

    override  fun actor2() {
        if (y != 0) {
            outcome = x
        }
    }

    init {
        setupOutcomes {
            accepted = setOf(1, null)
             interesting = setOf(0)
        }
    }
}

class UPUBVolatileTest : BasicLitmusTest("publication + volatile") {
    class Holder(val x: Int)

    @OptIn(ExperimentalStdlibApi::class)
    @Volatile
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

class UPUBArrayTest : BasicLitmusTest("publication + array") {

    var arr: Array<Int>? = null

    override fun actor1() {
        arr = Array(10) { 0 }
    }

    override fun actor2() {
        val t = arr
        if (t != null) {
            outcome = t[0]
        }
    }

    init {
        setupOutcomes {
            accepted = setOf(0, null)
        }
    }
}

class UPUBRefTest : BasicLitmusTest("publication + reference") {
    class Inner(val x: Int)

    class Holder(val ref: Inner)

    var h: Holder? = null

    override fun actor1() {
        val ref = Inner(1)
        h = Holder(ref)
    }

    override fun actor2() {
        val t = h
        if (t != null) {
            val ref = t.ref
            outcome = ref.x
        }
    }

    init {
        setupOutcomes {
            accepted = setOf(1, null)
        }
    }
}
