package komem.litmus

import komem.litmus.barriers.CinteropSpinBarrier
import komem.litmus.testsuite.ATOM
import kotlin.test.Test

class NativeTest {
    @Test
    fun stubTest() { // TODO: common default runner function for all tests
        val test = ATOM
        val runner = WorkerRunner
        val params = LTRunParams(1_000_000, 100, null, ::CinteropSpinBarrier)
        runner.runTest(params, test).prettyPrint()
    }
}
