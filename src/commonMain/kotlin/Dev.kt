import barriers.*
import tests.*

fun distributionTest() {
    val runner = WorkerTestRunner
    val parameters = variateParameters(
        getAffinityManager()?.presetShort() ?: listOf(null),
        generateSequence(2) { it * 2 }.take(8).toList(),
        listOf(null /* { MemShuffler(50_000) } */),
        listOf(::SpinBarrier)
    ).toList()

    for (testCount in generateSequence(1000) { it * 2 }.take(10)) {
        println("test count: $testCount...")

        val results = List(10) {
            parameters.map { param ->
                runner.runTest(testCount, param, ::SBTest)
            }.flatten().mergeOutcomes()
        }

        val outcomes = results.map { it.map { it.outcome }.toSet() }.reduce(Set<Any?>::union).toList()
        val samples = results.map { r -> outcomes.map { o -> r.firstOrNull { it.outcome == o }?.count ?: 0 } }
        // TODO double freq (not for chi-sq)? long in test?
        val accepted = chiSquaredTest(samples.map { it.map { it.toInt() } })
        println(if (accepted) "accepted" else "rejected")
    }
}