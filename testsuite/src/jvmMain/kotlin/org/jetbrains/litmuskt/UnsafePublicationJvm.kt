package org.jetbrains.litmuskt

import org.jetbrains.litmuskt.autooutcomes.LitmusIOutcome
import org.jetbrains.litmuskt.autooutcomes.accept
import org.jetbrains.litmuskt.autooutcomes.forbid
import java.util.concurrent.atomic.AtomicIntegerArray

@LitmusTestContainer
object UnsafePublicationJvm {
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
    }
}
