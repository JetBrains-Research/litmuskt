import komem.litmus.*
import komem.litmus.barriers.CinteropSpinBarrier
import komem.litmus.testsuite.SB
import kotlin.time.Duration.Companion.seconds

fun main() {
    // shortest example of running a test
    val runner: LitmusRunner = WorkerRunner
    val test = SB
    val params = LitmusRunParams(
        batchSize = 1_000_000,
        syncPeriod = 10,
        affinityMap = null,
        barrierProducer = ::CinteropSpinBarrier
    )
    runner.runTest(30.seconds, params, test).prettyPrint()
}
