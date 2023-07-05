import barriers.CinteropBarrier
import tests.*
import kotlin.time.Duration.Companion.seconds

fun main() {
    val runner = WorkerTestRunner

    val parameters = variateParameters(
        affinitySchedule = getAffinityManager()?.presetShort() ?: listOf(null),
        syncPeriodSchedule = generateSequence(3) { it * 2 }.take(5).toList(),
        memShufflerProducerSchedule = listOf(null),
        barrierSchedule = listOf(::CinteropBarrier)
    ).toList()

    val singleTestDuration = 5.seconds
    val testProducer = ::UPUBCtorLeakingTest

    val totalDuration = singleTestDuration * parameters.size
    println("ETA: T+ ${totalDuration.toComponents { m, s, _ -> "${m}m ${s}s" }}")
    val results = parameters.map {
        runner.runTestParallel(singleTestDuration, 100_000, it, testProducer)
    }.flatten().mergeOutcomes()
    results.prettyPrint()
}
