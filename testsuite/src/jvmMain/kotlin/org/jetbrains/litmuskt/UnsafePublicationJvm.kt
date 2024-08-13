package org.jetbrains.litmuskt

import org.jetbrains.litmuskt.autooutcomes.*
import org.jetbrains.litmuskt.*
import java.util.concurrent.atomic.AtomicIntegerArray

@LitmusTestContainer
object UnsafePublicationJvm {
    val PlainAtomicIntegerArray = litmusTest({
        object : LitmusIOutcome() {
            var arr: AtomicIntegerArray? = null
        }
    }) {
        reset {
            arr = null
            outcomeReset()
        }
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
    }
}
