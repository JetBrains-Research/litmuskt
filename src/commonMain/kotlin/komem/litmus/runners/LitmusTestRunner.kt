package komem.litmus.runners

import komem.litmus.*
import kotlin.time.Duration
import kotlin.time.TimeSource

interface LitmusTestRunner {
    fun runTest(params: RunningParams, testProducer: () -> LitmusTest): List<LitmusOutcome>
}
