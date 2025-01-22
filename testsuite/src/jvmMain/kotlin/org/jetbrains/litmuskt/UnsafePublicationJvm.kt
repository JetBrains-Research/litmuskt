package org.jetbrains.litmuskt

import org.jetbrains.litmuskt.autooutcomes.LitmusIOutcome
import org.jetbrains.litmuskt.autooutcomes.accept
import org.jetbrains.litmuskt.autooutcomes.forbid
import org.jetbrains.litmuskt.autooutcomes.interesting
import java.util.concurrent.atomic.AtomicIntegerArray

@LitmusTestContainer
object UnsafePublicationJvm {

// TODO: adding new tests or test classes leads to compilation errors 
/*    val PlainArray = litmusTest({
        object : LitmusIOutcome() {
            var arr: Array<Int>? = null
        }
    }) {
        thread {
            arr = Array(1) { 1 }
        }
        thread {
            r1 = arr?.get(0) ?: -1
        }
        spec {
            accept(1)
            // 0 is the default value for `Int`. However, since Int-s in `Array<Int>` are boxed, we don't get to see a 0.
            // On JVM, a NullPointerException here is technically valid. Currently, there is no support for exceptions as accepted outcomes.
            // On Native, there is no NullPointerException, so we can see a segmentation fault.
            interesting(0)
            accept(-1)
        }
        reset {
            arr = null
        }
    }
*/
    val PlainAtomicIntegerArray = litmusTest({
        object : LitmusIOutcome() {
            var arr: AtomicIntegerArray? = null
        }
    }) {
        thread {
            arr = AtomicIntegerArray(intArrayOf(1))
        }
        thread {
            r1 = arr?.get(0) ?: -1
        }
        spec {
            accept(1)
            forbid(0)
            accept(-1)
        }
        reset {
            arr = null
        }
    }
}
