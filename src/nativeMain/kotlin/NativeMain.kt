import komem.litmus.*
import komem.litmus.barriers.CinteropSpinBarrier
import komem.litmus.testsuite.IRIWVolatile
import kotlin.time.Duration.Companion.seconds

fun main() {
    val runner: LTRunner = WorkerRunner
    val test = IRIWVolatile
    val params = LTRunParams(
        batchSize = 1_000_000,
        syncPeriod = 10,
        affinityMap = null,
        barrierProducer = ::CinteropSpinBarrier
    )
    runner.runTest(30.seconds, params, test).prettyPrint()
}
