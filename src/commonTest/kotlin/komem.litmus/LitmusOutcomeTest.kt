package komem.litmus

import kotlin.test.Test
import kotlin.test.assertEquals

class LitmusOutcomeTest {
    @Test
    fun mergeStatsTest() {
        val stats = listOf(
            listOf(
                LTOutcomeStats(1, 10, LTOutcomeType.ACCEPTED),
                LTOutcomeStats(0, 10, LTOutcomeType.ACCEPTED),
            ),
            listOf(
                LTOutcomeStats(1, 20, LTOutcomeType.ACCEPTED),
                LTOutcomeStats(0, 10, LTOutcomeType.ACCEPTED),
            ),
            listOf(
                LTOutcomeStats(0, 10, LTOutcomeType.ACCEPTED),
                LTOutcomeStats(2, 1, LTOutcomeType.INTERESTING),
            )
        )
        assertEquals(
            setOf(
                LTOutcomeStats(1, 30, LTOutcomeType.ACCEPTED),
                LTOutcomeStats(0, 30, LTOutcomeType.ACCEPTED),
                LTOutcomeStats(2, 1, LTOutcomeType.INTERESTING),
            ),
            stats.mergeStats().toSet()
        )
    }
}



