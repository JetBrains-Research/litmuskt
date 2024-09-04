package org.jetbrains.litmuskt.tests

import org.jetbrains.litmuskt.LitmusTestContainer
import org.jetbrains.litmuskt.autooutcomes.LitmusIOutcome
import org.jetbrains.litmuskt.autooutcomes.accept
import org.jetbrains.litmuskt.autooutcomes.interesting
import org.jetbrains.litmuskt.litmusTest
import kotlin.concurrent.AtomicIntArray

@LitmusTestContainer
object UnsafePublicationNative {

    @OptIn(ExperimentalStdlibApi::class)
    val PlainAtomicIntArray = litmusTest({
        object : LitmusIOutcome() {
            var arr: AtomicIntArray? = null
        }
    }) {
        thread {
            arr = AtomicIntArray(1) { 1 }
        }
        thread {
            r1 = arr?.get(0) ?: -1
        }
        spec {
            accept(1)
            interesting(0)
            accept(-1)
        }
        reset {
            arr = null
        }
    }
}
