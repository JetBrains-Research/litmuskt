import komem.litmus.RunParams
import komem.litmus.barriers.CinteropSpinBarrier
import komem.litmus.calcStats
import komem.litmus.litmusTest
import komem.litmus.prettyPrint
import komem.litmus.runners.LitmusTestRunner
import komem.litmus.runners.WorkerTestRunner
import kotlin.time.measureTime

fun main() {

    class Data {
        var x = 0
        var y = 0
        var a = 0
        var b = 0
    }

    val sb = litmusTest(::Data) {
        thread {
            x = 1
            a = y
        }
        thread {
            y = 1
            b = x
        }
        outcome {
            listOf(a, b)
        }
        spec {
            accepted = setOf(
                listOf(1, 1), listOf(1, 0), listOf(0, 1)
            )
            interesting = setOf(
                listOf(0, 0)
            )
        }
    }
    val runner: LitmusTestRunner = WorkerTestRunner
    val test = sb
    val params = RunParams(
        batchSize = 10_000_000,
        syncPeriod = 10,
        affinityMap = null,
        barrierProducer = ::CinteropSpinBarrier
    )

    measureTime {
        val outcomes = runner.runTest(params, test)
        outcomes.calcStats(test.outcomeSpec).prettyPrint()
    }.let { println("${it.inWholeSeconds} seconds") }

}
