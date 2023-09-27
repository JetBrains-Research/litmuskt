import komem.litmus.*
import komem.litmus.barriers.CinteropSpinBarrier
import komem.litmus.generated.LitmusTestRegistry

fun main() {
    // shortest example of running a test

    val reg = LitmusTestRegistry

    val runner: LitmusRunner = WorkerRunner
    val params = LitmusRunParams(
        batchSize = 1_000_000,
        syncPeriod = 10,
        affinityMap = null,
        barrierProducer = ::CinteropSpinBarrier
    )
    val test = reg.tests[1] as LitmusTest<Any>
    val hahaTest = test.copy(
        stateProducer = {
            object : LitmusIIOutcome() {
                var x = 5
                var y = 5
            }
        }
    )

    runner.runTest(params, hahaTest).prettyPrint()
}
