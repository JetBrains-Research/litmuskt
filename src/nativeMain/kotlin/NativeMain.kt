import komem.litmus.*
import komem.litmus.barriers.CinteropSpinBarrier
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime

fun main() {

    val mp = litmusTest({
        object : IIOutcome() {
            var x = 0
            var y = 0
//            var r1 = 0
//            var r2 = 0
        }
    }) {
        thread {
            x = 1
            y = 1
        }
        thread {
            r1 = y
            r2 = x
        }
//        outcome {
//            listOf(r1, r2)
//        }
        spec {
            accepted = setOf(
                listOf(1, 1), listOf(0, 0), listOf(0, 1)
            )
            interesting = setOf(
                listOf(1, 0)
            )
        }
    }
    val runner: LTRunner = WorkerRunner
    val test = mp
    val params = LTRunParams(
        batchSize = 1_000_000,
        syncPeriod = 100,
        affinityMap = null,
        barrierProducer = ::CinteropSpinBarrier
    )

    measureTime {
        runner.runTest(10.seconds, params, test).prettyPrint()
    }.let { println("${it.inWholeSeconds} seconds") }

}
