import komem.litmus.*
import komem.litmus.barriers.CinteropSpinBarrier
import komem.litmus.testsuite.VolatileAsFence
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

fun main() {
    val runner: LTRunner = WorkerRunner
    val test = VolatileAsFence

    val paramsList = variateRunParams(
        batchSizeSchedule = listOf(1_000_000),
        affinityMapSchedule = listOf(null),
        syncPeriodSchedule = generateSequence(7) { (it * 1.5).toInt() }.takeWhile { it < 1000 }.toList(),
        barrierSchedule = listOf(::CinteropSpinBarrier)
    ).toList()
    val timeLimit = 10.seconds
    println("params size: ${paramsList.size}")
    println("ETA: T+${(timeLimit * paramsList.size).toString(unit = DurationUnit.MINUTES)}")
    paramsList.map { params ->
        runner.runTestParallel(timeLimit, params, test)
    }.mergeResults().prettyPrint()
}
