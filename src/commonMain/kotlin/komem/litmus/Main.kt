package komem.litmus

import komem.litmus.barriers.CinteropSpinBarrier
import komem.litmus.tests.UPUBTest
import kotlin.time.Duration.Companion.seconds

fun main() {
    val runner = WorkerRunner

    val parameters = variateParameters(
        affinitySchedule = getAffinityManager()?.presetShort() ?: listOf(null),
        syncPeriodSchedule = generateSequence(3) { it * 4 }.take(3).toList(),
        memShufflerProducerSchedule = listOf(null),
        barrierSchedule = listOf(::CinteropSpinBarrier)
    ).toList()

    val singleTestDuration = 3.seconds
    val testProducer = ::UPUBTest

    val totalDuration = singleTestDuration * parameters.size
    println("ETA: T+ ${totalDuration.toComponents { m, s, _ -> "${m}m ${s}s" }}")
    val results = parameters.map {
        runner.runTestParallel(singleTestDuration, 100_000, it, testProducer)
    }.flatten().mergeOutcomes()
    results.prettyPrint()
}
