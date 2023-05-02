import kotlin.test.Test
import kotlin.test.assertTrue

// TODO
class ChiSquareTest {

    @Test
    fun someTest() {
        val sample1 = listOf(10200, 20000, 30000)
        val sample2 = listOf(5000, 10001, 15000)
        val sample3 = listOf(1000, 2000, 3050)
        val sample4 = listOf(3000, 6000, 8900)
        assertTrue { chiSquaredTest(listOf(sample1, sample2, sample3, sample4)) }
    }

}