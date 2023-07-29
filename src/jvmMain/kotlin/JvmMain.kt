import komem.litmus.*
import komem.litmus.barriers.JvmBarrier

fun main() {

    val iriw = litmusTest({
        object {
            var x: Int = 0
            var y: Int = 0
            var a: Int = 0
            var b: Int = 0
            var c: Int = 0
            var d: Int = 0
        }
    }) {
        thread {
            x = 1
        }
        thread {
            y = 1
        }
        thread {
            a = x
            b = y
        }
        thread {
            c = y
            d = x
        }
        outcome {
            listOf(a, b, c, d)
        }
        spec {
            interesting = setOf(
                listOf(1, 0, 1, 0),
                listOf(0, 1, 0, 1),
            )
            default = LTOutcomeType.ACCEPTED
        }
    }
    val runner: LTRunner = JvmThreadRunner
    val test = iriw

    val syncEverySchedule = generateSequence(50) { (it * 1.2).toInt() }.takeWhile { it < 100000 }.toList()
    println("len = ${syncEverySchedule.size}")

    syncEverySchedule.map { sync ->
        val params = LTRunParams(
            batchSize = 10_000_000,
            syncPeriod = sync,
            affinityMap = null,
            barrierProducer = ::JvmBarrier
        )
        runner.runTest(params, test).calcStats(test.outcomeSpec)
    }.mergeResults().prettyPrint()
}

private data class IntAlphabet(
    var a: Int = 0,
    var b: Int = 0,
    var c: Int = 0,
    var d: Int = 0,
    var e: Int = 0,
    var f: Int = 0,
    var g: Int = 0,
    var h: Int = 0,
    var i: Int = 0,
    var j: Int = 0,
    var k: Int = 0,
    var l: Int = 0,
    var m: Int = 0,
    var n: Int = 0,
    var o: Int = 0,
    var o1: Int = 0,
    var o2: Int = 0,
    var o3: Int = 0,
    var o4: Int = 0,
    var o5: Int = 0,
    var o6: Int = 0,
    var o7: Int = 0,
    var o8: Int = 0,
    var o9: Int = 0,
    var p: Int = 0,
    var q: Int = 0,
    var r: Int = 0,
    var s: Int = 0,
    var t: Int = 0,
    var u: Int = 0,
    var v: Int = 0,
    var w: Int = 0,
    var x: Int = 0,
    var y: Int = 0,
    var z: Int = 0,
)
