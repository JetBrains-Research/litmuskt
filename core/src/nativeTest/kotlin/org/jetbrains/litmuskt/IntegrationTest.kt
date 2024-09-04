package org.jetbrains.litmuskt

import org.jetbrains.litmuskt.autooutcomes.LitmusIIOutcome
import org.jetbrains.litmuskt.autooutcomes.accept
import org.jetbrains.litmuskt.barriers.CinteropSpinBarrier
import kotlin.test.Test
import kotlin.test.assertNotEquals

class IntegrationTest {

    private val sampleLitmusTest = litmusTest({
        object : LitmusIIOutcome() {
            var x = 0
        }
    }) {
        // TODO: reset
        thread {
            x = 1
        }
        thread {
            r1 = x
            x = 2
            r2 = x
        }
        spec {
            accept(0, 2)
            accept(1, 2)
            accept(0, 1) // r1 = 0; x = 2; x = 1 (t1); r2 = 1
        }
    }

    @Test
    fun testBasic() {
        val runner = PthreadRunner()
        val result = runner.runTests(
            tests = listOf(sampleLitmusTest),
            params = LitmusRunParams(
                batchSize = 1_000_000,
                syncPeriod = 10,
                affinityMap = null,
                barrierProducer = ::CinteropSpinBarrier
            )
        ).first()
        assertNotEquals(LitmusOutcomeType.FORBIDDEN, result.overallStatus())
    }

}
