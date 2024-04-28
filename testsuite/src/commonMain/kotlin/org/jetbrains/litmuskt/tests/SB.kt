package org.jetbrains.litmuskt.tests

import org.jetbrains.litmuskt.*
import kotlin.concurrent.Volatile

@LitmusTestContainer
object SB {
    val plain = litmusTest({
        object : LitmusIIOutcome() {
            var x = 0
            var y = 0
        }
    }) {
        thread {
            x = 1
            r1 = y
        }
        thread {
            y = 1
            r2 = x
        }
        // no need for explicit outcome{}
        spec {
            accept(0, 1)
            accept(1, 0)
            accept(1, 1)
            interesting(0, 0)
        }
    }

    // a sample of infix syntax
    val infix = litmusTest {
        object : LitmusIIOutcome() {
            var x = 0
            var y = 0
        }
    } thread {
        x = 1
        r1 = y
    } thread {
        y = 1
        r2 = x
    } spec {
        accept(0, 1)
        accept(1, 0)
        accept(1, 1)
        interesting(0, 0)
    }

    val volatile = litmusTest({
        object : LitmusIIOutcome() {
            @Volatile
            var x = 0

            @Volatile
            var y = 0
        }
    }) {
        thread {
            x = 1
            r1 = y
        }
        thread {
            y = 1
            r2 = x
        }
        spec {
            accept(0, 1)
            accept(1, 0)
            accept(1, 1)
            forbid(0, 0) // redundant as forbidden is the default
        }
    }
}
