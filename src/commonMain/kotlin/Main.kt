import barriers.CinteropBarrier
import barriers.SpinBarrier
import tests.*
import kotlin.time.Duration.Companion.seconds

fun main() {
//    distributionTest()

    val testProducer = ::SBTest
    val runner = WorkerTestRunner

    val param = LitmusTestParameters(
        null, 20, null, ::CinteropBarrier
    )
    val res = runner.runTest(15.seconds, param, testProducer)
    res.prettyPrint()

//    val parameters = variateParameters(
//            getAffinityManager()?.scheduleShort2().orUnrestricted(2), // affinityScheduleUnrestricted(2),
//            generateSequence(3) { it * 3 }.take(5).toList(),
//            listOf(null /* { MemShuffler(50_000) } */),
//            listOf(::CinteropBarrier)
//    ).toList()
//    val singleTestDuration = 1.seconds
//
//    println("ETA: T+ ${(singleTestDuration * parameters.size).toComponents { m, s, _ -> "$m m $s s" }}")
//    val results = parameters.map { runner.runTest(singleTestDuration, it, testProducer) }.flatten().merge()
//    results.prettyPrint()
}

fun distributionTest() {
    val runner = WorkerTestRunner
    val parameters = variateParameters(
        getAffinityManager()?.scheduleShort2().orUnrestricted(2),
        generateSequence(2) { it * 2 }.take(8).toList(),
        listOf(null /* { MemShuffler(50_000) } */),
        listOf(::SpinBarrier)
    ).toList()

    for (testCount in generateSequence(1000) { it * 2 }.take(10)) {
        println("test count: $testCount...")

        val results = List(10) {
            parameters.map { param ->
                runner.runTest(testCount, param, ::SBTest)
            }.flatten().merge()
        }

        val outcomes = results.map { it.map { it.outcome }.toSet() }.reduce(Set<Any?>::union).toList()
        val samples = results.map { r -> outcomes.map { o -> r.firstOrNull { it.outcome == o }?.count ?: 0 } }
        val accepted = chiSquaredTest(samples)
        println(if (accepted) "accepted" else "rejected")
    }
}
