package org.jetbrains.litmuskt.tests

import org.jetbrains.litmuskt.autooutcomes.*
import org.jetbrains.litmuskt.*
import kotlin.concurrent.AtomicIntArray

object UnsafePublicationNative {

    @OptIn(kotlin.ExperimentalStdlibApi::class)
    val PlainAtomicIntArray = litmusTest({
        object : LitmusIOutcome() {
            var arr: AtomicIntArray? = null
        }
    }) {
        reset {
            arr = null
            outcomeReset()
        }
        thread {
            arr = AtomicIntArray(10) { 1 }
        }
        thread {
            r1 = arr?.get(0) ?: -1
        }
        spec {
            accept(1)
            interesting(0)
            accept(-1)
        }
    }
}
