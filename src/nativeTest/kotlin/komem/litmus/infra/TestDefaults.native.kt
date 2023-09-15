package komem.litmus.infra

import komem.litmus.LTRunParams
import komem.litmus.LTRunner
import komem.litmus.WorkerRunner
import komem.litmus.barriers.CinteropSpinBarrier

actual val defaultRunner: LTRunner = WorkerRunner
actual val defaultParams: LTRunParams = LTRunParams(
    batchSize = 1_000_000,
    syncPeriod = 10,
    affinityMap = null,
    barrierProducer = ::CinteropSpinBarrier
)
