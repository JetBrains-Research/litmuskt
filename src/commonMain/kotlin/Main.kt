import komem.litmus.RunParams
import komem.litmus.barriers.CinteropSpinBarrier
import komem.litmus.groupIntoInfo
import komem.litmus.litmusTest
import komem.litmus.prettyPrint
import komem.litmus.runners.LitmusTestRunner
import komem.litmus.runners.WorkerTestRunner
import kotlin.time.measureTime

fun main() {

    class Data {
        var x = 0
        var y = 0
    }

    val sb = litmusTest(::Data)
        .thread {
            x = 1
            y
        }
        .thread {
            y = 1
            x
        }
        .outcomeSetup {
            accepted = setOf(
                listOf(1, 1), listOf(1, 0), listOf(0, 1)
            )
            interesting = setOf(
                listOf(0, 0)
            )
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
        outcomes.groupIntoInfo(test.outcomeSetup).prettyPrint()
    }.let { println("${it.inWholeSeconds} seconds") }

}
