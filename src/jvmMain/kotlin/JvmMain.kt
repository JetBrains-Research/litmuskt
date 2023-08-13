import komem.litmus.*
import komem.litmus.barriers.JvmCyclicBarrier
import komem.litmus.testsuite.IRIW

fun main() {
    val runner: LTRunner = JvmThreadRunner
    val test = IRIW

    val syncEverySchedule = generateSequence(100) { (it * 1.4).toInt() }.takeWhile { it < 100000 }.toList()
    println("len = ${syncEverySchedule.size}")

    syncEverySchedule.map { sync ->
        val params = LTRunParams(
            batchSize = 10_000_000,
            syncPeriod = sync,
            affinityMap = null,
            barrierProducer = ::JvmCyclicBarrier
        )
        runner.runTest(params, test)
    }.mergeResults().prettyPrint()
}
