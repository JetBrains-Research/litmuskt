package komem.litmus.testsuite

import komem.litmus.infra.run
import kotlin.test.Test

class CustomTests {
    @Test
    fun mpNoDrf() = MPNoDRF.run()
}
