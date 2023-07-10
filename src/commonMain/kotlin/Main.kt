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

    val mp = litmusTest<Data> {
        init { Data() }
        thread {
            x = 321321321
            y = 123123123
        }
        thread {
            o1 = y
            o2 = x
        }
        // interesting: [1, 0]
    }

    val corr = litmusTest<Data> {
        thread {
            x = 1
        }
        thread {
            o1 = x
            o2 = x
        }
        // interesting: [1, 0]
    }

    class Holder(var x: Int)
    class CorrCseData(val h1: Holder, val h2: Holder)

    val corrCse = litmusTest<CorrCseData> {
        init {
            val h = Holder(0)
            CorrCseData(h, h)
        }
        thread {
            h1.x = 1
        }
        thread {
            val a = h1.x
            val b = h2.x
            val c = h1.x
            o1 = listOf(a, b, c)
        }
        // interesting: [1, 0, 0], [1, 1, 0]
    }

    data class IriwData(var x: Int, var y: Int, var a: Int, var b: Int, var c: Int, var d: Int)

    val iriw = litmusTest<IriwData> {
        init { IriwData(0, 0, 0, 0, 0, 0) }
        thread {
            x = 1
        }
        thread {
            a = x
            b = y
        }
        thread {
            c = y
            d = x
        }
        thread {
            y = 1
        }
        after {
            o1 = listOf(a, b, c, d)
        }
    }

    println("start")
    measureTime {
        mp.run(10_000_000)
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
