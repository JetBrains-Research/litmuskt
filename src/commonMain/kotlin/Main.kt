import barriers.CinteropBarrier
import barriers.SpinBarrier
import tests.*
import kotlin.time.Duration.Companion.seconds

fun main() {
    val runner = WorkerTestRunner

//    variateParameters(
//        getAffinityManager()?.scheduleShort2().orUnrestricted(2), // affinityScheduleUnrestricted(2),
//        generateSequence(3) { it * 3 }.take(5).toList(),
//        listOf(null /* { MemShuffler(50_000) } */),
//        listOf(::CinteropBarrier)
//    ).toList()
    val parameters = variateParameters(
        listOf(null),
        generateSequence(3) { it * 3 }.take(5).toList(),
        listOf(null),
        listOf(::CinteropBarrier)
    ).toList()
    // TODO: on x86 manually run tests with some affinity
    // TODO: on macos also run without parallel (just in case they interfere too much)
    // TODO: also run release AND debug
    val singleTestDuration = 10.seconds

    val tests = listOf(
        ::AtomTest,
        ::SBTest,
        ::SBVolatileTest,
        ::MPTest,
        ::MPVolatileTest,
        ::MP_DRF_Test,
        ::CoRRTest,
        ::CoRR_CSE_Test,
        ::IRIWTest,
        ::IRIWVolatileTest,
        ::UPUBTest,
        ::UPUBCtorTest,
        ::LB_DEPS_Test,
        ::LBTest,
        ::LBFakeDEPSTest,
        ::LBVolatileTest
    )

    val estimateTotalDuration = singleTestDuration * parameters.size * tests.size
    println("Total runs: ${parameters.size * tests.size}")
    println("Total duration >= ${estimateTotalDuration.toComponents { m, s, _ -> "${m}m ${s}s" }}")
    for (testProducer in tests) {
        val sampleTest = testProducer()
        print(sampleTest.name + ",")
        val results = parameters.map {
            runner.runTest(singleTestDuration, 100_000, it, testProducer)
        }.flatten().mergeOutcomes()
        val interestingCount = results.countOfType(OutcomeType.INTERESTING)
        val forbiddenCount = results.countOfType(OutcomeType.FORBIDDEN)
        val totalCount = results.sumOf { it.count }
        println("$totalCount,$interestingCount,$forbiddenCount")
    }

}
