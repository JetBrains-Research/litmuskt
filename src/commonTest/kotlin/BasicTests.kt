import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class BasicTests {

    lateinit var runner: LitmusTestRunner
    lateinit var defaultParameters: LitmusTestParameters

    @BeforeTest
    fun setup() {
        runner = WorkerTestRunner()
        defaultParameters = LitmusTestParameters(
                AffinitySchedules.separatePhysicalCores(2),
                50,
                null
        )
    }

    @Test
    fun testSB() {
        val result = runner.runTest(1.seconds, defaultParameters, ::JCS07_SB)
        assertEquals(4, result.size)
    }

    @Test
    fun testLB() {
        val result = runner.runTest(1.seconds, defaultParameters, ::LB)
        // TODO
//        assertEquals(4, result.size)
    }

    @Test
    fun testMP() {
        val result = runner.runTest(1.seconds, defaultParameters, ::JCS06_MP)
        // TODO
//        assertEquals(4, result.size)
    }

    @Test
    fun testConsensus() {
        val result = runner.runTest(1.seconds, defaultParameters, ::JCS05)
        assertTrue { result.size < 4 }
    }

}