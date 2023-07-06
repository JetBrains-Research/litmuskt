package komem.litmus.statistics

import komem.litmus.*
import komem.litmus.runners.WorkerTestRunner
import komem.litmus.barriers.*
import komem.litmus.tests.*

fun distributionTest() {
    val runner = WorkerTestRunner
    val parameters = variateParams(
        generateSequence(1000) { it * 2 }.take(10).toList(),
        getAffinityManager()?.presetShort() ?: listOf(null),
        generateSequence(2) { it * 2 }.take(8).toList(),
        listOf(null /* { MemShuffler(50_000) } */),
        listOf(::SpinBarrier)
    ).toList()

    val testProducer = ::SBTest
    val results = List(10) {
        parameters.map { param ->
            runner.runTest(param, testProducer).groupToInfo(testProducer().getOutcomeSetup())
        }.flatten().mergeOutcomes()
    }

    val outcomes = results.map { it.map { it.outcome }.toSet() }.reduce(Set<Any?>::union).toList()
    val samples = results.map { r -> outcomes.map { o -> r.firstOrNull { it.outcome == o }?.count ?: 0 } }
    // TODO double freq (not for chi-sq)? long in test?
    val accepted = chiSquaredTest(samples.map { it.map { it.toInt() } })
    println(if (accepted) "accepted" else "rejected")
}