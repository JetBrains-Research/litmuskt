package org.jetbrains.litmuskt.tests

import org.jetbrains.litmuskt.LitmusTestContainer
import org.jetbrains.litmuskt.autooutcomes.LitmusIOutcome
import org.jetbrains.litmuskt.autooutcomes.accept
import org.jetbrains.litmuskt.litmusTest

data class IntHolder(val x: Int)

class IntHolderCtor {
    val x = 1
}

@LitmusTestContainer
object UPUB {
    val plain = litmusTest({
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

    val ctor = litmusTest({
        object : LitmusIOutcome() {
            var h: IntHolderCtor? = null
        }
    }) {
        thread {
            h = IntHolderCtor()
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
