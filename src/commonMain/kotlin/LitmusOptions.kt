// TODO: put code into package!!!

import barriers.BarrierProducer

data class LitmusOptions(
    val batchSize: Int,
    val syncPeriod: Int,
    val affinityMap: AffinityMap?,
    val memShufflerProducer: (() -> MemShuffler)?,
    val barrierProducer: BarrierProducer,
)