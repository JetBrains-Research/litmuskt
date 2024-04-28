package org.jetbrains.litmuskt.tests

import org.jetbrains.litmuskt.LitmusIOutcome
import org.jetbrains.litmuskt.LitmusTestContainer
import org.jetbrains.litmuskt.accept
import org.jetbrains.litmuskt.litmusTest

@LitmusTestContainer
object ATOM {
    val plain = litmusTest({
        object : LitmusIOutcome() {
            var x = 0
        }
    }) {
        thread {
            x = -1 // signed 0xFFFFFFFF
        }
        thread {
            r1 = x
        }
        spec {
            accept(0)
            accept(-1)
        }
    }
}
