package komem.litmus

import komem.litmus.barriers.BarrierProducer

data class RunParams(
    val batchSize: Int,
    val syncPeriod: Int,
    val affinityMap: AffinityMap?,
    val memoryShufflerProducer: (() -> MemoryShuffler)?,
    val barrierProducer: BarrierProducer,
)

fun variateRunParams(
    batchSizeSchedule: List<Int>,
    affinityMapSchedule: List<AffinityMap?>,
    syncPeriodSchedule: List<Int>,
    memShufflerProducerSchedule: List<(MemShufflerProducer?)>,
    barrierSchedule: List<BarrierProducer>
) = sequence {
    for (batchSize in batchSizeSchedule) {
        for (affinityMap in affinityMapSchedule) {
            for (syncPeriod in syncPeriodSchedule) {
                for (memShufflerProducer in memShufflerProducerSchedule) {
                    for (barrierProducer in barrierSchedule) {
                        yield(
                            RunParams(
                                batchSize = batchSize,
                                syncPeriod = syncPeriod,
                                affinityMap = affinityMap,
                                memoryShufflerProducer = memShufflerProducer,
                                barrierProducer = barrierProducer
                            )
                        )
                    }
                }
            }
        }
    }
}
