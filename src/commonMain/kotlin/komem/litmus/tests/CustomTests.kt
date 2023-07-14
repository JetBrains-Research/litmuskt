//package komem.litmus.tests
//
//import komem.litmus.LitmusTest
//import komem.litmus.setupOutcomes
//import kotlin.concurrent.Volatile
//
//class MP_NoDRF_Test : LitmusTest("MP + broken DRF") {
//
//    var x = 0
//    var y = 0
//
//    override  fun thread1() {
//        y = 1
//        x = 1
//    }
//
//    override  fun thread2() {
//        if (y != 0) {
//            outcome = x
//        }
//    }
//
//    init {
//        setupOutcomes {
//            accepted = setOf(1, null)
//             interesting = setOf(0)
//        }
//    }
//}
//
//class UPUBVolatileTest : LitmusTest("publication + volatile") {
//    class Holder(val x: Int)
//
//    @Volatile
//    var h: Holder? = null
//
//    override fun thread1() {
//        h = Holder(0)
//    }
//
//    override fun thread2() {
//        val t = h
//        if (t != null) {
//            outcome = t.x
//        }
//    }
//
//    init {
//        setupOutcomes {
//            accepted = setOf(0, null)
//        }
//    }
//}
//
//class UPUBArrayTest : LitmusTest("publication + array") {
//
//    var arr: Array<Int>? = null
//
//    override fun thread1() {
//        arr = Array(10) { 0 }
//    }
//
//    override fun thread2() {
//        val t = arr
//        if (t != null) {
//            outcome = t[0]
//        }
//    }
//
//    init {
//        setupOutcomes {
//            accepted = setOf(0, null)
//        }
//    }
//}
//
//class UPUBRefTest : LitmusTest("publication + reference") {
//    class Inner(val x: Int)
//
//    class Holder(val ref: Inner)
//
//    var h: Holder? = null
//
//    override fun thread1() {
//        val ref = Inner(1)
//        h = Holder(ref)
//    }
//
//    override fun thread2() {
//        val t = h
//        if (t != null) {
//            val ref = t.ref
//            outcome = ref.x
//        }
//    }
//
//    init {
//        setupOutcomes {
//            accepted = setOf(1, null)
//        }
//    }
//}
//
//class UPUBCtorLeakingTest : BasicLitmusTest("publication + leaking 'this'") {
//
//    var h: Holder? = null
//
//    inner class Holder {
//        val x: Int
//
//        init {
//            x = 1
//            h = this
//        }
//    }
//
//    override fun actor1() {
//        h = Holder()
//    }
//
//    override fun actor2() {
//        val t = h
//        if (t != null) {
//            outcome = t.x
//        }
//    }
//
//    init {
//        setupOutcomes {
//            accepted = setOf(0, 1, null)
//        }
//    }
//}
