package komem.litmus.infra

import komem.litmus.JvmThreadRunner
import komem.litmus.LitmusRunParams
import komem.litmus.LitmusRunner
import komem.litmus.barriers.JvmSpinBarrier

actual val defaultRunner: LitmusRunner = JvmThreadRunner
actual val defaultParams: LitmusRunParams = LitmusRunParams(
    batchSize = 1_000_000,
    syncPeriod = 1000,
    affinityMap = null,
    barrierProducer = ::JvmSpinBarrier
)
