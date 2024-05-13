package org.jetbrains.litmuskt.tests

import org.jetbrains.litmuskt.LitmusIOutcome
import org.jetbrains.litmuskt.LitmusTestContainer
import org.jetbrains.litmuskt.accept
import org.jetbrains.litmuskt.litmusTest

data class IntHolder(val x: Int)

class IntHolderWithConstructor(val x: Int = 1)

@LitmusTestContainer
object UnsafePublication {

    val Plain = litmusTest({
        object : LitmusIOutcome() {
            var h: IntHolder? = null
        }
    }) {
        thread {
            h = IntHolder(0)
        }
        thread {
            r1 = h?.x ?: -1
        }
        spec {
            accept(0)
            accept(-1)
        }
    }

    val PlainWithConstructor = litmusTest({
        object : LitmusIOutcome() {
            var h: IntHolderWithConstructor? = null
        }
    }) {
        thread {
            h = IntHolderWithConstructor()
        }
        thread {
            r1 = h?.x ?: -1
        }
        spec {
            accept(1)
            accept(-1)
        }
    }

}
