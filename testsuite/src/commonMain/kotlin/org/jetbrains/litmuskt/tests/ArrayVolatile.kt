package org.jetbrains.litmuskt.tests

import org.jetbrains.litmuskt.LitmusTestContainer
import org.jetbrains.litmuskt.autooutcomes.LitmusIIOutcome
import org.jetbrains.litmuskt.autooutcomes.accept
import org.jetbrains.litmuskt.autooutcomes.interesting
import org.jetbrains.litmuskt.litmusTest

// source: https://github.com/openjdk/jcstress/blob/master/jcstress-samples/src/main/java/org/openjdk/jcstress/samples/jmm/advanced/AdvancedJMM_08_ArrayVolatility.java
@LitmusTestContainer
object ArrayVolatile {

    val vol = litmusTest({
        object : LitmusIIOutcome() {
            val arr = IntArray(2)
        }
    }) {
        thread {
            arr[0] = 1
            arr[1] = 1
        }
        thread {
            r1 = arr[1]
            r2 = arr[0]
        }
        spec {
            accept(0, 0)
            accept(1, 1)
            accept(0, 1)
            interesting(1, 0)
        }
    }

}
