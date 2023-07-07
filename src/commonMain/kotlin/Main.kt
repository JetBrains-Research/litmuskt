import komem.litmus.RunParams
import komem.litmus.barriers.SpinBarrier
import komem.litmus.getOutcomeSetup
import komem.litmus.groupIntoInfo
import komem.litmus.prettyPrint
import komem.litmus.runners.WorkerTestRunner
import komem.litmus.tests.SBTest
import komem.litmus.variateRunParams
import kotlin.time.Duration.Companion.seconds
import komem.litmus.dslTest.*
import kotlin.time.measureTime

fun main() {

    class Data {
        var x = 0
        var y = 0
    }

    val sb = litmusTest<Data> {
        init {
            Data()
        }
        thread {
            x = 1
            o1 = y
        }
        thread {
            y = 1
            o2 = x
        }
    }
    measureTime {
        sb.run(10_000)
    }.let { println("${it.inWholeSeconds} seconds") }

//    val runner = WorkerTestRunner
//    measureTime {
//        runner.runTest(
//            RunningParams(
//                10_000_000,
//                10,
//                null,
//                null,
//                ::SpinBarrier
//            ),
//            ::SBTest
//        ).groupToInfo(SBTest().getOutcomeSetup()).prettyPrint()
//    }.let { println("and ${it.inWholeSeconds} seconds") }

//    val paramsList = variateParams(
//        batchSizeSchedule = listOf(100_000),
//        affinityMapSchedule = listOf(null), // getAffinityManager()?.presetShort() ?: listOf(null),
//        syncPeriodSchedule = generateSequence(3) { it * 4 }.take(3).toList(),
//        memShufflerProducerSchedule = listOf(null),
//        barrierSchedule = listOf(::CinteropSpinBarrier)
//    ).toList()

//    val singleTestDuration = 3.seconds
//    val testProducer = ::SBTest

//    val totalDuration = singleTestDuration * paramsList.size
//    println("ETA: T+ ${totalDuration.toComponents { m, s, _ -> "${m}m ${s}s" }}")
//    val results = paramsList.map {
//        runner.runTestParallel(singleTestDuration, it, testProducer)
//    }.flatten().mergeOutcomes()
//    results.prettyPrint()
}
