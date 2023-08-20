import komem.litmus.*
import komem.litmus.barriers.JvmSpinBarrier
import komem.litmus.testsuite.IRIWVolatile
import kotlin.time.Duration.Companion.seconds

fun main() {
    val runner: LTRunner = JvmThreadRunner
    val test = IRIWVolatile

    val syncEverySchedule = generateSequence(10) { (it * 1.7).toInt() }.takeWhile { it < 1000 }.toList()
    println("len = ${syncEverySchedule.size}")

    syncEverySchedule.map { sync ->
        val params = LTRunParams(
            batchSize = 1_000_000,
            syncPeriod = sync,
            affinityMap = null,
            barrierProducer = ::JvmSpinBarrier
        )
        println("one less")
        runner.runTest(10.seconds, params, test)
    }.mergeResults().prettyPrint()
}
