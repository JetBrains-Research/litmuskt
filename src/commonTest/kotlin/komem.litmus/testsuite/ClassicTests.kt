package komem.litmus.testsuite

import komem.litmus.infra.run
import kotlin.test.Test

class ClassicTests {

    @Test
    fun atom() = ATOM.run()

    @Test
    fun sb() = SB.run()

    @Test
    fun sbVolatile() = SBVolatile.run()

    @Test
    fun mp() = MP.run()

    @Test
    fun mpVolatile() = MPVolatile.run()

    @Test
    fun mpDrf() = MP_DRF.run()

    @Test
    fun coRR() = CoRR.run()

    @Test
    fun coRRCse() = CoRR_CSE.run()

    @Test
    fun iriw() = IRIW.run()

    @Test
    fun iriwVolatile() = IRIWVolatile.run()

    @Test
    fun upub() = UPUB.run()

    @Test
    fun upubCtor() = UPUBCtor.run()

    @Test
    fun lbDepsOOTA() = LB_DEPS_OOTA.run()

    @Test
    fun lb() = LB.run()

    @Test
    fun lbVolatile() = LBVolatile.run()
}
