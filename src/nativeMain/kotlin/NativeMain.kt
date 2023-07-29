import komem.litmus.*
import komem.litmus.barriers.CinteropSpinBarrier
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
    val runner: LTRunner = WorkerRunner
    val test = sb
    val params = LTRunParams(
        batchSize = 10_000_000,
        syncPeriod = 10000,
        affinityMap = null,
        barrierProducer = ::CinteropSpinBarrier
    )

    measureTime {
        val outcomes = runner.runTest(params, test)
        outcomes.calcStats(test.outcomeSpec).prettyPrint()
    }.let { println("${it.inWholeSeconds} seconds") }

}
