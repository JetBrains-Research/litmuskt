package org.jetbrains.litmuskt.tests

import org.jetbrains.litmuskt.LitmusTestContainer
import org.jetbrains.litmuskt.autooutcomes.LitmusZZOutcome
import org.jetbrains.litmuskt.autooutcomes.accept
import org.jetbrains.litmuskt.litmusTest

@LitmusTestContainer
object WordTearing {

    val array = litmusTest({
        object : LitmusZZOutcome() {
            val arr = BooleanArray(2)
        }
    }) {
        thread {
            arr[0] = true
        }
        thread {
            arr[1] = true
        }
        outcome {
            r1 = arr[0]
            r2 = arr[1]
            this
        }
        spec {
            accept(true, true)
        }
    }

}
