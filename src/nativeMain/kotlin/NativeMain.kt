import komem.litmus.*
import komem.litmus.barriers.CinteropSpinBarrier
import komem.litmus.testsuite.MP
import kotlin.time.Duration.Companion.seconds

fun main() {
    val runner: LTRunner = WorkerRunner
    val test = MP
    val params = LTRunParams(
        batchSize = 1_000_000,
        syncPeriod = 100,
        affinityMap = null,
        barrierProducer = ::CinteropSpinBarrier
    )
    runner.runTest(10.seconds, params, test).prettyPrint()
}
