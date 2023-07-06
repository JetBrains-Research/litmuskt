package komem.litmus

import komem.litmus.barriers.BarrierProducer

data class RunningParams(
    val batchSize: Int,
    val syncPeriod: Int,
    val affinityMap: AffinityMap?,
    val memShufflerProducer: (() -> MemShuffler)?,
    val barrierProducer: BarrierProducer,
)

fun variateParams(
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
                            RunningParams(
                                batchSize = batchSize,
                                syncPeriod = syncPeriod,
                                affinityMap = affinityMap,
                                memShufflerProducer = memShufflerProducer,
                                barrierProducer = barrierProducer
                            )
                        )
                    }
                }
            }
        }
    }
}
