package komem.litmus.testsuite

import komem.litmus.LitmusIIOutcome
import komem.litmus.LitmusTestScope
import komem.litmus.LitmusTestState
import komem.litmus.toTest

class SBState(
    var x: Int = 0,
    var y: Int = 0,
) : LitmusTestState<SBState>, LitmusIIOutcome() {
    override fun LitmusTestScope<SBState>.setup() {
        thread {
            x = 1
            r1 = y
        }
        thread {
            y = 1
            r2 = x
        }
        spec {
            accept(1, 1)
            accept(1, 0)
            accept(0, 1)
            interesting(0, 0)
        }
    }
}


fun haha() {
    { SBState() }.toTest()
}