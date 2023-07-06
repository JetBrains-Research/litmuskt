package komem.litmus.runners

import komem.litmus.LitmusOutcome
import komem.litmus.LitmusTest
import komem.litmus.RunningParams

interface LitmusTestRunner {
    fun runTest(params: RunningParams, testProducer: () -> LitmusTest): List<LitmusOutcome>
}
