package komem.litmus

import komem.litmus.barriers.JvmSpinBarrier

class CliJvm : CliCommon() {
    override val runner = JvmThreadRunner
    override val barrierProducer = ::JvmSpinBarrier
    override val affinityMapSchedule = listOf(null)
}
