import barriers.BarrierProducer

object SyncPeriodSchedules {
    fun quadGrowing(until: Int) = generateSequence(1) { (it * 1.2).toInt() + 1 }.takeWhile { it < until }.toList()
}

fun variateParameters(
    affinitySchedule: List<AffinityMap?>,
    syncPeriodSchedule: List<Int>,
    memShufflerProducerSchedule: List<(MemShufflerProducer?)>,
    barrierSchedule: List<BarrierProducer>
) = sequence {
    for (affinity in affinitySchedule) {
        for (syncPeriod in syncPeriodSchedule) {
            for (memShuffler in memShufflerProducerSchedule) {
                for (barrierProducer in barrierSchedule) {
                    yield(LitmusTestParameters(affinity, syncPeriod, memShuffler, barrierProducer))
                }
            }
        }
    }
}
