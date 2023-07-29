package komem.litmus.testsuite

import komem.litmus.LTDefinition
import komem.litmus.litmusTest

val MPNoDRF: LTDefinition<*> = litmusTest({
    object {
        var x = 0
        var y = 0
        var o = 0
    }
}) {
    thread {
        x = 1
        y = 1
    }
    thread {
        o = if (y != 0) x else -1
    }
    outcome { o }
    spec {
        accepted = setOf(1, -1)
        interesting = setOf(0)
    }
}
