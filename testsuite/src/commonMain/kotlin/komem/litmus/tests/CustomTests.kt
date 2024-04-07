package komem.litmus.tests

import komem.litmus.*

val MPNoDRF: LitmusTest<*> = litmusTest({
    object : LitmusIOutcome() {
        var x = 0
        var y = 0
    }
}) {
    thread {
        x = 1
        y = 1
    }
    thread {
        r1 = if (y != 0) x else -1
    }
    spec {
        accept(1)
        accept(-1)
        interesting(0)
    }
}
