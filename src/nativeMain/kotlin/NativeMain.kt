import komem.litmus.*
import komem.litmus.barriers.CinteropSpinBarrier
import komem.litmus.testsuite.VolatileAsFence
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

fun main() {
    val runner: LTRunner = WorkerRunner
    val test = VolatileAsFence

    val paramsList = variateRunParams(
        batchSizeSchedule = listOf(100_000, 1_000_000),
        affinityMapSchedule = listOf(null),
        syncPeriodSchedule = generateSequence(5) { (it * 1.3).toInt() }.takeWhile { it < 1000 }.toList(),
        barrierSchedule = listOf(::CinteropSpinBarrier)
    ).toList()
    val timeLimit = 30.seconds
    println("params size: ${paramsList.size}")
    println("ETA: T+${(timeLimit * paramsList.size).toString(unit = DurationUnit.MINUTES)}")
    paramsList.map { params ->
        runner.runTestParallel(30.seconds, params, test)
    }.mergeResults().prettyPrint()
}
