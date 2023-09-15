import komem.litmus.*
import komem.litmus.barriers.CinteropSpinBarrier
import komem.litmus.testsuite.*
import kotlin.time.Duration.Companion.seconds

fun main() {
    // shortest example of running a test
    val runner: LTRunner = WorkerRunner
    val test = SB
    val params = LTRunParams(
        batchSize = 1_000_000,
        syncPeriod = 10,
        affinityMap = null,
        barrierProducer = ::CinteropSpinBarrier
    )
    runner.runTest(30.seconds, params, test).prettyPrint()
}
