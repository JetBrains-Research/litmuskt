package komem.litmus.infra

import komem.litmus.JvmThreadRunner
import komem.litmus.LTRunParams
import komem.litmus.LTRunner
import komem.litmus.barriers.JvmSpinBarrier

actual val defaultRunner: LTRunner = JvmThreadRunner
actual val defaultParams: LTRunParams = LTRunParams(
    batchSize = 1_000_000,
    syncPeriod = 1000,
    affinityMap = null,
    barrierProducer = ::JvmSpinBarrier
)
