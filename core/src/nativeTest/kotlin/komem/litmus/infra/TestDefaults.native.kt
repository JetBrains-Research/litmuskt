package komem.litmus.infra

import komem.litmus.LitmusRunParams
import komem.litmus.LitmusRunner
import komem.litmus.WorkerRunner
import komem.litmus.barriers.CinteropSpinBarrier

actual val defaultRunner: LitmusRunner = WorkerRunner
actual val defaultParams: LitmusRunParams = LitmusRunParams(
    batchSize = 1_000_000,
    syncPeriod = 10,
    affinityMap = null,
    barrierProducer = ::CinteropSpinBarrier
)
