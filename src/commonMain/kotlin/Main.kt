import komem.litmus.barriers.CinteropSpinBarrier
import komem.litmus.mergeOutcomes
import komem.litmus.prettyPrint
import komem.litmus.runners.WorkerTestRunner
import komem.litmus.runners.runTestParallel
import komem.litmus.tests.SBTest
import komem.litmus.variateRunParams
import kotlin.time.Duration.Companion.seconds

fun main() {
    val runner = WorkerTestRunner

    val paramsList = variateRunParams(
        batchSizeSchedule = listOf(100_000),
        affinityMapSchedule = listOf(null), // getAffinityManager()?.presetShort() ?: listOf(null),
        syncPeriodSchedule = generateSequence(3) { it * 4 }.take(3).toList(),
        memShufflerProducerSchedule = listOf(null),
        barrierSchedule = listOf(::CinteropSpinBarrier)
    ).toList()

    val singleTestDuration = 3.seconds
    val testProducer = ::SBTest

    val totalDuration = singleTestDuration * paramsList.size
    println("ETA: T+ ${totalDuration.toComponents { m, s, _ -> "${m}m ${s}s" }}")
    val results = paramsList.map {
        runner.runTestParallel(singleTestDuration, it, testProducer)
    }.flatten().mergeOutcomes()
    results.prettyPrint()
}
