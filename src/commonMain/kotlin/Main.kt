import barriers.CinteropBarrier
import tests.*
import kotlin.time.Duration.Companion.seconds

fun main() {
    val runner = WorkerTestRunner

    val parameters = variateParameters(
        listOf(null),
        generateSequence(3) { it * 3 }.take(5).toList(),
        listOf(null),
        listOf(::CinteropBarrier)
    ).toList()
    val singleTestDuration = 3.seconds

    val tests = listOf(
        ::MP_NoDRF_Test
//        ::AtomTest,
//        ::SBTest,
//        ::SBVolatileTest,
//        ::MutexTest,
//        ::SBMutexTest,
//        ::MPTest,
//        ::MPVolatileTest,
//        ::MPMutexTest,
//        ::MP_DRF_Test,
//        ::CoRRTest,
//        ::CoRR_CSE_Test,
//        ::IRIWTest,
//        ::IRIWVolatileTest,
//        ::UPUBTest,
//        ::UPUBCtorTest,
//        ::LB_DEPS_Test,
//        ::LBTest,
//        ::LBFakeDEPSTest,
//        ::LBVolatileTest
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
        if(forbiddenCount != 0L)
            results.prettyPrint()
    }

}
