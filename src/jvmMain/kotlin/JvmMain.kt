import komem.litmus.*
import komem.litmus.barriers.JvmSpinBarrier
import komem.litmus.testsuite.IRIWVolatile
import kotlin.time.Duration.Companion.seconds

fun main() {
    // example of iterating through multiple parameters and aggregating the results
    val runner: LTRunner = JvmThreadRunner
    val test = IRIWVolatile

    val paramsList = variateRunParams(
        batchSizeSchedule = listOf(100_000, 1_000_000),
        affinityMapSchedule = listOf(null), // not available on JVM
        syncPeriodSchedule = generateSequence(100) { (it * 1.5).toInt() }.takeWhile { it < 3000 }.toList(),
        barrierSchedule = listOf(::JvmSpinBarrier)
    ).toList()
    val singleTestDuration = 10.seconds

    println("ETA: T+${singleTestDuration * paramsList.size}")

    paramsList.map { params ->
        runner.runTest(singleTestDuration, params, test)
    }.mergeResults().prettyPrint()
}
