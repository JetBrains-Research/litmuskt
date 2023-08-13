package komem.litmus.testsuite

import komem.litmus.infra.run
import kotlin.test.Test

class UPUBExtraTests {
    @Test
    fun upubVolatile() = UPUBVolatile.run()

    @Test
    fun upubArray() = UPUBArray.run()

    @Test
    fun upubRef() = UPUBRef.run()

    @Test
    fun upubCtorLeaking() = UBUBCtorLeaking.run()
}
